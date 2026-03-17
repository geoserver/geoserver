package org.geoserver.web;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geotools.api.filter.Filter;

public class SearchInputPanel extends Panel {

    // --- 1. Class-Level Field Declarations ---
    private TextField<String> searchInput;
    private WebMarkupContainer resultsContainer;
    private ListView<SearchResult> initialResults;
    private AbstractDefaultAjaxBehavior loadMoreBehavior;

    private String currentQuery = "";
    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;
    private final boolean autocompleteEnabled;

    private final Catalog catalog = GeoServerApplication.get().getCatalog();

    // --- 2. Resource References ---
    private static final CssResourceReference CSS =
            new CssResourceReference(SearchInputPanel.class, "SearchInputPanel.css");
    private static final JavaScriptResourceReference JS =
            new JavaScriptResourceReference(SearchInputPanel.class, "SearchInputPanel.js");

    public SearchInputPanel(String id) {
        this(id, true);
    }

    public SearchInputPanel(String id, boolean autocompleteEnabled) {
        super(id);
        this.autocompleteEnabled = autocompleteEnabled;

        // --- 3. Initialize Search Input ---
        searchInput = new TextField<>("searchInput", Model.of(""));
        searchInput.setOutputMarkupId(true);
        searchInput.add(new org.apache.wicket.AttributeModifier("placeholder", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                String selectedWorkspace =
                        getPage().getPageParameters().get("workspace").toOptionalString();
                if (Strings.isEmpty(selectedWorkspace)) {
                    return "Search...";
                }
                return "Search in " + selectedWorkspace + "...";
            }
        }));
        add(searchInput);

        // --- 4. Initialize Results Container ---
        resultsContainer = new WebMarkupContainer("resultsContainer");
        resultsContainer.setOutputMarkupId(true);
        add(resultsContainer);

        // --- 5. Initial Results ListView ---
        initialResults =
                new ListView<SearchResult>("initialResults", new LoadableDetachableModel<List<SearchResult>>() {
                    @Override
                    protected List<SearchResult> load() {
                        if (Strings.isEmpty(currentQuery)) return new ArrayList<>();
                        return fetchResults(currentQuery, 0, PAGE_SIZE);
                    }
                }) {
                    @Override
                    protected void populateItem(ListItem<SearchResult> item) {
                        SearchResult result = item.getModelObject();

                        String iconPath;
                        if ("workspace".equals(result.type)) {
                            iconPath = "img/icons/silk/folder.png";
                        } else if ("layer".equals(result.type)) {
                            iconPath = "img/icons/silk/picture_empty.png";
                        } else if ("layerGroup".equals(result.type)) {
                            iconPath = "img/icons/silk/layers.png";
                        } else {
                            iconPath = "img/icons/silk/help.png";
                        }
                        item.add(
                                new Image("itemIcon", new PackageResourceReference(GeoServerBasePage.class, iconPath)));

                        Label itemLabel = new Label("itemLabel", highlightLabel(result.label, currentQuery));
                        itemLabel.setEscapeModelStrings(false);
                        item.add(itemLabel);
                        item.add(new org.apache.wicket.AttributeModifier("data-type", result.type));
                        if (result.workspace != null) {
                            item.add(new org.apache.wicket.AttributeModifier("data-workspace", result.workspace));
                        }
                        if (result.layer != null) {
                            item.add(new org.apache.wicket.AttributeModifier("data-layer", result.layer));
                        }
                    }
                };
        resultsContainer.add(initialResults);

        // --- 6. Input Behavior (Triggers search on typing) ---
        searchInput.add(new AjaxFormComponentUpdatingBehavior("input") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                currentQuery = searchInput.getModelObject();
                currentPage = 0; // Reset page on new search

                // Re-render the container to show Page 0
                target.add(resultsContainer);

                // Re-initialize the JS behavior for the new DOM (keyboard, scroll, click handlers)
                String reinitScript = String.format(
                        "initSearchInputPanel('%s','%s','%s',%s);",
                        resultsContainer.getMarkupId(),
                        searchInput.getMarkupId(),
                        loadMoreBehavior.getCallbackUrl(),
                        autocompleteEnabled);

                // Re-initialize the JS state for the new dropdown list (visibility + ARIA)
                String stateScript = "var $ul = $('#" + resultsContainer.getMarkupId() + "');"
                        + "$('#" + searchInput.getMarkupId()
                        + "').attr('aria-expanded', '"
                        + (autocompleteEnabled ? "true" : "false")
                        + "');"
                        + "$ul."
                        + (autocompleteEnabled ? "show()" : "hide()")
                        + ".scrollTop(0);"
                        + "$ul.data('hasMore', true);"
                        + "$('#search-announcer').text('Results updated.');";

                target.appendJavaScript(reinitScript + stateScript);
            }
        });

        // --- 7. Infinite Scroll Behavior (Fetches next pages) ---
        loadMoreBehavior = new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                currentPage++;
                List<SearchResult> nextBatch = fetchResults(currentQuery, currentPage, PAGE_SIZE);

                if (nextBatch.isEmpty()) {
                    target.appendJavaScript("$('#" + resultsContainer.getMarkupId() + "').data('hasMore', false);");
                    return;
                }

                // Resolve icon URLs for each result type
                String workspaceIconUrl = RequestCycle.get()
                        .urlFor(
                                new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/folder.png"),
                                null)
                        .toString();
                String layerIconUrl = RequestCycle.get()
                        .urlFor(
                                new PackageResourceReference(
                                        GeoServerBasePage.class, "img/icons/silk/picture_empty.png"),
                                null)
                        .toString();
                String layerGroupIconUrl = RequestCycle.get()
                        .urlFor(
                                new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/layers.png"),
                                null)
                        .toString();

                // Build the HTML snippet with Accessibility attributes
                StringBuilder htmlSnippet = new StringBuilder();
                for (SearchResult result : nextBatch) {
                    String highlightedText = highlightLabel(result.label, currentQuery);
                    String ws = result.workspace == null ? "" : result.workspace;
                    String layer = result.layer == null ? "" : result.layer;
                    String safeWs = Strings.escapeMarkup(ws).toString().replace("\"", "&quot;");
                    String safeLayer = Strings.escapeMarkup(layer).toString().replace("\"", "&quot;");
                    String type = result.type == null ? "" : result.type;
                    String iconUrl;
                    if ("workspace".equals(type)) {
                        iconUrl = workspaceIconUrl;
                    } else if ("layer".equals(type)) {
                        iconUrl = layerIconUrl;
                    } else if ("layerGroup".equals(type)) {
                        iconUrl = layerGroupIconUrl;
                    } else {
                        iconUrl = workspaceIconUrl;
                    }
                    htmlSnippet
                            .append("<li class=\"search-result-item\" role=\"option\" tabindex=\"-1\"")
                            .append(" data-type=\"")
                            .append(type)
                            .append("\"")
                            .append(" data-workspace=\"")
                            .append(safeWs)
                            .append("\"")
                            .append(" data-layer=\"")
                            .append(safeLayer)
                            .append("\"")
                            .append(">")
                            .append("<img class=\\\"item-icon\\\" src=\\\"")
                            .append(iconUrl)
                            .append("\\\" alt=\\\"\\\" />")
                            .append("<span class=\"item-label\">")
                            .append(highlightedText)
                            .append("</span></li>");
                }

                // Append the HTML
                String jsEscapedHtml =
                        htmlSnippet.toString().replace("'", "\\'").replace("\n", "");
                target.appendJavaScript(
                        "$('#" + resultsContainer.getMarkupId() + "').append('" + jsEscapedHtml + "');");
                target.appendJavaScript("$('#" + resultsContainer.getMarkupId() + "').data('hasMore', true);");
            }
        };
        add(loadMoreBehavior);
    }

    // --- 8. Inject CSS/JS and Initialize JS Logic ---
    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(CSS));
        response.render(JavaScriptHeaderItem.forReference(JS));

        String initScript = String.format(
                "initSearchInputPanel('%s', '%s', '%s', %s);",
                resultsContainer.getMarkupId(),
                searchInput.getMarkupId(),
                loadMoreBehavior.getCallbackUrl(),
                autocompleteEnabled);

        response.render(OnDomReadyHeaderItem.forScript(initScript));
    }

    // --- 9. Data Fetching Logic (GeoServer Catalog-backed) ---
    private List<SearchResult> fetchResults(String query, int page, int pageSize) {
        List<SearchResult> results = new ArrayList<>();
        if (Strings.isEmpty(query)) {
            return results;
        }

        int offset = page * pageSize;
        int count = pageSize;

        // Simple full-text search on AnyText
        Filter textFilter = Predicates.fullTextSearch(query);

        // If a workspace is selected (via page parameters), restrict layer and layer group
        // searches to that workspace only. Workspace search itself remains global.
        String selectedWorkspace =
                getPage().getPageParameters().get("workspace").toOptionalString();

        // 1) Workspaces
        try (CloseableIterator<WorkspaceInfo> it = catalog.list(WorkspaceInfo.class, textFilter, offset, count, null)) {
            while (it.hasNext() && results.size() < pageSize) {
                WorkspaceInfo ws = it.next();
                // Show only the workspace name as label (no type prefix)
                results.add(new SearchResult(ws.getName(), ws.getName(), null, "workspace"));
            }
        }

        // 2) Layers
        if (results.size() < pageSize) {
            Filter layerFilter = textFilter;
            if (selectedWorkspace != null) {
                Filter wsFilter = Predicates.equal("resource.store.workspace.name", selectedWorkspace);
                layerFilter = Predicates.and(textFilter, wsFilter);
            }
            try (CloseableIterator<LayerInfo> it = catalog.list(LayerInfo.class, layerFilter, offset, count, null)) {
                while (it.hasNext() && results.size() < pageSize) {
                    LayerInfo layer = it.next();
                    String wsName =
                            layer.getResource().getStore().getWorkspace().getName();
                    // Show only the layer name as label
                    results.add(new SearchResult(layer.getName(), wsName, layer.getName(), "layer"));
                }
            }
        }

        // 3) Layer groups
        if (results.size() < pageSize) {
            Filter groupFilter = textFilter;
            if (selectedWorkspace != null) {
                Filter wsFilter = Predicates.equal("workspace.name", selectedWorkspace);
                groupFilter = Predicates.and(textFilter, wsFilter);
            }
            try (CloseableIterator<LayerGroupInfo> it =
                    catalog.list(LayerGroupInfo.class, groupFilter, offset, count, null)) {
                while (it.hasNext() && results.size() < pageSize) {
                    LayerGroupInfo group = it.next();
                    String wsName = group.getWorkspace() == null
                            ? "(global)"
                            : group.getWorkspace().getName();
                    // Show only the layer group name as label
                    results.add(new SearchResult(
                            group.getName(),
                            group.getWorkspace() == null ? null : wsName,
                            group.getName(),
                            "layerGroup"));
                }
            }
        }

        return results;
    }

    private static final class SearchResult implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        final String label;
        final String workspace;
        final String layer;
        final String type;

        SearchResult(String label, String workspace, String layer, String type) {
            this.label = label;
            this.workspace = workspace;
            this.layer = layer;
            this.type = type;
        }
    }

    private static String highlightLabel(String label, String query) {
        if (label == null) {
            return "";
        }
        if (Strings.isEmpty(query)) {
            return Strings.escapeMarkup(label).toString();
        }

        String labelLower = label.toLowerCase(Locale.ROOT);
        String queryLower = query.toLowerCase(Locale.ROOT);

        int start = 0;
        int match = labelLower.indexOf(queryLower, start);
        if (match < 0) {
            return Strings.escapeMarkup(label).toString();
        }

        StringBuilder out = new StringBuilder();
        while (match >= 0) {
            if (match > start) {
                out.append(Strings.escapeMarkup(label.substring(start, match)));
            }
            int end = match + query.length();
            out.append("<mark>")
                    .append(Strings.escapeMarkup(label.substring(match, end)))
                    .append("</mark>");
            start = end;
            match = labelLower.indexOf(queryLower, start);
        }
        if (start < label.length()) {
            out.append(Strings.escapeMarkup(label.substring(start)));
        }
        return out.toString();
    }
}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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

    private TextField<String> searchInput;
    private WebMarkupContainer resultsContainer;
    private ListView<SearchResult> initialResults;
    private AbstractDefaultAjaxBehavior loadMoreBehavior;

    private String currentQuery = "";
    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;
    private static final int MAX_QUERY_LENGTH = 100;
    private final boolean autocompleteEnabled;

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

        searchInput = new TextField<>("searchInput", Model.of(""));
        searchInput.setOutputMarkupId(true);
        searchInput.add(new org.apache.wicket.AttributeModifier("placeholder", new LoadableDetachableModel<String>() {
            @Override
            protected String load() {
                String selectedWorkspace =
                        getPage().getPageParameters().get("workspace").toOptionalString();
                if (Strings.isEmpty(selectedWorkspace)) {
                    return getString("SearchInputPanel.placeholder", null, "Search...");
                }
                return getString(
                                "SearchInputPanel.placeholderWorkspace", null, "Search in " + selectedWorkspace + "...")
                        .replace("${workspace}", selectedWorkspace);
            }
        }));
        add(searchInput);

        resultsContainer = new WebMarkupContainer("resultsContainer");
        resultsContainer.setOutputMarkupId(true);
        add(resultsContainer);

        LoadableDetachableModel<List<SearchResult>> resultsModel = new LoadableDetachableModel<>() {
            @Override
            protected List<SearchResult> load() {
                if (Strings.isEmpty(currentQuery)) return new ArrayList<>();
                return fetchResults(currentQuery, 0, PAGE_SIZE);
            }
        };
        initialResults = new ListView<>("initialResults", resultsModel) {
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
                item.add(new Image("itemIcon", new PackageResourceReference(GeoServerBasePage.class, iconPath)));

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

        searchInput.add(new AjaxFormComponentUpdatingBehavior("input") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                currentQuery = searchInput.getModelObject();
                currentPage = 0;

                target.add(resultsContainer);

                String reinitScript = String.format(
                        "initSearchInputPanel('%s','%s','%s',%s);",
                        resultsContainer.getMarkupId(),
                        searchInput.getMarkupId(),
                        loadMoreBehavior.getCallbackUrl(),
                        autocompleteEnabled);

                String stateScript = "var $ul = $('#" + resultsContainer.getMarkupId() + "');"
                        + "var isAutocompleteEnabled = " + autocompleteEnabled
                        + " || window.matchMedia('(max-width: 768px)').matches;"
                        + "$('#" + searchInput.getMarkupId()
                        + "').attr('aria-expanded', isAutocompleteEnabled ? 'true' : 'false');"
                        + "$ul[isAutocompleteEnabled ? 'show' : 'hide']().scrollTop(0);"
                        + "$ul.data('hasMore', true);"
                        + "$('#search-announcer').text('Results updated.');";

                target.appendJavaScript(reinitScript + stateScript);
            }
        });
        loadMoreBehavior = new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                currentPage++;
                List<SearchResult> nextBatch = fetchResults(currentQuery, currentPage, PAGE_SIZE);

                if (nextBatch.isEmpty()) {
                    target.appendJavaScript("$('#" + resultsContainer.getMarkupId() + "').data('hasMore', false);");
                    return;
                }

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

                StringBuilder htmlSnippet = new StringBuilder();
                for (SearchResult result : nextBatch) {
                    String highlightedText = highlightLabel(result.label, currentQuery);
                    String ws = result.workspace == null ? "" : result.workspace;
                    String layer = result.layer == null ? "" : result.layer;
                    String safeWs = Strings.escapeMarkup(ws).toString().replace("\"", "&quot;");
                    String safeLayer = Strings.escapeMarkup(layer).toString().replace("\"", "&quot;");
                    String type = result.type == null ? "" : result.type;
                    String safeType = Strings.escapeMarkup(type).toString().replace("\"", "&quot;");
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
                            .append(safeType)
                            .append("\"")
                            .append(" data-workspace=\"")
                            .append(safeWs)
                            .append("\"")
                            .append(" data-layer=\"")
                            .append(safeLayer)
                            .append("\"")
                            .append(">")
                            .append("<img class=\"item-icon\" src=\"")
                            .append(iconUrl)
                            .append("\" alt=\"\" />")
                            .append("<span class=\"item-label\">")
                            .append(highlightedText)
                            .append("</span></li>");
                }

                String jsEscapedHtml = escapeForJsString(htmlSnippet.toString());
                target.appendJavaScript(
                        "$('#" + resultsContainer.getMarkupId() + "').append('" + jsEscapedHtml + "');");
                target.appendJavaScript("$('#" + resultsContainer.getMarkupId() + "').data('hasMore', true);");
            }
        };
        add(loadMoreBehavior);
    }

    private Catalog getCatalog() {
        return GeoServerApplication.get().getCatalog();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
        response.render(JavaScriptHeaderItem.forReference(JS));

        String initScript = String.format(
                "initSearchInputPanel('%s', '%s', '%s', %s);",
                escapeForJsString(resultsContainer.getMarkupId()),
                escapeForJsString(searchInput.getMarkupId()),
                escapeForJsString(loadMoreBehavior.getCallbackUrl().toString()),
                autocompleteEnabled);

        response.render(OnDomReadyHeaderItem.forScript(initScript));
    }

    private List<SearchResult> fetchResults(String query, int page, int pageSize) {
        List<SearchResult> results = new ArrayList<>();
        if (Strings.isEmpty(query) || query.length() > MAX_QUERY_LENGTH) return results;

        Catalog catalog = getCatalog();
        int limit = pageSize;
        int remainingOffset = page * pageSize;

        Filter textFilter = Predicates.fullTextSearch(query);
        String selectedWorkspace =
                getPage().getPageParameters().get("workspace").toOptionalString();

        if (selectedWorkspace == null) {
            int wsCount = catalog.count(WorkspaceInfo.class, textFilter);
            if (remainingOffset < wsCount) {
                int toTake = Math.min(limit, wsCount - remainingOffset);
                try (CloseableIterator<WorkspaceInfo> it =
                        catalog.list(WorkspaceInfo.class, textFilter, remainingOffset, toTake, null)) {
                    while (it.hasNext() && results.size() < pageSize) {
                        WorkspaceInfo ws = it.next();
                        results.add(new SearchResult(ws.getName(), ws.getName(), null, "workspace"));
                    }
                }
            }
            remainingOffset = Math.max(0, remainingOffset - wsCount);
            limit = pageSize - results.size();
        }

        if (limit > 0) {
            Filter layerFilter = textFilter;
            if (selectedWorkspace != null) {
                layerFilter = Predicates.and(
                        textFilter, Predicates.equal("resource.store.workspace.name", selectedWorkspace));
            }

            int layerCount = catalog.count(LayerInfo.class, layerFilter);
            if (remainingOffset < layerCount) {
                int toTake = Math.min(limit, layerCount - remainingOffset);
                try (CloseableIterator<LayerInfo> it =
                        catalog.list(LayerInfo.class, layerFilter, remainingOffset, toTake, null)) {
                    while (it.hasNext() && results.size() < pageSize) {
                        LayerInfo layer = it.next();
                        String wsName = null;

                        if (layer.getResource() != null
                                && layer.getResource().getStore() != null
                                && layer.getResource().getStore().getWorkspace() != null) {
                            wsName = layer.getResource()
                                    .getStore()
                                    .getWorkspace()
                                    .getName();
                        }

                        results.add(new SearchResult(layer.getName(), wsName, layer.getName(), "layer"));
                    }
                }
            }
            remainingOffset = Math.max(0, remainingOffset - layerCount);
            limit = pageSize - results.size();
        }

        if (limit > 0) {
            Filter groupFilter = textFilter;
            if (selectedWorkspace != null) {
                groupFilter = Predicates.and(textFilter, Predicates.equal("workspace.name", selectedWorkspace));
            }

            try (CloseableIterator<LayerGroupInfo> it =
                    catalog.list(LayerGroupInfo.class, groupFilter, remainingOffset, limit, null)) {
                while (it.hasNext() && results.size() < pageSize) {
                    LayerGroupInfo group = it.next();
                    String wsName = group.getWorkspace() == null
                            ? "(global)"
                            : group.getWorkspace().getName();
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

    /** Escape a string for safe embedding inside a JavaScript single-quoted string literal. */
    static String escapeForJsString(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("/", "\\/")
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029");
    }
}

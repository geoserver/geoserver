package org.geoserver.web;

import java.util.ArrayList;
import java.util.List;
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
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.util.string.Strings;

public class SearchInputPanel extends Panel {

    // --- 1. Class-Level Field Declarations ---
    private TextField<String> searchInput;
    private WebMarkupContainer resultsContainer;
    private ListView<String> initialResults;
    private AbstractDefaultAjaxBehavior loadMoreBehavior;

    private String currentQuery = "";
    private int currentPage = 0;
    private static final int PAGE_SIZE = 20;

    // --- 2. Resource References ---
    private static final CssResourceReference CSS =
            new CssResourceReference(SearchInputPanel.class, "SearchInputPanel.css");
    private static final JavaScriptResourceReference JS =
            new JavaScriptResourceReference(SearchInputPanel.class, "SearchInputPanel.js");

    public SearchInputPanel(String id) {
        super(id);

        // --- 3. Initialize Search Input ---
        searchInput = new TextField<>("searchInput", Model.of(""));
        searchInput.setOutputMarkupId(true);
        add(searchInput);

        // --- 4. Initialize Results Container ---
        resultsContainer = new WebMarkupContainer("resultsContainer");
        resultsContainer.setOutputMarkupId(true);
        add(resultsContainer);

        // --- 5. Initial Results ListView ---
        initialResults =
                new ListView<String>("initialResults", new LoadableDetachableModel<List<String>>() {
                    @Override
                    protected List<String> load() {
                        if (Strings.isEmpty(currentQuery)) return new ArrayList<>();
                        return fetchResults(currentQuery, 0, PAGE_SIZE);
                    }
                }) {
                    @Override
                    protected void populateItem(ListItem<String> item) {
                        item.add(new Label("itemLabel", item.getModelObject()));
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

                // Re-initialize the JS state for the new dropdown list
                target.appendJavaScript("var $ul = $('#" + resultsContainer.getMarkupId() + "');" + "$('#"
                        + searchInput.getMarkupId() + "').attr('aria-expanded', 'true');" + "$ul.show().scrollTop(0);"
                        + "$ul.data('hasMore', true);"
                        + "$('#search-announcer').text('Results updated.');");
            }
        });

        // --- 7. Infinite Scroll Behavior (Fetches next pages) ---
        loadMoreBehavior = new AbstractDefaultAjaxBehavior() {
            @Override
            protected void respond(AjaxRequestTarget target) {
                currentPage++;
                List<String> nextBatch = fetchResults(currentQuery, currentPage, PAGE_SIZE);

                if (nextBatch.isEmpty()) {
                    target.appendJavaScript("$('#" + resultsContainer.getMarkupId() + "').data('hasMore', false);");
                    return;
                }

                // Build the HTML snippet with Accessibility attributes
                StringBuilder htmlSnippet = new StringBuilder();
                for (String result : nextBatch) {
                    String safeText = Strings.escapeMarkup(result).toString();
                    htmlSnippet
                            .append("<li class=\"search-result-item\" role=\"option\" tabindex=\"-1\">")
                            .append("<span class=\"item-label\">")
                            .append(safeText)
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
                "initSearchInputPanel('%s', '%s', '%s');",
                resultsContainer.getMarkupId(), searchInput.getMarkupId(), loadMoreBehavior.getCallbackUrl());

        response.render(OnDomReadyHeaderItem.forScript(initScript));
    }

    // --- 9. Data Fetching Logic (Replace with GeoServer Catalog) ---
    private List<String> fetchResults(String query, int page, int pageSize) {
        List<String> mockData = new ArrayList<>();
        if (page > 2) return mockData; // Simulate end of data

        int start = page * pageSize;
        for (int i = 0; i < pageSize; i++) {
            mockData.add(query + " - Result " + (start + i + 1));
        }
        return mockData;
    }
}

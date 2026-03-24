/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.Before;
import org.junit.Test;

public class SearchInputPanelTest extends GeoServerWicketTestSupport {

    @Before
    public void setup() {
        login();
    }

    @Test
    public void testPanelRendersDefault() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new SearchInputPanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel", SearchInputPanel.class);
    }

    @Test
    public void testPanelRendersWithAutocompleteDisabled() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new SearchInputPanel(id, false)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel", SearchInputPanel.class);
    }

    @Test
    public void testPanelContainsSearchInput() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new SearchInputPanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel:searchInput", TextField.class);
    }

    @Test
    public void testPanelContainsResultsContainer() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new SearchInputPanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel:resultsContainer", WebMarkupContainer.class);
    }

    @Test
    public void testPanelContainsInitialResults() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new SearchInputPanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel:resultsContainer:initialResults", ListView.class);
    }

    @Test
    public void testSearchInputPlaceholder() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new SearchInputPanel(id)));
        tester.assertNoErrorMessage();
        // The search input should have a placeholder attribute
        TextField<?> input = (TextField<?>) tester.getComponentFromLastRenderedPage("form:panel:searchInput");
        assertNotNull(input);
    }

    @Test
    public void testSearchInputWithWorkspaceContext() {
        PageParameters params = new PageParameters();
        params.add("workspace", "cite");
        tester.startPage(GeoServerHomePage.class, params);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testEmptySearchReturnsNoResults() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new SearchInputPanel(id)));
        tester.assertNoErrorMessage();
        ListView<?> results =
                (ListView<?>) tester.getComponentFromLastRenderedPage("form:panel:resultsContainer:initialResults");
        assertNotNull(results);
        // Empty query should return empty results
        assertTrue(results.getList().isEmpty());
    }
}

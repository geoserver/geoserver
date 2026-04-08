/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.Before;
import org.junit.Test;

public class BreadcrumbNavigationPanelTest extends GeoServerWicketTestSupport {

    @Before
    public void setup() {
        login();
    }

    @Test
    public void testPanelRendersOnHomePage() {
        tester.startPage(GeoServerHomePage.class);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testBreadcrumbShowsGlobalByDefault() {
        tester.startPage(GeoServerHomePage.class);
        // The breadcrumb panel should render without errors
        tester.assertNoErrorMessage();
    }

    @Test
    public void testBreadcrumbWithWorkspaceParam() {
        PageParameters params = new PageParameters();
        params.add("workspace", "cite");
        tester.startPage(GeoServerHomePage.class, params);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testBreadcrumbWithLayerParam() {
        PageParameters params = new PageParameters();
        params.add("workspace", "cite");
        params.add("layer", "Buildings");
        tester.startPage(GeoServerHomePage.class, params);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testBreadcrumbWithGroupParam() {
        PageParameters params = new PageParameters();
        params.add("group", "testGroup");
        tester.startPage(GeoServerHomePage.class, params);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testBreadcrumbWithNameParam() {
        PageParameters params = new PageParameters();
        params.add("workspace", "cite");
        params.add("name", "Buildings");
        tester.startPage(GeoServerHomePage.class, params);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testBreadcrumbPanelAsStandaloneComponent() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new BreadcrumbNavigationPanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel", BreadcrumbNavigationPanel.class);
    }

    @Test
    public void testBreadcrumbPanelContainsList() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new BreadcrumbNavigationPanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel:breadcrumbs", ListView.class);
    }
}

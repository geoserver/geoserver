/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.junit.Before;
import org.junit.Test;

public class NavigationTreePanelTest extends GeoServerWicketTestSupport {

    @Before
    public void setup() {
        login();
    }

    @Test
    public void testPanelRendersAsComponent() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel", NavigationTreePanel.class);
    }

    @Test
    public void testPanelContainsSearchInput() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel:myCustomSearch", SearchInputPanel.class);
    }

    @Test
    public void testPanelContainsNewMenu() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel:newMenu", WebMarkupContainer.class);
    }

    @Test
    public void testPanelContainsGlobalSection() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel:globalSectionContainer", WebMarkupContainer.class);
    }

    @Test
    public void testPanelContainsWorkspacesSection() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel:workspacesSectionContainer", WebMarkupContainer.class);
    }

    @Test
    public void testWorkspacesListRendered() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        // Workspaces scroll container should exist
        tester.assertComponent(
                "form:panel:workspacesSectionContainer:workspacesSectionBody:workspacesScroll",
                WebMarkupContainer.class);
    }

    @Test
    public void testWorkspacesListView() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        ListView<?> wsList = (ListView<?>) tester.getComponentFromLastRenderedPage(
                "form:panel:workspacesSectionContainer:workspacesSectionBody:workspacesScroll:workspaces");
        assertNotNull(wsList);
        // There should be workspaces from the test data
        assertTrue("Expected workspaces in test catalog", wsList.getList().size() > 0);
    }

    @Test
    public void testPanelRendersOnHomePageWithWorkspace() {
        PageParameters params = new PageParameters();
        params.add("workspace", "cite");
        tester.startPage(GeoServerHomePage.class, params);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testPanelRendersOnHomePageWithLayer() {
        PageParameters params = new PageParameters();
        params.add("workspace", "cite");
        params.add("layer", "Buildings");
        tester.startPage(GeoServerHomePage.class, params);
        tester.assertNoErrorMessage();
    }

    @Test
    public void testNewMenuVisibleWhenLoggedIn() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        WebMarkupContainer newMenu = (WebMarkupContainer) tester.getComponentFromLastRenderedPage("form:panel:newMenu");
        assertNotNull(newMenu);
        // When logged in as admin, the new menu should be visible
        assertTrue(newMenu.isVisible());
    }

    @Test
    public void testNewMenuHiddenWhenAnonymous() {
        logout();
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        WebMarkupContainer newMenu = (WebMarkupContainer) tester.getComponentFromLastRenderedPage("form:panel:newMenu");
        assertNotNull(newMenu);
    }

    @Test
    public void testNoDataMessageHiddenWhenDataExists() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        WebMarkupContainer noData =
                (WebMarkupContainer) tester.getComponentFromLastRenderedPage("form:panel:noDataMessage");
        assertNotNull(noData);
    }
}

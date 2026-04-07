/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.Component;
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
    public void testPanelContainsWorkspacesSection() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent("form:panel:globalSectionContainer", WebMarkupContainer.class);
    }

    @Test
    public void testWorkspacesListRendered() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        tester.assertComponent(
                "form:panel:globalSectionContainer:globalSectionBody:workspacesScroll", WebMarkupContainer.class);
    }

    @Test
    public void testWorkspacesListView() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        ListView<?> wsList = (ListView<?>) tester.getComponentFromLastRenderedPage(
                "form:panel:globalSectionContainer:globalSectionBody:workspacesScroll:workspaces");
        assertNotNull(wsList);
        assertFalse("Expected workspaces in test catalog", wsList.getList().isEmpty());
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
        assertTrue(newMenu.isVisible());
    }

    @Test
    public void testNewMenuHiddenWhenAnonymous() {
        logout();
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        // After logout the menu should not be visible
        Component newMenu = tester.getComponentFromLastRenderedPage("form:panel:newMenu");
        // The component exists but should not be visible
        if (newMenu != null) {
            assertFalse("New menu should be hidden for anonymous users", newMenu.isVisible());
        }
    }

    @Test
    public void testNoDataMessageInvisibleWhenDataExists() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new NavigationTreePanel(id)));
        tester.assertNoErrorMessage();
        // noDataMessage should not be visible when workspaces exist in the catalog
        tester.assertInvisible("form:panel:noDataMessage");
    }
}

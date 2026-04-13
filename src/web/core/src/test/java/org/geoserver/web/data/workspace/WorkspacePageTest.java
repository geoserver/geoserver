/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class WorkspacePageTest extends GeoServerWicketTestSupport {

    @Before
    public void init() {
        login();
        tester.startPage(WorkspacePage.class);

        // print(tester.getLastRenderedPage(), true, true);
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), getCatalog().getWorkspaces().size());
        WorkspaceInfo ws = (WorkspaceInfo) dv.getDataProvider().iterator(0, 1).next();
        assertEquals("cdf", ws.getName());
    }

    @Test
    public void testTimeColumnsToggle() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowCreatedTimeColumnsInAdminList(true);
        info.getSettings().setShowModifiedTimeColumnsInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);
        login();

        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        // Ensure the data provider is an instance of WorkspaceProvider
        assertTrue(dv.getDataProvider() instanceof WorkspaceProvider);

        // Cast to WorkspaceProvider
        WorkspaceProvider provider = (WorkspaceProvider) dv.getDataProvider();

        // should show both columns
        assertTrue(provider.getProperties().contains(WorkspaceProvider.CREATED_TIMESTAMP));
        assertTrue(provider.getProperties().contains(WorkspaceProvider.MODIFIED_TIMESTAMP));
    }

    @Test
    public void testWorkspaceParameterFiltersToSingleWorkspace() {
        tester.startPage(WorkspacePage.class, new PageParameters().add("workspace", "cite"));

        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(1, dv.size());
        WorkspaceInfo ws = (WorkspaceInfo) dv.getDataProvider().iterator(0, 1).next();
        assertEquals("cite", ws.getName());
    }

    @Test
    public void testLayerAndWorkspaceParameterFiltersToLayerWorkspace() {
        // Typical URL: ?workspace=cite&layer=BasicPolygons
        // workspace+layer are joined to "cite:BasicPolygons" for the catalog lookup;
        // the result is just the workspace that owns the layer ("cite").
        tester.startPage(
                WorkspacePage.class,
                new PageParameters().add("workspace", "cite").add("layer", "BasicPolygons"));
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(1, dv.size());
        WorkspaceInfo ws = (WorkspaceInfo) dv.getDataProvider().iterator(0, 1).next();
        assertEquals("cite", ws.getName());
    }

    @Test
    public void testLayerParameterAloneFiltersToLayerWorkspace() {
        // ?layer=BasicPolygons (no workspace): catalog looks up the unqualified name and
        // returns the workspace that owns the matching layer.
        tester.startPage(WorkspacePage.class, new PageParameters().add("layer", "BasicPolygons"));
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(1, dv.size());
        WorkspaceInfo ws = (WorkspaceInfo) dv.getDataProvider().iterator(0, 1).next();
        assertEquals("cite", ws.getName());
    }

    @Test
    public void testGroupParameterFiltersToMemberWorkspaces() {
        // ?group=testGroup — explicit group param returns all workspaces referenced by the group's layers
        LayerInfo citeLayer = getCatalog().getLayerByName("cite:BasicPolygons");
        LayerInfo sfLayer = getCatalog().getLayerByName("sf:PrimitiveGeoFeature");

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testGroup");
        group.getLayers().add(citeLayer);
        group.getLayers().add(sfLayer);
        group.getStyles().add(null);
        group.getStyles().add(null);
        getCatalog().add(group);

        try {
            tester.startPage(WorkspacePage.class, new PageParameters().add("group", "testGroup"));
            tester.assertRenderedPage(WorkspacePage.class);
            tester.assertNoErrorMessage();

            DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
            assertEquals(2, dv.size());
        } finally {
            getCatalog().remove(getCatalog().getLayerGroupByName("testGroup"));
        }
    }

    @Test
    public void testLayerParamFallsBackToLayerGroup() {
        // ?layer=testGroup — backwards-compat: the layer branch tries getLayerGroupByName first
        LayerInfo citeLayer = getCatalog().getLayerByName("cite:BasicPolygons");
        LayerInfo sfLayer = getCatalog().getLayerByName("sf:PrimitiveGeoFeature");

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testGroup");
        group.getLayers().add(citeLayer);
        group.getLayers().add(sfLayer);
        group.getStyles().add(null);
        group.getStyles().add(null);
        getCatalog().add(group);

        try {
            tester.startPage(WorkspacePage.class, new PageParameters().add("layer", "testGroup"));
            tester.assertRenderedPage(WorkspacePage.class);
            tester.assertNoErrorMessage();

            DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
            assertEquals(2, dv.size());
        } finally {
            getCatalog().remove(getCatalog().getLayerGroupByName("testGroup"));
        }
    }

    @Test
    public void testUnknownGroupParameterYieldsNoResults() {
        // An explicit group param that doesn't match any group → EXCLUDE (no silent fallback)
        tester.startPage(WorkspacePage.class, new PageParameters().add("group", "nonExistentGroup"));
        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(0, dv.size());
    }

    @Test
    public void testUserModifiedColumnsToggle() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowModifiedUserInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);
        login();

        tester.assertRenderedPage(WorkspacePage.class);
        tester.assertNoErrorMessage();

        DataView dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        // Ensure the data provider is an instance of WorkspaceProvider
        assertTrue(dv.getDataProvider() instanceof WorkspaceProvider);

        // Cast to WorkspaceProvider
        WorkspaceProvider provider = (WorkspaceProvider) dv.getDataProvider();

        // should show both columns
        assertTrue(provider.getProperties().contains(WorkspaceProvider.MODIFIED_BY));
    }
}

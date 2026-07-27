/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;

public class LayerGroupPageTest extends LayerGroupBaseTest {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        login();
        tester.startPage(LayerGroupPage.class);
    }

    @Test
    public void testLoad() {
        tester.assertRenderedPage(LayerGroupPage.class);
        tester.assertNoErrorMessage();

        @SuppressWarnings("unchecked")
        DataView<LayerGroupInfo> dv =
                (DataView<LayerGroupInfo>) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(getCatalog().getLayerGroups().size(), dv.size());
        LayerGroupInfo lg = dv.getDataProvider().iterator(0, 1).next();

        List<LayerGroupInfo> groups = new ArrayList<>(getCatalog().getLayerGroups());
        Collections.sort(groups, (g1, g2) -> g1.getName().compareTo(g2.getName()));

        assertEquals(groups.get(0), lg);
    }

    @Test
    public void testWorkspaceParameterFiltersToWorkspaceGroups() {
        // ?workspace=cite → only the workspace-scoped "cite:bridges" group is returned;
        // global groups ("lakes", "nestedLayerGroup", "styleGroup") are excluded.
        login();
        tester.startPage(LayerGroupPage.class, new PageParameters().add("workspace", "cite"));
        tester.assertRenderedPage(LayerGroupPage.class);
        tester.assertNoErrorMessage();

        @SuppressWarnings("unchecked")
        DataView<LayerGroupInfo> dv =
                (DataView<LayerGroupInfo>) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(1, dv.size());

        LayerGroupInfo group = dv.getDataProvider().iterator(0, 1).next();
        assertEquals("bridges", group.getName());
        assertNotNull(group.getWorkspace());
        assertEquals("cite", group.getWorkspace().getName());
    }

    @Test
    public void testWorkspaceParameterWithNoGroupsYieldsNoResults() {
        // ?workspace=sf → no layer groups belong to the "sf" workspace
        login();
        tester.startPage(LayerGroupPage.class, new PageParameters().add("workspace", "sf"));
        tester.assertRenderedPage(LayerGroupPage.class);
        tester.assertNoErrorMessage();

        @SuppressWarnings("unchecked")
        DataView<LayerGroupInfo> dv =
                (DataView<LayerGroupInfo>) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(0, dv.size());
    }

    @Test
    public void testTimeColumnsToggle() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowCreatedTimeColumnsInAdminList(true);
        info.getSettings().setShowModifiedTimeColumnsInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);

        login();

        tester.assertRenderedPage(LayerGroupPage.class);
        tester.assertNoErrorMessage();

        @SuppressWarnings("unchecked")
        DataView<LayerGroupInfo> dv =
                (DataView<LayerGroupInfo>) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        LayerGroupProvider provider = (LayerGroupProvider) dv.getDataProvider();

        // should have these columns
        assertTrue(provider.getProperties().contains(LayerGroupProvider.CREATED_TIMESTAMP));
        assertTrue(provider.getProperties().contains(LayerGroupProvider.MODIFIED_TIMESTAMP));
    }

    @Test
    public void testUserModifiedColumnToggle() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowModifiedUserInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);

        login();

        tester.assertRenderedPage(LayerGroupPage.class);
        tester.assertNoErrorMessage();

        @SuppressWarnings("unchecked")
        DataView<LayerGroupInfo> dv =
                (DataView<LayerGroupInfo>) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        LayerGroupProvider provider = (LayerGroupProvider) dv.getDataProvider();

        // should have these columns
        assertTrue(provider.getProperties().contains(LayerGroupProvider.MODIFIED_BY));
    }
}

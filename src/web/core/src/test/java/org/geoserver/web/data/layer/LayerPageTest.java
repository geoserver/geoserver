/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.geoserver.data.test.CiteTestData.BUILDINGS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import org.apache.wicket.request.mapper.parameter.INamedParameters.Type;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.Test;

public class LayerPageTest extends GeoServerWicketTestSupport {

    public static QName GS_BUILDINGS = new QName(MockData.DEFAULT_URI, "Buildings", MockData.DEFAULT_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // we don't want any of the defaults
        testData.setUpSecurity();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addVectorLayer(BUILDINGS, getCatalog());

        Map<LayerProperty, Object> props = new HashMap<>();
        props.put(LayerProperty.STYLE, BUILDINGS.getLocalPart());
        testData.addVectorLayer(GS_BUILDINGS, props, getCatalog());
    }

    @Test
    public void testBasicActions() {
        login();

        // test that we can load the page
        tester.startPage(new LayerPage());
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        // check it has two layers
        GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(2, table.getDataProvider().size());
        List<String> workspaces = getWorkspaces(table);
        assertTrue(workspaces.contains("cite"));
        assertTrue(workspaces.contains("gs"));

        // sort on workspace once (top to bottom)
        String wsSortPath = "table:listContainer:sortableLinks:3:header:link";
        tester.clickLink(wsSortPath, true);
        workspaces = getWorkspaces(table);
        assertEquals("cite", workspaces.get(0));
        assertEquals("gs", workspaces.get(1));

        // sort on workspace twice (bottom to top)
        tester.clickLink(wsSortPath, true);
        workspaces = getWorkspaces(table);
        assertEquals("gs", workspaces.get(0));
        assertEquals("cite", workspaces.get(1));

        // select second layer

        table.selectIndex(1);
        assertEquals(1, table.getSelection().size());
        LayerInfo li = (LayerInfo) table.getSelection().get(0);
        assertEquals("cite", li.getResource().getStore().getWorkspace().getName());
    }

    @Test
    public void testFilterState() {
        login();

        // test that we can load the page
        tester.startPage(new LayerPage());
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        // check it has two layers
        GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(2, table.getDataProvider().size());
        List<String> workspaces = getWorkspaces(table);
        assertTrue(workspaces.contains("cite"));
        assertTrue(workspaces.contains("gs"));

        // apply filter by only viewing layer from workspace cite
        FormTester ft = tester.newFormTester("table:filterForm");
        ft.setValue("filter", "cite");
        ft.submit("submit");

        // verify clear button is visible
        tester.assertVisible("table:filterForm:clear");

        // verify the table is only showing 1 layer
        table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(1, table.getDataProvider().size());

        // navigate to a ResourceConfigurationPage; the page derives workspace from the layer model,
        // which will be carried back to LayerPage on save, scoping results to that workspace
        LayerInfo layerInfo = getCatalog().getLayers().get(0);
        tester.startPage(new ResourceConfigurationPage(layerInfo, false));
        tester.assertRenderedPage(ResourceConfigurationPage.class);
        tester.assertNoErrorMessage();

        // click submit and go back to LayerPage
        ft = tester.newFormTester("publishedinfo");
        ft.submit("save");

        // verify when user navigates back to Layer Page
        // the clear link is visible and filter is populated in text field
        // and table is in filtered state
        tester.assertRenderedPage(LayerPage.class);
        tester.assertVisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "cite");
        table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(1, table.getDataProvider().size());

        // clear the filter by click the Clear button
        tester.clickLink("table:filterForm:clear", true);
        // verify clear button has disappeared and filter is set to empty
        tester.assertInvisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "");
        // verify table is showing only layers from the workspace context (1 layer)
        table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(1, table.getDataProvider().size());
    }

    @Test
    public void testFilterStateReset() {
        login();

        // test that we can load the page
        tester.startPage(new LayerPage());
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        // check it has two layers
        GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(2, table.getDataProvider().size());
        List<String> workspaces = getWorkspaces(table);
        assertTrue(workspaces.contains("cite"));
        assertTrue(workspaces.contains("gs"));

        // apply filter by only viewing layer from workspace cite
        FormTester ft = tester.newFormTester("table:filterForm");
        ft.setValue("filter", "cite");
        ft.submit("submit");

        // verify clear button is visible
        tester.assertVisible("table:filterForm:clear");

        // verify the table is only showing 1 layer
        table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(1, table.getDataProvider().size());

        // simulate click from left menu which passes
        // show the page with no filter
        PageParameters pageParms = new PageParameters();
        pageParms.set(GeoServerTablePanel.FILTER_PARAM, false, Type.PATH);
        tester.startPage(LayerPage.class, pageParms);
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        tester.assertInvisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", null);
        table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(2, table.getDataProvider().size());
    }

    @Test
    public void testWorkspaceParameterFiltersLayers() {
        login();
        tester.startPage(LayerPage.class, new PageParameters().add("workspace", "cite"));
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(1, table.getDataProvider().size());
        List<String> workspaces = getWorkspaces(table);
        assertTrue(workspaces.stream().allMatch(ws -> ws.equals("cite")));
    }

    @Test
    public void testWorkspaceAndLayerParameterFiltersScopedLayer() {
        // ?workspace=cite&layer=Buildings → scoped lookup as "cite:Buildings"
        login();
        tester.startPage(
                LayerPage.class, new PageParameters().add("workspace", "cite").add("layer", "Buildings"));
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(1, table.getDataProvider().size());
        LayerInfo layer = (LayerInfo) table.getDataProvider().iterator(0, 1).next();
        assertEquals("Buildings", layer.getName());
        assertEquals("cite", layer.getResource().getStore().getWorkspace().getName());
    }

    @Test
    public void testGroupParameterFiltersToGroupLayers() {
        // ?group=testGroup — explicit group param returns all layers referenced by the group
        LayerInfo citeLayer = getCatalog().getLayerByName("cite:Buildings");
        LayerInfo gsLayer = getCatalog().getLayerByName("gs:Buildings");

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testGroup");
        group.getLayers().add(citeLayer);
        group.getLayers().add(gsLayer);
        group.getStyles().add(null);
        group.getStyles().add(null);
        getCatalog().add(group);

        try {
            login();
            tester.startPage(LayerPage.class, new PageParameters().add("group", "testGroup"));
            tester.assertRenderedPage(LayerPage.class);
            tester.assertNoErrorMessage();

            GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
            assertEquals(2, table.getDataProvider().size());
        } finally {
            getCatalog().remove(getCatalog().getLayerGroupByName("testGroup"));
        }
    }

    @Test
    public void testLayerParamFallsBackToLayerGroup() {
        // ?layer=testGroup — backwards-compat: the layer branch tries getLayerGroupByName first
        LayerInfo citeLayer = getCatalog().getLayerByName("cite:Buildings");
        LayerInfo gsLayer = getCatalog().getLayerByName("gs:Buildings");

        LayerGroupInfo group = getCatalog().getFactory().createLayerGroup();
        group.setName("testGroup");
        group.getLayers().add(citeLayer);
        group.getLayers().add(gsLayer);
        group.getStyles().add(null);
        group.getStyles().add(null);
        getCatalog().add(group);

        try {
            login();
            tester.startPage(LayerPage.class, new PageParameters().add("layer", "testGroup"));
            tester.assertRenderedPage(LayerPage.class);
            tester.assertNoErrorMessage();

            GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
            assertEquals(2, table.getDataProvider().size());
        } finally {
            getCatalog().remove(getCatalog().getLayerGroupByName("testGroup"));
        }
    }

    @Test
    public void testUnknownGroupParameterYieldsNoResults() {
        // An explicit group param that doesn't match any group or layer → EXCLUDE
        login();
        tester.startPage(LayerPage.class, new PageParameters().add("group", "nonExistentGroup"));
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        assertEquals(0, table.getDataProvider().size());
    }

    private List<String> getWorkspaces(GeoServerTablePanel table) {
        Iterator it = table.getDataProvider().iterator(0, 2);
        List<String> workspaces = new ArrayList<>();
        while (it.hasNext()) {
            LayerInfo li = (LayerInfo) it.next();
            String wsName = li.getResource().getStore().getWorkspace().getName();
            workspaces.add(wsName);
        }
        return workspaces;
    }

    @Test
    public void testTimeColumnsToggle() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowCreatedTimeColumnsInAdminList(true);
        info.getSettings().setShowModifiedTimeColumnsInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);

        login();

        // test that we can load the page
        tester.startPage(new LayerPage());
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        // check it has two columns
        GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        LayerProvider layerProvider = (LayerProvider) table.getDataProvider();
        assertTrue(layerProvider.getProperties().contains(LayerProvider.CREATED_TIMESTAMP));
        assertTrue(layerProvider.getProperties().contains(LayerProvider.MODIFIED_TIMESTAMP));
    }

    @Test
    public void testModificationUserColumnToggle() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowModifiedUserInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);

        login();

        // test that we can load the page
        tester.startPage(new LayerPage());
        tester.assertRenderedPage(LayerPage.class);
        tester.assertNoErrorMessage();

        // check it has two columns
        GeoServerTablePanel table = (GeoServerTablePanel) tester.getComponentFromLastRenderedPage("table");
        LayerProvider layerProvider = (LayerProvider) table.getDataProvider();
        assertTrue(layerProvider.getProperties().contains(LayerProvider.MODIFIED_BY));
    }
}

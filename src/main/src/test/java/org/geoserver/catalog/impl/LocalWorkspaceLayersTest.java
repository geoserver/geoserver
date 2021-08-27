/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalPublished;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;

public class LocalWorkspaceLayersTest extends GeoServerSystemTestSupport {

    static final String GLOBAL_GROUP = "globalGroup";
    static final String GLOBAL_GROUP2 = "globalGroup2";
    static final String NESTED_GROUP = "nestedGroup";
    static final String LOCAL_GROUP = "localGroup";

    Catalog catalog;

    @Before
    public void setUpInternal() {
        catalog = getCatalog();
        LocalPublished.remove();
        LocalWorkspace.remove();
        Dispatcher.REQUEST.remove();

        cleanupGroupByName(GLOBAL_GROUP);
        cleanupGroupByName(GLOBAL_GROUP2);
        cleanupGroupByName(NESTED_GROUP);
        cleanupGroupByName(LOCAL_GROUP);
    }

    private void cleanupGroupByName(String name) {
        for (LayerGroupInfo lg : new ArrayList<>(catalog.getLayerGroups())) {
            if (lg.getName().equals(name)) {
                catalog.remove(lg);
            }
        }
    }

    @Test
    public void testGroupLayerInWorkspace() {
        // System.out.println(catalog.getLayerGroups());

        WorkspaceInfo workspace = catalog.getWorkspaceByName("sf");
        WorkspaceInfo workspace2 = catalog.getWorkspaceByName("cite");
        CatalogFactory factory = catalog.getFactory();
        LayerGroupInfo globalGroup = factory.createLayerGroup();
        globalGroup.setName("globalGroup");
        globalGroup.setWorkspace(workspace2);
        globalGroup.getLayers().add(catalog.getLayerByName("Lakes"));
        globalGroup.getStyles().add(null);
        catalog.add(globalGroup);

        LayerGroupInfo localGroup = factory.createLayerGroup();
        localGroup.setName("localGroup");
        localGroup.setWorkspace(workspace);
        localGroup.getLayers().add(catalog.getLayerByName("GenericEntity"));
        localGroup.getStyles().add(null);
        catalog.add(localGroup);
        String localName = localGroup.prefixedName();
        assertEquals("sf:localGroup", localName);

        assertEquals(2, catalog.getLayerGroups().size());

        LocalWorkspace.set(workspace2);
        assertNull(catalog.getLayerGroupByName("localGroup"));
        LocalWorkspace.remove();

        LocalWorkspace.set(workspace);
        assertNotNull(catalog.getLayerGroupByName("localGroup"));
        assertEquals(1, catalog.getLayerGroups().size());
        assertEquals("localGroup", catalog.getLayerGroupByName("localGroup").prefixedName());

        GeoServer gs = getGeoServer();
        SettingsInfo settings = gs.getFactory().createSettings();
        settings.setLocalWorkspaceIncludesPrefix(true);
        settings.setWorkspace(workspace);
        gs.add(settings);
        assertEquals("sf:localGroup", catalog.getLayerGroupByName("localGroup").prefixedName());
        assertEquals("sf:localGroup", catalog.getLayerGroups().get(0).prefixedName());
        gs.remove(settings);

        LocalWorkspace.remove();
    }

    @Test
    public void testLayersInLocalWorkspace() {
        WorkspaceInfo sf = catalog.getWorkspaceByName("sf");
        WorkspaceInfo cite = catalog.getWorkspaceByName("cite");

        CatalogFactory factory = catalog.getFactory();

        DataStoreInfo citeStore = factory.createDataStore();
        citeStore.setEnabled(true);
        citeStore.setName("globalStore");
        citeStore.setWorkspace(cite);
        catalog.add(citeStore);

        FeatureTypeInfo citeFeatureType = factory.createFeatureType();
        citeFeatureType.setName("citeLayer");
        citeFeatureType.setStore(citeStore);
        citeFeatureType.setNamespace(catalog.getNamespaceByPrefix("cite"));
        catalog.add(citeFeatureType);

        LayerInfo citeLayer = factory.createLayer();
        citeLayer.setResource(citeFeatureType);
        citeLayer.setEnabled(true);
        // citeLayer.setName("citeLayer");
        catalog.add(citeLayer);

        assertNotNull(catalog.getLayerByName("citeLayer"));
        assertEquals("cite:citeLayer", catalog.getLayerByName("citeLayer").prefixedName());

        DataStoreInfo sfStore = factory.createDataStore();
        sfStore.setEnabled(true);
        sfStore.setName("localStore");
        sfStore.setWorkspace(sf);
        catalog.add(sfStore);

        FeatureTypeInfo sfFeatureType = factory.createFeatureType();
        sfFeatureType.setName("sfLayer");
        sfFeatureType.setStore(sfStore);
        sfFeatureType.setNamespace(catalog.getNamespaceByPrefix("sf"));
        catalog.add(sfFeatureType);

        LayerInfo sfLayer = factory.createLayer();
        sfLayer.setResource(sfFeatureType);
        sfLayer.setEnabled(true);
        // sfLayer.setName("sfLayer");
        catalog.add(sfLayer);

        assertNotNull(catalog.getLayerByName("citeLayer"));
        assertNotNull(catalog.getLayerByName("sfLayer"));

        LocalWorkspace.set(sf);
        assertNull(catalog.getLayerByName("citeLayer"));
        assertNotNull(catalog.getLayerByName("sfLayer"));
        assertEquals("sfLayer", catalog.getLayerByName("sfLayer").prefixedName());
        LocalWorkspace.remove();

        LocalWorkspace.set(cite);
        assertNull(catalog.getLayerByName("sfLayer"));
        assertNotNull(catalog.getLayerByName("citeLayer"));
        assertEquals("citeLayer", catalog.getLayerByName("citeLayer").prefixedName());
        LocalWorkspace.remove();
    }

    @Test
    public void testGlobalGroupSpecificRequest() {
        CatalogFactory factory = catalog.getFactory();

        LayerGroupInfo globalGroup = factory.createLayerGroup();

        globalGroup.setName(GLOBAL_GROUP);
        globalGroup.getLayers().add(getBuildingsLayer());
        globalGroup.getLayers().add(getAggregateGeoFeatureLayer());
        globalGroup.getStyles().add(null);
        globalGroup.getStyles().add(null);
        catalog.add(globalGroup);

        LayerGroupInfo globalGroup2 = factory.createLayerGroup();
        globalGroup2.setName(GLOBAL_GROUP2);
        globalGroup2.getLayers().add(getBridgesLayer());
        globalGroup2.getStyles().add(null);
        catalog.add(globalGroup2);

        LocalPublished.set(catalog.getLayerGroupByName(GLOBAL_GROUP));

        // some direct access tests, generic request
        assertNull(catalog.getLayerByName(getLayerId(SystemTestData.BASIC_POLYGONS)));
        assertNull(getBridgesLayer());
        assertNull(catalog.getLayerGroupByName(GLOBAL_GROUP2));
        assertNotNull(getBuildingsLayer());
        assertNotNull(getAggregateGeoFeatureLayer());
        assertNotNull(catalog.getLayerGroupByName(GLOBAL_GROUP));
        List<LayerInfo> layers = catalog.getLayers();
        assertEquals(2, layers.size());
        assertThat(layers, containsInAnyOrder(getBuildingsLayer(), getAggregateGeoFeatureLayer()));

        // now simulate WMS getCaps, the layers should not appear in the caps document
        Request request = new Request();
        request.setService("WMS");
        request.setRequest("GetCapabilities");
        Dispatcher.REQUEST.set(request);
        assertNull(catalog.getLayerByName(getLayerId(SystemTestData.BASIC_POLYGONS)));
        assertNull(getBridgesLayer());
        assertNull(catalog.getLayerGroupByName(GLOBAL_GROUP2));
        assertNull(getBuildingsLayer());
        assertNull(getAggregateGeoFeatureLayer());
        assertNotNull(catalog.getLayerGroupByName(GLOBAL_GROUP));
        assertEquals(0, catalog.getLayers().size());

        LocalPublished.remove();
    }

    @Test
    public void testNestedGroupSpecificRequest() {
        CatalogFactory factory = catalog.getFactory();

        LayerGroupInfo nestedGroup = factory.createLayerGroup();
        nestedGroup.setName(NESTED_GROUP);
        nestedGroup.getLayers().add(getBridgesLayer());
        nestedGroup.getStyles().add(null);
        catalog.add(nestedGroup);

        LayerGroupInfo globalGroup = factory.createLayerGroup();
        globalGroup.setName(GLOBAL_GROUP);
        globalGroup.getLayers().add(getBuildingsLayer());
        globalGroup.getLayers().add(getAggregateGeoFeatureLayer());
        globalGroup.getLayers().add(nestedGroup);
        globalGroup.getStyles().add(null);
        globalGroup.getStyles().add(null);
        globalGroup.getStyles().add(null);
        catalog.add(globalGroup);

        LocalPublished.set(catalog.getLayerGroupByName(GLOBAL_GROUP));

        // some direct access tests, generic request, everything nested
        assertNull(catalog.getLayerByName(getLayerId(SystemTestData.BASIC_POLYGONS)));
        assertNotNull(getBridgesLayer());
        assertNotNull(getBuildingsLayer());
        assertNotNull(getAggregateGeoFeatureLayer());
        assertNotNull(catalog.getLayerGroupByName(NESTED_GROUP));
        assertNotNull(catalog.getLayerGroupByName(GLOBAL_GROUP));
        assertThat(
                catalog.getLayers(),
                containsInAnyOrder(
                        getBuildingsLayer(), getAggregateGeoFeatureLayer(), getBridgesLayer()));

        // now simulate WMS getCaps, the layers should not appear in the caps document
        Request request = new Request();
        request.setService("WMS");
        request.setRequest("GetCapabilities");
        Dispatcher.REQUEST.set(request);
        assertNull(catalog.getLayerByName(getLayerId(SystemTestData.BASIC_POLYGONS)));
        assertNull(getBridgesLayer());
        assertNull(catalog.getLayerGroupByName(NESTED_GROUP));
        assertNull(getBuildingsLayer());
        assertNull(getAggregateGeoFeatureLayer());
        assertNotNull(catalog.getLayerGroupByName(GLOBAL_GROUP));
        assertEquals(0, catalog.getLayers().size());

        // and then change the mode of the group to tree mode, contents will show up in caps too
        globalGroup = catalog.getLayerGroupByName(GLOBAL_GROUP);
        globalGroup.setMode(Mode.NAMED);
        catalog.save(globalGroup);

        assertNull(catalog.getLayerByName(getLayerId(SystemTestData.BASIC_POLYGONS)));
        assertNotNull(getBridgesLayer());
        assertNotNull(catalog.getLayerGroupByName(NESTED_GROUP));
        assertNotNull(getBuildingsLayer());
        assertNotNull(getAggregateGeoFeatureLayer());
        assertNotNull(catalog.getLayerGroupByName(GLOBAL_GROUP));
        assertThat(
                catalog.getLayers(),
                containsInAnyOrder(
                        getBuildingsLayer(), getAggregateGeoFeatureLayer(), getBridgesLayer()));

        LocalPublished.remove();
    }

    @Test
    public void testWorkspaceGroupSpecificRequest() {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo citeWs = catalog.getWorkspaceByName("cite");

        addLocalGroup(factory, citeWs);

        LayerGroupInfo globalGroup = factory.createLayerGroup();
        globalGroup.setName(GLOBAL_GROUP);
        globalGroup.getLayers().add(getAggregateGeoFeatureLayer());
        globalGroup.getStyles().add(null);
        catalog.add(globalGroup);

        LocalWorkspace.set(citeWs);
        LocalPublished.set(catalog.getLayerGroupByName(LOCAL_GROUP));

        // some direct access tests, generic request
        assertNull(catalog.getLayerByName(getLayerId(SystemTestData.BASIC_POLYGONS)));
        assertNull(getAggregateGeoFeatureLayer());
        assertNull(catalog.getLayerGroupByName(GLOBAL_GROUP));
        assertNotNull(getBridgesLayer());
        assertNotNull(getBuildingsLayer());
        List<LayerInfo> layers = catalog.getLayers();
        assertEquals(2, layers.size());
        assertThat(layers, containsInAnyOrder(getBuildingsLayer(), getBridgesLayer()));

        // now simulate WMS getCaps, the layers should not appear in the caps document
        Request request = new Request();
        request.setService("WMS");
        request.setRequest("GetCapabilities");
        Dispatcher.REQUEST.set(request);
        assertNull(catalog.getLayerByName(getLayerId(SystemTestData.BASIC_POLYGONS)));
        assertNull(getBridgesLayer());
        assertNull(catalog.getLayerGroupByName(GLOBAL_GROUP));
        assertNull(getBuildingsLayer());
        assertNull(getAggregateGeoFeatureLayer());
        assertEquals(0, catalog.getLayers().size());

        LocalPublished.remove();
        LocalWorkspace.remove();
    }

    @Test
    public void testLayerLocalWithContainingGroup() throws Exception {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo citeWs = catalog.getWorkspaceByName("cite");
        addLocalGroup(factory, citeWs);

        // set a local layer that's in the group
        final LayerInfo buildingsLayer = getBuildingsLayer();
        LocalPublished.set(buildingsLayer);
        assertNotNull(catalog.getLayerByName(buildingsLayer.prefixedName()));
        assertNull(catalog.getLayerGroupByName(GLOBAL_GROUP));
        assertNull(catalog.getLayerGroupByName(LOCAL_GROUP));
        assertEquals(1, catalog.getLayers().size());
        assertThat(catalog.getLayerGroups(), empty());
    }

    @Test
    public void testLayerLocalWithNonContainingGroup() throws Exception {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo citeWs = catalog.getWorkspaceByName("cite");
        addLocalGroup(factory, citeWs);

        // set a local layer that's not in the group
        final LayerInfo dividedRoutes =
                catalog.getLayerByName(getLayerId(SystemTestData.DIVIDED_ROUTES));
        LocalPublished.set(dividedRoutes);
        assertNotNull(catalog.getLayerByName(dividedRoutes.prefixedName()));
        assertNull(catalog.getLayerGroupByName(GLOBAL_GROUP));
        assertNull(catalog.getLayerGroupByName(LOCAL_GROUP));
        assertEquals(1, catalog.getLayers().size());
        assertThat(catalog.getLayerGroups(), empty());
    }

    private void addLocalGroup(CatalogFactory factory, WorkspaceInfo citeWs) {
        LayerGroupInfo localGroup = factory.createLayerGroup();
        localGroup.setName(LOCAL_GROUP);
        localGroup.setWorkspace(citeWs);
        localGroup.getLayers().add(getBuildingsLayer());
        localGroup.getLayers().add(getBridgesLayer());
        localGroup.getStyles().add(null);
        localGroup.getStyles().add(null);
        catalog.add(localGroup);
    }

    /**
     * Tests that layers, layer groups and styles nested in a group are getting properly
     * de-qualified names
     *
     * @throws Exception
     */
    @Test
    public void testNestedLayerAndGroups() throws Exception {
        CatalogFactory factory = catalog.getFactory();
        WorkspaceInfo citeWs = catalog.getWorkspaceByName("cite");

        // turn a styles to workspace specific, check it's qualified in its normal state
        LayerInfo bridges = getBridgesLayer();
        StyleInfo bridgesStyle = bridges.getDefaultStyle();
        bridgesStyle.setWorkspace(bridges.getResource().getStore().getWorkspace());
        catalog.save(bridgesStyle);
        assertEquals("cite:Bridges", bridgesStyle.prefixedName());

        // create the group that's going to be nested
        LayerGroupInfo nested = factory.createLayerGroup();
        nested.setMode(Mode.NAMED);
        final String NESTED_NAME = "nested";
        nested.setName(NESTED_NAME);
        nested.setWorkspace(citeWs);
        nested.getLayers().add(getBuildingsLayer());
        nested.getLayers().add(bridges);
        nested.getStyles().add(null);
        nested.getStyles().add(bridgesStyle);
        catalog.add(nested);

        // create a top level group
        LayerGroupInfo top = factory.createLayerGroup();
        top.setMode(Mode.NAMED);
        final String TOP_NAME = "top";
        top.setName(TOP_NAME);
        top.setWorkspace(citeWs);
        top.getLayers().add(nested);
        top.getLayers().add(getForestsLayer());
        top.getStyles().add(null);
        top.getStyles().add(null);
        catalog.add(top);

        // go in local workspace mode, check names have been de-qualified
        LocalWorkspace.set(citeWs);

        LayerGroupInfo wqTop = catalog.getLayerGroupByName(TOP_NAME);
        assertEquals(TOP_NAME, wqTop.prefixedName());
        List<PublishedInfo> topPublished = wqTop.getLayers();
        LayerGroupInfo wqNested = (LayerGroupInfo) topPublished.get(0);
        assertEquals(NESTED_NAME, wqNested.prefixedName());
        assertEquals("Forests", topPublished.get(1).prefixedName());

        // check nested group contents got de-qualified too
        List<PublishedInfo> nestedLayers = wqNested.getLayers();
        assertEquals("Buildings", nestedLayers.get(0).prefixedName());
        assertEquals("Bridges", nestedLayers.get(1).prefixedName());
        assertEquals("Bridges", wqNested.styles().get(1).prefixedName());
    }

    private LayerInfo getBridgesLayer() {
        return catalog.getLayerByName(getLayerId(SystemTestData.BRIDGES));
    }

    private LayerInfo getForestsLayer() {
        return catalog.getLayerByName(getLayerId(SystemTestData.FORESTS));
    }

    private LayerInfo getAggregateGeoFeatureLayer() {
        return catalog.getLayerByName(getLayerId(SystemTestData.AGGREGATEGEOFEATURE));
    }

    private LayerInfo getBuildingsLayer() {
        return catalog.getLayerByName(getLayerId(SystemTestData.BUILDINGS));
    }
}

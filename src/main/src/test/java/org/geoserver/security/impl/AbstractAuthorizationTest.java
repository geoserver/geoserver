/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.DataAccessManager;
import org.geoserver.security.DataAccessManagerAdapter;
import org.geoserver.security.ResourceAccessManager;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureStore;
import org.geotools.factory.Hints;
import org.junit.Before;
import org.opengis.util.ProgressListener;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;


public abstract class AbstractAuthorizationTest extends SecureObjectsTest {

    protected Authentication rwUser;

    protected Authentication roUser;

    protected Authentication anonymous;

    protected Authentication milUser;
    
    protected TestingAuthenticationToken root;

    protected Catalog catalog;

    protected WorkspaceInfo toppWs;

    protected WorkspaceInfo nurcWs;

    protected LayerInfo statesLayer;

    protected LayerInfo landmarksLayer;

    protected LayerInfo basesLayer;

    protected LayerInfo arcGridLayer;

    protected LayerInfo roadsLayer;

    protected FeatureTypeInfo states;

    protected CoverageInfo arcGrid;

    protected FeatureTypeInfo roads;

    protected FeatureTypeInfo landmarks;

    protected FeatureTypeInfo bases;

    protected DataStoreInfo statesStore;

    protected DataStoreInfo roadsStore;

    protected CoverageStoreInfo arcGridStore;

    protected StyleInfo pointStyle;
    
    protected StyleInfo lineStyle;

    protected LayerGroupInfo layerGroupGlobal;

    protected LayerGroupInfo layerGroupTopp;
    
    protected LayerGroupInfo layerGroupWithSomeLockedLayer;
    
    protected List<LayerInfo> layers;

    protected List<FeatureTypeInfo> featureTypes;

    protected List<CoverageInfo> coverages;

    protected List<WorkspaceInfo> workspaces;

    @Before
    public void setUp() throws Exception {
        rwUser = new TestingAuthenticationToken("rw", "supersecret", Arrays.asList(new GrantedAuthority[] {
                new GeoServerRole("READER"), new GeoServerRole("WRITER") }));
        roUser = new TestingAuthenticationToken("ro", "supersecret",
                Arrays.asList( new GrantedAuthority[] { new GeoServerRole("READER") }));
        anonymous = new TestingAuthenticationToken("anonymous", null);
        milUser = new TestingAuthenticationToken("military", "supersecret",
                Arrays.asList(new GrantedAuthority[] { new GeoServerRole("MILITARY") }));
        root = new TestingAuthenticationToken("admin", "geoserver", Arrays.asList(new GrantedAuthority[] { new GeoServerRole(SecureTreeNode.ROOT_ROLE) }));

        catalog = createNiceMock(Catalog.class);
        expect(catalog.getWorkspace((String) anyObject())).andReturn(
                createNiceMock(WorkspaceInfo.class)).anyTimes();
        replay(catalog);

        toppWs = createNiceMock(WorkspaceInfo.class);
        expect(toppWs.getName()).andReturn("topp").anyTimes();
        replay(toppWs);

        nurcWs = createNiceMock(WorkspaceInfo.class);
        expect(nurcWs.getName()).andReturn("nurc").anyTimes();
        replay(nurcWs);

        statesLayer = buildLayer("states", toppWs, FeatureTypeInfo.class, false);
        roadsLayer = buildLayer("roads", toppWs, FeatureTypeInfo.class, false);
        landmarksLayer = buildLayer("landmarks", toppWs, FeatureTypeInfo.class);
        basesLayer = buildLayer("bases", toppWs, FeatureTypeInfo.class);
        // let's add one with a dot inside the name
        arcGridLayer = buildLayer("arc.grid", nurcWs, CoverageInfo.class);

        // resources
        states = (FeatureTypeInfo) statesLayer.getResource();
        statesStore = states.getStore();
        arcGrid = (CoverageInfo) arcGridLayer.getResource();
        arcGridStore = (CoverageStoreInfo) arcGrid.getStore();
        roads = (FeatureTypeInfo) roadsLayer.getResource();
        roadsStore = roads.getStore();
        landmarks = (FeatureTypeInfo) landmarksLayer.getResource();
        bases = (FeatureTypeInfo) basesLayer.getResource();

        // styles
        pointStyle = buildStyle("point", null);
        lineStyle = buildStyle("line", toppWs);
        
        // layer groups
        layerGroupGlobal = buildLayerGroup("layerGroup", pointStyle, null, arcGridLayer);
        layerGroupTopp = buildLayerGroup("layerGroupTopp", lineStyle, toppWs, statesLayer);
        layerGroupWithSomeLockedLayer = buildLayerGroup("layerGroupWithSomeLockedLayer", lineStyle, toppWs, statesLayer, roadsLayer);
    }

    protected LayerInfo buildLayer(String name, WorkspaceInfo ws,
            Class<? extends ResourceInfo> resourceClass) throws Exception {
        return buildLayer(name, ws, resourceClass, true);
    }
    
    protected LayerInfo buildLayer(String name, WorkspaceInfo ws,
            Class<? extends ResourceInfo> resourceClass, boolean advertised) throws Exception {
        
        FeatureStore fs = createNiceMock(FeatureStore.class);
        replay(fs);
        
        DataStore dstore = createNiceMock(DataStore.class);
        replay(dstore);
        
        StoreInfo store;
        if (resourceClass.equals(CoverageInfo.class)) {
            store = createNiceMock(CoverageStoreInfo.class);
        } else {
            store = createNiceMock(DataStoreInfo.class);
            expect((DataStore)((DataStoreInfo) store).getDataStore(null)).andReturn(dstore);
        }
        expect(store.getWorkspace()).andReturn(ws).anyTimes();
        replay(store);

        ResourceInfo resource = createNiceMock(resourceClass);
        expect(resource.getStore()).andReturn(store).anyTimes();
        expect(resource.getName()).andReturn(name).anyTimes();
        if (resource instanceof FeatureTypeInfo) {
            expect(
                    ((FeatureTypeInfo) resource).getFeatureSource((ProgressListener) anyObject(),
                            (Hints) anyObject())).andReturn(fs).anyTimes();
        }
        if (!advertised) expect(resource.isAdvertised()).andReturn(advertised).anyTimes();
        replay(resource);

        LayerInfo layer = createNiceMock(LayerInfo.class);
        expect(layer.getName()).andReturn(name).anyTimes();
        expect(layer.getResource()).andReturn(resource).anyTimes();
        if (!advertised) expect(layer.isAdvertised()).andReturn(advertised).anyTimes();
        replay(layer);

        return layer;
    }

    protected StyleInfo buildStyle(String name, WorkspaceInfo ws) {
        StyleInfo style = createNiceMock(StyleInfo.class);
        expect(style.getName()).andReturn(name).anyTimes();
        expect(style.getFilename()).andReturn(name+".sld").anyTimes();
        expect(style.getWorkspace()).andReturn(ws).anyTimes();
        replay(style);
        return style;
    }

    protected LayerGroupInfo buildLayerGroup(String name, StyleInfo style, WorkspaceInfo ws, LayerInfo... layer) {
        return buildLayerGroup(name, LayerGroupInfo.Mode.SINGLE, null, style, ws, layer);
    }

    protected LayerGroupInfo buildLayerGroup(String name, LayerGroupInfo.Mode type, LayerInfo rootLayer, StyleInfo style, WorkspaceInfo ws, LayerInfo... layer) {
        LayerGroupInfo layerGroup = createNiceMock(LayerGroupInfo.class);
        expect(layerGroup.getName()).andReturn(name).anyTimes();
        expect(layerGroup.getMode()).andReturn(type).anyTimes();
        expect(layerGroup.getRootLayer()).andReturn(rootLayer).anyTimes();
        expect(layerGroup.getLayers()).andReturn(new ArrayList<PublishedInfo>(Arrays.asList(layer))).anyTimes();
        expect(layerGroup.getStyles()).andReturn(Arrays.asList(style)).anyTimes();
        expect(layerGroup.getWorkspace()).andReturn(ws).anyTimes();
        replay(layerGroup);
        return layerGroup;
    }
    
    protected ResourceAccessManager buildManager(String propertyFile) throws Exception {
        return new DataAccessManagerAdapter(buildLegacyAccessManager(propertyFile));
    }
    
    protected DataAccessManager buildLegacyAccessManager(String propertyFile) throws Exception {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream(propertyFile));
        return new DefaultDataAccessManager(new MemoryDataAccessRuleDAO(catalog, props));
    }
    
    /**
     * Sets up a mock catalog.
     */
    protected void populateCatalog() {
        // build resource collections
        layers = Arrays.asList(statesLayer, roadsLayer, landmarksLayer, basesLayer, arcGridLayer);
        featureTypes = new ArrayList<FeatureTypeInfo>();
        coverages = new ArrayList<CoverageInfo>();
        for (LayerInfo layer : layers) {
            if (layer.getResource() instanceof FeatureTypeInfo)
                featureTypes.add((FeatureTypeInfo) layer.getResource());
            else
                coverages.add((CoverageInfo) layer.getResource());
        }
        workspaces = Arrays.asList(toppWs, nurcWs);

        // prime the catalog
        catalog = createNiceMock(Catalog.class);
        expect(catalog.getFeatureTypeByName("topp:states")).andReturn((FeatureTypeInfo) states)
                .anyTimes();
        expect(catalog.getResourceByName("topp:states", FeatureTypeInfo.class)).andReturn(
                (FeatureTypeInfo) states).anyTimes();
        expect(catalog.getLayerByName("topp:states")).andReturn(statesLayer).anyTimes();
        expect(catalog.getCoverageByName("nurc:arcgrid")).andReturn((CoverageInfo) arcGrid)
                .anyTimes();
        expect(catalog.getResourceByName("nurc:arcgrid", CoverageInfo.class)).andReturn(
                (CoverageInfo) arcGrid).anyTimes();
        expect(catalog.getFeatureTypeByName("topp:roads")).andReturn((FeatureTypeInfo) roads)
                .anyTimes();
        expect(catalog.getLayerByName("topp:roads")).andReturn(roadsLayer).anyTimes();
        expect(catalog.getFeatureTypeByName("topp:landmarks")).andReturn(
                (FeatureTypeInfo) landmarks).anyTimes();
        expect(catalog.getFeatureTypeByName("topp:bases")).andReturn((FeatureTypeInfo) bases)
                .anyTimes();
        expect(catalog.getDataStoreByName("states")).andReturn((DataStoreInfo) statesStore)
                .anyTimes();
        expect(catalog.getDataStoreByName("roads")).andReturn((DataStoreInfo) roadsStore)
                .anyTimes();
        expect(catalog.getCoverageStoreByName("arcGrid")).andReturn(
                (CoverageStoreInfo) arcGridStore).anyTimes();
        expect(catalog.getLayers()).andReturn(layers).anyTimes();
        expect(catalog.getFeatureTypes()).andReturn(featureTypes).anyTimes();
        expect(catalog.getCoverages()).andReturn(coverages).anyTimes();
        expect(catalog.getWorkspaces()).andReturn(workspaces).anyTimes();
        expect(catalog.getWorkspaceByName("topp")).andReturn(toppWs).anyTimes();
        expect(catalog.getWorkspaceByName("nurc")).andReturn(nurcWs).anyTimes();
        expect(catalog.getStyles()).andReturn(Arrays.asList(pointStyle, lineStyle)).anyTimes();
        expect(catalog.getStylesByWorkspace(toppWs)).andReturn(Arrays.asList(pointStyle, lineStyle)).anyTimes();
        expect(catalog.getStylesByWorkspace(nurcWs)).andReturn(Arrays.asList(pointStyle)).anyTimes();
        expect(catalog.getLayerGroups()).andReturn(Arrays.asList(layerGroupGlobal, layerGroupTopp, layerGroupWithSomeLockedLayer)).anyTimes();
        expect(catalog.getLayerGroupsByWorkspace("topp")).andReturn(Arrays.asList(new LayerGroupInfo[] { layerGroupTopp, layerGroupWithSomeLockedLayer })).anyTimes();
        expect(catalog.getLayerGroupsByWorkspace("nurc")).andReturn(Arrays.asList(layerGroupGlobal)).anyTimes();
        expect(catalog.getLayerGroupByName("topp", layerGroupWithSomeLockedLayer.getName())).andReturn(layerGroupWithSomeLockedLayer).anyTimes();
        replay(catalog);
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.DataAccessManager;
import org.geoserver.security.DataAccessManagerAdapter;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.ResourceAccessManagerWrapper;
import org.geoserver.security.SecureCatalogImpl;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureStore;
import org.geotools.factory.Hints;
import org.junit.Before;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
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

    protected SecureCatalogImpl sc;

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
        arcGridStore = arcGrid.getStore();
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

        NamespaceInfo ns = createNiceMock(NamespaceInfo.class);
        expect(ns.getName()).andReturn(ws.getName()).anyTimes();
        expect(ns.getPrefix()).andReturn(ws.getName()).anyTimes();
        expect(ns.getURI()).andReturn("http://www.geoserver.org/test/" + ws.getName()).anyTimes();
        replay(ns);

        ResourceInfo resource = createNiceMock(resourceClass);
        expect(resource.getStore()).andReturn(store).anyTimes();
        expect(resource.getName()).andReturn(name).anyTimes();
        expect(resource.getNamespace()).andReturn(ns).anyTimes();
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
        return buildManager(propertyFile, null);
    }

    protected ResourceAccessManager buildManager(String propertyFile,
            ResourceAccessManagerWrapper wrapper) throws Exception {
        ResourceAccessManager manager = new DataAccessManagerAdapter(
                buildLegacyAccessManager(propertyFile));

        if (wrapper != null) {
            wrapper.setDelegate(manager);
            manager = wrapper;
        }

        sc = new SecureCatalogImpl(catalog, manager) {

            @Override
            protected boolean isAdmin(Authentication authentication) {
                return false;
            }

        };
        GeoServerExtensionsHelper.singleton("secureCatalog", sc, SecureCatalogImpl.class);

        return manager;
    }

    protected DataAccessManager buildLegacyAccessManager(String propertyFile) throws Exception {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream(propertyFile));
        return new DefaultResourceAccessManager(new MemoryDataAccessRuleDAO(catalog, props));
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
        expect(catalog.getFeatureTypeByName("topp:states")).andReturn(states)
                .anyTimes();
        expect(catalog.getResourceByName("topp:states", FeatureTypeInfo.class)).andReturn(
                states).anyTimes();
        expect(catalog.getLayerByName("topp:states")).andReturn(statesLayer).anyTimes();
        expect(catalog.getCoverageByName("nurc:arcgrid")).andReturn(arcGrid)
                .anyTimes();
        expect(catalog.getResourceByName("nurc:arcgrid", CoverageInfo.class)).andReturn(
                arcGrid).anyTimes();
        expect(catalog.getFeatureTypeByName("topp:roads")).andReturn(roads)
                .anyTimes();
        expect(catalog.getLayerByName("topp:roads")).andReturn(roadsLayer).anyTimes();
        expect(catalog.getFeatureTypeByName("topp:landmarks")).andReturn(
                landmarks).anyTimes();
        expect(catalog.getFeatureTypeByName("topp:bases")).andReturn(bases)
                .anyTimes();
        expect(catalog.getDataStoreByName("states")).andReturn(statesStore)
                .anyTimes();
        expect(catalog.getDataStoreByName("roads")).andReturn(roadsStore)
                .anyTimes();
        expect(catalog.getCoverageStoreByName("arcGrid")).andReturn(
                arcGridStore).anyTimes();
        expect(catalog.getLayers()).andReturn(layers).anyTimes();
        stubList(catalog, LayerInfo.class, layers);
        expect(catalog.getFeatureTypes()).andReturn(featureTypes).anyTimes();
        stubList(catalog, FeatureTypeInfo.class, featureTypes);
        expect(catalog.getCoverages()).andReturn(coverages).anyTimes();
        stubList(catalog, CoverageInfo.class, coverages);
        expect(catalog.getWorkspaces()).andReturn(workspaces).anyTimes();
        stubList(catalog, WorkspaceInfo.class, workspaces);
        stubList(catalog, StyleInfo.class, Arrays.asList(pointStyle, lineStyle));
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

        GeoServerExtensionsHelper.singleton("catalog", catalog);
    }
    
    <T extends CatalogInfo> void stubList(Catalog mock, Class<T> clazz, final List<T> source) {
        final Capture<Filter> cap = new Capture<Filter>();
        expect(catalog.list(eq(clazz), capture(cap))).andStubAnswer(new IAnswer<CloseableIterator<T>>(){
            @Override
            public CloseableIterator<T> answer() throws Throwable {
                return makeCIterator(source, cap.getValue());
            }
        });
        expect(catalog.list(eq(clazz), capture(cap), EasyMock.anyInt(), EasyMock.anyInt(), (SortBy)anyObject())).andStubAnswer(new IAnswer<CloseableIterator<T>>(){
            @Override
            public CloseableIterator<T> answer() throws Throwable {
                return makeCIterator(source, cap.getValue());
            }
        });
    }
    
    static <T> CloseableIterator<T> makeCIterator(List<T> source, Filter f) {
        return CloseableIteratorAdapter.filter(source.iterator(), f);
    }
}

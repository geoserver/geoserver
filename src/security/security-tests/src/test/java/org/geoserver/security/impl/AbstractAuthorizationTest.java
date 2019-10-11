/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.*;

import java.util.*;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.geoserver.catalog.*;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.ResourceAccessManagerWrapper;
import org.geoserver.security.SecureCatalogImpl;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureStore;
import org.geotools.util.factory.Hints;
import org.junit.After;
import org.junit.Before;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public abstract class AbstractAuthorizationTest extends SecureObjectsTest {

    private static final String NULL_STRING = (String) null;

    protected Authentication rwUser;

    protected Authentication roUser;

    protected Authentication anonymous;

    protected Authentication milUser;

    protected TestingAuthenticationToken root;

    protected Catalog catalog;

    protected WorkspaceInfo toppWs;

    protected WorkspaceInfo nurcWs;

    protected LayerInfo statesLayer;

    protected LayerInfo regionsLayer;

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

    protected LayerInfo cascadedLayer;

    protected LayerInfo cascadedWmtsLayer;

    protected LayerInfo forestsLayer;

    protected WMSLayerInfo cascaded;

    protected List<WMSLayerInfo> wmsLayers;

    protected WMTSLayerInfo cascadedWmts;

    protected List<WMTSLayerInfo> wmtsLayers;

    protected LayerGroupInfo namedTreeA;

    protected LayerGroupInfo namedTreeB;

    protected LayerGroupInfo namedTreeC;

    protected LayerGroupInfo containerTreeB;

    protected LayerGroupInfo singleGroupC;

    protected LayerGroupInfo wsContainerD;

    protected LayerGroupInfo nestedContainerE;

    protected LayerInfo citiesLayer;

    protected FeatureTypeInfo cities;

    protected List<LayerGroupInfo> layerGroups;

    @Before
    public void setUp() throws Exception {
        rwUser =
                new TestingAuthenticationToken(
                        "rw",
                        "supersecret",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new GeoServerRole("READER"), new GeoServerRole("WRITER")
                                }));
        roUser =
                new TestingAuthenticationToken(
                        "ro",
                        "supersecret",
                        Arrays.asList(new GrantedAuthority[] {new GeoServerRole("READER")}));
        anonymous = new TestingAuthenticationToken("anonymous", null);
        milUser =
                new TestingAuthenticationToken(
                        "military",
                        "supersecret",
                        Arrays.asList(new GrantedAuthority[] {new GeoServerRole("MILITARY")}));
        root =
                new TestingAuthenticationToken(
                        "admin",
                        "geoserver",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new GeoServerRole(SecureTreeNode.ROOT_ROLE)
                                }));

        catalog = createNiceMock(Catalog.class);
        expect(catalog.getWorkspace((String) anyObject()))
                .andReturn(createNiceMock(WorkspaceInfo.class))
                .anyTimes();
        replay(catalog);

        toppWs = createNiceMock(WorkspaceInfo.class);
        expect(toppWs.getName()).andReturn("topp").anyTimes();
        replay(toppWs);

        nurcWs = createNiceMock(WorkspaceInfo.class);
        expect(nurcWs.getName()).andReturn("nurc").anyTimes();
        replay(nurcWs);

        statesLayer = buildLayer("states", toppWs, FeatureTypeInfo.class, false);
        roadsLayer = buildLayer("roads", toppWs, FeatureTypeInfo.class, false);
        citiesLayer = buildLayer("cities", nurcWs, FeatureTypeInfo.class);
        landmarksLayer = buildLayer("landmarks", toppWs, FeatureTypeInfo.class);
        basesLayer = buildLayer("bases", toppWs, FeatureTypeInfo.class);
        forestsLayer = buildLayer("forests", toppWs, FeatureTypeInfo.class);
        regionsLayer = buildLayer("regions", toppWs, FeatureTypeInfo.class);
        // let's add one with a dot inside the name
        arcGridLayer = buildLayer("arc.grid", nurcWs, CoverageInfo.class);

        // resources
        states = (FeatureTypeInfo) statesLayer.getResource();
        statesStore = states.getStore();
        arcGrid = (CoverageInfo) arcGridLayer.getResource();
        arcGridStore = arcGrid.getStore();
        roads = (FeatureTypeInfo) roadsLayer.getResource();
        cities = (FeatureTypeInfo) citiesLayer.getResource();
        roadsStore = roads.getStore();
        landmarks = (FeatureTypeInfo) landmarksLayer.getResource();
        bases = (FeatureTypeInfo) basesLayer.getResource();

        // styles
        pointStyle = buildStyle("point", null);
        lineStyle = buildStyle("line", toppWs);

        // layer groups
        layerGroupGlobal = buildLayerGroup("layerGroup", pointStyle, null, arcGridLayer);
        layerGroupTopp = buildLayerGroup("layerGroupTopp", lineStyle, toppWs, statesLayer);
        layerGroupWithSomeLockedLayer =
                buildLayerGroup(
                        "layerGroupWithSomeLockedLayer",
                        lineStyle,
                        toppWs,
                        statesLayer,
                        roadsLayer);

        // container groups for testing group security
        namedTreeA =
                buildLayerGroup(
                        "namedTreeA", Mode.NAMED, null, statesLayer, roadsLayer, citiesLayer);
        namedTreeB = buildLayerGroup("namedTreeB", Mode.NAMED, null, true, false, regionsLayer);
        namedTreeC = buildLayerGroup("namedTreeC", Mode.NAMED, null, false, true, regionsLayer);

        nestedContainerE = buildLayerGroup("nestedContainerE", Mode.CONTAINER, null, forestsLayer);
        containerTreeB =
                buildLayerGroup(
                        "containerTreeB",
                        Mode.CONTAINER,
                        null,
                        roadsLayer,
                        landmarksLayer,
                        nestedContainerE);
        singleGroupC = buildLayerGroup("singleGroupC", Mode.SINGLE, null, statesLayer, basesLayer);
        wsContainerD = buildLayerGroup("wsContainerD", Mode.CONTAINER, nurcWs, arcGridLayer);

        layerGroups =
                Arrays.asList(
                        layerGroupGlobal,
                        layerGroupTopp,
                        layerGroupWithSomeLockedLayer,
                        namedTreeA,
                        namedTreeB,
                        namedTreeC,
                        containerTreeB,
                        singleGroupC,
                        wsContainerD,
                        nestedContainerE);

        // cascaded WMS layer
        cascadedLayer = buildLayer("cascaded", toppWs, WMSLayerInfo.class);
        cascaded = (WMSLayerInfo) cascadedLayer.getResource();

        // cascaded WMTS layer
        cascadedWmtsLayer = buildLayer("cascadedWmts", toppWs, WMTSLayerInfo.class);
        cascadedWmts = (WMTSLayerInfo) cascadedWmtsLayer.getResource();
    }

    @After
    public void cleanupRequestThreadLocal() throws Exception {
        Dispatcher.REQUEST.remove();
    }

    protected void setupRequestThreadLocal(String service) {
        Request request = new Request();
        request.setService(service);
        Dispatcher.REQUEST.set(request);
    }

    protected LayerInfo buildLayer(
            String name, WorkspaceInfo ws, Class<? extends ResourceInfo> resourceClass)
            throws Exception {
        return buildLayer(name, ws, resourceClass, true);
    }

    protected LayerInfo buildLayer(
            String name,
            WorkspaceInfo ws,
            Class<? extends ResourceInfo> resourceClass,
            boolean advertised)
            throws Exception {

        FeatureStore fs = createNiceMock(FeatureStore.class);
        replay(fs);

        DataStore dstore = createNiceMock(DataStore.class);
        replay(dstore);

        StoreInfo store;
        if (resourceClass.equals(CoverageInfo.class)) {
            store = createNiceMock(CoverageStoreInfo.class);
        } else if (resourceClass.equals(WMSLayerInfo.class)) {
            store = createNiceMock(WMSStoreInfo.class);
        } else if (resourceClass.equals(WMTSLayerInfo.class)) {
            store = createNiceMock(WMTSStoreInfo.class);
        } else {
            store = createNiceMock(DataStoreInfo.class);
            expect((DataStore) ((DataStoreInfo) store).getDataStore(null)).andReturn(dstore);
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
        expect(resource.prefixedName()).andReturn(ws.getName() + ":" + name).anyTimes();
        expect(resource.getNamespace()).andReturn(ns).anyTimes();
        if (resource instanceof FeatureTypeInfo) {
            expect(
                            ((FeatureTypeInfo) resource)
                                    .getFeatureSource(
                                            (ProgressListener) anyObject(), (Hints) anyObject()))
                    .andReturn(fs)
                    .anyTimes();
        }
        if (!advertised) expect(resource.isAdvertised()).andReturn(advertised).anyTimes();
        expect(resource.getId()).andReturn(name + "-id").anyTimes();
        replay(resource);

        LayerInfo layer = createNiceMock(LayerInfo.class);
        expect(layer.getName()).andReturn(name).anyTimes();
        expect(layer.prefixedName()).andReturn(ws.getName() + ":" + name).anyTimes();
        expect(layer.getResource()).andReturn(resource).anyTimes();
        expect(layer.getId()).andReturn(name + "-lid").anyTimes();
        if (!advertised) expect(layer.isAdvertised()).andReturn(advertised).anyTimes();
        replay(layer);

        return layer;
    }

    protected StyleInfo buildStyle(String name, WorkspaceInfo ws) {
        StyleInfo style = createNiceMock(StyleInfo.class);
        expect(style.getName()).andReturn(name).anyTimes();
        expect(style.getFilename()).andReturn(name + ".sld").anyTimes();
        expect(style.getWorkspace()).andReturn(ws).anyTimes();
        replay(style);
        return style;
    }

    protected LayerGroupInfo buildLayerGroup(
            String name, StyleInfo style, WorkspaceInfo ws, LayerInfo... layer) {
        return buildLayerGroup(name, Mode.SINGLE, ws, layer);
    }

    protected LayerGroupInfo buildLayerGroup(
            String name, Mode type, WorkspaceInfo ws, PublishedInfo... contents) {
        return buildLayerGroup(name, type, ws, true, true, contents);
    }

    protected LayerGroupInfo buildLayerGroup(
            String name,
            Mode type,
            WorkspaceInfo ws,
            boolean advertised,
            PublishedInfo... contents) {
        return buildLayerGroup(name, type, ws, advertised, true, contents);
    }

    protected LayerGroupInfo buildLayerGroup(
            String name,
            Mode type,
            WorkspaceInfo ws,
            boolean advertised,
            boolean enabled,
            PublishedInfo... contents) {
        LayerGroupInfo layerGroup = createNiceMock(LayerGroupInfo.class);
        expect(layerGroup.getName()).andReturn(name).anyTimes();
        expect(layerGroup.prefixedName())
                .andReturn((ws != null ? ws.getName() + ":" : "") + name)
                .anyTimes();
        expect(layerGroup.getMode()).andReturn(type).anyTimes();
        expect(layerGroup.getLayers())
                .andReturn(new ArrayList<PublishedInfo>(Arrays.asList(contents)))
                .anyTimes();
        expect(layerGroup.getStyles()).andReturn(buildUniqueStylesForLayers(contents)).anyTimes();
        expect(layerGroup.getWorkspace()).andReturn(ws).anyTimes();
        expect(layerGroup.layers())
                .andAnswer(() -> new LayerGroupHelper(layerGroup).allLayers())
                .anyTimes();
        expect(layerGroup.getId())
                .andAnswer(() -> (ws == null ? name : ws.getName() + ":" + name) + "-id")
                .anyTimes();
        expect(layerGroup.isAdvertised()).andReturn(advertised).anyTimes();
        replay(layerGroup);
        return layerGroup;
    }

    private List<StyleInfo> buildUniqueStylesForLayers(PublishedInfo[] contents) {
        if (contents == null) {
            return null;
        }

        List<StyleInfo> result = new ArrayList<>();
        for (PublishedInfo pi : contents) {
            if (pi instanceof LayerInfo) {
                StyleInfo style = buildStyle(pi.prefixedName().replace(':', '-') + "-style", null);
                result.add(style);
            } else {
                // group
                result.add(null);
            }
        }
        return result;
    }

    protected LayerGroupInfo buildEOLayerGroup(
            String name,
            LayerInfo rootLayer,
            StyleInfo style,
            WorkspaceInfo ws,
            PublishedInfo... contents) {
        LayerGroupInfo layerGroup = createNiceMock(LayerGroupInfo.class);
        expect(layerGroup.getName()).andReturn(name).anyTimes();
        expect(layerGroup.prefixedName())
                .andReturn((ws != null ? ws.getName() + ":" : "") + name)
                .anyTimes();
        expect(layerGroup.getMode()).andReturn(Mode.EO).anyTimes();
        expect(layerGroup.getRootLayer()).andReturn(rootLayer).anyTimes();
        expect(layerGroup.getLayers())
                .andReturn(new ArrayList<PublishedInfo>(Arrays.asList(contents)))
                .anyTimes();
        expect(layerGroup.getStyles()).andReturn(Arrays.asList(style)).anyTimes();
        expect(layerGroup.getWorkspace()).andReturn(ws).anyTimes();
        expect(layerGroup.layers())
                .andAnswer(() -> new LayerGroupHelper(layerGroup).allLayers())
                .anyTimes();
        expect(layerGroup.getId())
                .andAnswer(() -> (ws == null ? name : ws.getName() + ":" + name) + "-id")
                .anyTimes();
        replay(layerGroup);
        return layerGroup;
    }

    protected ResourceAccessManager buildManager(String propertyFile) throws Exception {
        return buildManager(propertyFile, null);
    }

    protected ResourceAccessManager buildManager(
            String propertyFile, ResourceAccessManagerWrapper wrapper) throws Exception {
        ResourceAccessManager manager = buildAccessManager(propertyFile);

        if (wrapper != null) {
            wrapper.setDelegate(manager);
            manager = wrapper;
        }

        sc =
                new SecureCatalogImpl(catalog, manager) {

                    @Override
                    protected boolean isAdmin(Authentication authentication) {
                        return false;
                    }
                };
        GeoServerExtensionsHelper.singleton("secureCatalog", sc, SecureCatalogImpl.class);

        return manager;
    }

    protected DefaultResourceAccessManager buildAccessManager(String propertyFile)
            throws Exception {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream(propertyFile));
        return new DefaultResourceAccessManager(
                new MemoryDataAccessRuleDAO(catalog, props), catalog);
    }

    /** Sets up a mock catalog. */
    protected void populateCatalog() {
        // build resource collections
        layers =
                Arrays.asList(
                        statesLayer,
                        roadsLayer,
                        landmarksLayer,
                        basesLayer,
                        arcGridLayer,
                        cascadedLayer,
                        cascadedWmtsLayer);
        featureTypes = new ArrayList<>();
        coverages = new ArrayList<>();
        wmsLayers = new ArrayList<>();
        wmtsLayers = new ArrayList<>();
        for (LayerInfo layer : layers) {
            if (layer.getResource() instanceof FeatureTypeInfo) {
                featureTypes.add((FeatureTypeInfo) layer.getResource());
            } else if (layer.getResource() instanceof WMSLayerInfo) {
                wmsLayers.add((WMSLayerInfo) layer.getResource());
            } else if (layer.getResource() instanceof WMTSLayerInfo) {
                wmtsLayers.add((WMTSLayerInfo) layer.getResource());
            } else {
                coverages.add((CoverageInfo) layer.getResource());
            }
        }
        workspaces = Arrays.asList(toppWs, nurcWs);

        // prime the catalog
        catalog = createNiceMock(Catalog.class);
        expect(catalog.getFeatureTypeByName("topp:states")).andReturn(states).anyTimes();
        expect(catalog.getLayerByName("topp:cascaded")).andReturn(cascadedLayer).anyTimes();
        expect(catalog.getResourceByName("topp:cascaded", WMSLayerInfo.class))
                .andReturn(cascaded)
                .anyTimes();
        expect(catalog.getLayerByName("topp:cascadedWmts")).andReturn(cascadedWmtsLayer).anyTimes();
        expect(catalog.getResourceByName("topp:cascadedWmts", WMTSLayerInfo.class))
                .andReturn(cascadedWmts)
                .anyTimes();
        expect(catalog.getResourceByName("topp:states", FeatureTypeInfo.class))
                .andReturn(states)
                .anyTimes();
        expect(catalog.getLayerByName("topp:states")).andReturn(statesLayer).anyTimes();
        expect(catalog.getCoverageByName("nurc:arcgrid")).andReturn(arcGrid).anyTimes();
        expect(catalog.getResourceByName("nurc:arcgrid", CoverageInfo.class))
                .andReturn(arcGrid)
                .anyTimes();
        expect(catalog.getFeatureTypeByName("topp:roads")).andReturn(roads).anyTimes();
        expect(catalog.getFeatureTypeByName("nurc:cities")).andReturn(cities).anyTimes();
        expect(catalog.getLayerByName("topp:roads")).andReturn(roadsLayer).anyTimes();
        expect(catalog.getLayerByName("nurc:cities")).andReturn(citiesLayer).anyTimes();
        expect(catalog.getFeatureTypeByName("topp:landmarks")).andReturn(landmarks).anyTimes();
        expect(catalog.getFeatureTypeByName("topp:bases")).andReturn(bases).anyTimes();
        expect(catalog.getDataStoreByName("states")).andReturn(statesStore).anyTimes();
        expect(catalog.getDataStoreByName("roads")).andReturn(roadsStore).anyTimes();
        expect(catalog.getCoverageStoreByName("arcGrid")).andReturn(arcGridStore).anyTimes();
        expect(catalog.getLayerByName("topp:landmarks")).andReturn(landmarksLayer).anyTimes();
        expect(catalog.getLayerByName("topp:bases")).andReturn(basesLayer).anyTimes();
        expect(catalog.getLayerByName("nurc:arc.grid")).andReturn(arcGridLayer).anyTimes();
        expect(catalog.getLayerByName("topp:forests")).andReturn(forestsLayer).anyTimes();
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
        expect(catalog.getStylesByWorkspace(toppWs))
                .andReturn(Arrays.asList(pointStyle, lineStyle))
                .anyTimes();
        expect(catalog.getStylesByWorkspace(nurcWs))
                .andReturn(Arrays.asList(pointStyle))
                .anyTimes();
        expect(catalog.getLayerGroups()).andReturn(layerGroups).anyTimes();
        for (LayerGroupInfo lg : layerGroups) {
            expect(catalog.getLayerGroup(lg.getId())).andReturn(lg).anyTimes();
            if (lg.getWorkspace() == null) {
                expect(catalog.getLayerGroupByName(lg.getName())).andReturn(lg).anyTimes();
                expect(catalog.getLayerGroupByName(NULL_STRING, lg.getName()))
                        .andReturn(lg)
                        .anyTimes();
            } else {
                expect(catalog.getLayerGroupByName(lg.getWorkspace(), lg.getName()))
                        .andReturn(lg)
                        .anyTimes();
                expect(catalog.getLayerGroupByName(lg.getWorkspace().getName(), lg.getName()))
                        .andReturn(lg)
                        .anyTimes();
            }
        }

        expect(catalog.getLayerGroupsByWorkspace("topp"))
                .andReturn(
                        Arrays.asList(
                                new LayerGroupInfo[] {
                                    layerGroupTopp, layerGroupWithSomeLockedLayer
                                }))
                .anyTimes();
        expect(catalog.getLayerGroupsByWorkspace("nurc"))
                .andReturn(Arrays.asList(layerGroupGlobal))
                .anyTimes();
        expect(catalog.list(eq(LayerGroupInfo.class), anyObject(Filter.class)))
                .andAnswer(
                        () -> {
                            List<LayerGroupInfo> groups = catalog.getLayerGroups();
                            Filter f = (Filter) EasyMock.getCurrentArguments()[1];
                            Iterator<LayerGroupInfo> it =
                                    groups.stream().filter(lg -> f.evaluate(lg)).iterator();
                            return new CloseableIteratorAdapter<LayerGroupInfo>(it);
                        })
                .anyTimes();
        replay(catalog);

        GeoServerExtensionsHelper.singleton("catalog", catalog);
    }

    <T extends CatalogInfo> void stubList(Catalog mock, Class<T> clazz, final List<T> source) {
        final Capture<Filter> cap = Capture.newInstance(CaptureType.LAST);
        expect(catalog.list(eq(clazz), capture(cap)))
                .andStubAnswer(
                        new IAnswer<CloseableIterator<T>>() {
                            @Override
                            public CloseableIterator<T> answer() throws Throwable {
                                return makeCIterator(source, cap.getValue());
                            }
                        });
        expect(
                        catalog.list(
                                eq(clazz),
                                capture(cap),
                                EasyMock.anyInt(),
                                EasyMock.anyInt(),
                                (SortBy) anyObject()))
                .andStubAnswer(
                        new IAnswer<CloseableIterator<T>>() {
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

/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.geoserver.gwc.GWC.tileLayerName;
import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.RenderedOp;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.GWCSynchEnv;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.dispatch.GwcServiceDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.util.DimensionWarning;
import org.geoserver.util.DimensionWarning.WarningType;
import org.geoserver.util.HTTPWarningAppender;
import org.geoserver.wms.GetLegendGraphicOutputFormat;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.capabilities.LegendSample;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geoserver.wms.map.RenderedImageTimeDecorator;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.config.DefaultGridsets;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.conveyor.Conveyor.CacheResult;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.OutsideCoverageException;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.ExpirationRule;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.geowebcache.layer.meta.MetadataURL;
import org.geowebcache.layer.meta.TileJSON;
import org.geowebcache.layer.meta.VectorLayerMetadata;
import org.geowebcache.locks.MemoryLockProvider;
import org.geowebcache.mime.ApplicationMime;
import org.geowebcache.mime.FormatModifier;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.StorageBroker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class GeoServerTileLayerTest {

    static final Logger LOGGER = Logging.getLogger(GeoServerTileLayerTest.class);
    private static final int MAX_AGE_VALUE = 123;

    private LayerInfoImpl layerInfo;

    private GeoServerTileLayer layerInfoTileLayer;

    private LayerGroupInfoImpl layerGroup;

    private GeoServerTileLayer layerGroupInfoTileLayer;

    private Catalog catalog;

    private GridSetBroker gridSetBroker;

    private GWCConfig defaults;

    private GWC mockGWC;

    private GWCSynchEnv mockGWCSynchEnv;

    private FeatureTypeInfoImpl resource;

    private NamespaceInfoImpl ns;

    @After
    public void tearDown() throws Exception {
        GWC.set(null, null);
        Dispatcher.REQUEST.remove();
    }

    @Before
    public void setUp() throws Exception {
        mockGWC = mock(GWC.class);
        mockGWCSynchEnv = mock(GWCSynchEnv.class);

        MemoryLockProvider lockProvider = new MemoryLockProvider();
        when(mockGWC.getLockProvider()).thenReturn(lockProvider);
        GWC.set(mockGWC, mockGWCSynchEnv);

        final String layerInfoId = "mock-layer-info";

        ns = new NamespaceInfoImpl();
        ns.setPrefix("test");
        ns.setURI("http://goserver.org/test");

        WorkspaceInfo workspaceInfo = new WorkspaceInfoImpl();
        workspaceInfo.setName("workspace");

        DataStoreInfoImpl storeInfo = new DataStoreInfoImpl(null);
        storeInfo.setId("mock-store-info");
        storeInfo.setEnabled(true);
        storeInfo.setWorkspace(workspaceInfo);

        resource = new FeatureTypeInfoImpl(null);
        resource.setStore(storeInfo);
        resource.setId("mock-resource-info");
        resource.setName("MockLayerInfoName");
        resource.setNamespace(ns);
        resource.setTitle("Test resource title");
        resource.setAbstract("Test resource abstract");
        resource.setEnabled(true);
        resource.setDescription("Test resource description");
        resource.setLatLonBoundingBox(new ReferencedEnvelope(-180, -90, 0, 0, WGS84));
        resource.setNativeBoundingBox(new ReferencedEnvelope(-180, -90, 0, 0, WGS84));
        resource.setSRS("EPSG:4326");
        resource.setKeywords(Arrays.asList(new Keyword("kwd1"), new Keyword("kwd2")));

        // add metadata links
        MetadataLinkInfoImpl metadataLinkInfo = new MetadataLinkInfoImpl();
        metadataLinkInfo.setAbout("metadata-about");
        metadataLinkInfo.setContent("metadata-content");
        metadataLinkInfo.setId("metadata-id");
        metadataLinkInfo.setMetadataType("metadata-type");
        metadataLinkInfo.setType("metadata-format");
        resource.setMetadataLinks(Collections.singletonList(metadataLinkInfo));

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("testType");
        builder.setNamespaceURI(ns.getURI());
        builder.setSRS("EPSG:4326");
        builder.add("stringField", String.class);
        builder.add("numberField", Number.class);
        SimpleFeatureType featureType = builder.buildFeatureType();

        ResourcePool resourcePool = mock(ResourcePool.class);
        Style style = mock(Style.class);
        when(style.featureTypeStyles()).thenReturn(Collections.emptyList());
        when(resourcePool.getStyle(any(StyleInfo.class))).thenReturn(style);
        catalog = mock(Catalog.class);
        when(catalog.getResourcePool()).thenReturn(resourcePool);
        when(resourcePool.getFeatureType(eq(resource))).thenReturn(featureType);

        layerInfo = new LayerInfoImpl();
        layerInfo.setId(layerInfoId);
        layerInfo.setResource(resource);
        layerInfo.setEnabled(true);
        layerInfo.setName("MockLayerInfoName");
        layerInfo.setType(PublishedType.VECTOR);
        StyleInfo defaultStyle = new StyleInfoImpl(catalog);
        defaultStyle.setName("default_style");

        layerInfo.setDefaultStyle(defaultStyle);

        StyleInfo alternateStyle1 = new StyleInfoImpl(catalog);
        alternateStyle1.setName("alternateStyle-1");
        StyleInfo alternateStyle2 = new StyleInfoImpl(catalog);
        alternateStyle2.setName("alternateStyle-2");
        Set<StyleInfo> alternateStyles = new HashSet<>(Arrays.asList(alternateStyle1, alternateStyle2));
        LegendInfo legendInfo = new LegendInfoImpl();
        legendInfo.setWidth(150);
        legendInfo.setHeight(200);
        legendInfo.setFormat("image/png");
        legendInfo.setOnlineResource(
                "some-url                                                                                         ");
        alternateStyle2.setLegend(legendInfo);
        layerInfo.setStyles(alternateStyles);

        layerGroup = new LayerGroupInfoImpl();
        final String layerGroupId = "mock-layergroup-id";
        layerGroup.setId(layerGroupId);
        layerGroup.setName("MockLayerGroup");
        layerGroup.setTitle("Group title");
        layerGroup.setAbstract("Group abstract");
        layerGroup.setLayers(Collections.singletonList(layerInfo));

        defaults = GWCConfig.getOldDefaults();

        when(catalog.getLayer(eq(layerInfoId))).thenReturn(layerInfo);
        when(catalog.getLayerGroup(eq(layerGroupId))).thenReturn(layerGroup);

        gridSetBroker = new GridSetBroker(Collections.singletonList(new DefaultGridsets(true, true)));
    }

    @Test
    public void testEnabled() {
        layerInfo.setEnabled(true);
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        assertTrue(layerInfoTileLayer.isEnabled());

        layerInfo.setEnabled(false);
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        assertFalse(layerInfoTileLayer.isEnabled());

        layerInfo.setEnabled(true);
        layerInfoTileLayer.setEnabled(true);
        assertTrue(layerInfoTileLayer.isEnabled());
        assertTrue(layerInfoTileLayer.getInfo().isEnabled());

        layerInfoTileLayer.setConfigErrorMessage("fake error message");
        assertFalse(layerInfoTileLayer.isEnabled());
        layerInfoTileLayer.setConfigErrorMessage(null);

        layerInfoTileLayer.setEnabled(false);
        assertFalse(layerInfoTileLayer.isEnabled());
        assertFalse(layerInfoTileLayer.getInfo().isEnabled());

        layerGroupInfoTileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);
        assertTrue(layerGroupInfoTileLayer.isEnabled());
    }

    @Test
    public void testGetMetaTilingFactors() {

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);

        int[] metaTilingFactors = layerInfoTileLayer.getMetaTilingFactors();
        assertEquals(defaults.getMetaTilingX(), metaTilingFactors[0]);
        assertEquals(defaults.getMetaTilingY(), metaTilingFactors[1]);

        GeoServerTileLayerInfo info = layerInfoTileLayer.getInfo();
        info.setMetaTilingX(1 + defaults.getMetaTilingX());
        info.setMetaTilingY(2 + defaults.getMetaTilingY());

        LegacyTileLayerInfoLoader.save(info, layerInfo.getMetadata());

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        metaTilingFactors = layerInfoTileLayer.getMetaTilingFactors();
        assertEquals(1 + defaults.getMetaTilingX(), metaTilingFactors[0]);
        assertEquals(2 + defaults.getMetaTilingY(), metaTilingFactors[1]);
    }

    @Test
    public void testIsQueryable() {

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);

        when(mockGWC.isQueryable(same(layerInfoTileLayer))).thenReturn(true);
        assertTrue(layerInfoTileLayer.isQueryable());

        when(mockGWC.isQueryable(same(layerInfoTileLayer))).thenReturn(false);
        assertFalse(layerInfoTileLayer.isQueryable());

        verify(mockGWC, times(2)).isQueryable(same(layerInfoTileLayer));
    }

    @Test
    public void testGetName() {

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        assertEquals(tileLayerName(layerInfo), layerInfoTileLayer.getName());

        layerGroupInfoTileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);
        assertEquals(GWC.tileLayerName(layerGroup), layerGroupInfoTileLayer.getName());
    }

    @Test
    public void testGetParameterFilters() {

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        List<ParameterFilter> parameterFilters = layerInfoTileLayer.getParameterFilters();
        assertNotNull(parameterFilters);
        assertEquals(1, parameterFilters.size());
        assertTrue(parameterFilters.get(0) instanceof StyleParameterFilter);
        StyleParameterFilter styleFilter = (StyleParameterFilter) parameterFilters.get(0);
        assertEquals("STYLES", styleFilter.getKey());
        assertEquals("default_style", styleFilter.getDefaultValue());
        assertEquals(
                new HashSet<>(Arrays.asList("default_style", "alternateStyle-1", "alternateStyle-2")),
                new HashSet<>(styleFilter.getLegalValues()));

        // layerInfoTileLayer.getInfo().getCachedStyles().add("alternateStyle-2");
    }

    @Test
    public void testGetDefaultParameterFilters() {

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        Map<String, String> defaultFilters = layerInfoTileLayer.getDefaultParameterFilters();
        assertEquals(1, defaultFilters.size());
        assertEquals("default_style", defaultFilters.get("STYLES"));
    }

    // public void testResetParameterFilters() {
    //
    // layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
    //
    // layerInfoTileLayer.getInfo().cachedStyles().clear();
    // layerInfoTileLayer.getInfo().cachedStyles().add("alternateStyle-2");
    //
    // layerInfoTileLayer.resetParameterFilters();
    // List<ParameterFilter> parameterFilters = layerInfoTileLayer.getParameterFilters();
    // StringParameterFilter styleFilter = (StringParameterFilter) parameterFilters.get(0);
    // assertEquals(new HashSet<String>(Arrays.asList("default_style", "alternateStyle-2")),
    // new HashSet<String>(styleFilter.getLegalValues()));
    //
    // }

    @Test
    public void testGetModifiableParameters() throws GeoWebCacheException {

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        ParameterFilter stylesParamFilter =
                layerInfoTileLayer.getParameterFilters().get(0);
        List<String> legalValues = stylesParamFilter.getLegalValues();

        Map<String, String> requestParams = Collections.singletonMap("sTyLeS", "");
        Map<String, String> modifiedParams = layerInfoTileLayer.getModifiableParameters(requestParams, "UTF-8");
        assertEquals(0, modifiedParams.size());

        for (String legalStyle : legalValues) {
            requestParams = new HashMap<>();
            requestParams.put("sTyLeS", legalStyle);
            modifiedParams = layerInfoTileLayer.getModifiableParameters(requestParams, "UTF-8");
            if (legalStyle.equals(stylesParamFilter.getDefaultValue())) {
                assertEquals(0, modifiedParams.size());
            } else {
                assertEquals(Collections.singletonMap("STYLES", legalStyle), modifiedParams);
            }
        }
    }

    @Test
    public void testGetMetaInformation() {
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        layerGroupInfoTileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);

        LayerMetaInformation metaInformation = layerInfoTileLayer.getMetaInformation();
        assertNotNull(metaInformation);
        String title = metaInformation.getTitle();
        String description = metaInformation.getDescription();
        List<String> keywords = metaInformation.getKeywords();
        assertEquals(layerInfo.getResource().getTitle(), title);
        assertEquals(layerInfo.getResource().getAbstract(), description);
        assertEquals(layerInfo.getResource().getKeywords().size(), keywords.size());
        for (String kw : keywords) {
            assertTrue(layerInfo.getResource().getKeywords().contains(new Keyword(kw)));
        }

        metaInformation = layerGroupInfoTileLayer.getMetaInformation();
        assertNotNull(metaInformation);
        title = metaInformation.getTitle();
        description = metaInformation.getDescription();
        keywords = metaInformation.getKeywords();
        // these properties are missing from LayerGroupInfo interface
        assertEquals("Group title", title);
        assertEquals("Group abstract", description);

        assertEquals(0, keywords.size());
    }

    @Test
    public void testGetStyles() {
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        layerGroupInfoTileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);

        assertEquals("default_style", layerInfoTileLayer.getStyles());
        assertNull(layerGroupInfoTileLayer.getStyles());

        StyleInfo newDefaultStyle = new StyleInfoImpl(null);
        newDefaultStyle.setName("newDefault");
        layerInfo.setDefaultStyle(newDefaultStyle);

        assertEquals("newDefault", layerInfoTileLayer.getStyles());
    }

    @Test
    public void testGetGridSubsets() throws Exception {
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        Set<String> gridSubsets = layerInfoTileLayer.getGridSubsets();
        assertNotNull(gridSubsets);
        assertEquals(2, gridSubsets.size());

        Set<XMLGridSubset> subsets = layerInfoTileLayer.getInfo().getGridSubsets();
        subsets.clear();
        XMLGridSubset xmlGridSubset = new XMLGridSubset();
        xmlGridSubset.setGridSetName("EPSG:900913");
        subsets.add(xmlGridSubset);
        LegacyTileLayerInfoLoader.save(layerInfoTileLayer.getInfo(), layerInfo.getMetadata());
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);

        gridSubsets = layerInfoTileLayer.getGridSubsets();
        assertNotNull(gridSubsets);
        assertEquals(1, gridSubsets.size());

        layerGroup.setBounds(layerInfo.getResource().getLatLonBoundingBox());
        layerGroupInfoTileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);
        gridSubsets = layerGroupInfoTileLayer.getGridSubsets();
        assertNotNull(gridSubsets);
        assertEquals(2, gridSubsets.size());
    }

    @Test
    public void testGetGridSubsetsDynamic() throws Exception {
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);

        GridSubset subset = layerInfoTileLayer.getGridSubset("EPSG:4326");

        assertThat(subset, instanceOf(DynamicGridSubset.class));

        assertThat(subset, hasProperty("originalExtent", hasProperty("minX", closeTo(-180.0, 0.0000001))));

        layerInfoTileLayer.removeGridSubset("EPSG:4326");
        layerInfoTileLayer.addGridSubset(subset);

        resource.setLatLonBoundingBox(new ReferencedEnvelope(-90, -90, 0, 0, WGS84));
        resource.setNativeBoundingBox(new ReferencedEnvelope(-90, -90, 0, 0, WGS84));

        GridSubset subset2 = layerInfoTileLayer.getGridSubset("EPSG:4326");

        // the extent should be that of resource
        assertThat(subset2, hasProperty("originalExtent", hasProperty("minX", closeTo(-90.0, 0.0000001))));
    }

    @Test
    public void testGetGridSubsetsStatic() throws Exception {
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);

        GridSubset subset = layerInfoTileLayer.getGridSubset("EPSG:4326");

        assertThat(subset, instanceOf(DynamicGridSubset.class));

        assertThat(subset, hasProperty("originalExtent", hasProperty("minX", closeTo(-180.0, 0.0000001))));

        layerInfoTileLayer.removeGridSubset("EPSG:4326");
        layerInfoTileLayer.addGridSubset(new GridSubset(subset)); // Makes the dynamic extent static

        resource.setLatLonBoundingBox(new ReferencedEnvelope(-90, -90, 0, 0, WGS84));
        resource.setNativeBoundingBox(new ReferencedEnvelope(-90, -90, 0, 0, WGS84));

        GridSubset subset2 = layerInfoTileLayer.getGridSubset("EPSG:4326");

        // the extent should not change with that of resource
        assertThat(subset2, hasProperty("originalExtent", hasProperty("minX", closeTo(-180.0, 0.0000001))));
    }

    @Test
    public void testGridSubsetBoundsClippedToTargetCrsAreaOfValidity() throws Exception {

        CoordinateReferenceSystem nativeCrs = CRS.decode("EPSG:4326", true);
        ReferencedEnvelope nativeBounds = new ReferencedEnvelope(-180, 180, -90, 90, nativeCrs);
        layerGroup.setBounds(nativeBounds);
        defaults.getDefaultCachingGridSetIds().clear();
        defaults.getDefaultCachingGridSetIds().add("EPSG:900913");
        layerGroupInfoTileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);

        // force building and setting the bounds to the saved representation
        layerGroupInfoTileLayer.getGridSubset("EPSG:900913");

        XMLGridSubset savedSubset =
                layerGroupInfoTileLayer.getInfo().getGridSubsets().iterator().next();

        BoundingBox gridSubsetExtent = savedSubset.getExtent();
        BoundingBox expected = gridSetBroker.getWorldEpsg3857().getOriginalExtent();
        // don't use equals(), it uses an equality threshold we want to avoid here
        double threshold = 1E-16;
        assertTrue( // NOPMD
                "Expected " + expected + ", got " + gridSubsetExtent, expected.equals(gridSubsetExtent, threshold));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetFeatureInfo() throws Exception {

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);

        ConveyorTile convTile = new ConveyorTile(null, null, null, null);
        convTile.setTileLayer(layerInfoTileLayer);
        convTile.setMimeType(MimeType.createFromFormat("image/png"));
        convTile.setGridSetId("EPSG:4326");
        convTile.servletReq = new MockHttpServletRequest();
        BoundingBox bbox = new BoundingBox(0, 0, 10, 10);

        Resource mockResult = mock(Resource.class);
        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        when(mockGWC.dispatchOwsRequest(argument.capture(), any())).thenReturn(mockResult);

        Resource result = layerInfoTileLayer.getFeatureInfo(convTile, bbox, 100, 100, 50, 50);
        assertSame(mockResult, result);

        final Map<String, String> capturedParams = argument.getValue();

        assertEquals("image/png", capturedParams.get("INFO_FORMAT"));
        assertEquals("0.0,0.0,10.0,10.0", capturedParams.get("BBOX"));
        assertEquals("test:MockLayerInfoName", capturedParams.get("QUERY_LAYERS"));
        assertEquals("WMS", capturedParams.get("SERVICE"));
        assertEquals("100", capturedParams.get("HEIGHT"));
        assertEquals("100", capturedParams.get("WIDTH"));
        assertEquals("GetFeatureInfo", capturedParams.get("REQUEST"));
        assertEquals("default_style", capturedParams.get("STYLES"));
        assertEquals("SE_XML", capturedParams.get("EXCEPTIONS"));
        assertEquals("1.1.1", capturedParams.get("VERSION"));
        assertEquals("image/png", capturedParams.get("FORMAT"));
        assertEquals("test:MockLayerInfoName", capturedParams.get("LAYERS"));
        assertEquals("EPSG:4326", capturedParams.get("SRS"));
        assertEquals("50", capturedParams.get("X"));
        assertEquals("50", capturedParams.get("Y"));

        verify(mockGWC, times(1)).dispatchOwsRequest(any(), any());

        when(mockGWC.dispatchOwsRequest(any(), any())).thenThrow(new RuntimeException("mock exception"));
        try {
            layerInfoTileLayer.getFeatureInfo(convTile, bbox, 100, 100, 50, 50);
            fail("Expected GeoWebCacheException");
        } catch (GeoWebCacheException e) {
            assertTrue(true);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetTileJSON() throws Exception {

        layerInfoTileLayer = new GeoServerTileLayer(
                catalog, layerInfo.getId(), gridSetBroker, TileLayerInfoUtil.loadOrCreate(layerInfo, defaults));

        ConveyorTile convTile = new ConveyorTile(null, null, null, null);
        convTile.setTileLayer(layerInfoTileLayer);
        convTile.setMimeType(ApplicationMime.mapboxVector);
        convTile.setGridSetId("EPSG:900913");
        convTile.servletReq = new MockHttpServletRequest();

        Resource mockResult = mock(Resource.class);
        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        when(mockGWC.dispatchOwsRequest(argument.capture(), any())).thenReturn(mockResult);

        TileJSON result = layerInfoTileLayer.getTileJSON();
        assertEquals("test:MockLayerInfoName", result.getName());
        assertEquals("Test resource abstract", result.getDescription());
        assertArrayEquals(new double[] {-180.0d, 0.0d, -90.0d, 0.0d}, result.getBounds(), 1E-6);

        List<VectorLayerMetadata> layers = result.getLayers();
        assertEquals(1, layers.size());
        VectorLayerMetadata layer = layers.get(0);
        assertEquals("MockLayerInfoName", layer.getId());

        Map<String, String> fields = layer.getFields();
        assertEquals("String", fields.get("stringField"));
        assertEquals("Number", fields.get("numberField"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetTileJSONLayerGroup() throws Exception {
        WorkspaceInfo workspaceInfo = new WorkspaceInfoImpl();
        workspaceInfo.setName("workspace");

        DataStoreInfoImpl storeInfo = new DataStoreInfoImpl(null);
        storeInfo.setId("mock-store-info");
        storeInfo.setEnabled(true);
        storeInfo.setWorkspace(workspaceInfo);

        ResourcePool resourcePool = mock(ResourcePool.class);
        catalog = mock(Catalog.class);
        when(catalog.getResourcePool()).thenReturn(resourcePool);

        LayerInfoImpl layerInfo1 = createMockVectorLayer(storeInfo, resourcePool, 1);
        LayerInfoImpl layerInfo2 = createMockVectorLayer(storeInfo, resourcePool, 2);

        LayerGroupInfoImpl layerGroupVectors = new LayerGroupInfoImpl();
        final String layerGroupVectorsId = "mock-layergroup-vectors-id";
        layerGroupVectors.setId(layerGroupVectorsId);
        layerGroupVectors.setName("MockLayerGroupVectors");
        layerGroupVectors.setTitle("Group title");
        layerGroupVectors.setAbstract("Group abstract");
        layerGroupVectors.setLayers(Arrays.asList(layerInfo1, layerInfo2));

        when(catalog.getLayerGroup(eq(layerGroupVectorsId))).thenReturn(layerGroupVectors);

        CoordinateReferenceSystem nativeCrs = CRS.decode("EPSG:4326", true);
        ReferencedEnvelope nativeBounds = new ReferencedEnvelope(-180, 180, -90, 90, nativeCrs);
        layerGroupVectors.setBounds(nativeBounds);
        GeoServerTileLayer layerGroupInfoVectorTileLayer = new GeoServerTileLayer(
                catalog,
                layerGroupVectors.getId(),
                gridSetBroker,
                TileLayerInfoUtil.loadOrCreate(layerGroupVectors, defaults));

        ConveyorTile convTile = new ConveyorTile(null, null, null, null);
        convTile.setTileLayer(layerGroupInfoVectorTileLayer);
        convTile.setMimeType(ApplicationMime.mapboxVector);
        convTile.setGridSetId("EPSG:900913");
        convTile.servletReq = new MockHttpServletRequest();

        Resource mockResult = mock(Resource.class);
        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        when(mockGWC.dispatchOwsRequest(argument.capture(), any())).thenReturn(mockResult);

        TileJSON result = layerGroupInfoVectorTileLayer.getTileJSON();
        assertEquals("MockLayerGroupVectors", result.getName());
        assertEquals("Group abstract", result.getDescription());
        List<VectorLayerMetadata> layers = result.getLayers();
        assertEquals(2, layers.size());
        int id = 1;
        for (VectorLayerMetadata vectorLayerMetadata : layers) {
            assertEquals("MockLayerInfoName" + id, vectorLayerMetadata.getId());
            Map<String, String> fields = vectorLayerMetadata.getFields();
            assertEquals("String", fields.get("stringField" + id));
            assertEquals("Number", fields.get("numberField" + id));
            id++;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetTileJSONLayerGroupMixed() throws Exception {
        WorkspaceInfo workspaceInfo = new WorkspaceInfoImpl();
        workspaceInfo.setName("workspace");

        DataStoreInfoImpl storeInfo = new DataStoreInfoImpl(null);
        storeInfo.setId("mock-store-info");
        storeInfo.setEnabled(true);
        storeInfo.setWorkspace(workspaceInfo);

        ResourcePool resourcePool = mock(ResourcePool.class);
        catalog = mock(Catalog.class);
        when(catalog.getResourcePool()).thenReturn(resourcePool);

        LayerInfoImpl layerInfo1 = createMockVectorLayer(storeInfo, resourcePool, 1);
        LayerInfoImpl layerInfo = new LayerInfoImpl();

        CoverageInfoImpl resource = new CoverageInfoImpl(null);
        resource.setId("mock-resource-info");
        resource.setName("MockLayerInfoName");
        resource.setNamespace(ns);
        resource.setEnabled(true);

        final String layerInfoId = "mock-layer-info";
        layerInfo.setId(layerInfoId);
        layerInfo.setResource(resource);
        layerInfo.setEnabled(true);
        layerInfo.setName("MockLayerInfoName");
        layerInfo.setType(PublishedType.RASTER);
        when(catalog.getLayer(eq(layerInfoId))).thenReturn(layerInfo);

        LayerGroupInfoImpl layerGroupMixed = new LayerGroupInfoImpl();
        final String layerGroupVectorsId = "mock-layergroup-mixed-id";
        layerGroupMixed.setId(layerGroupVectorsId);
        layerGroupMixed.setName("MockLayerGroupMixed");
        layerGroupMixed.setTitle("Group title");
        layerGroupMixed.setAbstract("Group abstract");
        layerGroupMixed.setLayers(Arrays.asList(layerInfo1, layerInfo));

        when(catalog.getLayerGroup(eq(layerGroupVectorsId))).thenReturn(layerGroupMixed);

        CoordinateReferenceSystem nativeCrs = CRS.decode("EPSG:4326", true);
        ReferencedEnvelope nativeBounds = new ReferencedEnvelope(-180, 180, -90, 90, nativeCrs);
        layerGroupMixed.setBounds(nativeBounds);
        GeoServerTileLayer layerGroupInfoVectorTileLayer = new GeoServerTileLayer(
                catalog,
                layerGroupMixed.getId(),
                gridSetBroker,
                TileLayerInfoUtil.loadOrCreate(layerGroupMixed, defaults));

        ConveyorTile convTile = new ConveyorTile(null, null, null, null);
        convTile.setTileLayer(layerGroupInfoVectorTileLayer);
        convTile.setMimeType(MimeType.createFromFormat("image/png"));
        convTile.setGridSetId("EPSG:900913");
        convTile.servletReq = new MockHttpServletRequest();

        Resource mockResult = mock(Resource.class);
        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        when(mockGWC.dispatchOwsRequest(argument.capture(), any())).thenReturn(mockResult);

        TileJSON result = layerGroupInfoVectorTileLayer.getTileJSON();
        assertEquals("MockLayerGroupMixed", result.getName());
        assertEquals("Group abstract", result.getDescription());
        List<VectorLayerMetadata> layers = result.getLayers();

        // No vectorLayerMetadata has been produced with mixed layergroup
        assertEquals(0, layers.size());
    }

    private LayerInfoImpl createMockVectorLayer(DataStoreInfoImpl storeInfo, ResourcePool resourcePool, int id)
            throws IOException {
        LayerInfoImpl layerInfo = new LayerInfoImpl();

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("testType");
        builder.setNamespaceURI("http://goserver.org/test");
        builder.setSRS("EPSG:4326");
        builder.add("stringField" + id, String.class);
        builder.add("numberField" + id, Number.class);
        SimpleFeatureType featureType = builder.buildFeatureType();

        FeatureTypeInfoImpl resource = new FeatureTypeInfoImpl(null);
        resource.setStore(storeInfo);
        resource.setId("mock-resource-info");
        resource.setName("MockLayerInfoName" + id);
        resource.setNamespace(ns);
        resource.setTitle("Test resource title");
        resource.setAbstract("Test resource abstract");
        resource.setEnabled(true);
        resource.setDescription("Test resource description");
        resource.setLatLonBoundingBox(new ReferencedEnvelope(-180, -90, 0, 0, WGS84));
        resource.setNativeBoundingBox(new ReferencedEnvelope(-180, -90, 0, 0, WGS84));
        resource.setSRS("EPSG:4326");

        final String layerInfoId = "mock-layer-info" + id;
        layerInfo.setId(layerInfoId);
        layerInfo.setResource(resource);
        layerInfo.setEnabled(true);
        layerInfo.setName("MockLayerInfoName" + id);
        layerInfo.setType(PublishedType.VECTOR);
        when(resourcePool.getFeatureType(eq(resource))).thenReturn(featureType);
        when(catalog.getLayer(eq(layerInfoId))).thenReturn(layerInfo);

        return layerInfo;
    }

    @Test
    public void testGetTilePreconditions() throws Exception {

        StorageBroker storageBroker = mock(StorageBroker.class);

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);

        MockHttpServletRequest servletReq = new MockHttpServletRequest();
        HttpServletResponse servletResp = new MockHttpServletResponse();

        ConveyorTile tile = new ConveyorTile(storageBroker, layerInfoTileLayer.getName(), servletReq, servletResp);
        tile.setMimeType(MimeType.createFromFormat("image/gif"));
        try {
            layerInfoTileLayer.getTile(tile);
            fail("Expected exception, requested mime is invalid for the layer");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("is not a supported format"));
        }

        tile.setMimeType(MimeType.createFromFormat("image/png"));
        tile.setGridSetId("EPSG:2003");
        try {
            layerInfoTileLayer.getTile(tile);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("gridset not found"));
        }
        // layer bounds (in WGS84) is -180, -90, 0, 0
        long[][] outsideTiles = {{0, 1, 1}, {1, 1, 1}, {1, 0, 1}};

        for (long[] tileIndex : outsideTiles) {
            MimeType mimeType = MimeType.createFromFormat("image/png");
            tile = new ConveyorTile(
                    storageBroker,
                    layerInfoTileLayer.getName(),
                    "EPSG:900913",
                    tileIndex,
                    mimeType,
                    null,
                    servletReq,
                    servletResp);
            try {
                layerInfoTileLayer.getTile(tile);
                fail("Expected outside coverage exception");
            } catch (OutsideCoverageException e) {
                assertTrue(true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected class GetTileMockTester {

        public GeoServerTileLayer prepareTileLayer() throws Exception {
            Resource mockResult = mock(Resource.class);
            ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
            when(mockGWC.dispatchOwsRequest(argument.capture(), any())).thenReturn(mockResult);

            return new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        }

        public RenderedImageMap prepareFakeMap() {
            return prepareFakeMap(256, 256);
        }

        public RenderedImageMap prepareFakeMap(int width, int height) {
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            return new RenderedImageMap(new WMSMapContent(), image, "image/png");
        }

        public ConveyorTile prepareConveyorTile(GeoServerTileLayer tileLayer, long[] tileIndex) throws Exception {

            MockHttpServletRequest servletReq = new MockHttpServletRequest();
            HttpServletResponse servletResp = new MockHttpServletResponse();

            RenderedImageMapResponse fakeResponseEncoder = mock(RenderedImageMapResponse.class);
            MimeType mimeType = MimeType.createFromFormat("image/png");
            when(mockGWC.getResponseEncoder(eq(mimeType), any())).thenReturn(fakeResponseEncoder);

            StorageBroker storageBroker = mock(StorageBroker.class);
            when(storageBroker.get(any())).thenReturn(false);

            return new ConveyorTile(
                    storageBroker,
                    tileLayer.getName(),
                    "EPSG:4326",
                    tileIndex,
                    mimeType,
                    null,
                    servletReq,
                    servletResp);
        }

        /** By default, checks that the tile has been cached permanently */
        protected void performAssertions(ConveyorTile result) throws Exception {

            assertNotNull(result);
            assertNotNull(result.getBlob());
            assertEquals(CacheResult.MISS, result.getCacheResult());
            assertEquals(200, result.getStatus());

            verify(result.getStorageBroker(), atLeastOnce()).get(any());
            verify(result.getStorageBroker(), times(1)).put(Mockito.any());
            verify(result.getStorageBroker(), never()).putTransient(Mockito.any());
            verify(mockGWC, times(1)).getResponseEncoder(eq(result.getMimeType()), isA(RenderedImageMap.class));
        }
    }

    @Test
    public void testGetTile() throws Exception {
        long[] tileIndex = {0, 0, 0};
        GetTileMockTester tester = new GetTileMockTester();
        GeoServerTileLayer tileLayer = tester.prepareTileLayer();
        ConveyorTile conveyorTile = tester.prepareConveyorTile(tileLayer, tileIndex);
        GeoServerTileLayer.WEB_MAP.set(tester.prepareFakeMap());
        ConveyorTile result = tileLayer.getTile(conveyorTile);
        tester.performAssertions(result);
    }

    private FeatureTypeInfo getMockTimeFeatureType() {
        FeatureTypeInfo resource = mock(FeatureTypeInfo.class);
        MetadataMap metadata = new MetadataMap();
        DimensionInfoImpl dimension = new DimensionInfoImpl();
        dimension.setUnits("mockUnit");
        metadata.put(ResourceInfo.TIME, dimension);
        when(resource.getMetadata()).thenReturn(metadata);
        return resource;
    }

    @Test
    public void testGetTileWarningNoSkip() throws Exception {
        // no skips setup, will cache permanently
        long[] tileIndex = {0, 0, 0};
        GetTileMockTester tester = new GetTileMockTester();
        GeoServerTileLayer tileLayer = tester.prepareTileLayer();

        tileLayer.getInfo().setCacheWarningSkips(Collections.emptySet());

        FeatureTypeInfo resource = getMockTimeFeatureType();
        HTTPWarningAppender.addWarning(DimensionWarning.defaultValue(resource, "time", new Date()));

        ConveyorTile conveyorTile = tester.prepareConveyorTile(tileLayer, tileIndex);
        GeoServerTileLayer.WEB_MAP.set(tester.prepareFakeMap());
        ConveyorTile result = tileLayer.getTile(conveyorTile);
        tester.performAssertions(result);
    }

    @Test
    public void testGetTileWarningMismatchedSkip() throws Exception {
        // skips on nearest, gets a warning as default, caches permanently
        long[] tileIndex = {0, 0, 0};
        GetTileMockTester tester = new GetTileMockTester();
        GeoServerTileLayer tileLayer = tester.prepareTileLayer();

        tileLayer.getInfo().setCacheWarningSkips(Collections.singleton(WarningType.Nearest));

        FeatureTypeInfo resource = getMockTimeFeatureType();
        HTTPWarningAppender.addWarning(DimensionWarning.defaultValue(resource, "time", new Date()));

        ConveyorTile conveyorTile = tester.prepareConveyorTile(tileLayer, tileIndex);
        GeoServerTileLayer.WEB_MAP.set(tester.prepareFakeMap());
        ConveyorTile result = tileLayer.getTile(conveyorTile);
        tester.performAssertions(result);
    }

    @Test
    public void testGetTileWarningSkip() throws Exception {
        // skips on nearest and default, gets a warning as default, no persistent cache occurs
        long[] tileIndex = {0, 0, 0};
        GetTileMockTester tester = new GetTileMockTester();
        GeoServerTileLayer tileLayer = tester.prepareTileLayer();

        tileLayer
                .getInfo()
                .setCacheWarningSkips(new HashSet<>(Arrays.asList(WarningType.Nearest, WarningType.Default)));

        FeatureTypeInfo resource = getMockTimeFeatureType();
        HTTPWarningAppender.addWarning(DimensionWarning.defaultValue(resource, "time", new Date()));

        ConveyorTile conveyorTile = tester.prepareConveyorTile(tileLayer, tileIndex);
        GeoServerTileLayer.WEB_MAP.set(tester.prepareFakeMap());
        ConveyorTile result = tileLayer.getTile(conveyorTile);

        // check only transient caching has been performed
        verify(result.getStorageBroker(), atLeastOnce()).get(any());
        verify(result.getStorageBroker(), never()).put(Mockito.any());
        verify(result.getStorageBroker(), times(1)).putTransient(Mockito.any());
        verify(mockGWC, times(1)).getResponseEncoder(eq(result.getMimeType()), isA(RenderedImageMap.class));
    }

    @Test
    public void testGetTileWithMetaTilingExecutor() throws Exception {
        GetTileMockTester tester = new GetTileMockTester();
        GeoServerTileLayer tileLayer = tester.prepareTileLayer();

        ExecutorService executorServiceSpy = spy(Executors.newFixedThreadPool(2));
        when(mockGWC.getMetaTilingExecutor()).thenReturn(executorServiceSpy);

        // Ensure enough valid coverage to support metatiling
        resource.setLatLonBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90, WGS84));
        resource.setNativeBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90, WGS84));

        int zoomLevel = 4; // pick a zoom level that has enough tiles for at least one meta-tile
        long[] coverage = tileLayer.getGridSubset("EPSG:4326").getCoverage(zoomLevel); // {minx,miny,max,maxy,zoomlevel}

        long[] tileIndex = {coverage[0], coverage[1], zoomLevel};
        ConveyorTile conveyorTile = tester.prepareConveyorTile(tileLayer, tileIndex);
        Dispatcher.REQUEST.set(new Request());

        GeoServerTileLayer.WEB_MAP.set(tester.prepareFakeMap(1024, 1024));
        ConveyorTile result = tileLayer.getTile(conveyorTile);

        assertNotNull(result);
        assertNotNull(result.getBlob());
        assertEquals(CacheResult.MISS, result.getCacheResult());
        assertEquals(200, result.getStatus());

        executorServiceSpy.awaitTermination(2, TimeUnit.SECONDS);

        // 1 async threads: 1 to save the requested tile, 15 for encoding/saving additional tiles,
        // and 1 to dispose of metatile (one of the tiles is performed on main thread)
        verify(executorServiceSpy, times(17)).execute(any());

        // 16 tiles to put in storage
        verify(result.getStorageBroker(), times(16)).put(Mockito.any());
    }

    /**
     * If there is a metatiling executor service configured, but a conveyor tile comes in that is missing the "servlet
     * request" object, this is not a user-initiated request and is likely a seed attempt, and therefore it should
     * ignore the executor service and encode/save all tiles from a metatile on the main thread.
     */
    @Test
    public void testGetTileWithMetaTilingExecutorButNoDispatcherRequest() throws Exception {
        GetTileMockTester tester = new GetTileMockTester();
        GeoServerTileLayer tileLayer = tester.prepareTileLayer();

        ExecutorService executorServiceSpy = spy(Executors.newFixedThreadPool(2));
        when(mockGWC.getMetaTilingExecutor()).thenReturn(executorServiceSpy);

        // Ensure enough valid coverage to support metatiling
        resource.setLatLonBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90, WGS84));
        resource.setNativeBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90, WGS84));

        int zoomLevel = 4; // pick a zoom level that has enough tiles for at least one meta-tile
        long[] coverage = tileLayer.getGridSubset("EPSG:4326").getCoverage(zoomLevel); // {minx,miny,max,maxy,zoomlevel}

        long[] tileIndex = {coverage[0], coverage[1], zoomLevel};
        ConveyorTile conveyorTile = tester.prepareConveyorTile(tileLayer, tileIndex);

        Dispatcher.REQUEST.remove();

        GeoServerTileLayer.WEB_MAP.set(tester.prepareFakeMap(1024, 1024));
        ConveyorTile result = tileLayer.getTile(conveyorTile);

        assertNotNull(result);
        assertNotNull(result.getBlob());
        assertEquals(CacheResult.MISS, result.getCacheResult());
        assertEquals(200, result.getStatus());

        executorServiceSpy.awaitTermination(2, TimeUnit.SECONDS);

        // There should be no executions on the executor service (it should all be on main thread)
        verify(executorServiceSpy, times(0)).execute(any());

        // 16 tiles to put in storage
        verify(result.getStorageBroker(), times(16)).put(Mockito.any());
    }

    @Test
    public void testGetTileWithNullMetaTilingExecutor() throws Exception {
        GetTileMockTester tester = new GetTileMockTester();
        GeoServerTileLayer tileLayer = tester.prepareTileLayer();

        when(mockGWC.getMetaTilingExecutor()).thenReturn(null);

        // Ensure enough valid coverage to support metatiling
        resource.setLatLonBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90, WGS84));
        resource.setNativeBoundingBox(new ReferencedEnvelope(-180, 180, -90, 90, WGS84));

        int zoomLevel = 4; // pick a zoom level that has enough tiles for at least one meta-tile
        long[] coverage = tileLayer.getGridSubset("EPSG:4326").getCoverage(zoomLevel); // {minx,miny,max,maxy,zoomlevel}

        long[] tileIndex = {coverage[0], coverage[1], zoomLevel};
        ConveyorTile conveyorTile = tester.prepareConveyorTile(tileLayer, tileIndex);
        GeoServerTileLayer.WEB_MAP.set(tester.prepareFakeMap(1024, 1024));
        ConveyorTile result = tileLayer.getTile(conveyorTile);

        assertNotNull(result);
        assertNotNull(result.getBlob());
        assertEquals(CacheResult.MISS, result.getCacheResult());
        assertEquals(200, result.getStatus());

        // 16 storage puts instead of the typical 1
        verify(result.getStorageBroker(), times(16)).put(Mockito.any());
    }

    /** Test expire web cache without any setup of LayerInfo resource. */
    @Test
    public void testExpireClientsDisabledLayer() {
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        int expire = layerInfoTileLayer.getExpireClients(0);

        assertEquals(0, expire);
    }

    /** Test expire web cache with metadata value of LayerInfo resource. */
    @Test
    public void testExpireClientsEnabledLayer() {
        ((ResourceInfoImpl) layerInfo.getResource()).setMetadata(getCachingEnabledMetadata(MAX_AGE_VALUE));
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        int expire = layerInfoTileLayer.getExpireClients(0);

        assertEquals(MAX_AGE_VALUE, expire);
    }

    /** Test expire web cache without any setup LayerGroup. */
    @Test
    public void testExpireClientsDisabledLayerGroup() {
        layerInfoTileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);
        int expire = layerInfoTileLayer.getExpireClients(0);
        assertEquals(0, expire);
    }

    /** Test expire web cache with metadata value of LayerGroup resource. No setup of LayerInfo expiration. */
    @Test
    public void testExpireClientsEnabledLayerGroup() {
        layerGroup.setMetadata(getCachingEnabledMetadata(MAX_AGE_VALUE));
        layerInfoTileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);
        int expire = layerInfoTileLayer.getExpireClients(0);

        assertEquals(MAX_AGE_VALUE, expire);
    }

    /**
     * Test expire web cache with metadata value of LayerGroup resource. Use Layer Group HTTP configuration even if any
     * of lyaers in lyaer group has lower max age value.
     */
    @Test
    public void testExpireClientsEnabledLayerGroupLayerInfoLower() {
        ((ResourceInfoImpl) layerInfo.getResource()).setMetadata(getCachingEnabledMetadata(MAX_AGE_VALUE - 1));
        layerGroup.setMetadata(getCachingEnabledMetadata(MAX_AGE_VALUE));
        layerInfoTileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);
        int expire = layerInfoTileLayer.getExpireClients(0);

        assertEquals(MAX_AGE_VALUE, expire);
    }

    /** Test expire web cache with metadata value of LayerGroup resource. Set higher LayerInfo expiration. */
    @Test
    public void testExpireClientsEnabledLayerGroupLayerInfoHigher() {
        ((ResourceInfoImpl) layerInfo.getResource()).setMetadata(getCachingEnabledMetadata(MAX_AGE_VALUE + 1));
        layerGroup.setMetadata(getCachingEnabledMetadata(MAX_AGE_VALUE));
        layerInfoTileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);
        int expire = layerInfoTileLayer.getExpireClients(0);

        assertEquals(MAX_AGE_VALUE, expire);
    }

    private MetadataMap getCachingEnabledMetadata(int maxAgeValue) {
        Map<String, Serializable> mapItems = new HashMap<>();
        mapItems.put(ResourceInfo.CACHING_ENABLED, Boolean.TRUE);
        mapItems.put(ResourceInfo.CACHE_AGE_MAX, maxAgeValue);

        return new MetadataMap(mapItems);
    }

    @Test
    public void testGetMimeTypes() throws Exception {

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        List<MimeType> mimeTypes = layerInfoTileLayer.getMimeTypes();
        assertEquals(defaults.getDefaultOtherCacheFormats().size(), mimeTypes.size());

        layerInfoTileLayer.getInfo().getMimeFormats().clear();
        layerInfoTileLayer.getInfo().getMimeFormats().add("image/gif");

        mimeTypes = layerInfoTileLayer.getMimeTypes();
        assertEquals(1, mimeTypes.size());
        assertEquals(MimeType.createFromFormat("image/gif"), mimeTypes.get(0));
    }

    @Test
    public void testTileExpirationList() {
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);

        List<ExpirationRule> list = new ArrayList<>();
        list.add(new ExpirationRule(0, 10));
        list.add(new ExpirationRule(10, 20));

        layerInfoTileLayer.getInfo().setExpireCacheList(list);

        assertEquals(10, layerInfoTileLayer.getExpireCache(0));
        assertEquals(10, layerInfoTileLayer.getExpireCache(9));
        assertEquals(20, layerInfoTileLayer.getExpireCache(10));
        assertEquals(20, layerInfoTileLayer.getExpireCache(15));

        assertEquals(0, layerInfoTileLayer.getExpireCache(-1));
    }

    @Test
    public void testCacheExpiration() {
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        assertEquals(0, layerInfoTileLayer.getInfo().getExpireCache());
        layerInfoTileLayer.getInfo().setExpireCache(40);
        assertEquals(40, layerInfoTileLayer.getInfo().getExpireCache());
    }

    @Test
    public void testAdvertised() {
        // Testing the advertised parameter
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        assertTrue(layerInfoTileLayer.isAdvertised());
    }

    @Test
    public void testTransient() {
        // Testing the transient parameter
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        assertFalse(layerInfoTileLayer.isTransientLayer());
    }

    @Test
    public void testGetPublishedInfo() {
        // Checking that the getLayerInfo and getLayerGroupInfo methods
        // returns a not null object
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        assertThat(layerInfoTileLayer.getPublishedInfo(), instanceOf(LayerInfo.class));

        layerGroupInfoTileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);
        assertThat(layerGroupInfoTileLayer.getPublishedInfo(), instanceOf(LayerGroupInfo.class));
    }

    @Test
    public void testGetLayerNameForGetCapabilitiesRequest() throws NoSuchFieldException, IllegalAccessException {
        // workspace namespace
        NamespaceInfo nameSpaceA = new NamespaceInfoImpl();
        nameSpaceA.setPrefix("workspace-a");
        nameSpaceA.setURI("http://goserver.org/test");
        // create the workspace
        WorkspaceInfo workspaceA = new WorkspaceInfoImpl();
        workspaceA.setName("workspace-a");
        // register the workspace in catalog
        when(catalog.getWorkspaceByName("workspace-a")).thenReturn(workspaceA);
        // layer resource
        FeatureTypeInfoImpl resourceA = new FeatureTypeInfoImpl(null);
        resourceA.setNamespace(nameSpaceA);
        // create the layer
        LayerInfoImpl layerA = new LayerInfoImpl();
        layerA.setResource(resourceA);
        layerA.setName("layer-a");
        layerA.setId("layer-a");
        // register the layer in catalog
        when(catalog.getLayer("layer-a")).thenReturn(layerA);
        // creating a layer group without workspace
        LayerGroupInfoImpl layerGroupA = new LayerGroupInfoImpl();
        layerGroupA.setName("random-prefix:layer-group-a");
        layerGroupA.setId("layer-group-a");
        layerGroupA.setLayers(Collections.singletonList(layerA));
        // register the layer group in catalog
        when(catalog.getLayerGroup("layer-group-a")).thenReturn(layerGroupA);
        // creating the tiled layers
        GeoServerTileLayer tileLayerA = new GeoServerTileLayer(layerA, defaults, gridSetBroker);
        GeoServerTileLayer tileLayerB = new GeoServerTileLayer(layerGroupA, defaults, gridSetBroker);
        // setting the catalog in both tile layers using reflection
        Field catalogField = GeoServerTileLayer.class.getDeclaredField("catalog");
        catalogField.setAccessible(true);
        catalogField.set(tileLayerA, catalog);
        catalogField.set(tileLayerB, catalog);

        // no local workspace, no gwc operation
        GwcServiceDispatcherCallback.GWC_OPERATION.remove();
        assertThat(tileLayerA.getName(), is("workspace-a:layer-a"));
        assertThat(tileLayerB.getName(), is("random-prefix:layer-group-a"));

        // no local workspace, some gwc operation
        GwcServiceDispatcherCallback.GWC_OPERATION.set("some-operation");
        assertThat(tileLayerA.getName(), is("workspace-a:layer-a"));
        assertThat(tileLayerB.getName(), is("random-prefix:layer-group-a"));

        // no local workspace, get capabilities gwc operation
        GwcServiceDispatcherCallback.GWC_OPERATION.set("GetCapabilities");
        assertThat(tileLayerA.getName(), is("workspace-a:layer-a"));
        assertThat(tileLayerB.getName(), is("random-prefix:layer-group-a"));

        try {
            // setting a local workspace (workspace-a)
            LocalWorkspace.set(workspaceA);

            // local workspace, no gwc operation
            GwcServiceDispatcherCallback.GWC_OPERATION.remove();
            assertThat(tileLayerA.getName(), is("workspace-a:layer-a"));
            assertThat(tileLayerB.getName(), is("random-prefix:layer-group-a"));

            // local workspace, some gwc operation
            GwcServiceDispatcherCallback.GWC_OPERATION.set("some-operation");
            assertThat(tileLayerA.getName(), is("workspace-a:layer-a"));
            assertThat(tileLayerB.getName(), is("random-prefix:layer-group-a"));

            // local workspace, get capabilities gwc operation
            GwcServiceDispatcherCallback.GWC_OPERATION.set("GetCapabilities");
            assertThat(tileLayerA.getName(), is("layer-a"));
            assertThat(tileLayerB.getName(), is("random-prefix:layer-group-a"));
        } finally {
            // cleaning
            LocalWorkspace.remove();
        }
    }

    @Test
    public void testGetMetadataUrlsFromLayer() throws MalformedURLException {
        setupUrlContext();
        // create a tile layer using a layer
        GeoServerTileLayer tileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        List<MetadataURL> metadata = tileLayer.getMetadataURLs();
        assertThat(metadata.size(), is(1));
        assertThat(metadata.get(0).getType(), is("metadata-type"));
        assertThat(metadata.get(0).getFormat(), is("metadata-format"));
        assertThat(metadata.get(0).getUrl(), is(new URL("http://localhost:8080/geoserver/metadata-content")));
    }

    @Test
    public void testGetMetadataUrlsFromLayerGroup() throws MalformedURLException {
        setupUrlContext();
        // create a tile layer using a layer group
        GeoServerTileLayer tileLayer = new GeoServerTileLayer(layerGroup, defaults, gridSetBroker);
        List<MetadataURL> metadata = tileLayer.getMetadataURLs();
        assertThat(metadata.size(), is(1));
        assertThat(metadata.get(0).getType(), is("metadata-type"));
        assertThat(metadata.get(0).getFormat(), is("metadata-format"));
        assertThat(metadata.get(0).getUrl(), is(new URL("http://localhost:8080/geoserver/metadata-content")));
    }

    @Test
    public void testGetLegendsLayer() throws Exception {
        setupUrlContext();
        LegendSample legendSample = mock(LegendSample.class);
        when(legendSample.getLegendURLSize(any(StyleInfo.class))).thenReturn(new Dimension(120, 150));
        WMS wms = mock(WMS.class);
        GetLegendGraphicOutputFormat outputFormat = mock(GetLegendGraphicOutputFormat.class);
        when(wms.getLegendGraphicOutputFormat("image/png")).thenReturn(outputFormat);
        GeoServerTileLayer tileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        tileLayer.setLegendSample(legendSample);
        tileLayer.setWms(wms);
        Map<String, org.geowebcache.config.legends.LegendInfo> legendsInfo = tileLayer.getLayerLegendsInfo();
        assertThat(legendsInfo.size(), is(3));
        // default_style
        assertThat(legendsInfo.get("default_style"), notNullValue());
        assertThat(legendsInfo.get("default_style").getWidth(), is(120));
        assertThat(legendsInfo.get("default_style").getHeight(), is(150));
        assertThat(legendsInfo.get("default_style").getFormat(), is("image/png"));
        assertThat(
                legendsInfo.get("default_style").getLegendUrl(),
                is(
                        "http://localhost:8080/geoserver/ows?service="
                                + "WMS&request=GetLegendGraphic&version=1.1.0&format=image%2Fpng&width=120&height=150&layer=workspace%3AMockLayerInfoName"));
        // alternateStyle-1
        assertThat(legendsInfo.get("alternateStyle-1"), notNullValue());
        assertThat(legendsInfo.get("alternateStyle-1").getWidth(), is(120));
        assertThat(legendsInfo.get("alternateStyle-1").getHeight(), is(150));
        assertThat(legendsInfo.get("alternateStyle-1").getFormat(), is("image/png"));
        assertThat(
                legendsInfo.get("alternateStyle-1").getLegendUrl(),
                is(
                        "http://localhost:8080/geoserver/ows?service"
                                + "=WMS&request=GetLegendGraphic&version=1.1.0&format=image%2Fpng&width=120&height=150&layer=workspace%3AMockLayerInfoName&style=alternateStyle-1"));
        // alternateStyle-2
        assertThat(legendsInfo.get("alternateStyle-2"), notNullValue());
        assertThat(legendsInfo.get("alternateStyle-2").getWidth(), is(150));
        assertThat(legendsInfo.get("alternateStyle-2").getHeight(), is(200));
        assertThat(legendsInfo.get("alternateStyle-2").getFormat(), is("image/png"));
        assertThat(
                legendsInfo.get("alternateStyle-2").getLegendUrl().trim(),
                is("http://localhost:8080/geoserver/some-url"));
    }

    @Test
    public void testReaderDisposeCalledOnMetaTileImage() {

        Object reader = mock(ImageReader.class);
        RenderedImageTimeDecorator metaTile = getMockRenderedImageTimeDecoratorWithParameters(reader);

        GeoServerMetaTile gsMetaTile = getTestGeoServerMetaTile();
        gsMetaTile.setImage(metaTile);
        gsMetaTile.dispose();

        verify((ImageReader) reader, times(1)).dispose();
    }

    @Test
    public void testImageInputStreamIsClosedForMetaTileImage() {
        Object imageInputStream = mock(ImageInputStream.class);
        RenderedImageTimeDecorator metaTile = getMockRenderedImageTimeDecoratorWithParameters(imageInputStream);

        GeoServerMetaTile gsMetaTile = getTestGeoServerMetaTile();
        gsMetaTile.setImage(metaTile);
        gsMetaTile.dispose();

        try {
            verify((ImageInputStream) imageInputStream, times(1)).close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "", e);
        }
    }

    private RenderedImageTimeDecorator getMockRenderedImageTimeDecoratorWithParameters(Object param) {
        ParameterBlock parameterBlock = new ParameterBlock();
        parameterBlock.add(param);

        RenderedOp image = mock(RenderedOp.class);
        when(image.getParameterBlock()).thenReturn(parameterBlock);

        RenderedImageTimeDecorator metaTile = mock(RenderedImageTimeDecorator.class);
        when(metaTile.getDelegate()).thenReturn(image);

        return metaTile;
    }

    private GeoServerMetaTile getTestGeoServerMetaTile() {
        long[] testArray = {1L, 1L, 1L, 1L, 1L};
        GridSubset mockGridSubset = mock(GridSubset.class);
        when(mockGridSubset.getTileWidth()).thenReturn(0);
        when(mockGridSubset.getTileHeight()).thenReturn(0);
        when(mockGridSubset.getCoverage(1)).thenReturn(testArray);
        when(mockGridSubset.boundsFromRectangle(testArray)).thenReturn(mock(BoundingBox.class));

        GeoServerMetaTile gsMetaTile = new GeoServerMetaTile(
                mockGridSubset, mock(MimeType.class), mock(FormatModifier.class), testArray, 1, 1, 4);

        return gsMetaTile;
    }

    private void setupUrlContext() {
        // setup request context (needed to compute the base url)
        Request request = mock(Request.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(request.getHttpRequest()).thenReturn(httpRequest);
        when(httpRequest.getScheme()).thenReturn("http");
        when(httpRequest.getServerName()).thenReturn("localhost");
        when(httpRequest.getServerPort()).thenReturn(8080);
        when(httpRequest.getContextPath()).thenReturn("/geoserver");
        Dispatcher.REQUEST.set(request);
    }

    @Test
    public void testGridsetNames() {
        // set a empty reference envelope, like the importer would do
        resource.setLatLonBoundingBox(new ReferencedEnvelope());

        // grab the gridsets, check this does not trigger computation of bounds
        GeoServerTileLayer layer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        assertThat(layer.getGridSubsets(), containsInAnyOrder("EPSG:4326", "EPSG:900913"));

        // half planet
        resource.setLatLonBoundingBox(new ReferencedEnvelope(-180, 0, -90, 0, WGS84));
        GridSubset subset = layer.getGridSubset("EPSG:4326");
        assertArrayEquals(new long[] {0, 1, 0, 0, 1}, subset.getCoverage(1));
    }

    /** GEOS-10249 */
    @Test
    public void testGetPublishedInfoOnRaceCondition() throws Exception {
        GeoServerTileLayer tileLayer = new GeoServerTileLayer(
                catalog, layerInfo.getId(), gridSetBroker, TileLayerInfoUtil.loadOrCreate(layerInfo, defaults));
        // here, parallelism equals to cpu cores
        int parallelism = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(parallelism);
        List<Future<PublishedInfo>> results = IntStream.range(0, parallelism)
                .mapToObj(i -> pool.submit(tileLayer::getPublishedInfo))
                .collect(Collectors.toList());
        for (Future<PublishedInfo> result : results) {
            assertNotNull(result.get());
        }
        pool.shutdown();
    }
}

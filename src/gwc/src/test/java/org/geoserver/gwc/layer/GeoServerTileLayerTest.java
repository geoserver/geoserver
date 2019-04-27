/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.geoserver.gwc.GWC.tileLayerName;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.LegendInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.dispatch.GwcServiceDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.wms.GetLegendGraphicOutputFormat;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.capabilities.LegendSample;
import org.geoserver.wms.map.RenderedImageMap;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
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
import org.geowebcache.locks.MemoryLockProvider;
import org.geowebcache.mime.MimeType;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.TileObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class GeoServerTileLayerTest {

    private LayerInfoImpl layerInfo;

    private GeoServerTileLayer layerInfoTileLayer;

    private LayerGroupInfoImpl layerGroup;

    private GeoServerTileLayer layerGroupInfoTileLayer;

    private Catalog catalog;

    private GridSetBroker gridSetBroker;

    private GWCConfig defaults;

    private GWC mockGWC;

    private FeatureTypeInfoImpl resource;

    @After
    public void tearDown() throws Exception {
        GWC.set(null);
        Dispatcher.REQUEST.remove();
    }

    @Before
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setUp() throws Exception {
        mockGWC = mock(GWC.class);
        MemoryLockProvider lockProvider = new MemoryLockProvider();
        when(mockGWC.getLockProvider()).thenReturn(lockProvider);
        GWC.set(mockGWC);

        final String layerInfoId = "mock-layer-info";

        NamespaceInfo ns = new NamespaceInfoImpl();
        ns.setPrefix("test");
        ns.setURI("http://goserver.org/test");

        WorkspaceInfo workspaceInfo = new WorkspaceInfoImpl();
        workspaceInfo.setName("workspace");

        DataStoreInfoImpl storeInfo = new DataStoreInfoImpl(null);
        storeInfo.setId("mock-store-info");
        storeInfo.setEnabled(true);
        storeInfo.setWorkspace(workspaceInfo);

        resource = new FeatureTypeInfoImpl((Catalog) null);
        resource.setStore(storeInfo);
        resource.setId("mock-resource-info");
        resource.setName("MockLayerInfoName");
        resource.setNamespace(ns);
        resource.setTitle("Test resource title");
        resource.setAbstract("Test resource abstract");
        resource.setEnabled(true);
        resource.setDescription("Test resource description");
        resource.setLatLonBoundingBox(
                new ReferencedEnvelope(-180, -90, 0, 0, DefaultGeographicCRS.WGS84));
        resource.setNativeBoundingBox(
                new ReferencedEnvelope(-180, -90, 0, 0, DefaultGeographicCRS.WGS84));
        resource.setSRS("EPSG:4326");
        resource.setKeywords((List) Arrays.asList(new Keyword("kwd1"), new Keyword("kwd2")));

        // add metadata links
        MetadataLinkInfoImpl metadataLinkInfo = new MetadataLinkInfoImpl();
        metadataLinkInfo.setAbout("metadata-about");
        metadataLinkInfo.setContent("metadata-content");
        metadataLinkInfo.setId("metadata-id");
        metadataLinkInfo.setMetadataType("metadata-type");
        metadataLinkInfo.setType("metadata-format");
        resource.setMetadataLinks(Collections.singletonList(metadataLinkInfo));

        ResourcePool resourcePool = mock(ResourcePool.class);
        Style style = mock(Style.class);
        when(style.featureTypeStyles()).thenReturn(Collections.emptyList());
        when(resourcePool.getStyle(any(StyleInfo.class))).thenReturn(style);
        catalog = mock(Catalog.class);
        when(catalog.getResourcePool()).thenReturn(resourcePool);

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
        Set<StyleInfo> alternateStyles =
                new HashSet<StyleInfo>(Arrays.asList(alternateStyle1, alternateStyle2));
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
        layerGroup.setLayers(Collections.singletonList((PublishedInfo) layerInfo));

        defaults = GWCConfig.getOldDefaults();

        when(catalog.getLayer(eq(layerInfoId))).thenReturn(layerInfo);
        when(catalog.getLayerGroup(eq(layerGroupId))).thenReturn(layerGroup);

        gridSetBroker =
                new GridSetBroker(Collections.singletonList(new DefaultGridsets(true, true)));
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
                new HashSet<>(
                        Arrays.asList("default_style", "alternateStyle-1", "alternateStyle-2")),
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
        ParameterFilter stylesParamFilter = layerInfoTileLayer.getParameterFilters().get(0);
        List<String> legalValues = stylesParamFilter.getLegalValues();

        Map<String, String> requestParams;
        Map<String, String> modifiedParams;

        requestParams = Collections.singletonMap("sTyLeS", "");
        modifiedParams = layerInfoTileLayer.getModifiableParameters(requestParams, "UTF-8");
        assertEquals(0, modifiedParams.size());

        for (String legalStyle : legalValues) {
            requestParams = new HashMap<String, String>();
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

        assertThat(
                subset,
                hasProperty("originalExtent", hasProperty("minX", closeTo(-180.0, 0.0000001))));

        layerInfoTileLayer.removeGridSubset("EPSG:4326");
        layerInfoTileLayer.addGridSubset(subset);

        resource.setLatLonBoundingBox(
                new ReferencedEnvelope(-90, -90, 0, 0, DefaultGeographicCRS.WGS84));
        resource.setNativeBoundingBox(
                new ReferencedEnvelope(-90, -90, 0, 0, DefaultGeographicCRS.WGS84));

        GridSubset subset2 = layerInfoTileLayer.getGridSubset("EPSG:4326");

        // the extent should be that of resource
        assertThat(
                subset2,
                hasProperty("originalExtent", hasProperty("minX", closeTo(-90.0, 0.0000001))));
    }

    @Test
    public void testGetGridSubsetsStatic() throws Exception {
        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);

        GridSubset subset = layerInfoTileLayer.getGridSubset("EPSG:4326");

        assertThat(subset, instanceOf(DynamicGridSubset.class));

        assertThat(
                subset,
                hasProperty("originalExtent", hasProperty("minX", closeTo(-180.0, 0.0000001))));

        layerInfoTileLayer.removeGridSubset("EPSG:4326");
        layerInfoTileLayer.addGridSubset(new GridSubset(subset)); // Makes the dynamic extent static

        resource.setLatLonBoundingBox(
                new ReferencedEnvelope(-90, -90, 0, 0, DefaultGeographicCRS.WGS84));
        resource.setNativeBoundingBox(
                new ReferencedEnvelope(-90, -90, 0, 0, DefaultGeographicCRS.WGS84));

        GridSubset subset2 = layerInfoTileLayer.getGridSubset("EPSG:4326");

        // the extent should not change with that of resource
        assertThat(
                subset2,
                hasProperty("originalExtent", hasProperty("minX", closeTo(-180.0, 0.0000001))));
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
        layerGroupInfoTileLayer.getGridSubsets();

        XMLGridSubset savedSubset =
                layerGroupInfoTileLayer.getInfo().getGridSubsets().iterator().next();

        BoundingBox gridSubsetExtent = savedSubset.getExtent();
        BoundingBox expected = gridSetBroker.getWorldEpsg3857().getOriginalExtent();
        // don't use equals(), it uses an equality threshold we want to avoid here
        double threshold = 1E-16;
        assertTrue(
                "Expected " + expected + ", got " + gridSubsetExtent,
                expected.equals(gridSubsetExtent, threshold));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
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
        Mockito.when(mockGWC.dispatchOwsRequest(argument.capture(), (Cookie[]) any()))
                .thenReturn(mockResult);

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

        verify(mockGWC, times(1)).dispatchOwsRequest((Map) any(), (Cookie[]) any());

        when(mockGWC.dispatchOwsRequest((Map) any(), (Cookie[]) any()))
                .thenThrow(new RuntimeException("mock exception"));
        try {
            layerInfoTileLayer.getFeatureInfo(convTile, bbox, 100, 100, 50, 50);
            fail("Expected GeoWebCacheException");
        } catch (GeoWebCacheException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testGetTilePreconditions() throws Exception {

        StorageBroker storageBroker = mock(StorageBroker.class);

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);

        MockHttpServletRequest servletReq = new MockHttpServletRequest();
        HttpServletResponse servletResp = new MockHttpServletResponse();

        ConveyorTile tile =
                new ConveyorTile(
                        storageBroker, layerInfoTileLayer.getName(), servletReq, servletResp);
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
            tile =
                    new ConveyorTile(
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

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testGetTile() throws Exception {

        Resource mockResult = mock(Resource.class);
        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        Mockito.when(mockGWC.dispatchOwsRequest(argument.capture(), (Cookie[]) any()))
                .thenReturn(mockResult);

        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        RenderedImageMap fakeDispatchedMap =
                new RenderedImageMap(new WMSMapContent(), image, "image/png");

        RenderedImageMapResponse fakeResponseEncoder = mock(RenderedImageMapResponse.class);
        MimeType mimeType = MimeType.createFromFormat("image/png");
        when(mockGWC.getResponseEncoder(eq(mimeType), (RenderedImageMap) any()))
                .thenReturn(fakeResponseEncoder);

        StorageBroker storageBroker = mock(StorageBroker.class);
        when(storageBroker.get((TileObject) any())).thenReturn(false);

        layerInfoTileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);

        MockHttpServletRequest servletReq = new MockHttpServletRequest();
        HttpServletResponse servletResp = new MockHttpServletResponse();
        long[] tileIndex = {0, 0, 0};

        ConveyorTile tile =
                new ConveyorTile(
                        storageBroker,
                        layerInfoTileLayer.getName(),
                        "EPSG:4326",
                        tileIndex,
                        mimeType,
                        null,
                        servletReq,
                        servletResp);

        GeoServerTileLayer.WEB_MAP.set(fakeDispatchedMap);
        ConveyorTile returned = layerInfoTileLayer.getTile(tile);
        assertNotNull(returned);
        assertNotNull(returned.getBlob());
        assertEquals(CacheResult.MISS, returned.getCacheResult());
        assertEquals(200, returned.getStatus());

        verify(storageBroker, atLeastOnce()).get((TileObject) any());
        verify(mockGWC, times(1)).getResponseEncoder(eq(mimeType), isA(RenderedImageMap.class));
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

        List<ExpirationRule> list = new ArrayList<ExpirationRule>();
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
    public void testGetLayerNameForGetCapabilitiesRequest()
            throws NoSuchFieldException, IllegalAccessException {
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
        GeoServerTileLayer tileLayerB =
                new GeoServerTileLayer(layerGroupA, defaults, gridSetBroker);
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
        assertThat(
                metadata.get(0).getUrl(),
                is(new URL("http://localhost:8080/geoserver/metadata-content")));
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
        assertThat(
                metadata.get(0).getUrl(),
                is(new URL("http://localhost:8080/geoserver/metadata-content")));
    }

    @Test
    public void testGetLegendsLayer() throws Exception {
        setupUrlContext();
        LegendSample legendSample = mock(LegendSample.class);
        when(legendSample.getLegendURLSize(any(StyleInfo.class)))
                .thenReturn(new Dimension(120, 150));
        WMS wms = mock(WMS.class);
        GetLegendGraphicOutputFormat outputFormat = mock(GetLegendGraphicOutputFormat.class);
        when(wms.getLegendGraphicOutputFormat("image/png")).thenReturn(outputFormat);
        GeoServerTileLayer tileLayer = new GeoServerTileLayer(layerInfo, defaults, gridSetBroker);
        tileLayer.setLegendSample(legendSample);
        tileLayer.setWms(wms);
        Map<String, org.geowebcache.config.legends.LegendInfo> legendsInfo =
                tileLayer.getLayerLegendsInfo();
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
                                + "WMS&request=GetLegendGraphic&format=image%2Fpng&width=120&height=150&layer=workspace%3AMockLayerInfoName"));
        // alternateStyle-1
        assertThat(legendsInfo.get("alternateStyle-1"), notNullValue());
        assertThat(legendsInfo.get("alternateStyle-1").getWidth(), is(120));
        assertThat(legendsInfo.get("alternateStyle-1").getHeight(), is(150));
        assertThat(legendsInfo.get("alternateStyle-1").getFormat(), is("image/png"));
        assertThat(
                legendsInfo.get("alternateStyle-1").getLegendUrl(),
                is(
                        "http://localhost:8080/geoserver/ows?service"
                                + "=WMS&request=GetLegendGraphic&format=image%2Fpng&width=120&height=150&layer=workspace%3AMockLayerInfoName&style=alternateStyle-1"));
        // alternateStyle-2
        assertThat(legendsInfo.get("alternateStyle-2"), notNullValue());
        assertThat(legendsInfo.get("alternateStyle-2").getWidth(), is(150));
        assertThat(legendsInfo.get("alternateStyle-2").getHeight(), is(200));
        assertThat(legendsInfo.get("alternateStyle-2").getFormat(), is("image/png"));
        assertThat(
                legendsInfo.get("alternateStyle-2").getLegendUrl().trim(),
                is("http://localhost:8080/geoserver/some-url"));
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
}

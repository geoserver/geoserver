/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static java.lang.String.format;
import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.geoserver.gwc.GWC.tileLayerName;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.DateUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupHelper;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.wmts.WMTSInfo;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.feature.NameImpl;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.config.XMLGridSet;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration.ConnectionPoolConfiguration;
import org.geowebcache.diskquota.jdbc.JDBCQuotaStore;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.seed.TruncateAllRequest;
import org.geowebcache.service.wmts.WMTSService;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GWCIntegrationTest extends GeoServerSystemTestSupport {

    // WMTS 1.0 namespaces
    private static final Map<String, String> WMTS_NAMESPACES_10 = new HashMap<>();

    static {
        // populate WMTS 1.0 namespaces map
        WMTS_NAMESPACES_10.put("xlink", "http://www.w3.org/1999/xlink");
        WMTS_NAMESPACES_10.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        WMTS_NAMESPACES_10.put("ows", "http://www.opengis.net/ows/1.1");
        WMTS_NAMESPACES_10.put("wmts", "http://www.opengis.net/wmts/1.0");
        // set this as the default namespaces
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(WMTS_NAMESPACES_10));
    }

    // WMTS 1.0  XPATH engine
    private static final XpathEngine WMTS_XPATH_10 = XMLUnit.newXpathEngine();

    static final String SIMPLE_LAYER_GROUP = "SIMPLE_LAYER_GROUP";

    static final String FLAT_LAYER_GROUP = "flatLayerGroup";
    static final String NESTED_LAYER_GROUP = "nestedLayerGroup";
    static final String CONTAINER_LAYER_GROUP = "containerLayerGroup";
    static final String WORKSPACED_LAYER_GROUP = "workspacedLayerGroup";

    static final String TEST_WORKSPACE_NAME = "testWorkspace";
    static final String TEST_WORKSPACE_URI =
            "http://geoserver.org/GWCIntegerationTest/" + TEST_WORKSPACE_NAME;
    static final String WORKSPACED_STYLE_NAME = "workspacedStyle";
    static final String WORKSPACED_STYLE_FILE = "workspacedStyle.sld";
    static final String WORKSPACED_LAYER = "workspacedLayer";
    static final QName WORKSPACED_LAYER_QNAME =
            new QName(TEST_WORKSPACE_URI, WORKSPACED_LAYER, TEST_WORKSPACE_NAME);

    static QName BASIC_POLYGONS_NO_CRS =
            new QName(MockData.CITE_URI, "BasicPolygonsNoCrs", MockData.CITE_PREFIX);

    @Value("${gwc.context.suffix}")
    private String suffix;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("gwc-integration-test.xml");
    }

    @After
    public void cleanupDispatcherRequest() {
        // some test set the dispatcher request manually, avoid cross test contamination
        Dispatcher.REQUEST.remove();
    }

    private void prepareDataDirectory(SystemTestData testData) throws Exception {
        Catalog catalog = getCatalog();
        testData.addWorkspace(TEST_WORKSPACE_NAME, TEST_WORKSPACE_URI, catalog);
        WorkspaceInfo wi = catalog.getWorkspaceByName(TEST_WORKSPACE_NAME);
        testData.addStyle(
                wi, WORKSPACED_STYLE_NAME, WORKSPACED_STYLE_FILE, this.getClass(), catalog);

        assertThat(
                catalog.getStyleByName(wi, WORKSPACED_STYLE_NAME),
                Matchers.describedAs(
                        "Style %0 should be in workspace %1.",
                        (not(nullValue())), WORKSPACED_STYLE_NAME, TEST_WORKSPACE_NAME));

        Map<LayerProperty, Object> props = new HashMap<>();
        props.put(LayerProperty.STYLE, WORKSPACED_STYLE_NAME);
        testData.addVectorLayer(WORKSPACED_LAYER_QNAME, props, this.getClass(), catalog);
        LayerInfo li = catalog.getLayerByName(getLayerId(WORKSPACED_LAYER_QNAME));
        li.setDefaultStyle(catalog.getStyleByName(wi, WORKSPACED_STYLE_NAME));
        catalog.save(li);

        // add a simple layer group with two layers
        createLayerGroup(SIMPLE_LAYER_GROUP, MockData.BUILDINGS, MockData.BRIDGES);

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);

        // add a layer with no native CRS
        props = new HashMap<>();
        props.put(LayerProperty.SRS, "4326");
        props.put(LayerProperty.PROJECTION_POLICY, ProjectionPolicy.FORCE_DECLARED);
        testData.addVectorLayer(
                BASIC_POLYGONS_NO_CRS,
                null,
                "BasicPolygonsNoCrs.properties",
                this.getClass(),
                catalog);

        // add a style group (any caps request would fail without the fix in GEOS-9111)
        testData.addStyle("stylegroup", "stylegroup.sld", GWCIntegrationTest.class, catalog);
        final LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.getLayers().add(null);
        group.getStyles().add(catalog.getStyleByName("stylegroup"));
        group.setName("stylegroup");
        new LayerGroupHelper(group).calculateBounds();
        catalog.add(group);

        // clean up the recorded http requests
        HttpRequestRecorderCallback.reset();
    }

    @Before
    public void cleanup() throws Exception {
        SystemTestData testData = getTestData();
        for (File f : testData.getDataDirectoryRoot().listFiles()) {
            FileUtils.deleteQuietly(f);
        }
        testData.setUp();
        testData.setUpDefault();
        getGeoServer().reload();
        //        GeoServerLoader loader = GeoServerExtensions.bean(DefaultGeoServerLoader.class);
        //        loader.initializeStyles(getCatalog());
        prepareDataDirectory(testData);
    }

    protected GridSet namedGridsetCopy(final String newName, final GridSet oldGridset) {
        final GridSet newGridset;
        {
            XMLGridSet xmlGridSet = new XMLGridSet(oldGridset);
            xmlGridSet.setName(newName);
            newGridset = xmlGridSet.makeGridSet();
        }
        return newGridset;
    }

    @Test
    public void testPngIntegration() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        MockHttpServletResponse sr =
                getAsServletResponse(
                        "gwc/service/wmts?request=GetTile&layer="
                                + layerId
                                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
    }

    @Test
    public void testRequestReplacement() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        MockHttpServletResponse sr =
                getAsServletResponse(
                        "gwc/service/wmts?request=GetTile&layer="
                                + layerId
                                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=1");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
    }

    @Test
    public void testWorkspacedStyle() throws Exception {
        String layerId = getLayerId(WORKSPACED_LAYER_QNAME);
        MockHttpServletResponse sr =
                getAsServletResponse(
                        "gwc/service/wmts?request=GetTile&layer="
                                + layerId
                                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
    }

    @Test
    public void testGetLegendGraphics() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        MockHttpServletResponse sr =
                getAsServletResponse(
                        "gwc/service/wms?service=wms&version=1.1.1&request=GetLegendGraphic&layer="
                                + layerId
                                + "&style=&format=image/png");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
    }

    @Test
    public void testCachingHeadersSingleLayer() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        setCachingMetadata(layerId, true, 7200);

        MockHttpServletResponse sr =
                getAsServletResponse(
                        "gwc/service/wmts?request=GetTile&layer="
                                + layerId
                                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertEquals("max-age=7200, must-revalidate", sr.getHeader("Cache-Control"));
    }

    @Test
    public void testCachingHeadersSingleLayerNoHeaders() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        setCachingMetadata(layerId, false, -1);

        MockHttpServletResponse sr =
                getAsServletResponse(
                        "gwc/service/wmts?request=GetTile&layer="
                                + layerId
                                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertNull(sr.getHeader("Cache-Control"));
    }

    @Test
    public void testCachingHeadersFlatLayerGroup() throws Exception {
        // set two different caching headers for the two layers
        String bpLayerId = getLayerId(MockData.BASIC_POLYGONS);
        setCachingMetadata(bpLayerId, true, 7200);
        String mpLayerId = getLayerId(MockData.MPOINTS);
        setCachingMetadata(mpLayerId, true, 1000);

        // build a flat layer group with them
        LayerGroupInfo lg = getCatalog().getFactory().createLayerGroup();
        lg.setName(FLAT_LAYER_GROUP);
        lg.getLayers().add(getCatalog().getLayerByName(bpLayerId));
        lg.getLayers().add(getCatalog().getLayerByName(mpLayerId));
        lg.getStyles().add(null);
        lg.getStyles().add(null);
        new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(lg);
        getCatalog().add(lg);

        MockHttpServletResponse sr =
                getAsServletResponse(
                        "gwc/service/wmts?request=GetTile&layer="
                                + FLAT_LAYER_GROUP
                                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertEquals("max-age=1000, must-revalidate", sr.getHeader("Cache-Control"));
    }

    @Test
    public void testCachingHeadersNestedLayerGroup() throws Exception {
        // set two different caching headers for the two layers
        String bpLayerId = getLayerId(MockData.BASIC_POLYGONS);
        setCachingMetadata(bpLayerId, true, 7200);
        String mpLayerId = getLayerId(MockData.MPOINTS);
        setCachingMetadata(mpLayerId, true, 1000);

        CatalogBuilder builder = new CatalogBuilder(getCatalog());

        // build the nested layer group, only one layer in it
        LayerGroupInfo nested = getCatalog().getFactory().createLayerGroup();
        nested.setName(NESTED_LAYER_GROUP);
        nested.getLayers().add(getCatalog().getLayerByName(bpLayerId));
        nested.getStyles().add(null);
        builder.calculateLayerGroupBounds(nested);
        getCatalog().add(nested);

        // build the container layer group
        LayerGroupInfo container = getCatalog().getFactory().createLayerGroup();
        container.setName(CONTAINER_LAYER_GROUP);
        container.getLayers().add(getCatalog().getLayerByName(mpLayerId));
        container.getLayers().add(getCatalog().getLayerGroupByName(NESTED_LAYER_GROUP));
        container.getStyles().add(null);
        container.getStyles().add(null);
        builder.calculateLayerGroupBounds(container);
        getCatalog().add(container);

        // check the caching headers on the nested group
        MockHttpServletResponse sr =
                getAsServletResponse(
                        "gwc/service/wmts?request=GetTile&layer="
                                + NESTED_LAYER_GROUP
                                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertEquals("max-age=7200, must-revalidate", sr.getHeader("Cache-Control"));

        // check the caching headers on the container layer group
        sr =
                getAsServletResponse(
                        "gwc/service/wmts?request=GetTile&layer="
                                + CONTAINER_LAYER_GROUP
                                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertEquals("max-age=1000, must-revalidate", sr.getHeader("Cache-Control"));
    }

    private void setCachingMetadata(String layerId, boolean cachingEnabled, int cacheAgeMax) {
        FeatureTypeInfo ft = getCatalog().getResourceByName(layerId, FeatureTypeInfo.class);
        ft.getMetadata().put(ResourceInfo.CACHING_ENABLED, cachingEnabled);
        ft.getMetadata().put(ResourceInfo.CACHE_AGE_MAX, cacheAgeMax);
        getCatalog().save(ft);
    }

    /**
     * If direct WMS integration is enabled, a GetMap requests that hits the regular WMS but matches
     * a gwc tile should return with the proper {@code geowebcache-tile-index} HTTP response header.
     */
    @Test
    public void testDirectWMSIntegration() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String layerName = BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();
        String request;
        MockHttpServletResponse response;

        request = buildGetMap(true, layerName, "EPSG:4326", null);
        response = getAsServletResponse(request);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertNull(response.getHeader("geowebcache-tile-index"));

        request = request + "&tiled=true";
        response = getAsServletResponse(request);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testDirectWMSIntegrationResponseHeaders() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String layerName = BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();

        String request = buildGetMap(true, layerName, "EPSG:4326", null) + "&tiled=true";
        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertEquals(layerName, response.getHeader("geowebcache-layer"));
        assertEquals("[0, 0, 0]", response.getHeader("geowebcache-tile-index"));
        assertEquals("-180.0,-90.0,0.0,90.0", response.getHeader("geowebcache-tile-bounds"));
        assertEquals("EPSG:4326", response.getHeader("geowebcache-gridset"));
        assertEquals("EPSG:4326", response.getHeader("geowebcache-crs"));
    }

    @Test
    public void testDirectWMSIntegrationResponseHeaders13() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String layerName = BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();

        String request =
                "wms?service=wms&version=1.3.0&request=GetMap&styles=&layers="
                        + layerName
                        + "&srs=EPSG:4326&bbox=-90,-180,90,0&format=image/png&width=256&height=256&tiled=true";
        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertEquals(layerName, response.getHeader("geowebcache-layer"));
        assertEquals("[0, 0, 0]", response.getHeader("geowebcache-tile-index"));
        assertEquals("-180.0,-90.0,0.0,90.0", response.getHeader("geowebcache-tile-bounds"));
        assertEquals("EPSG:4326", response.getHeader("geowebcache-gridset"));
        assertEquals("EPSG:4326", response.getHeader("geowebcache-crs"));
    }

    @Test
    public void testDirectWMSIntegrationIfModifiedSinceSupport() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String layerName = BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();

        final String path = buildGetMap(true, layerName, "EPSG:4326", null) + "&tiled=true";

        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());

        String lastModifiedHeader = response.getHeader("Last-Modified");
        assertNotNull(lastModifiedHeader);
        Date lastModified = DateUtils.parseDate(lastModifiedHeader);

        MockHttpServletRequest httpReq = createGetRequest(path);
        httpReq.addHeader("If-Modified-Since", lastModifiedHeader);

        response = dispatch(httpReq, "UTF-8");

        assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());

        // set the If-Modified-Since header to some point in the past of the last modified value
        Date past = new Date(lastModified.getTime() - 5000);
        String ifModifiedSince = DateUtils.formatDate(past);

        httpReq = createGetRequest(path);
        httpReq.addHeader("If-Modified-Since", ifModifiedSince);
        response = dispatch(httpReq, "UTF-8");
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        Date future = new Date(lastModified.getTime() + 5000);
        ifModifiedSince = DateUtils.formatDate(future);

        httpReq = createGetRequest(path);
        httpReq.addHeader("If-Modified-Since", ifModifiedSince);
        response = dispatch(httpReq, "UTF-8");
        assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getStatus());
    }

    private MockHttpServletRequest createGetRequest(final String path) {
        MockHttpServletRequest httpReq = createRequest(path);
        httpReq.setMethod("GET");
        httpReq.setContent(new byte[] {});
        return httpReq;
    }

    @Test
    public void testDirectWMSIntegrationMaxAge() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);
        final String layerName = BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();
        final String path = buildGetMap(true, layerName, "EPSG:4326", null) + "&tiled=true";
        final String qualifiedName = super.getLayerId(BASIC_POLYGONS);
        final GeoServerTileLayer tileLayer =
                (GeoServerTileLayer) gwc.getTileLayerByName(qualifiedName);
        ((LayerInfo) tileLayer.getPublishedInfo())
                .getResource()
                .getMetadata()
                .put(ResourceInfo.CACHING_ENABLED, "true");
        ((LayerInfo) tileLayer.getPublishedInfo())
                .getResource()
                .getMetadata()
                .put(ResourceInfo.CACHE_AGE_MAX, 3456);

        MockHttpServletResponse response = getAsServletResponse(path);
        String cacheControl = response.getHeader("Cache-Control");
        assertEquals("max-age=3456", cacheControl);
        assertNotNull(response.getHeader("Last-Modified"));

        ((LayerInfo) tileLayer.getPublishedInfo())
                .getResource()
                .getMetadata()
                .put(ResourceInfo.CACHING_ENABLED, "false");
        response = getAsServletResponse(path);
        cacheControl = response.getHeader("Cache-Control");
        assertEquals("no-cache", cacheControl);

        // make sure a boolean is handled, too - see comment in CachingWebMapService
        ((LayerInfo) tileLayer.getPublishedInfo())
                .getResource()
                .getMetadata()
                .put(ResourceInfo.CACHING_ENABLED, Boolean.FALSE);
        response = getAsServletResponse(path);
        cacheControl = response.getHeader("Cache-Control");
        assertEquals("no-cache", cacheControl);
    }

    @Test
    public void testDirectWMSIntegrationWithVirtualServices() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String qualifiedName = super.getLayerId(BASIC_POLYGONS);
        final String localName = BASIC_POLYGONS.getLocalPart();

        final TileLayer tileLayer = gwc.getTileLayerByName(qualifiedName);
        assertNotNull(tileLayer);
        boolean directWMSIntegrationEndpoint = true;
        String request =
                MockData.CITE_PREFIX
                        + "/"
                        + buildGetMap(
                                directWMSIntegrationEndpoint,
                                localName,
                                "EPSG:4326",
                                null,
                                tileLayer)
                        + "&tiled=true";

        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertEquals(qualifiedName, response.getHeader("geowebcache-layer"));
    }

    @Test
    public void testDirectWMSIntegrationWithVirtualServicesAndWorkspacedStyle() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String qualifiedName = super.getLayerId(WORKSPACED_LAYER_QNAME);
        final String localName = WORKSPACED_LAYER_QNAME.getLocalPart();

        final TileLayer tileLayer = gwc.getTileLayerByName(qualifiedName);
        assertNotNull(tileLayer);
        boolean directWMSIntegrationEndpoint = true;
        String request =
                TEST_WORKSPACE_NAME
                        + "/"
                        + buildGetMap(
                                directWMSIntegrationEndpoint,
                                localName,
                                "EPSG:4326",
                                null,
                                tileLayer)
                        + "&tiled=true";

        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertEquals(qualifiedName, response.getHeader("geowebcache-layer"));
        assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));

        MockHttpServletResponse response2 = getAsServletResponse(request);
        assertEquals(200, response2.getStatus());
        assertEquals("image/png", response2.getContentType());
        assertEquals(qualifiedName, response2.getHeader("geowebcache-layer"));
        assertThat(response2.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // now try with the style name too, should be another hit
        request =
                TEST_WORKSPACE_NAME
                        + "/"
                        + buildGetMap(
                                directWMSIntegrationEndpoint,
                                localName,
                                "EPSG:4326",
                                WORKSPACED_STYLE_NAME,
                                tileLayer)
                        + "&tiled=true";

        MockHttpServletResponse response3 = getAsServletResponse(request);
        assertEquals(200, response3.getStatus());
        assertEquals("image/png", response3.getContentType());
        assertEquals(qualifiedName, response3.getHeader("geowebcache-layer"));
        assertThat(response3.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // finally, rename the workspace
        String oldWorkspaceName = TEST_WORKSPACE_NAME;
        WorkspaceInfo ws = getCatalog().getWorkspaceByName(oldWorkspaceName);
        String newWorkspaceName = oldWorkspaceName + "_renamed";
        ws.setName(newWorkspaceName);
        getCatalog().save(ws);

        // rename the bits in the request, it should be another hit
        request =
                newWorkspaceName
                        + "/"
                        + buildGetMap(
                                directWMSIntegrationEndpoint,
                                localName,
                                "EPSG:4326",
                                WORKSPACED_STYLE_NAME,
                                tileLayer)
                        + "&tiled=true";
        MockHttpServletResponse response4 = getAsServletResponse(request);
        assertEquals(200, response4.getStatus());
        assertEquals("image/png", response4.getContentType());
        assertEquals(
                newWorkspaceName + ":" + WORKSPACED_LAYER,
                response4.getHeader("geowebcache-layer"));
        assertThat(response4.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));
    }

    @Test
    public void testAutomaticTruncationDefaultStyleContentsChange() throws Exception {
        final GWC gwc = GWC.get();
        final Catalog catalog = getCatalog();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String qualifiedName = super.getLayerId(WORKSPACED_LAYER_QNAME);
        final String localName = WORKSPACED_LAYER_QNAME.getLocalPart();

        final TileLayer tileLayer = gwc.getTileLayerByName(qualifiedName);

        assertNotNull(tileLayer);

        LayerInfo layer = catalog.getLayerByName(qualifiedName);

        assertNotNull(layer);

        String request =
                "gwc/service/wmts?request=GetTile&layer="
                        + qualifiedName
                        + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0";

        MockHttpServletResponse response = getAsServletResponse(request);

        // First request should be a MISS
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));

        // Second request should be a HIT
        MockHttpServletResponse response2 = getAsServletResponse(request);
        assertEquals(200, response2.getStatus());
        assertEquals("image/png", response2.getContentType());
        assertThat(response2.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // Rewrite the contents of the style; this should truncate the blobStore
        // write out the SLD, we try to use the old style so the same path is used
        StyleInfo styleToRewrite = layer.getDefaultStyle();
        StyleInfo generic = catalog.getStyleByName("generic");
        // ask the catalog to write the style
        catalog.getResourcePool()
                .writeStyle(
                        styleToRewrite,
                        new GeoServerDataDirectory(catalog.getResourceLoader())
                                .style(generic)
                                .in());

        // update the catalog
        catalog.save(styleToRewrite);

        waitTileBreederCompletion();

        MockHttpServletResponse response3 = getAsServletResponse(request);
        assertEquals(200, response3.getStatus());
        assertEquals("image/png", response3.getContentType());
        assertThat(response3.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));
    }

    @Test
    public void testAutomaticTruncationDefaultStyleChange() throws Exception {
        final GWC gwc = GWC.get();
        final Catalog catalog = getCatalog();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String qualifiedName = super.getLayerId(WORKSPACED_LAYER_QNAME);
        final String localName = WORKSPACED_LAYER_QNAME.getLocalPart();

        final TileLayer tileLayer = gwc.getTileLayerByName(qualifiedName);

        assertNotNull(tileLayer);

        LayerInfo layer = catalog.getLayerByName(qualifiedName);

        assertNotNull(layer);

        String request =
                "gwc/service/wmts?request=GetTile&layer="
                        + qualifiedName
                        + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0";

        MockHttpServletResponse response = getAsServletResponse(request);

        // First request should be a MISS
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));

        // Second request should be a HIT
        MockHttpServletResponse response2 = getAsServletResponse(request);
        assertEquals(200, response2.getStatus());
        assertEquals("image/png", response2.getContentType());
        assertThat(response2.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // Change the style; this should truncate the blobStore
        layer.setDefaultStyle(catalog.getStyleByName("generic"));
        catalog.save(layer);

        waitTileBreederCompletion();

        MockHttpServletResponse response3 = getAsServletResponse(request);
        assertEquals(200, response3.getStatus());
        assertEquals("image/png", response3.getContentType());
        assertThat(response3.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));
    }

    @Test
    public void testAutomaticTruncationLayerStyleChange() throws Exception {
        final GWC gwc = GWC.get();
        final Catalog catalog = getCatalog();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String qualifiedName = super.getLayerId(WORKSPACED_LAYER_QNAME);
        final String localName = WORKSPACED_LAYER_QNAME.getLocalPart();

        final TileLayer tileLayer = gwc.getTileLayerByName(qualifiedName);

        assertNotNull(tileLayer);

        LayerInfo layer = catalog.getLayerByName(qualifiedName);

        assertNotNull(layer);

        layer.getStyles().add(catalog.getStyleByName("generic"));
        catalog.save(layer);
        layer = catalog.getLayerByName(qualifiedName);

        String request =
                "gwc/service/wmts?request=GetTile&layer="
                        + qualifiedName
                        + "&style=generic"
                        + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0";

        MockHttpServletResponse response = getAsServletResponse(request);

        // First request should be a MISS
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));

        // Second request should be a HIT
        MockHttpServletResponse response2 = getAsServletResponse(request);
        assertEquals(200, response2.getStatus());
        assertEquals("image/png", response2.getContentType());
        assertThat(response2.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // Remove the "generic" style, and add it back; this should truncate the layer
        layer.getStyles().remove(catalog.getStyleByName("generic"));
        catalog.save(layer);
        layer = catalog.getLayerByName(qualifiedName);
        layer.getStyles().add(catalog.getStyleByName("generic"));
        catalog.save(layer);
        layer = catalog.getLayerByName(qualifiedName);

        waitTileBreederCompletion();

        MockHttpServletResponse response3 = getAsServletResponse(request);
        assertEquals(200, response3.getStatus());
        assertEquals("image/png", response3.getContentType());
        assertThat(response3.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));
    }

    @Test
    public void testAutomaticTruncationLayerStyleContentsChange() throws Exception {
        final GWC gwc = GWC.get();
        final Catalog catalog = getCatalog();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String qualifiedName = super.getLayerId(WORKSPACED_LAYER_QNAME);
        final String localName = WORKSPACED_LAYER_QNAME.getLocalPart();

        final TileLayer tileLayer = gwc.getTileLayerByName(qualifiedName);

        assertNotNull(tileLayer);

        LayerInfo layer = catalog.getLayerByName(qualifiedName);

        assertNotNull(layer);

        layer.getStyles().add(catalog.getStyleByName("generic"));
        catalog.save(layer);
        layer = catalog.getLayerByName(qualifiedName);

        String request =
                "gwc/service/wmts?request=GetTile&layer="
                        + qualifiedName
                        + "&style=generic"
                        + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0";

        MockHttpServletResponse response = getAsServletResponse(request);

        // First request should be a MISS
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));

        // Second request should be a HIT
        MockHttpServletResponse response2 = getAsServletResponse(request);
        assertEquals(200, response2.getStatus());
        assertEquals("image/png", response2.getContentType());
        assertThat(response2.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // Rewrite the contents of the style; this should truncate the blobStore
        StyleInfo styleToRewrite = catalog.getStyleByName("generic");
        try (InputStream is = GeoServerLoader.class.getResourceAsStream("default_generic.sld")) {
            catalog.getResourcePool().writeStyle(styleToRewrite, is);
        }

        catalog.save(styleToRewrite);

        waitTileBreederCompletion();

        MockHttpServletResponse response3 = getAsServletResponse(request);
        assertEquals(200, response3.getStatus());
        assertEquals("image/png", response3.getContentType());
        assertThat(response3.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));
    }

    @Test
    public void testAutomaticTruncationFeatureChange() throws Exception {
        final GWC gwc = GWC.get();
        final Catalog catalog = getCatalog();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String qualifiedName = super.getLayerId(WORKSPACED_LAYER_QNAME);
        final String localName = WORKSPACED_LAYER_QNAME.getLocalPart();

        final TileLayer tileLayer = gwc.getTileLayerByName(qualifiedName);

        assertNotNull(tileLayer);

        LayerInfo layer = catalog.getLayerByName(qualifiedName);

        assertNotNull(layer);

        String request =
                "gwc/service/wmts?request=GetTile&layer="
                        + qualifiedName
                        + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0";

        MockHttpServletResponse response = getAsServletResponse(request);

        // make sure deletion is not still underway (works in a background thread)
        waitTileBreederCompletion();

        // First request should be a MISS
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));

        // Second request should be a HIT
        MockHttpServletResponse response2 = getAsServletResponse(request);
        assertEquals(200, response2.getStatus());
        assertEquals("image/png", response2.getContentType());
        assertThat(response2.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));
        // Change the feature via the GeoServerFeatureStore wrapper; this should truncate the
        // blobStore
        // SimpleFeatureStore store = DataUtilities.simple((FeatureStore)
        // ((FeatureTypeInfo)layer.getResource()).getFeatureSource(null,null));

        // Do a WFS Insert against the layer. This should trigger a cache miss.
        final String wfsInsert =
                "<wfs:Transaction service=\"WFS\" version=\"1.0.0\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:testWorkspace=\"http://geoserver.org/GWCIntegerationTest/testWorkspace\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.0.0/WFS-transaction.xsd http://geoserver.org/GWCIntegerationTest/testWorkspace http://localhost:8080/geoserver/wfs/DescribeFeatureType?typename=testWorkspace:workspacedLayer\">\n"
                        + "    <wfs:Insert>\n"
                        + "        <testWorkspace:workspacedLayer>\n"
                        + "            <testWorkspace:location>\n"
                        + "                    <gml:Point srsName=\"http://www.opengis.net/gml/srs/epsg.xml#4326\" >\n"
                        + "                      <gml:coordinates decimal=\".\" cs=\",\" ts=\" \">0,0</gml:coordinates>\n"
                        + "                    </gml:Point>\n"
                        + "            </testWorkspace:location>\n"
                        + "            <testWorkspace:name>origin</testWorkspace:name>\n"
                        + "            <testWorkspace:value>0</testWorkspace:value>\n"
                        + "        </testWorkspace:workspacedLayer>\n"
                        + "    </wfs:Insert>\n"
                        + "</wfs:Transaction>";

        String wfsRequest =
                TEST_WORKSPACE_NAME + "/wfs?service=WFS&version=1.0.0&request=Transaction";
        MockHttpServletResponse wfsResponse = postAsServletResponse(wfsRequest, wfsInsert);
        assertEquals(200, wfsResponse.getStatus());

        waitTileBreederCompletion();

        MockHttpServletResponse response3 = getAsServletResponse(request);
        assertEquals(200, response3.getStatus());
        assertEquals("image/png", response3.getContentType());
        assertThat(response3.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));
    }

    private void waitTileBreederCompletion() throws InterruptedException {
        long start = System.currentTimeMillis();
        final int MAX_WAIT_SECS = 10;
        while (GWC.get().getPendingTasks().hasNext()) {
            Thread.sleep(10);
            long now = System.currentTimeMillis();
            if (now - start > MAX_WAIT_SECS * 1000) {
                String message =
                        format(
                                "Waited for tile breeder to finish its tasks for more than %d seconds",
                                MAX_WAIT_SECS);
                fail(message);
            }
        }
    }

    @Test
    public void testLayerGroupInWorkspace() throws Exception {
        // the workspace for the tests
        String workspaceName = MockData.BASIC_POLYGONS.getPrefix();

        // build a flat layer group with them, in the test workspace
        LayerGroupInfo lg = getCatalog().getFactory().createLayerGroup();
        lg.setName(WORKSPACED_LAYER_GROUP);
        String bpLayerId = getLayerId(MockData.BASIC_POLYGONS);
        String mpLayerId = getLayerId(MockData.LAKES);
        lg.getLayers().add(getCatalog().getLayerByName(bpLayerId));
        lg.getLayers().add(getCatalog().getLayerByName(mpLayerId));
        lg.getStyles().add(null);
        lg.getStyles().add(null);

        lg.setWorkspace(getCatalog().getWorkspaceByName(workspaceName));
        new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(lg);
        getCatalog().add(lg);

        // wmts request, use the qualified name, first request, works, but it's a cache miss of
        // course
        String request =
                "gwc/service/wmts?request=GetTile&layer="
                        + workspaceName
                        + ":"
                        + WORKSPACED_LAYER_GROUP
                        + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0";
        MockHttpServletResponse sr = getAsServletResponse(request);
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertThat(sr.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));

        // run again, it should be a hit
        sr = getAsServletResponse(request);
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertThat(sr.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // try direct integration too
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);
        final TileLayer tileLayer = gwc.getTileLayerByName(lg.prefixedName());
        request =
                buildGetMap(true, lg.prefixedName(), "EPSG:4326", null, tileLayer) + "&tiled=true";
        sr = getAsServletResponse(request);
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertEquals(lg.prefixedName(), sr.getHeader("geowebcache-layer"));
        assertThat(sr.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // and direct integration against the workspace local name
        request =
                workspaceName
                        + "/"
                        + buildGetMap(true, lg.getName(), "EPSG:4326", null, tileLayer)
                        + "&tiled=true";
        sr = getAsServletResponse(request);
        assertEquals(200, sr.getStatus());
        assertEquals(lg.prefixedName(), sr.getHeader("geowebcache-layer"));
        assertThat(sr.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // now change the workspace name
        WorkspaceInfo ws = getCatalog().getWorkspaceByName(workspaceName);
        String newWorkspaceName = workspaceName + "_renamed";
        ws.setName(newWorkspaceName);
        getCatalog().save(ws);

        // prepare the wmts request anew, it should be a hit, the cache should be preserved
        request =
                "gwc/service/wmts?request=GetTile&layer="
                        + newWorkspaceName
                        + ":"
                        + WORKSPACED_LAYER_GROUP
                        + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0";
        sr = getAsServletResponse(request);
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertThat(sr.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // and now direct integration
        String newQualifiedName = newWorkspaceName + ":" + lg.getName();
        request = buildGetMap(true, newQualifiedName, "EPSG:4326", null, tileLayer) + "&tiled=true";
        sr = getAsServletResponse(request);
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertEquals(lg.prefixedName(), sr.getHeader("geowebcache-layer"));
        assertThat(sr.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // and direct integration against the workspace local name
        request =
                newWorkspaceName
                        + "/"
                        + buildGetMap(true, lg.getName(), "EPSG:4326", null, tileLayer)
                        + "&tiled=true";
        sr = getAsServletResponse(request);
        assertEquals(200, sr.getStatus());
        assertEquals(newQualifiedName, sr.getHeader("geowebcache-layer"));
        assertThat(sr.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));
    }

    @Test
    public void testDirectWMSIntegrationWithVirtualServicesHiddenLayer() throws Exception {
        /*
         * Nothing special needs to be done at the GWC integration level for this to work. The hard
         * work should already be done by WMSWorkspaceQualifier so that when the request hits GWC
         * the layer name is already qualified
         */
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String qualifiedName = super.getLayerId(BASIC_POLYGONS);
        final String localName = BASIC_POLYGONS.getLocalPart();

        final TileLayer tileLayer = gwc.getTileLayerByName(qualifiedName);
        assertNotNull(tileLayer);
        boolean directWMSIntegrationEndpoint = true;
        String request =
                MockData.CDF_PREFIX // asking /geoserver/cdf/wms? for cite:BasicPolygons
                        + "/"
                        + buildGetMap(
                                directWMSIntegrationEndpoint,
                                localName,
                                "EPSG:4326",
                                null,
                                tileLayer)
                        + "&tiled=true";

        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals(200, response.getStatus());

        assertTrue(
                response.getContentType(),
                response.getContentType().startsWith("application/vnd.ogc.se_xml"));

        assertTrue(
                response.getContentAsString(),
                response.getContentAsString().contains("Could not find layer cdf:BasicPolygons"));
    }

    @Test
    public void testDirectWMSIntegrationCustomHost() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String layerName = BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();

        String requestURL = buildGetMap(true, layerName, "EPSG:4326", null) + "&tiled=true";
        MockHttpServletRequest request = createRequest(requestURL);
        request.setMethod("GET");
        request.setContent(new byte[] {});
        final String THE_HOST = "foobar";
        request.setRemoteHost(THE_HOST);
        MockHttpServletResponse response = dispatch(request, null);

        // check everything went as expected
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertEquals(layerName, response.getHeader("geowebcache-layer"));
        assertEquals("[0, 0, 0]", response.getHeader("geowebcache-tile-index"));
        assertEquals("-180.0,-90.0,0.0,90.0", response.getHeader("geowebcache-tile-bounds"));
        assertEquals("EPSG:4326", response.getHeader("geowebcache-gridset"));
        assertEquals("EPSG:4326", response.getHeader("geowebcache-crs"));

        // check we have the two requests recorded, and the
        ArrayList<HttpServletRequest> requests = HttpRequestRecorderCallback.getRequests();
        assertEquals(2, requests.size());
        assertThat(requests.get(1), instanceOf(FakeHttpServletRequest.class));
        FakeHttpServletRequest fake = (FakeHttpServletRequest) requests.get(1);
        assertEquals(THE_HOST, fake.getRemoteHost());
    }

    @Test
    public void testReloadConfiguration() throws Exception {
        String path = "/gwc/rest/reload";
        String content = "reload_configuration=1";
        String contentType = "application/x-www-form-urlencoded";
        MockHttpServletResponse response = postAsServletResponse(path, content, contentType);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testBasicIntegration() throws Exception {
        Catalog cat = getCatalog();
        TileLayerDispatcher tld = GeoWebCacheExtensions.bean(TileLayerDispatcher.class);
        assertNotNull(tld);

        GridSetBroker gridSetBroker = GeoWebCacheExtensions.bean(GridSetBroker.class);
        assertNotNull(gridSetBroker);

        try {
            tld.getTileLayer("");
        } catch (Exception gwce) {

        }

        // 1) Check that cite:Lakes is present
        boolean foundLakes = false;
        for (TileLayer tl : tld.getLayerList()) {
            if (tl.getName().equals("cite:Lakes")) {
                foundLakes = true;
                break;
            }
        }
        assertTrue(foundLakes);

        // 2) Check sf:GenerictEntity is present and initialized
        boolean foudAGF = false;
        for (TileLayer tl : tld.getLayerList()) {
            if (tl.getName().equals("sf:AggregateGeoFeature")) {
                // tl.isInitialized();
                foudAGF = true;
                GridSubset epsg4326 = tl.getGridSubset(gridSetBroker.getWorldEpsg4326().getName());
                assertTrue(
                        epsg4326.getGridSetBounds()
                                .equals(new BoundingBox(-180.0, -90.0, 180.0, 90.0)));
                String mime = tl.getMimeTypes().get(1).getMimeType();
                assertTrue(
                        mime.startsWith("image/")
                                || mime.startsWith("application/vnd.google-earth.kml+xml"));
            }
        }
        assertTrue(foudAGF);

        // 3) Basic get
        LayerInfo li = cat.getLayerByName(super.getLayerId(MockData.MPOINTS));
        String layerName = tileLayerName(li);

        TileLayer tl = tld.getTileLayer(layerName);

        assertEquals(layerName, tl.getName());

        // 4) Removal of LayerInfo from catalog
        cat.remove(li);

        assertNull(cat.getLayerByName(tl.getName()));

        try {
            tld.getTileLayer(layerName);
            fail("Layer should not exist");
        } catch (GeoWebCacheException gwce) {
            assertTrue(true);
        }
    }

    private String buildGetMap(
            final boolean directWMSIntegrationEndpoint,
            final String layerName,
            final String gridsetId,
            String styles) {

        final GWC gwc = GWC.get();
        final TileLayer tileLayer = gwc.getTileLayerByName(layerName);
        return buildGetMap(directWMSIntegrationEndpoint, layerName, gridsetId, styles, tileLayer);
    }

    private String buildGetMap(
            final boolean directWMSIntegrationEndpoint,
            final String queryLayerName,
            final String gridsetId,
            String styles,
            final TileLayer tileLayer) {

        final GridSubset gridSubset = tileLayer.getGridSubset(gridsetId);

        long[] coverage = gridSubset.getCoverage(0);
        long[] tileIndex = {coverage[0], coverage[1], coverage[4]};
        BoundingBox bounds = gridSubset.boundsFromIndex(tileIndex);

        final String endpoint = directWMSIntegrationEndpoint ? "wms" : "gwc/service/wms";

        StringBuilder sb = new StringBuilder(endpoint);
        sb.append("?service=WMS&request=GetMap&version=1.1.1&format=image/png");
        sb.append("&layers=").append(queryLayerName);
        sb.append("&srs=").append(gridSubset.getSRS());
        sb.append("&width=").append(gridSubset.getGridSet().getTileWidth());
        sb.append("&height=").append(gridSubset.getGridSet().getTileHeight());
        sb.append("&styles=");
        if (styles != null) {
            sb.append(styles);
        }
        sb.append("&bbox=").append(bounds.toString());
        return sb.toString();
    }

    /**
     * See GEOS-5092, check server startup is not hurt by a tile layer out of sync (say someone
     * manually removed the GeoServer layer)
     */
    @Test
    public void testMissingGeoServerLayerAtStartUp() throws Exception {

        Catalog catalog = getCatalog();
        GWC mediator = GWC.get();

        final String layerName = getLayerId(BASIC_POLYGONS);
        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        assertNotNull(layerInfo);

        TileLayer tileLayer = mediator.getTileLayerByName(layerName);
        assertNotNull(tileLayer);
        assertTrue(tileLayer.isEnabled());

        getCatalog().remove(layerInfo);

        getGeoServer().reload();

        assertNull(catalog.getLayerByName(layerName));

        CatalogConfiguration config = GeoServerExtensions.bean(CatalogConfiguration.class);

        assertFalse(config.getLayer(layerName).isPresent());
        try {
            mediator.getTileLayerByName(layerName);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void testRemoveLayerAfterReload() throws Exception {
        Catalog cat = getCatalog();
        TileLayerDispatcher tld = GeoWebCacheExtensions.bean(TileLayerDispatcher.class);

        LayerInfo li = cat.getLayerByName(super.getLayerId(MockData.MPOINTS));
        String layerName = tileLayerName(li);

        assertNotNull(tld.getTileLayer(layerName));

        // force reload
        getGeoServer().reload();

        // now remove the layer and check it has been removed from GWC as well
        cat.remove(li);
        try {
            tld.getTileLayer(layerName);
            fail("Layer should not exist");
        } catch (GeoWebCacheException gwce) {
            // fine
        }
    }

    @Test
    public void testDiskQuotaStorage() throws Exception {
        // normal state, quota is not enabled by default
        GWC gwc = GWC.get();
        ConfigurableQuotaStoreProvider provider =
                GeoServerExtensions.bean(ConfigurableQuotaStoreProvider.class);
        DiskQuotaConfig quota = gwc.getDiskQuotaConfig();
        JDBCConfiguration jdbc = gwc.getJDBCDiskQuotaConfig();
        assertFalse("Disk quota is enabled??", quota.isEnabled());
        assertNull("jdbc quota config should be missing", jdbc);
        assertTrue(getActualStore(provider) instanceof DummyQuotaStore);

        // enable disk quota in H2 mode
        quota.setEnabled(true);
        quota.setQuotaStore("H2");
        gwc.saveDiskQuotaConfig(quota, null);
        GeoServerDataDirectory dd = GeoServerExtensions.bean(GeoServerDataDirectory.class);
        String jdbcConfigPath = "gwc/geowebcache-diskquota-jdbc.xml";
        assertNull(
                "jdbc config (" + jdbcConfigPath + ") should not be there",
                dd.findFile(jdbcConfigPath));
        String h2StorePath = "gwc/diskquota_page_store_h2";
        assertNotNull("jdbc store (" + h2StorePath + ") should be there", dd.findFile(h2StorePath));
        assertTrue(getActualStore(provider) instanceof JDBCQuotaStore);

        // disable again and clean up
        quota.setEnabled(false);
        gwc.saveDiskQuotaConfig(quota, null);
        FileUtils.deleteDirectory(dd.findFile("gwc/diskquota_page_store_h2"));

        // now enable it in JDBC mode, with H2 local storage
        quota.setEnabled(true);
        quota.setQuotaStore("JDBC");
        jdbc = new JDBCConfiguration();
        jdbc.setDialect("H2");
        ConnectionPoolConfiguration pool = new ConnectionPoolConfiguration();
        pool.setDriver("org.h2.Driver");
        pool.setUrl("jdbc:h2:./target/quota-h2");
        pool.setUsername("sa");
        pool.setPassword("");
        pool.setMinConnections(1);
        pool.setMaxConnections(1);
        pool.setMaxOpenPreparedStatements(50);
        jdbc.setConnectionPool(pool);
        gwc.saveDiskQuotaConfig(quota, jdbc);
        assertNotNull(
                "jdbc config (" + jdbcConfigPath + ") should be there",
                dd.findFile(jdbcConfigPath));
        assertNull(
                "jdbc store (" + h2StorePath + ") should be there", dd.findDataFile(h2StorePath));
        File newQuotaStore = new File("./target/quota-h2.data.db");
        assertTrue(newQuotaStore.exists());

        File jdbcConfigFile = dd.findFile(jdbcConfigPath);
        try (FileInputStream fis = new FileInputStream(jdbcConfigFile)) {
            Document dom = dom(fis);
            // print(dom);
            String storedPassword =
                    XMLUnit.newXpathEngine()
                            .evaluate("/gwcJdbcConfiguration/connectionPool/password", dom);
            // check the password has been encoded properly
            assertTrue(storedPassword.startsWith("crypt1:"));
        }
    }

    private QuotaStore getActualStore(ConfigurableQuotaStoreProvider provider)
            throws ConfigurationException, IOException {
        return ((ConfigurableQuotaStore) provider.getQuotaStore()).getStore();
    }

    @Test
    public void testPreserveHeaders() throws Exception {
        // the code defaults to localhost:8080/geoserver, but the tests work otherwise
        GeoWebCacheDispatcher dispatcher = GeoServerExtensions.bean(GeoWebCacheDispatcher.class);
        // dispatcher.setServletPrefix("http://localhost/geoserver/");
        MockHttpServletResponse response =
                getAsServletResponse(
                        "gwc/service/wms?service=wms&version=1.1.0&request=GetCapabilities");
        // System.out.println(response.getContentAsString());
        assertEquals("application/vnd.ogc.wms_xml", response.getContentType());
        assertEquals(
                "inline;filename=wms-getcapabilities.xml",
                response.getHeader("content-disposition"));
    }

    @Test
    public void testGutter() throws Exception {
        GeoServerTileLayer tileLayer =
                (GeoServerTileLayer) GWC.get().getTileLayerByName(getLayerId(BASIC_POLYGONS));
        GeoServerTileLayerInfo info = tileLayer.getInfo();
        info.setGutter(100);
        GWC.get().save(tileLayer);

        String request =
                "gwc/service/wms?LAYERS=cite%3ABasicPolygons&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&SRS=EPSG%3A4326&BBOX=0,0,11.25,11.25&WIDTH=256&HEIGHT=256";
        BufferedImage image = getAsImage(request, "image/png");
        // with GEOS-5786 active we would have gotten back a 356px image
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
    }

    @Test
    public void testSaveConfig() throws Exception {
        GWCConfig config = GWC.get().getConfig();
        // set a large gutter
        config.setGutter(100);
        // save the config
        GWC.get().saveConfig(config);
        // force a reload
        getGeoServer().reload();
        // grab the config, make sure it was saved as expected
        assertEquals(100, GWC.get().getConfig().getGutter());
    }

    @Test
    public void testRenameWorkspace() throws Exception {
        String wsName = MockData.CITE_PREFIX;
        String wsRenamed = MockData.CITE_PREFIX + "Renamed";
        Catalog catalog = getCatalog();
        WorkspaceInfo ws = catalog.getWorkspaceByName(wsName);

        try {
            // collect all the layer names that are in the CITE workspace
            List<String> layerNames = new ArrayList<String>();
            for (LayerInfo layer : catalog.getLayers()) {
                if (wsName.equals(layer.getResource().getStore().getWorkspace().getName())) {
                    String prefixedName = layer.prefixedName();
                    try {
                        // filter out geometryless layers and other stuff that cannot be hanlded by
                        // GWC
                        GWC.get().getTileLayerByName(prefixedName);
                        layerNames.add(layer.getName());
                    } catch (IllegalArgumentException e) {
                        // fine, we are skipping layers that cannot be handled
                    }
                }
            }

            // rename the workspace
            ws.setName(wsRenamed);
            catalog.save(ws);

            // check all the preview layers have been renamed too
            for (String name : layerNames) {
                String prefixedName = wsRenamed + ":" + name;
                GWC.get().getTileLayerByName(prefixedName);
            }
        } finally {
            if (wsRenamed.equals(ws.getName())) {
                ws.setName(wsName);
                catalog.save(ws);
            }
        }
    }

    /** Test that removing a layer from the catalog also removes its tile cache. */
    @Test
    public void testRemoveCachedLayer() throws Exception {
        // the prefixed name of the layer under test
        String layerName = getLayerId(MockData.BASIC_POLYGONS);
        assertEquals("cite:BasicPolygons", layerName);
        // resource path to cache directory (FileBlobStore)
        String cacheDirectory = "gwc/cite_BasicPolygons";
        // resource path to cached tile (FileBlobStore)
        String cachedTile = "gwc/cite_BasicPolygons/EPSG_4326_00/0_0/00_00.png";
        GeoServerResourceLoader loader = getResourceLoader();
        // cache directory and cached tile should not yet exist
        assertNull("Unexpected cache directory " + cacheDirectory, loader.find(cacheDirectory));
        assertNull("Unexpected cached tile " + cachedTile, loader.find(cachedTile));
        // trigger tile caching with a WMTS request
        MockHttpServletResponse response =
                getAsServletResponse(
                        "gwc/service/wmts" //
                                + "?request=GetTile" //
                                + "&layer="
                                + layerName //
                                + "&format=image/png" //
                                + "&tilematrixset=EPSG:4326" //
                                + "&tilematrix=EPSG:4326:0" //
                                + "&tilerow=0" //
                                + "&tilecol=0");
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        // cache directory and cached tile should now be present
        assertNotNull("Missing cache directory " + cacheDirectory, loader.find(cacheDirectory));
        assertNotNull("Missing cached tile " + cachedTile, loader.find(cachedTile));
        // remove layer from the catalog, which should also remove cache directory and thus cached
        // tile
        getCatalog().remove(getCatalog().getLayerByName(layerName));
        // cache directory and cached tile should now not exist
        assertNull("Unexpected cache directory " + cacheDirectory, loader.find(cacheDirectory));
        assertNull("Unexpected cached tile " + cachedTile, loader.find(cachedTile));
    }

    @Test
    public void testGetCapabilitiesWithLocalWorkspace() throws Exception {
        final Document doc = assertGetCapabilitiesWithLocalWorkspace();
        // print(doc);
        assertThat(
                WMTS_XPATH_10.evaluate("//wmts:ServiceMetadataURL[2]/@xlink:href", doc),
                equalTo(
                        "http://localhost:8080/geoserver/cite/gwc/service/wmts/rest/WMTSCapabilities.xml"));
    }

    public Document assertGetCapabilitiesWithLocalWorkspace() throws Exception {
        // getting capabilities document for CITE workspace
        Document document =
                getAsDOM(MockData.CITE_PREFIX + "/gwc/service/wmts?request=GetCapabilities");
        // checking get capabilities result for CITE workspace
        List<LayerInfo> citeLayers = getWorkspaceLayers(MockData.CITE_PREFIX);
        assertThat(
                Integer.parseInt(
                        WMTS_XPATH_10.evaluate("count(//wmts:Contents/wmts:Layer)", document)),
                greaterThan(0));
        assertThat(
                Integer.parseInt(
                        WMTS_XPATH_10.evaluate("count(//wmts:Contents/wmts:Layer)", document)),
                lessThanOrEqualTo(citeLayers.size()));
        assertThat(
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Contents/wmts:Layer[ows:Identifier='"
                                + MockData.BUILDINGS.getLocalPart()
                                + "'])",
                        document),
                is("1"));
        return document;
    }

    @Test
    public void testGetCapabilitiesWithLocalWorkspaceAndProxyBase() throws Exception {
        final GeoServer gs = getGeoServer();
        try {
            setProxyBase(gs, "http://fooBar/geoserver");

            final Document doc = assertGetCapabilitiesWithLocalWorkspace();
            // print(doc);
            assertThat(
                    WMTS_XPATH_10.evaluate("//wmts:ServiceMetadataURL[2]/@xlink:href", doc),
                    equalTo(
                            "http://fooBar/geoserver/cite/gwc/service/wmts/rest/WMTSCapabilities.xml"));

        } finally {
            setProxyBase(gs, null);
        }
    }

    public void setProxyBase(GeoServer gs, String s) {
        final GeoServerInfo global = gs.getGlobal();
        global.getSettings().setProxyBaseUrl(s);
        gs.save(global);
    }

    @Test
    public void testComputeGridsetBounds() throws Exception {
        // set native bounds whose CRS is null (which happens when going through the UI)
        Catalog catalog = getCatalog();
        FeatureTypeInfo ft = catalog.getFeatureTypeByName(getLayerId(BASIC_POLYGONS_NO_CRS));
        CatalogBuilder cb = new CatalogBuilder(catalog);
        ft.setNativeCRS(null);
        cb.setupBounds(ft);
        catalog.save(ft);

        // force recomputing the grid subsets by adding one (at runtime this happens while editing a
        // layer)
        GridSetBroker gridSetBroker = GWC.get().getGridSetBroker();
        GridSet testGridSet = namedGridsetCopy("TEST", gridSetBroker.getDefaults().worldEpsg4326());
        GridSubset testGridSubset =
                GridSubsetFactory.createGridSubSet(
                        testGridSet,
                        new BoundingBox(-180, 0, 0, 90),
                        0,
                        testGridSet.getNumLevels() - 1);
        GeoServerTileLayer tileLayer =
                (GeoServerTileLayer)
                        GWC.get().getTileLayerByName(getLayerId(BASIC_POLYGONS_NO_CRS));
        tileLayer.addGridSubset(testGridSubset);

        // get the capabilities and check the gridset bounds (not the whole world)
        Document document =
                getAsDOM(
                        MockData.CITE_PREFIX
                                + "/"
                                + BASIC_POLYGONS_NO_CRS.getLocalPart()
                                + "/gwc/service/wmts?request=GetCapabilities");
        // print(document);
        String basePath =
                "//wmts:Contents/wmts:Layer[ows:Title='BasicPolygonsNoCrs']"
                        + "/wmts:TileMatrixSetLink[wmts:TileMatrixSet='EPSG:4326']/wmts:TileMatrixSetLimits"
                        + "/wmts:TileMatrixLimits[wmts:TileMatrix='EPSG:4326:1']";
        assertEquals("0", WMTS_XPATH_10.evaluate(basePath + "/wmts:MinTileRow", document));
        assertEquals("1", WMTS_XPATH_10.evaluate(basePath + "/wmts:MaxTileRow", document));
        assertEquals("1", WMTS_XPATH_10.evaluate(basePath + "/wmts:MinTileCol", document));
        assertEquals("2", WMTS_XPATH_10.evaluate(basePath + "/wmts:MaxTileCol", document));
    }

    @Test
    public void testGetCapabilitiesWithLocalLayer() throws Exception {
        // getting capabilities document for CITE workspace
        Document document =
                getAsDOM(
                        MockData.CITE_PREFIX
                                + "/"
                                + MockData.BUILDINGS.getLocalPart()
                                + "/gwc/service/wmts?request=GetCapabilities");
        // checking get capabilities result for CITE workspace
        List<LayerInfo> citeLayers = getWorkspaceLayers(MockData.CITE_PREFIX);
        assertThat(
                Integer.parseInt(
                        WMTS_XPATH_10.evaluate("count(//wmts:Contents/wmts:Layer)", document)),
                equalTo(1));
        assertThat(
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Contents/wmts:Layer[ows:Identifier='"
                                + MockData.BUILDINGS.getLocalPart()
                                + "'])",
                        document),
                is("1"));
    }

    @Test
    public void testGetCapabilitiesWithLocalGroup() throws Exception {
        // getting capabilities document for CITE workspace
        Document document =
                getAsDOM(SIMPLE_LAYER_GROUP + "/gwc/service/wmts?request=GetCapabilities");
        // checking get capabilities result for CITE workspace
        assertThat(
                Integer.parseInt(
                        WMTS_XPATH_10.evaluate("count(//wmts:Contents/wmts:Layer)", document)),
                equalTo(1));
        assertThat(
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Contents/wmts:Layer[ows:Identifier='"
                                + SIMPLE_LAYER_GROUP
                                + "'])",
                        document),
                is("1"));
    }

    @Test
    public void testGetTileWithLocalWorkspace() throws Exception {
        // perform a get tile request using a virtual service
        MockHttpServletResponse response =
                getAsServletResponse(
                        MockData.CITE_PREFIX
                                + "/gwc/service/wmts?request=GetTile&layer="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        // redo the same request
        response =
                getAsServletResponse(
                        MockData.CITE_PREFIX
                                + "/gwc/service/wmts?request=GetTile&layer="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        // check that we got an hit
        String cacheResult = (String) response.getHeaderValue("geowebcache-cache-result");
        assertThat(cacheResult, notNullValue());
        assertThat(cacheResult, is("HIT"));
    }

    /** Helper method that will return the layers that belong to a certain workspace. */
    private List<LayerInfo> getWorkspaceLayers(String workspaceName) {
        List<LayerInfo> layers = new ArrayList<>();
        for (LayerInfo layer : getCatalog().getLayers()) {
            WorkspaceInfo workspace = layer.getResource().getStore().getWorkspace();
            if (workspace != null && workspace.getName().equals(workspaceName)) {
                layers.add(layer);
            }
        }
        return layers;
    }

    @Test
    public void testWMTSEnabling() throws Exception {
        // store original value to restore it
        boolean initialValue = getGeoServer().getService(WMTSInfo.class).isEnabled();
        try {
            LocalWorkspace.set(null);
            WMTSInfo wmtsInfo = getGeoServer().getService(WMTSInfo.class);
            wmtsInfo.setEnabled(false);
            getGeoServer().save(wmtsInfo);
            MockHttpServletResponse response =
                    getAsServletResponse(
                            "gwc/service/wmts?service=wmts&version=1.0.0&request=GetCapabilities");
            assertEquals(400, response.getStatus());
            wmtsInfo.setEnabled(true);
            getGeoServer().save(wmtsInfo);
            response =
                    getAsServletResponse(
                            "gwc/service/wmts?service=wmts&version=1.0.0&request=GetCapabilities");
            assertEquals(200, response.getStatus());
        } finally {
            // restoring initial configuration value
            getGeoServer().getService(WMTSInfo.class).setEnabled(initialValue);
            LocalWorkspace.set(null);
        }
    }

    @Test
    public void testWmtsGetCapabilitiesRequest() throws Exception {
        // getting the capabilities document
        MockHttpServletResponse response =
                getAsServletResponse("/gwc/service/wmts?request=GetCapabilities");
        // check that the request was successful
        assertThat(response.getStatus(), is(200));
        // parse XML response content
        Document document = dom(response, false);
        print(document);
        // check that default styles are advertised
        String result =
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Contents/wmts:Layer/wmts:Style[@isDefault='true']"
                                + "/ows:Identifier[text()='Default'])",
                        document);
        assertThat(Integer.parseInt(result), greaterThan(0));
        // check that GeoServer service metadata is available
        result =
                WMTS_XPATH_10.evaluate(
                        "count(//ows:ServiceProvider/ows:ProviderName[text()='http://geoserver.org'])",
                        document);
        assertThat(Integer.parseInt(result), is(1));
        // check that 0.0 and positive infinite scales are not advertised
        result =
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Contents/wmts:Layer/wmts:Style/"
                                + "wmts:LegendURL[@minScaleDenominator='0.0'])",
                        document);
        assertThat(Integer.parseInt(result), is(0));
        result =
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Contents/wmts:Layer/wmts:Style/"
                                + "wmts:LegendURL[@maxScaleDenominator='NaN'])",
                        document);
        assertThat(Integer.parseInt(result), is(0));
        // check that min and max scales are advertised
        result =
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Contents/wmts:Layer/wmts:Style/"
                                + "wmts:LegendURL[@minScaleDenominator='100000.0'][@maxScaleDenominator='300000.0'])",
                        document);
        assertThat(Integer.parseInt(result), greaterThan(0));
        // check the style group is reported
        result =
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Contents/wmts:Layer[ows:Identifier='stylegroup'])", document);
        assertThat(Integer.parseInt(result), equalTo(1));

        // check that legend URI are correctly encoded in the context of a local workspace
        WorkspaceInfo workspace = getCatalog().getWorkspaceByName(TEST_WORKSPACE_NAME);
        assertThat(workspace, notNullValue());
        LocalWorkspace.set(workspace);
        try {
            response =
                    getAsServletResponse(
                            TEST_WORKSPACE_NAME + "/gwc/service/wmts?request=GetCapabilities");
            document = dom(response, false);
            result =
                    WMTS_XPATH_10.evaluate(
                            "count(//wmts:Contents/wmts:Layer/wmts:Style/"
                                    + "wmts:LegendURL[contains(@xlink:href,'geoserver/"
                                    + TEST_WORKSPACE_NAME
                                    + "/ows')])",
                            document);
            assertThat(Integer.parseInt(result), greaterThan(0));
        } finally {
            // make sure we remove the local workspace
            LocalWorkspace.set(null);
        }
    }

    @Test
    public void testGetCapabilitiesRequestRestEndpoints() throws Exception {

        int totLayers = getCatalog().getLayers().size() + 1; // one cached layer group

        // getting capabilities document for CITE workspace
        Document doc = getAsDOM("/gwc/service/wmts?request=GetCapabilities");
        // checking ResourceURL
        assertEquals(
                String.valueOf(totLayers),
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Contents/wmts:Layer/wmts:ResourceURL[@resourceType='tile']"
                                + "[@format='image/png']"
                                + "[contains(@template,'http://localhost:8080/geoserver/gwc"
                                + WMTSService.REST_PATH
                                + "/')]"
                                + "[contains(@template,'/{style}/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}?format=image/png')])",
                        doc));

        assertEquals(
                String.valueOf(totLayers),
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Contents/wmts:Layer/wmts:ResourceURL[@resourceType='FeatureInfo']"
                                + "[@format='text/plain']"
                                + "[contains(@template,'http://localhost:8080/geoserver/gwc"
                                + WMTSService.REST_PATH
                                + "/')]"
                                + "[contains(@template,'/{style}/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}/{J}/{I}?format=text/plain')])",
                        doc));

        // checking the service metadata URL
        assertEquals(
                "1",
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:ServiceMetadataURL[@xlink:href='http://localhost:8080/geoserver/gwc"
                                + WMTSService.SERVICE_PATH
                                + "?SERVICE=wmts&REQUEST=getcapabilities&VERSION=1.0.0'])",
                        doc));
        assertEquals(
                "1",
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:ServiceMetadataURL[@xlink:href='http://localhost:8080/geoserver/gwc"
                                + WMTSService.REST_PATH
                                + "/WMTSCapabilities.xml'])",
                        doc));
    }

    @Test
    public void testGetCapabilitiesWithRestEndpoints() throws Exception {

        MockHttpServletRequest request =
                createRequest("/gwc" + WMTSService.REST_PATH + "/WMTSCapabilities.xml");
        request.setMethod("GET");
        request.setContent(new byte[] {});

        Request mockRequest = mock(Request.class);
        when(mockRequest.getHttpRequest()).thenReturn(request);
        Dispatcher.REQUEST.set(mockRequest);

        MockHttpServletResponse response = dispatch(request, null);
        // check that the request was successful
        assertThat(response.getStatus(), is(200));
        assertContentType("text/xml;charset=UTF-8", response);
    }

    @Test
    public void testGetCapabilitiesWithRestEndpointsWorkspaceService() throws Exception {
        MockHttpServletRequest request =
                createRequest(
                        MockData.CITE_PREFIX
                                + "/gwc"
                                + WMTSService.REST_PATH
                                + "/WMTSCapabilities.xml");
        request.setMethod("GET");

        Request mockRequest = mock(Request.class);
        when(mockRequest.getHttpRequest()).thenReturn(request);
        Dispatcher.REQUEST.set(mockRequest);

        MockHttpServletResponse response = dispatch(request, null);

        Document doc = dom(new ByteArrayInputStream(response.getContentAsByteArray()), true);
        print(doc);

        // check legend backlink and that it has the workspace specification
        assertEquals(
                "http://localhost:8080/geoserver/cite/ows?service=WMS&request=GetLegendGraphic&format=image%2Fpng&width=20&height=20&layer=cite%3ABasicPolygons",
                WMTS_XPATH_10.evaluate(
                        "//wmts:Contents/wmts:Layer[ows:Title='BasicPolygons']/wmts:Style/wmts:LegendURL/@xlink:href",
                        doc));
        // check tile resources
        assertEquals(
                "http://localhost:8080/geoserver/cite/gwc/service/wmts/rest/BasicPolygons/{style}/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}?format=image/png",
                WMTS_XPATH_10.evaluate(
                        "//wmts:Contents/wmts:Layer[ows:Title='BasicPolygons']/wmts:ResourceURL[@format='image/png' and @resourceType='tile']/@template",
                        doc));
        // check featureinfo resource
        assertEquals(
                "http://localhost:8080/geoserver/cite/gwc/service/wmts/rest/BasicPolygons/{style}/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}/{J}/{I}?format=text/plain",
                WMTS_XPATH_10.evaluate(
                        "//wmts:Contents/wmts:Layer[ows:Title='BasicPolygons']/wmts:ResourceURL[@format='text/plain' and @resourceType='FeatureInfo']/@template",
                        doc));
        // check backlink too
        assertEquals(
                "1",
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Capabilities/wmts:ServiceMetadataURL[@xlink:href='http://localhost:8080/geoserver/cite/gwc/service/wmts/rest/WMTSCapabilities.xml'])",
                        doc));
    }

    @Test
    public void testGetTileWithRestEndpoints() throws Exception {

        MockHttpServletRequest request =
                createRequest(
                        "/gwc"
                                + WMTSService.REST_PATH
                                + "/"
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "/EPSG:4326/EPSG:4326:0/0/0?format=image/png");
        request.setMethod("GET");
        request.setContent(new byte[] {});

        Request mockRequest = mock(Request.class);
        when(mockRequest.getHttpRequest()).thenReturn(request);
        Dispatcher.REQUEST.set(mockRequest);

        MockHttpServletResponse response = dispatch(request, null);
        // check that the request was successful
        assertThat(response.getStatus(), is(200));
        assertContentType("image/png", response);
    }

    @Test
    public void testGetTileWithRestEndpointsInVirtualService() throws Exception {
        // get tile
        MockHttpServletRequest request =
                createRequest(
                        MockData.BASIC_POLYGONS.getPrefix()
                                + "/gwc"
                                + WMTSService.REST_PATH
                                + "/"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "/EPSG:4326/EPSG:4326:0/0/0?format=image/png");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        // mock the request
        Request mockRequest = mock(Request.class);
        when(mockRequest.getHttpRequest()).thenReturn(request);
        Dispatcher.REQUEST.set(mockRequest);
        MockHttpServletResponse response = dispatch(request, null);
        // check that the request was successful
        assertThat(response.getStatus(), is(200));
        assertContentType("image/png", response);
    }

    @Test
    public void testFeatureInfoWithRestEndpoints() throws Exception {
        // get feature info
        MockHttpServletRequest request =
                createRequest(
                        "/gwc"
                                + WMTSService.REST_PATH
                                + "/"
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "/EPSG:4326/EPSG:4326:0/0/0/0/0?format=text/plain");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        // mock the request
        Request mockRequest = mock(Request.class);
        when(mockRequest.getHttpRequest()).thenReturn(request);
        Dispatcher.REQUEST.set(mockRequest);
        MockHttpServletResponse response = dispatch(request, null);
        // check that the request was successful
        assertThat(response.getStatus(), is(200));
        assertContentType("text/plain", response);
    }

    @Test
    public void testFeatureInfoWithRestEndpointsInVirtualService() throws Exception {
        // getting feature info
        MockHttpServletRequest request =
                createRequest(
                        MockData.BASIC_POLYGONS.getPrefix()
                                + "/gwc"
                                + WMTSService.REST_PATH
                                + "/"
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "/EPSG:4326/EPSG:4326:0/0/0/0/0?format=text/plain");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        // mock the request
        Request mockRequest = mock(Request.class);
        when(mockRequest.getHttpRequest()).thenReturn(request);
        Dispatcher.REQUEST.set(mockRequest);
        MockHttpServletResponse response = dispatch(request, null);
        // check that the request was successful
        assertThat(response.getStatus(), is(200));
        assertContentType("text/plain", response);
    }

    @Test
    public void testGetCapabilitiesWithRestEndpointsAndDimensions() throws Exception {
        GeoServerTileLayer tileLayer =
                (GeoServerTileLayer)
                        GWC.get().getTileLayerByName(getLayerId(MockData.BASIC_POLYGONS));
        GeoServerTileLayerInfo info = tileLayer.getInfo();

        // Add dimensions to layer
        StringParameterFilter time = new StringParameterFilter();
        time.setKey("time");
        time.setValues(Arrays.asList("2016-02-23T03:00:00.000Z"));
        info.addParameterFilter(time);

        StringParameterFilter elevation = new StringParameterFilter();
        elevation.setKey("elevation");
        elevation.setValues(Arrays.asList("500"));
        info.addParameterFilter(elevation);

        GWC.get().save(tileLayer);

        MockHttpServletRequest request =
                createRequest("/gwc" + WMTSService.REST_PATH + "/WMTSCapabilities.xml");
        request.setMethod("GET");
        request.setContent(new byte[] {});

        Request mockRequest = mock(Request.class);
        when(mockRequest.getHttpRequest()).thenReturn(request);
        Dispatcher.REQUEST.set(mockRequest);

        MockHttpServletResponse response = dispatch(request, null);

        ByteArrayInputStream bain =
                new ByteArrayInputStream(response.getContentAsString().getBytes());
        Document doc = dom(bain, true);

        assertEquals(
                "1",
                WMTS_XPATH_10.evaluate(
                        "count(//wmts:Contents/wmts:Layer/wmts:ResourceURL[@resourceType='tile']"
                                + "[@format='image/png']"
                                + "[contains(@template,'http://localhost:8080/geoserver/gwc"
                                + WMTSService.REST_PATH
                                + "/"
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "/{style}/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}?format=image/png')])",
                        doc));
    }

    /**
     * Test that using an invalid \ non-existing style in a GetTile request will throw the correct
     * OWS exception.
     */
    @Test
    public void testCetTileWithInvalidStyle() throws Exception {
        // using cite:BasicPolygons layer for testing
        String layerName = getLayerId(MockData.BASIC_POLYGONS);
        // get tile request with an invalid style, this should return an exception report
        MockHttpServletResponse response =
                getAsServletResponse(
                        "gwc/service/wmts"
                                + "?request=GetTile&layer="
                                + layerName
                                + "&style=invalid&format=image/png"
                                + "&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(400, response.getStatus());
        assertEquals("text/xml", response.getContentType());
        // parse XML response content
        Document document = dom(response, false);
        // let's check the content of the exception report
        String result =
                WMTS_XPATH_10.evaluate(
                        "count(//ows:ExceptionReport/ows:Exception"
                                + "[@exceptionCode='InvalidParameterValue'][@locator='Style'])",
                        document);
        assertThat(Integer.parseInt(result), is(1));
    }

    @Test
    public void testMassTruncateAll() throws Exception {
        final GWC gwc = GWC.get();
        final Catalog catalog = getCatalog();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String qualifiedName = super.getLayerId(WORKSPACED_LAYER_QNAME);

        final TileLayer tileLayer = gwc.getTileLayerByName(qualifiedName);

        assertNotNull(tileLayer);

        LayerInfo layer = catalog.getLayerByName(qualifiedName);

        assertNotNull(layer);

        String request =
                "gwc/service/wmts?request=GetTile&layer="
                        + qualifiedName
                        + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0";

        // first request
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals(200, response.getStatus());
        assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));

        // second request
        response = getAsServletResponse(request);
        assertEquals(200, response.getStatus());
        assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        waitTileBreederCompletion();

        TruncateAllRequest truncateAll = gwc.truncateAll();
        String truncatedLayers = truncateAll.getTrucatedLayersList();
        assertTrue(truncatedLayers.contains(qualifiedName));

        // checking for empty
        response = getAsServletResponse(request);
        assertEquals(200, response.getStatus());
        assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));
    }

    /** Helper method that creates a layer group using the provided name and layers names. */
    private void createLayerGroup(String layerGroupName, QName... layersNames) throws Exception {
        // get layers that match the layers names
        List<LayerInfo> layers =
                Arrays.stream(layersNames)
                        .map(layerName -> getCatalog().getLayerByName(new NameImpl(layerName)))
                        .collect(Collectors.toList());
        // create a new layer group using the provided name
        LayerGroupInfo layerGroup = getCatalog().getFactory().createLayerGroup();
        layerGroup.setName(layerGroupName);
        // add the provided layers
        for (LayerInfo layerInfo : layers) {
            layerGroup.getLayers().add(layerInfo);
            layerGroup.getStyles().add(null);
        }
        // set the layer group bounds by merging all layers bounds
        CatalogBuilder catalogBuilder = new CatalogBuilder(getCatalog());
        catalogBuilder.calculateLayerGroupBounds(layerGroup);
        getCatalog().add(layerGroup);
    }

    /**
     * Test verifying that GWC integration will work when layer belongs to default workspace and
     * layer name is passed without workspace being part of layer name or OWS request itself.
     */
    @Test
    public void testDirectDefaultWorkspaceWMSIntegration() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);
        final String layerName = BASIC_POLYGONS.getLocalPart();

        String request =
                "wms?service=WMS&request=GetMap&version=1.1.1&format=image/png"
                        + "&layers="
                        + layerName
                        + "&srs=EPSG:4326&width=256&height=256&styles="
                        + "&bbox=-180.0,-90.0,0.0,90.0&tiled=true";
        MockHttpServletResponse response = getAsServletResponse(request);

        response = getAsServletResponse(request);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertNotNull(response.getHeader("geowebcache-tile-index"));
        assertTrue(response.getHeader("geowebcache-cache-result").equalsIgnoreCase("HIT"));

        // with virtual services
        request =
                MockData.CITE_PREFIX
                        + "/wms?service=WMS&request=GetMap&version=1.1.1&format=image/png"
                        + "&layers="
                        + layerName
                        + "&srs=EPSG:4326&width=256&height=256&styles="
                        + "&bbox=-180.0,-90.0,0.0,90.0&tiled=true";
        response = getAsServletResponse(request);

        response = getAsServletResponse(request);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertNotNull(response.getHeader("geowebcache-tile-index"));
        assertTrue(response.getHeader("geowebcache-cache-result").equalsIgnoreCase("HIT"));
    }

    @Test
    public void testDirectDefaultWorkspaceWMSIntegrationLayerGroup() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);
        // build a layer group in the test workspace
        LayerGroupInfo lg = getCatalog().getFactory().createLayerGroup();
        lg.setName(WORKSPACED_LAYER_GROUP);
        String bpLayerId = getLayerId(MockData.BASIC_POLYGONS);
        String mpLayerId = getLayerId(MockData.LAKES);
        lg.getLayers().add(getCatalog().getLayerByName(bpLayerId));
        lg.getLayers().add(getCatalog().getLayerByName(mpLayerId));
        lg.getStyles().add(null);
        lg.getStyles().add(null);

        lg.setWorkspace(getCatalog().getWorkspaceByName(MockData.BASIC_POLYGONS.getPrefix()));
        new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(lg);
        getCatalog().add(lg);

        String request =
                MockData.BASIC_POLYGONS.getPrefix()
                        + "/wms?service=WMS&request=GetMap&version=1.1.1&format=image/png"
                        + "&layers="
                        + WORKSPACED_LAYER_GROUP
                        + "&srs=EPSG:4326&width=256&height=256&styles="
                        + "&bbox=-180.0,-90.0,0.0,90.0&tiled=true";
        MockHttpServletResponse response = getAsServletResponse(request);

        response = getAsServletResponse(request);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertNotNull(response.getHeader("geowebcache-tile-index"));
        assertTrue(response.getHeader("geowebcache-cache-result").equalsIgnoreCase("HIT"));
    }
}

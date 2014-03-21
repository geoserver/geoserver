/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.geoserver.data.test.MockData.MPOINTS;
import static org.geoserver.gwc.GWC.tileLayerName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.GeoWebCacheDispatcher;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration;
import org.geowebcache.diskquota.jdbc.JDBCConfiguration.ConnectionPoolConfiguration;
import org.geowebcache.diskquota.jdbc.JDBCQuotaStore;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class GWCIntegrationTest extends GeoServerSystemTestSupport {
    
    static final String FLAT_LAYER_GROUP = "flatLayerGroup";
    static final String NESTED_LAYER_GROUP = "nestedLayerGroup";
    static final String CONTAINER_LAYER_GROUP = "containerLayerGroup";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
    }
    
    @Before
    public void resetLayers() throws Exception {
        removeGroup(FLAT_LAYER_GROUP);
        removeGroup(CONTAINER_LAYER_GROUP);
        removeGroup(NESTED_LAYER_GROUP);
        
        final String layerName = getLayerId(BASIC_POLYGONS);
        LayerInfo layerInfo = getCatalog().getLayerByName(layerName);
        if(layerInfo != null) {
            getCatalog().remove(layerInfo);
            getGeoServer().reload();
            
        }

        revertLayer(BASIC_POLYGONS);
        revertLayer(MPOINTS);
    }

    private void removeGroup(String groupName) {
        LayerGroupInfo lg = getCatalog().getLayerGroupByName(groupName);
        if(lg != null) {
            getCatalog().remove(lg);
        }
    }

    @Test 
    public void testPngIntegration() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        MockHttpServletResponse sr = getAsServletResponse("gwc/service/wmts?request=GetTile&layer="
                + layerId
                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getErrorCode());
        assertEquals("image/png", sr.getContentType());
    }
    
    @Test 
    public void testGetLegendGraphics() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        MockHttpServletResponse sr = getAsServletResponse("gwc/service/wms?service=wms&version=1.1.1&request=GetLegendGraphic&layer="
                + layerId + "&style=&format=image/png");
        assertEquals(200, sr.getErrorCode());
        assertEquals("image/png", sr.getContentType());
    }
    
    @Test 
    public void testCachingHeadersSingleLayer() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        setCachingMetadata(layerId, true, 7200);
        
        MockHttpServletResponse sr = getAsServletResponse("gwc/service/wmts?request=GetTile&layer="
                + layerId
                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getErrorCode());
        assertEquals("image/png", sr.getContentType());
        assertEquals("max-age=7200, must-revalidate", sr.getHeader("Cache-Control"));
    }
    
    @Test 
    public void testCachingHeadersSingleLayerNoHeaders() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        setCachingMetadata(layerId, false, -1);
        
        MockHttpServletResponse sr = getAsServletResponse("gwc/service/wmts?request=GetTile&layer="
                + layerId
                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getErrorCode());
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
        new CatalogBuilder(getCatalog()).calculateLayerGroupBounds(lg);
        getCatalog().add(lg);        
        
        MockHttpServletResponse sr = getAsServletResponse("gwc/service/wmts?request=GetTile&layer="
                + FLAT_LAYER_GROUP
                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getErrorCode());
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
        builder.calculateLayerGroupBounds(nested);
        getCatalog().add(nested);
        
        // build the container layer group
        LayerGroupInfo container = getCatalog().getFactory().createLayerGroup();
        container.setName(CONTAINER_LAYER_GROUP);
        container.getLayers().add(getCatalog().getLayerByName(mpLayerId));
        container.getLayers().add(getCatalog().getLayerGroupByName(NESTED_LAYER_GROUP));
        builder.calculateLayerGroupBounds(container);
        getCatalog().add(container);
        
        
        // check the caching headers on the nested group
        MockHttpServletResponse sr = getAsServletResponse("gwc/service/wmts?request=GetTile&layer="
                + NESTED_LAYER_GROUP
                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getErrorCode());
        assertEquals("image/png", sr.getContentType());
        assertEquals("max-age=7200, must-revalidate", sr.getHeader("Cache-Control"));
        
        // check the caching headers on the container layer group
        sr = getAsServletResponse("gwc/service/wmts?request=GetTile&layer="
                + CONTAINER_LAYER_GROUP
                + "&format=image/png&tilematrixset=EPSG:4326&tilematrix=EPSG:4326:0&tilerow=0&tilecol=0");
        assertEquals(200, sr.getErrorCode());
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

        assertEquals(200, response.getStatusCode());
        assertEquals("image/png", response.getContentType());
        assertNull(response.getHeader("geowebcache-tile-index"));

        request = request + "&tiled=true";
        response = getAsServletResponse(request);

        assertEquals(200, response.getErrorCode());
        assertEquals("image/png", response.getContentType());
    }

    @Test 
    public void testDirectWMSIntegrationResponseHeaders() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String layerName = BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();

        String request = buildGetMap(true, layerName, "EPSG:4326", null) + "&tiled=true";
        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals(200, response.getStatusCode());
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

        String request = "wms?service=wms&version=1.3.0&request=GetMap&styles=&layers=" + layerName 
                + "&srs=EPSG:4326&bbox=-90,-180,90,0&format=image/png&width=256&height=256&tiled=true";
        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals(200, response.getStatusCode());
        assertEquals("image/png", response.getContentType());
        assertEquals(layerName, response.getHeader("geowebcache-layer"));
        assertEquals("[0, 0, 0]", response.getHeader("geowebcache-tile-index"));
        assertEquals("-180.0,-90.0,0.0,90.0", response.getHeader("geowebcache-tile-bounds"));
        assertEquals("EPSG:4326", response.getHeader("geowebcache-gridset"));
        assertEquals("EPSG:4326", response.getHeader("geowebcache-crs"));
    }

    @Test public void testDirectWMSIntegrationIfModifiedSinceSupport() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String layerName = BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();

        final String path = buildGetMap(true, layerName, "EPSG:4326", null) + "&tiled=true";

        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(200, response.getStatusCode());
        assertEquals("image/png", response.getContentType());

        String lastModifiedHeader = response.getHeader("Last-Modified");
        assertNotNull(lastModifiedHeader);
        Date lastModified = DateUtil.parseDate(lastModifiedHeader);

        MockHttpServletRequest httpReq = createRequest(path);
        httpReq.setMethod("GET");
        httpReq.setBodyContent(new byte[] {});
        httpReq.setHeader("If-Modified-Since", lastModifiedHeader);

        response = dispatch(httpReq, "UTF-8");

        assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getErrorCode());

        // set the If-Modified-Since header to some point in the past of the last modified value
        Date past = new Date(lastModified.getTime() - 5000);
        String ifModifiedSince = DateUtil.formatDate(past);

        httpReq.setHeader("If-Modified-Since", ifModifiedSince);
        response = dispatch(httpReq, "UTF-8");
        assertEquals(HttpServletResponse.SC_OK, response.getErrorCode());

        Date future = new Date(lastModified.getTime() + 5000);
        ifModifiedSince = DateUtil.formatDate(future);

        httpReq.setHeader("If-Modified-Since", ifModifiedSince);
        response = dispatch(httpReq, "UTF-8");
        assertEquals(HttpServletResponse.SC_NOT_MODIFIED, response.getErrorCode());
    }

    @Test public void testDirectWMSIntegrationMaxAge() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);
        final String layerName = BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();
        final String path = buildGetMap(true, layerName, "EPSG:4326", null) + "&tiled=true";
        final String qualifiedName = super.getLayerId(BASIC_POLYGONS);
        final GeoServerTileLayer tileLayer = (GeoServerTileLayer) gwc.getTileLayerByName(qualifiedName);
        tileLayer.getLayerInfo().getResource().getMetadata().put(ResourceInfo.CACHING_ENABLED, "true");
        tileLayer.getLayerInfo().getResource().getMetadata().put(ResourceInfo.CACHE_AGE_MAX, 3456);

        MockHttpServletResponse response = getAsServletResponse(path);
        String cacheControl = response.getHeader("Cache-Control");
        assertEquals("max-age=3456", cacheControl);
        assertNotNull(response.getHeader("Last-Modified"));

        tileLayer.getLayerInfo().getResource().getMetadata().put(ResourceInfo.CACHING_ENABLED, "false");
        response = getAsServletResponse(path);
        cacheControl = response.getHeader("Cache-Control");
        assertEquals("no-cache", cacheControl);

        // make sure a boolean is handled, too - see comment in CachingWebMapService
        tileLayer.getLayerInfo().getResource().getMetadata().put(ResourceInfo.CACHING_ENABLED, Boolean.FALSE);
        response = getAsServletResponse(path);
        cacheControl = response.getHeader("Cache-Control");
        assertEquals("no-cache", cacheControl);
    }

    @Test public void testDirectWMSIntegrationWithVirtualServices() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String qualifiedName = super.getLayerId(BASIC_POLYGONS);
        final String localName = BASIC_POLYGONS.getLocalPart();

        final TileLayer tileLayer = gwc.getTileLayerByName(qualifiedName);
        assertNotNull(tileLayer);
        boolean directWMSIntegrationEndpoint = true;
        String request = MockData.CITE_PREFIX
                + "/"
                + buildGetMap(directWMSIntegrationEndpoint, localName, "EPSG:4326", null, tileLayer)
                + "&tiled=true";

        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals(200, response.getStatusCode());
        assertEquals("image/png", response.getContentType());
        assertEquals(qualifiedName, response.getHeader("geowebcache-layer"));
    }

    @Test public void testDirectWMSIntegrationWithVirtualServicesHiddenLayer() throws Exception {
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
        String request = MockData.CDF_PREFIX // asking /geoserver/cdf/wms? for cite:BasicPolygons
                + "/"
                + buildGetMap(directWMSIntegrationEndpoint, localName, "EPSG:4326", null, tileLayer)
                + "&tiled=true";

        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals(200, response.getStatusCode());

        assertTrue(response.getContentType(),
                response.getContentType().startsWith("application/vnd.ogc.se_xml"));

        assertTrue(response.getOutputStreamContent(),
                response.getOutputStreamContent()
                        .contains("Could not find layer cdf:BasicPolygons"));
    }

    @Test public void testReloadConfiguration() throws Exception {
        String path = "/gwc/rest/reload";
        String content = "reload_configuration=1";
        String contentType = "application/x-www-form-urlencoded";
        MockHttpServletResponse response = postAsServletResponse(path, content, contentType);
        assertEquals(200, response.getStatusCode());
    }

    @Test public void testBasicIntegration() throws Exception {
        Catalog cat = getCatalog();
        TileLayerDispatcher tld = GeoWebCacheExtensions.bean(TileLayerDispatcher.class);
        assertNotNull(tld);

        GridSetBroker gridSetBroker = GeoWebCacheExtensions.bean(GridSetBroker.class);
        assertNotNull(gridSetBroker);

        try {
            tld.getTileLayer("");
        } catch (GeoWebCacheException gwce) {

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
                GridSubset epsg4326 = tl.getGridSubset(gridSetBroker.WORLD_EPSG4326.getName());
                assertTrue(epsg4326.getGridSetBounds().equals(
                        new BoundingBox(-180.0, -90.0, 180.0, 90.0)));
                String mime = tl.getMimeTypes().get(1).getMimeType();
                assertTrue(mime.startsWith("image/")
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

    private String buildGetMap(final boolean directWMSIntegrationEndpoint, final String layerName,
            final String gridsetId, String styles) {

        final GWC gwc = GWC.get();
        final TileLayer tileLayer = gwc.getTileLayerByName(layerName);
        return buildGetMap(directWMSIntegrationEndpoint, layerName, gridsetId, styles, tileLayer);
    }

    private String buildGetMap(final boolean directWMSIntegrationEndpoint,
            final String queryLayerName, final String gridsetId, String styles,
            final TileLayer tileLayer) {

        final GridSubset gridSubset = tileLayer.getGridSubset(gridsetId);

        long[] coverage = gridSubset.getCoverage(0);
        long[] tileIndex = { coverage[0], coverage[1], coverage[4] };
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

        assertNull(config.getTileLayer(layerName));
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
        ConfigurableQuotaStoreProvider provider = GeoServerExtensions.bean(ConfigurableQuotaStoreProvider.class);
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
        File jdbcConfigFile = dd.findDataFile("gwc/geowebcache-diskquota-jdbc.xml");
        assertNull("jdbc config should not be there", jdbcConfigFile);
        File h2DefaultStore = dd.findDataFile("gwc/diskquota_page_store_h2");
        assertNotNull("jdbc store should be there", h2DefaultStore);
        assertTrue(getActualStore(provider) instanceof JDBCQuotaStore);
        
        // disable again and clean up
        quota.setEnabled(false);
        gwc.saveDiskQuotaConfig(quota, null);
        FileUtils.deleteDirectory(h2DefaultStore);
        
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
        jdbcConfigFile = dd.findDataFile("gwc/geowebcache-diskquota-jdbc.xml");
        assertNotNull("jdbc config should be there", jdbcConfigFile);
        assertNull("jdbc store should be there", dd.findDataFile("gwc/diskquota_page_store_h2"));
        File newQuotaStore = new File("./target/quota-h2.data.db");
        assertTrue(newQuotaStore.exists());
        
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(jdbcConfigFile);
            Document dom = dom(fis);
            print(dom);
            String storedPassword = XMLUnit.newXpathEngine().evaluate("/gwcJdbcConfiguration/connectionPool/password", dom);
            // check the password has been encoded properly
            assertTrue(storedPassword.startsWith("crypt1:"));
        } finally {
            IOUtils.closeQuietly(fis);
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
        MockHttpServletResponse response = getAsServletResponse("gwc/service/wms?service=wms&version=1.1.0&request=GetCapabilities");
        System.out.println(response.getOutputStreamContent());
        assertEquals("application/vnd.ogc.wms_xml", response.getContentType());
        assertEquals("inline;filename=wms-getcapabilities.xml", response.getHeader("content-disposition"));
    }
    
    @Test
    public void testGutter() throws Exception {
        GeoServerTileLayer tileLayer = (GeoServerTileLayer) GWC.get().getTileLayerByName(getLayerId(BASIC_POLYGONS));
        GeoServerTileLayerInfo info = tileLayer.getInfo();
        info.setGutter(100);
        GWC.get().save(tileLayer);
                
        String request = "gwc/service/wms?LAYERS=cite%3ABasicPolygons&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&SRS=EPSG%3A4326&BBOX=0,0,11.25,11.25&WIDTH=256&HEIGHT=256";
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
                if(wsName.equals(layer.getResource().getStore().getWorkspace().getName())) {
                    String prefixedName = layer.prefixedName();
                    try {
                        // filter out geometryless layers and other stuff that cannot be hanlded by GWC
                        GWC.get().getTileLayerByName(prefixedName);
                        layerNames.add(layer.getName());
                    } catch(IllegalArgumentException e) {
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
            if(wsRenamed.equals(ws.getName())) {
                ws.setName(wsName);
                catalog.save(ws);
            }
        }
    }
}

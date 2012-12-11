/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static junit.framework.Assert.*;
import static org.geoserver.data.test.MockData.*;
import static org.geoserver.gwc.GWC.*;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.util.DateUtil;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.junit.Before;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class GWCIntegrationTest extends GeoServerSystemTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
    }
    
    @Before
    public void resetLayers() throws Exception {
        final String layerName = getLayerId(BASIC_POLYGONS);
        LayerInfo layerInfo = getCatalog().getLayerByName(layerName);
        if(layerInfo != null) {
            getCatalog().remove(layerInfo);
            getGeoServer().reload();
            
        }
        
        revertLayer(BASIC_POLYGONS);
        revertLayer(MPOINTS);
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

    /**
     * If direct WMS integration is enabled, a GetMap requests that hits the regular WMS but matches
     * a gwc tile should return with the proper {@code geowebcache-tile-index} HTTP response header.
     */
    @Test public void testDirectWMSIntegration() throws Exception {
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

    @Test public void testDirectWMSIntegrationResponseHeaders() throws Exception {
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
    
}
/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.geoserver.gwc.GWC.tileLayerName;
import junit.framework.Test;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GWCIntegrationTest extends GeoServerTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GWCIntegrationTest());
    }

    @Override
    protected void setUpInternal() {
        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
    }

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
    public void testDirectWMSIntegration() throws Exception {
        final GWC gwc = GWC.get();
        gwc.getConfig().setDirectWMSIntegrationEnabled(true);

        final String layerName = BASIC_POLYGONS.getPrefix() + ":" + BASIC_POLYGONS.getLocalPart();
        String request;
        MockHttpServletResponse response;

        request = buildGetMap(true, layerName, "EPSG:4326", null);
        response = getAsServletResponse(request);

        assertEquals(200, response.getErrorCode());
        assertEquals("image/png", response.getContentType());
        assertNull(response.getHeader("geowebcache-tile-index"));

        request = request + "&tiled=true";
        response = getAsServletResponse(request);

        assertEquals(200, response.getErrorCode());
        assertEquals("image/png", response.getContentType());
        assertEquals("[0, 0, 0]", response.getHeader("geowebcache-tile-index"));
    }

    public void testReloadConfiguration() throws Exception {
        String path = "/gwc/rest/reload";
        String content = "reload_configuration=1";
        String contentType = "application/x-www-form-urlencoded";
        MockHttpServletResponse response = postAsServletResponse(path, content, contentType);
        assertEquals(200, response.getStatusCode());
    }

    public void testBasicIntegration() throws Exception {
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
        LayerInfo li = cat.getLayers().get(1);
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
        final GridSubset gridSubset = tileLayer.getGridSubset(gridsetId);

        long[] coverage = gridSubset.getCoverage(0);
        long[] tileIndex = { coverage[0], coverage[1], coverage[4] };
        BoundingBox bounds = gridSubset.boundsFromIndex(tileIndex);

        final String endpoint = directWMSIntegrationEndpoint ? "wms" : "gwc/service/wms";

        StringBuilder sb = new StringBuilder(endpoint);
        sb.append("?service=WMS&request=GetMap&version=1.1.1&format=image/png");
        sb.append("&layers=").append(layerName);
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

}

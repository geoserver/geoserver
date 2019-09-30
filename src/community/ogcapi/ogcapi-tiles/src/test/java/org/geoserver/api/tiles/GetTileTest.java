/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.jayway.jsonpath.DocumentContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import no.ecc.vectortile.VectorTileDecoder;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;
import org.geowebcache.mime.ApplicationMime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class GetTileTest extends TilesTestSupport {

    @Before
    public void cleanup() throws Exception {
        // clean up all cached tiles to make sure there are no interactions between tests
        SystemTestData testData = getTestData();
        for (File f : new File(testData.getDataDirectoryRoot(), "gwc").listFiles()) {
            if (f.isDirectory()) {
                FileUtils.deleteQuietly(f);
            }
        }
    }

    @Test
    public void testPngIntegration() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        MockHttpServletResponse sr =
                getAsServletResponse(
                        "ogc/tiles/collections/"
                                + layerId
                                + "/map/BasicPolygons/tiles/EPSG:4326/EPSG:4326:0/0/0?f=image%2Fpng");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        checkRootTileHeaders(sr, "cite:BasicPolygons");
    }

    public void checkRootTileHeaders(MockHttpServletResponse sr, String layerName)
            throws IOException {
        // check the headers
        assertEquals("EPSG:4326", sr.getHeader("geowebcache-gridset"));
        assertEquals("EPSG:4326", sr.getHeader("geowebcache-crs"));
        assertEquals("[0, 0, 0]", sr.getHeader("geowebcache-tile-index"));
        assertEquals("-180.0,-90.0,0.0,90.0", sr.getHeader("geowebcache-tile-bounds"));
        assertEquals(layerName, sr.getHeader("geowebcache-layer"));
        assertEquals("MISS", sr.getHeader("geowebcache-cache-result"));
        assertEquals("no-cache", sr.getHeader("Cache-Control"));
        assertNotNull(sr.getHeader("ETag"));
        assertNotNull(sr.getHeader("Last-Modified"));
        assertValidPNGResponse(sr);
    }

    @Test
    public void testPngIntegrationWorkspaceSpecific() throws Exception {
        String layerId = MockData.BASIC_POLYGONS.getLocalPart();
        MockHttpServletResponse sr =
                getAsServletResponse(
                        MockData.BASIC_POLYGONS.getPrefix()
                                + "/ogc/tiles/collections/"
                                + layerId
                                + "/map/BasicPolygons/tiles/EPSG:4326/EPSG:4326:0/0/0?f=image%2Fpng");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());

        checkRootTileHeaders(sr, "BasicPolygons");
    }

    @Test
    public void testPngMissHit() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        String path =
                "ogc/tiles/collections/"
                        + layerId
                        + "/map/BasicPolygons/tiles/EPSG:4326/EPSG:4326:0/0/0?f=image%2Fpng";

        // first request, it's a miss, tile was not there
        MockHttpServletResponse sr1 = getAsServletResponse(path);
        assertEquals(200, sr1.getStatus());
        assertEquals("image/png", sr1.getContentType());
        assertEquals("MISS", sr1.getHeader("geowebcache-cache-result"));
        assertValidPNGResponse(sr1);

        // second request, tile was available in the tile cache
        MockHttpServletResponse sr2 = getAsServletResponse(path);
        assertEquals(200, sr2.getStatus());
        assertEquals("image/png", sr2.getContentType());
        assertEquals("HIT", sr2.getHeader("geowebcache-cache-result"));
        assertValidPNGResponse(sr2);
    }

    @Test
    public void testEtagIfNoneMatch() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        String path =
                "ogc/tiles/collections/"
                        + layerId
                        + "/map/BasicPolygons/tiles/EPSG:4326/EPSG:4326:0/0/0?f=image%2Fpng";

        // first request, it's a miss, tile was not there
        MockHttpServletResponse sr1 = getAsServletResponse(path);
        assertEquals(200, sr1.getStatus());
        assertEquals("image/png", sr1.getContentType());
        assertEquals("MISS", sr1.getHeader("geowebcache-cache-result"));
        assertValidPNGResponse(sr1);
        String eTag = sr1.getHeader("ETag");
        assertNotNull(eTag);

        // now do the same request but passing an etag, a 304 is expected back
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("GET");
        request.addHeader("If-None-Match", eTag);
        MockHttpServletResponse sr2 = dispatch(request);

        assertEquals(HttpStatus.NOT_MODIFIED.value(), sr2.getStatus());
    }

    public void assertValidPNGResponse(MockHttpServletResponse sr) throws IOException {
        // check it can actually be read as a PNG
        BufferedImage tile = ImageIO.read(new ByteArrayInputStream(sr.getContentAsByteArray()));
        assertNotNull(tile);
    }

    @Test
    public void testPngOnRawTilesIntegration() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        MockHttpServletResponse sr =
                getAsServletResponse(
                        "ogc/tiles/collections/"
                                + layerId
                                + "/tiles/EPSG:4326/EPSG:4326:0/0/0?f=image%2Fpng");
        assertEquals(400, sr.getStatus());
    }

    @Test
    public void testMapxboNotEnabled() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        MockHttpServletResponse sr =
                getAsServletResponse(
                        "ogc/tiles/collections/"
                                + layerId
                                + "/tiles/EPSG:4326/EPSG:4326:0/0/0?f="
                                + MapBoxTileBuilderFactory.MIME_TYPE);
        assertEquals(400, sr.getStatus());
    }

    @Test
    public void testMapboxTile() throws Exception {
        String layerId = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse sr =
                getAsServletResponse(
                        "ogc/tiles/collections/"
                                + layerId
                                + "/tiles/EPSG:900913/EPSG:900913:10/511/512?f="
                                + MapBoxTileBuilderFactory.MIME_TYPE);
        assertEquals(200, sr.getStatus());
        assertEquals(MapBoxTileBuilderFactory.MIME_TYPE, sr.getContentType());

        // check the headers
        checkRoadGwcHeaders(layerId, sr);

        // check it can actually be read as a vector tile
        VectorTileDecoder.FeatureIterable features =
                new VectorTileDecoder().decode(sr.getContentAsByteArray());
        // one road is before greenwich, not included in this tile
        assertEquals(4, features.asList().size());
    }

    @Test
    public void testJsonTile() throws Exception {
        String layerId = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse sr =
                getAsServletResponse(
                        "ogc/tiles/collections/"
                                + layerId
                                + "/tiles/EPSG:900913/EPSG:900913:10/511/512?f="
                                + ApplicationMime.geojson.getFormat());
        assertEquals(200, sr.getStatus());
        assertEquals(ApplicationMime.json.getFormat(), sr.getContentType());

        // check the headers
        checkRoadGwcHeaders(layerId, sr);

        // check it can actually be read as a geojson tile
        DocumentContext json = getAsJSONPath(sr);
        assertEquals("FeatureCollection", json.read("type"));
    }

    @Test
    public void testPng8Tile() throws Exception {
        String layerId = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse sr =
                getAsServletResponse(
                        "ogc/tiles/collections/"
                                + layerId
                                + "/map/RoadSegments/tiles/EPSG:900913/EPSG:900913:10/511/512?f=image/png8");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        checkRoadGwcHeaders(layerId, sr);

        // check it can actually be read as a PNG
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(sr.getContentAsByteArray()));
        assertNotBlank("testPng8Tile", image, null);
    }

    public void checkRoadGwcHeaders(String layerId, MockHttpServletResponse sr) {
        // check the headers
        assertEquals("EPSG:900913", sr.getHeader("geowebcache-gridset"));
        assertEquals("EPSG:900913", sr.getHeader("geowebcache-crs"));
        // gwc has y axis inverted, mind
        assertEquals("[512, 512, 10]", sr.getHeader("geowebcache-tile-index"));
        assertEquals(
                "0.0,0.0,39135.7584765628,39135.7584765628",
                sr.getHeader("geowebcache-tile-bounds"));
        assertEquals(layerId, sr.getHeader("geowebcache-layer"));
        assertEquals("no-cache", sr.getHeader("Cache-Control"));
        assertNotNull(sr.getHeader("ETag"));
        assertNotNull(sr.getHeader("Last-Modified"));
    }
}

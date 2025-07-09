/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.jayway.jsonpath.DocumentContext;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import no.ecc.vectortile.VectorTileDecoder;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;
import org.geotools.image.test.ImageAssert;
import org.geowebcache.mime.ApplicationMime;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
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
        MockHttpServletResponse sr = getAsServletResponse("ogc/tiles/v1/collections/"
                + layerId
                + "/styles/BasicPolygons/map/tiles/EPSG:4326/EPSG:4326:0/0/0?f=image%2Fpng");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertEquals(
                "Content-disposition",
                "inline; filename=\"BasicPolygons_0_0_0.png\"",
                sr.getHeader("Content-disposition"));
        checkRootTileHeaders(sr, "cite:BasicPolygons");
    }

    public void checkRootTileHeaders(MockHttpServletResponse sr, String layerName) throws IOException {
        // check the headers
        assertEquals("EPSG:4326", sr.getHeader("geowebcache-gridset"));
        assertEquals("EPSG:4326", sr.getHeader("geowebcache-crs"));
        assertEquals("[0, 0, 0]", sr.getHeader("geowebcache-tile-index"));
        assertEquals("-180.0,-90.0,0.0,90.0", sr.getHeader("geowebcache-tile-bounds"));
        assertEquals(layerName, sr.getHeader("geowebcache-layer"));
        assertEquals("MISS", sr.getHeader("geowebcache-cache-result"));
        assertEquals("no-cache, no-store, must-revalidate", sr.getHeader("Cache-Control"));
        assertNotNull(sr.getHeader("ETag"));
        assertNotNull(sr.getHeader("Last-Modified"));
        assertValidPNGResponse(sr);
    }

    @Test
    public void testPngIntegrationWorkspaceSpecific() throws Exception {
        String layerId = MockData.BASIC_POLYGONS.getLocalPart();
        MockHttpServletResponse sr = getAsServletResponse(MockData.BASIC_POLYGONS.getPrefix()
                + "/ogc/tiles/v1/collections/"
                + layerId
                + "/map/tiles/EPSG:4326/EPSG:4326:0/0/0?f=image%2Fpng");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());

        checkRootTileHeaders(sr, "BasicPolygons");
    }

    @Test
    public void testPngMissHit() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        String path = "ogc/tiles/v1/collections/"
                + layerId
                + "/styles/BasicPolygons/map/tiles/EPSG:4326/EPSG:4326:0/0/0?f=image%2Fpng";

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
        String path = "ogc/tiles/v1/collections/"
                + layerId
                + "/styles/BasicPolygons/map/tiles/EPSG:4326/EPSG:4326:0/0/0?f=image%2Fpng";

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
        MockHttpServletResponse sr = getAsServletResponse(
                "ogc/tiles/v1/collections/" + layerId + "/tiles/EPSG:4326/EPSG:4326:0/0/0?f=image%2Fpng");
        assertEquals(400, sr.getStatus());
    }

    @Test
    public void testMapxboNotEnabled() throws Exception {
        String layerId = getLayerId(MockData.BASIC_POLYGONS);
        MockHttpServletResponse sr = getAsServletResponse("ogc/tiles/v1/collections/"
                + layerId
                + "/tiles/EPSG:4326/EPSG:4326:0/0/0?f="
                + MapBoxTileBuilderFactory.MIME_TYPE);
        assertEquals(400, sr.getStatus());
    }

    @Test
    public void testMapboxTile() throws Exception {
        String layerId = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse sr = getAsServletResponse("ogc/tiles/v1/collections/"
                + layerId
                + "/tiles/EPSG:900913/EPSG:900913:10/511/512?f="
                + MapBoxTileBuilderFactory.MIME_TYPE);
        assertEquals(200, sr.getStatus());
        assertEquals(MapBoxTileBuilderFactory.MIME_TYPE, sr.getContentType());

        // check the headers
        checkGwcHeaders(layerId, sr);

        // check it can actually be read as a vector tile
        VectorTileDecoder.FeatureIterable features = new VectorTileDecoder().decode(sr.getContentAsByteArray());
        // one road is before greenwich, not included in this tile
        assertEquals(5, features.asList().size());
    }

    @Test
    public void testJsonTile() throws Exception {
        String layerId = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse sr = getAsServletResponse("ogc/tiles/v1/collections/"
                + layerId
                + "/tiles/EPSG:900913/EPSG:900913:10/511/512?f="
                + ApplicationMime.geojson.getFormat());
        assertEquals(200, sr.getStatus());
        assertEquals(ApplicationMime.json.getFormat(), sr.getContentType());

        // check the headers
        checkGwcHeaders(layerId, sr);

        // check it can actually be read as a geojson tile
        DocumentContext json = getAsJSONPath(sr);
        assertEquals("FeatureCollection", json.read("type"));
    }

    @Test
    public void testPng8Tile() throws Exception {
        String layerId = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse sr = getAsServletResponse("ogc/tiles/v1/collections/"
                + layerId
                + "/styles/RoadSegments/map/tiles/EPSG:900913/EPSG:900913:15/16383/16384?f=image/png8");
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());

        // check it can actually be read as a PNG
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(sr.getContentAsByteArray()));
        assertNotBlank("testPng8Tile", image, null);

        // check it looks as expected, with the roads actually filtered
        File expectedFile = new File("src/test/resources/org/geoserver/ogcapi/v1/tiles/rs_full.png");
        ImageAssert.assertEquals(expectedFile, image, 100);
    }

    @Test
    public void testCacheableFilteredPNGTile() throws Exception {
        String layerId = getLayerId(MockData.ROAD_SEGMENTS);
        String request = "ogc/tiles/v1/collections/"
                + layerId
                + "/map/tiles/EPSG:900913/EPSG:900913:15/16383/16384?f=image/png"
                + "&filter=NAME='Route 5'&filter-lang=cql-text";
        MockHttpServletResponse sr = getAsServletResponse(request);
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());

        // first time miss, no reason
        assertEquals("MISS", sr.getHeader("geowebcache-cache-result"));
        assertNull(sr.getHeader("geowebcache-miss-reason"));

        // run again, this time it should be a hit
        sr = getAsServletResponse(request);
        assertEquals(200, sr.getStatus());
        assertEquals("image/png", sr.getContentType());
        assertEquals("HIT", sr.getHeader("geowebcache-cache-result"));

        // check it can be read as a png image
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(sr.getContentAsByteArray()));
        assertNotBlank("testPng8Tile", image, null);

        // check it looks as expected, with the roads actually filtered
        File expectedFile = new File("src/test/resources/org/geoserver/ogcapi/v1/tiles/rs_filtered.png");
        ImageAssert.assertEquals(expectedFile, image, 100);
    }

    @Test
    public void testNonCacheableFilteredPNGTile() throws Exception {
        String layerId = getLayerId(MockData.STREAMS);
        String request =
                "ogc/tiles/v1/collections/" + layerId + "/map/tiles/EPSG:900913/EPSG:900913:15/16384/16384?f=image/png";
        RenderedImage fullImage = getAsImage(request, "image/png");
        File expectedFile = new File("src/test/resources/org/geoserver/ogcapi/v1/tiles/streams_full.png");
        ImageAssert.assertEquals(expectedFile, fullImage, 100);

        MockHttpServletResponse resp = getAsServletResponse(request + "&filter=NAME='Cam Stream'&filter-lang=cql-text");
        assertEquals("image/png", resp.getContentType());
        assertEquals("MISS", resp.getHeader("geowebcache-cache-result"));
        assertEquals(
                "CQL_FILTER filter parameter not cached or not condition not matched",
                resp.getHeader("geowebcache-miss-reason"));

        try (InputStream is = getBinaryInputStream(resp)) {
            RenderedImage filteredImage = ImageIO.read(is);
            File expectedFilteredFile =
                    new File("src/test/resources/org/geoserver/ogcapi/v1/tiles/streams_filterd.png");
            ImageAssert.assertEquals(expectedFilteredFile, filteredImage, 100);
        }
    }

    @Test
    public void testEmtpyMVTTile() throws Exception {
        String request = "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.ROAD_SEGMENTS)
                + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                + "&CQL_FILTER=1=0&format="
                + MapBoxTileBuilderFactory.MIME_TYPE;
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals(200, response.getStatus());
        assertEquals(MapBoxTileBuilderFactory.MIME_TYPE, response.getContentType());
        byte[] responseBytes = response.getContentAsByteArray();
        VectorTileDecoder decoder = new VectorTileDecoder();
        List<VectorTileDecoder.Feature> featuresList =
                decoder.decode(responseBytes).asList();
        assertEquals(0, featuresList.size());

        String layerId = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse sr = getAsServletResponse("ogc/tiles/v1/collections/"
                + layerId
                + "/tiles/EPSG:900913/EPSG:900913:10/511/512?f="
                + MapBoxTileBuilderFactory.MIME_TYPE
                + "&filter=1=0&filter-lang=cql-text");
        assertEquals(200, sr.getStatus());
        assertEquals(MapBoxTileBuilderFactory.MIME_TYPE, sr.getContentType());

        // check the headers
        checkGwcHeaders(layerId, sr);

        // check it can actually be read as a vector tile
        VectorTileDecoder.FeatureIterable features = new VectorTileDecoder().decode(sr.getContentAsByteArray());
        // one road is before greenwich, not included in this tile
        assertEquals(0, features.asList().size());
    }

    public void checkGwcHeaders(String layerId, MockHttpServletResponse sr) {
        // check the headers
        assertEquals("EPSG:900913", sr.getHeader("geowebcache-gridset"));
        assertEquals("EPSG:900913", sr.getHeader("geowebcache-crs"));
        // gwc has y axis inverted, mind
        assertEquals("[512, 512, 10]", sr.getHeader("geowebcache-tile-index"));
        assertEquals("0.0,0.0,39135.7584765628,39135.7584765628", sr.getHeader("geowebcache-tile-bounds"));
        assertEquals(layerId, sr.getHeader("geowebcache-layer"));
        assertEquals("no-cache, no-store, must-revalidate", sr.getHeader("Cache-Control"));
        assertNotNull(sr.getHeader("ETag"));
        assertNotNull(sr.getHeader("Last-Modified"));
    }

    @Test
    public void testTileOutOfRange() throws Exception {
        String layerId = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse sr = getAsServletResponse("ogc/tiles/v1/collections/"
                + layerId
                + "/tiles/EPSG:900913/EPSG:900913:10/0/0?f="
                + MapBoxTileBuilderFactory.MIME_TYPE);
        assertEquals(404, sr.getStatus());
    }

    @Test
    public void testNatureGroupMVT() throws Exception {
        MockHttpServletResponse sr = getAsServletResponse("ogc/tiles/v1/collections/"
                + NATURE_GROUP
                + "/tiles/EPSG:900913/EPSG:900913:10/511/512?f="
                + MapBoxTileBuilderFactory.MIME_TYPE);
        assertEquals(200, sr.getStatus());
        assertEquals(MapBoxTileBuilderFactory.MIME_TYPE, sr.getContentType());

        // check the headers
        checkGwcHeaders(NATURE_GROUP, sr);

        // check it can actually be read as a vector tile
        VectorTileDecoder.FeatureIterable features = new VectorTileDecoder().decode(sr.getContentAsByteArray());
        assertThat(features.getLayerNames(), Matchers.containsInAnyOrder("Lakes", "Forests"));
        // data has just one lake and one forest
        assertEquals(2, features.asList().size());
    }

    @Test
    public void testNatureGroupPNG() throws Exception {
        BufferedImage image = getAsImage(
                "ogc/tiles/v1/collections/"
                        + NATURE_GROUP
                        + "/map/tiles/EPSG:900913/EPSG:900913:16/32768/32768?f=image/png",
                "image/png");
        File expected = new File("src/test/resources/org/geoserver/ogcapi/v1/tiles/nature_tile_16.png");
        ImageAssert.assertEquals(expected, image, 120);
    }

    @Test
    public void testStyleGroupMVT() throws Exception {
        MockHttpServletResponse sr = getAsServletResponse("ogc/tiles/v1/collections/"
                + BASIC_STYLE_GROUP
                + "/tiles/EPSG:900913/EPSG:900913:10/511/512?f="
                + MapBoxTileBuilderFactory.MIME_TYPE);
        assertEquals(200, sr.getStatus());
        assertEquals(MapBoxTileBuilderFactory.MIME_TYPE, sr.getContentType());

        // check the headers
        checkGwcHeaders(BASIC_STYLE_GROUP, sr);

        // check it can actually be read as a vector tile
        VectorTileDecoder.FeatureIterable features = new VectorTileDecoder().decode(sr.getContentAsByteArray());
        assertThat(features.getLayerNames(), Matchers.containsInAnyOrder("BasicPolygons", "Lakes"));
        // data has 3 basic polygons and one lake
        assertEquals(2, features.asList().size());
    }

    @Test
    public void testStyleGroupPNG() throws Exception {
        BufferedImage image = getAsImage(
                "ogc/tiles/v1/collections/"
                        + BASIC_STYLE_GROUP
                        + "/styles/"
                        + BASIC_STYLE_GROUP_STYLE
                        + "/map/tiles/EPSG:900913/EPSG:900913:16/32768/32768?f=image/png",
                "image/png");
        File expected = new File("src/test/resources/org/geoserver/ogcapi/v1/tiles/styleGrup_tile_16.png");
        ImageAssert.assertEquals(expected, image, 100);
    }

    @Test
    public void testStyleGroupInvalidStyleName() throws Exception {
        DocumentContext json = getAsJSONPath(
                "ogc/tiles/v1/collections/"
                        + BASIC_STYLE_GROUP
                        + "/styles/"
                        + "_"
                        + "/map/tiles/EPSG:900913/EPSG:900913:16/32768/32768?f=image/png",
                400);
        assertEquals("InvalidParameterValue", json.read("type"));
        assertThat(
                json.read("title"),
                CoreMatchers.allOf(containsString("Invalid style name"), containsString("BasicStyleGroupStyle")));
    }
}

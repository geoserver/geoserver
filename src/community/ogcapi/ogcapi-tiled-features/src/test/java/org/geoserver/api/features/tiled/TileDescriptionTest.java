package org.geoserver.api.features.tiled;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.MockData;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class TileDescriptionTest extends TiledFeaturesTestSupport {

    @Test
    public void testGetTileMatrixSets() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/tileMatrixSets", 200);

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/features\\/tileMatrixSets\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/features\\/tileMatrixSets\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        // check some of the basic tile matrix sets are there
        assertThat(
                json.read("tileMatrixSets[*].identifier"),
                hasItems("GlobalCRS84Pixel", "EPSG:4326", "EPSG:900913"));

        // verify a specific one
        assertThat(
                json.read(
                        "tileMatrixSets[?(@.identifier == 'EPSG:4326')].links[?(@.type == 'application/json')].href"),
                contains(
                        "http://localhost:8080/geoserver/ogc/tiles/tileMatrixSets/EPSG%3A4326?f=application%2Fjson"));
        assertThat(
                json.read(
                        "tileMatrixSets[?(@.identifier == 'EPSG:4326')].links[?(@.type == 'application/json')].rel"),
                contains("tileMatrixSet"));
    }

    @Test
    public void testGetTileMatrixSetsHTML() throws Exception {
        Document document = getAsJSoup("ogc/features/tileMatrixSets?f=html");
        // TODO: add ids and actual checks in the generated HTML
    }

    @Test
    public void testGetTileMatrixSet() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/tileMatrixSets/EPSG:4326", 200);

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/features\\/tileMatrixSets\\/EPSG%3A4326\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/features\\/tileMatrixSets\\/EPSG%3A4326\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        // check basic properties
        assertThat(json.read("identifier"), equalTo("EPSG:4326"));
        assertThat(
                json.read("supportedCRS"), equalTo("http://www.opengis.net/def/crs/EPSG/0/4326"));
        assertThat(json.read("title"), startsWith("A default WGS84"));

        // check a tile matrix definitions
        assertThat(json.read("tileMatrix[0].identifier"), equalTo("EPSG:4326:0"));
        assertThat(
                json.read("tileMatrix[0].scaleDenominator", Double.class),
                closeTo(2.7954112E8, 1e3));
        assertThat(json.read("tileMatrix[0].tileWidth"), equalTo(256));
        assertThat(json.read("tileMatrix[0].tileWidth"), equalTo(256));
        assertThat(json.read("tileMatrix[0].matrixWidth"), equalTo(2));
        assertThat(json.read("tileMatrix[0].matrixHeight"), equalTo(1));
        assertThat(
                json.read("tileMatrix[0].topLeftCorner"),
                contains(closeTo(90, 0), closeTo(-180, 0)));
    }

    @Test
    public void getDataTilesMetadata() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath("ogc/features/collections/" + roadSegments + "/tiles", 200);

        // check the raw tiles links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/collections/cite%3ARoadSegments/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=application%2Fvnd.mapbox-vector-tile",
                readSingle(
                        json,
                        "$.links[?(@.rel=='item' && @.type=='application/vnd.mapbox-vector-tile')].href"));

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/features\\/collections\\/cite:RoadSegments\\/tiles\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/features\\/collections\\/cite:RoadSegments\\/tiles\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        checkRoadSegmentsTileMatrix(json);
    }

    @Test
    public void getDataTilesMetadataHTML() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        Document doc =
                getAsJSoup("ogc/features/collections/" + roadSegments + "/tiles?f=text/html");
        // TODO: add ids in the elemnets and check contents using jSoup
    }

    public void checkRoadSegmentsTileMatrix(DocumentContext json) {
        // check the tile matrices
        assertEquals(Integer.valueOf(2), json.read("$.tileMatrixSetLinks.size()"));
        // EPSG:4326
        assertEquals("EPSG:4326", json.read("$.tileMatrixSetLinks[0].tileMatrixSet"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/tileMatrixSets/EPSG%3A4326",
                json.read("$.tileMatrixSetLinks[0].tileMatrixSetURI"));
        assertEquals(
                Integer.valueOf(22),
                json.read("$.tileMatrixSetLinks[0].tileMatrixSetLimits.size()"));
        String crs84Limit0 = "$.tileMatrixSetLinks[0].tileMatrixSetLimits[0]";
        assertEquals("EPSG:4326:0", json.read(crs84Limit0 + ".tileMatrix"));
        // both tiles as it spans the origin
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".minTileRow"));
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".maxTileRow"));
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".minTileCol"));
        assertEquals(Integer.valueOf(1), json.read(crs84Limit0 + ".maxTileCol"));
        // one more zoom level just for satefy
        String crs84Limit10 = "$.tileMatrixSetLinks[0].tileMatrixSetLimits[10]";
        assertEquals("EPSG:4326:10", json.read(crs84Limit10 + ".tileMatrix"));
        assertEquals(Integer.valueOf(511), json.read(crs84Limit10 + ".minTileRow"));
        assertEquals(Integer.valueOf(512), json.read(crs84Limit10 + ".maxTileRow"));
        assertEquals(Integer.valueOf(1023), json.read(crs84Limit10 + ".minTileCol"));
        assertEquals(Integer.valueOf(1024), json.read(crs84Limit10 + ".maxTileCol"));
        // checking one in web marcator too (only one root tile)
        String webMercatorLimit10 = "$.tileMatrixSetLinks[1].tileMatrixSetLimits[10]";
        assertEquals("EPSG:900913:10", json.read(webMercatorLimit10 + ".tileMatrix"));
        assertEquals(Integer.valueOf(511), json.read(webMercatorLimit10 + ".minTileRow"));
        assertEquals(Integer.valueOf(512), json.read(webMercatorLimit10 + ".maxTileRow"));
        assertEquals(Integer.valueOf(511), json.read(webMercatorLimit10 + ".minTileCol"));
        assertEquals(Integer.valueOf(512), json.read(webMercatorLimit10 + ".maxTileCol"));
    }
}

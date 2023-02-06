package org.geoserver.ogcapi.v1.features.tiled;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.MockData;
import org.geoserver.ogcapi.v1.tiles.Tileset;
import org.junit.Test;

public class TilesetsDescriptionTest extends TiledFeaturesTestSupport {

    @Test
    public void testGetTileMatrixSets() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1/tileMatrixSets", 200);

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/features\\/v1\\/tileMatrixSets\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/features\\/v1\\/tileMatrixSets\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        // check some of the basic tile matrix sets are there
        assertThat(
                json.read("tileMatrixSets[*].id"),
                hasItems("GlobalCRS84Pixel", "EPSG:4326", "EPSG:900913"));

        // verify a specific one
        assertThat(
                json.read(
                        "tileMatrixSets[?(@.id == 'EPSG:4326')].links[?(@.type == 'application/json')].href"),
                contains(
                        "http://localhost:8080/geoserver/ogc/tiles/v1/tileMatrixSets/EPSG%3A4326?f=application%2Fjson"));
        assertThat(
                json.read(
                        "tileMatrixSets[?(@.id == 'EPSG:4326')].links[?(@.type == 'application/json')].rel"),
                contains("tileMatrixSet"));
    }

    @Test
    public void testGetTileMatrixSetsHTML() throws Exception {
        getAsJSoup("ogc/features/v1/tileMatrixSets?f=html");
        // TODO: add ids and actual checks in the generated HTML
    }

    @Test
    @SuppressWarnings("unchecked") // varargs generic in matcher
    public void testGetTileMatrixSet() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1/tileMatrixSets/EPSG:4326", 200);

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/features\\/v1\\/tileMatrixSets\\/EPSG%3A4326\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/features\\/v1\\/tileMatrixSets\\/EPSG%3A4326\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        // check basic properties
        assertThat(json.read("id"), equalTo("EPSG:4326"));
        assertThat(
                json.read("supportedCRS"), equalTo("http://www.opengis.net/def/crs/OGC/1.3/CRS84"));
        assertThat(json.read("title"), startsWith("A default WGS84"));

        // check a tile matrix definitions
        assertThat(json.read("tileMatrices[0].id"), equalTo("EPSG:4326:0"));
        assertThat(
                json.read("tileMatrices[0].scaleDenominator", Double.class),
                closeTo(2.7954112E8, 1e3));
        assertThat(json.read("tileMatrices[0].tileWidth"), equalTo(256));
        assertThat(json.read("tileMatrices[0].tileWidth"), equalTo(256));
        assertThat(json.read("tileMatrices[0].matrixWidth"), equalTo(2));
        assertThat(json.read("tileMatrices[0].matrixHeight"), equalTo(1));
        assertThat(
                json.read("tileMatrices[0].pointOfOrigin"),
                contains(closeTo(90, 0), closeTo(-180, 0)));
    }

    @Test
    public void getDataTilesMetadata() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath("ogc/features/v1/collections/" + roadSegments + "/tiles", 200);

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/features\\/v1\\/collections\\/cite:RoadSegments\\/tiles\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/features\\/v1\\/collections\\/cite:RoadSegments\\/tiles\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        // check the tile matrices
        assertEquals(Integer.valueOf(2), json.read("$.tilesets.size()"));
        String matrixDefinition =
                "http://localhost:8080/geoserver/ogc/features/v1/tileMatrixSets/EPSG%3A4326";
        assertEquals(matrixDefinition, json.read("$.tilesets[0].tileMatrixSetURI"));
        assertEquals(matrixDefinition, json.read("$.tilesets[0].tileMatrixSetDefinition"));
        assertEquals(Tileset.DataType.vector.toString(), json.read("$.tilesets[0].dataType"));
        String tilesetLink =
                readSingle(json, "$.tilesets[0].links[?(@.type == 'application/json')].href");
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite:RoadSegments/tiles/EPSG:4326?f=application%2Fjson",
                tilesetLink);
    }

    @Test
    public void getDataTilesMetadataHTML() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        getAsJSoup("ogc/features/v1/collections/" + roadSegments + "/tiles?f=text/html");
        // TODO: add ids in the elements and check contents using jSoup
    }
}

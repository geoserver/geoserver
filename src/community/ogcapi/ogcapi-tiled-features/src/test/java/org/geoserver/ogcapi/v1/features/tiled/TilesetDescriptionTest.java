package org.geoserver.ogcapi.v1.features.tiled;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.Arrays;
import org.geoserver.data.test.MockData;
import org.geoserver.ogcapi.v1.tiles.Tileset;
import org.junit.Test;

public class TilesetDescriptionTest extends TiledFeaturesTestSupport {

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
                getAsJSONPath(
                        "ogc/features/v1/collections/" + roadSegments + "/tiles/EPSG:4326", 200);

        // check the raw tiles links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments/tiles/EPSG:4326/{tileMatrix}/{tileRow}/{tileCol}?f=application%2Fvnd.mapbox-vector-tile",
                readSingle(
                        json,
                        "$.links[?(@.rel=='item' && @.type=='application/vnd.mapbox-vector-tile')].href"));

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/features\\/v1\\/collections\\/cite:RoadSegments\\/tiles\\/EPSG:4326\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/features\\/v1\\/collections\\/cite:RoadSegments\\/tiles\\/EPSG:4326\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        // test the describedBy template
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite:RoadSegments/tiles/EPSG:4326/metadata?f=application%2Fjson",
                readSingle(
                        json,
                        "$.links[?(@.rel=='describedBy' && @.type=='application/json')].href"));
        assertEquals(
                Boolean.TRUE,
                readSingle(
                        json,
                        "$.links[?(@.rel=='describedBy' && @.type=='application/json')].templated"));

        checkRoadSegmentsTileMatrix(json, Tileset.DataType.vector);
    }

    @Test
    public void getDataTilesMetadataHTML() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        getAsJSoup("ogc/features/v1/collections/" + roadSegments + "/tiles?f=text/html");
        // TODO: add ids in the elemnets and check contents using jSoup
    }

    public void checkRoadSegmentsTileMatrix(DocumentContext json, Tileset.DataType dataType) {
        // check the tile matrices
        String matrixDefinition =
                "http://localhost:8080/geoserver/ogc/features/v1/tileMatrixSets/EPSG%3A4326";
        assertEquals(matrixDefinition, json.read("tileMatrixSetURI"));
        assertEquals(matrixDefinition, json.read("tileMatrixSetDefinition"));
        assertEquals(dataType.toString(), json.read("dataType"));
        assertEquals(Integer.valueOf(22), json.read("tileMatrixSetLimits.size()"));
        String crs84Limit0 = "tileMatrixSetLimits[0]";
        assertEquals("EPSG:4326:0", json.read(crs84Limit0 + ".tileMatrix"));
        // both tiles as it spans the origin
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".minTileRow"));
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".maxTileRow"));
        assertEquals(Integer.valueOf(0), json.read(crs84Limit0 + ".minTileCol"));
        assertEquals(Integer.valueOf(1), json.read(crs84Limit0 + ".maxTileCol"));
        // one more zoom level just for satefy
        String crs84Limit10 = "tileMatrixSetLimits[10]";
        assertEquals("EPSG:4326:10", json.read(crs84Limit10 + ".tileMatrix"));
        assertEquals(Integer.valueOf(511), json.read(crs84Limit10 + ".minTileRow"));
        assertEquals(Integer.valueOf(512), json.read(crs84Limit10 + ".maxTileRow"));
        assertEquals(Integer.valueOf(1023), json.read(crs84Limit10 + ".minTileCol"));
        assertEquals(Integer.valueOf(1024), json.read(crs84Limit10 + ".maxTileCol"));
    }

    @Test
    public void testTileJSONSingleLayer() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/features/v1/collections/cite:RoadSegments/tiles/EPSG:4326/metadata?f=application%2Fjson",
                        200);
        assertThat(doc.read("name"), equalTo("cite:RoadSegments"));
        assertThat(doc.read("scheme"), equalTo("xyz"));
        assertThat(
                readSingle(doc, "tiles"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments/tiles/EPSG:4326/{z}/{y}/{x}?f=application%2Fvnd.mapbox-vector-tile"));
        assertThat(doc.read("center"), equalTo(Arrays.asList(0d, 0d, 0d)));
        assertThat(doc.read("bounds"), equalTo(Arrays.asList(-0.0042, -0.0024, 0.0042, 0.0024)));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'RoadSegments')].fields"),
                allOf(hasEntry("FID", "string"), hasEntry("NAME", "string")));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'RoadSegments')].geometry_type"),
                equalTo("line"));
    }
}

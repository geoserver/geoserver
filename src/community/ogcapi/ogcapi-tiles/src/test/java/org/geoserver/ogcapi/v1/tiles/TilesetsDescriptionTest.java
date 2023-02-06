package org.geoserver.ogcapi.v1.tiles;

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
import org.junit.Test;

public class TilesetsDescriptionTest extends TilesTestSupport {

    @Test
    public void testGetTileMatrixSets() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/tiles/v1/tileMatrixSets", 200);

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/tiles\\/v1\\/tileMatrixSets\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/tiles\\/v1\\/tileMatrixSets\\?.*/ && @.rel == 'alternate')].type"),
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
        getAsJSoup("ogc/tiles/v1/tileMatrixSets?f=html");
        // TODO: add ids and actual checks in the generated HTML
    }

    @Test
    @SuppressWarnings("unchecked") // matcher varargs
    public void testGetTileMatrixSet() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/tiles/v1/tileMatrixSets/EPSG:4326", 200);

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/tiles\\/v1\\/tileMatrixSets\\/EPSG%3A4326\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/tiles\\/v1\\/tileMatrixSets\\/EPSG%3A4326\\?.*/ && @.rel == 'alternate')].type"),
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
                getAsJSONPath("ogc/tiles/v1/collections/" + roadSegments + "/tiles", 200);

        //        // check the raw tiles links
        //        assertEquals(
        //
        // "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite%3ARoadSegments/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}?f=application%2Fvnd.mapbox-vector-tile",
        //                readSingle(
        //                        json,
        //                        "$.links[?(@.rel=='item' &&
        // @.type=='application/vnd.mapbox-vector-tile')].href"));

        // test self link and links to alternate formats and
        assertThat(
                json.read(
                        "links[?(@.type == 'application/json' && @.href =~ /.*ogc\\/tiles\\/v1\\/collections\\/cite:RoadSegments\\/tiles\\?.*/)].rel"),
                containsInAnyOrder("self"));
        assertThat(
                json.read(
                        "links[?(@.type != 'application/json' && @.href =~ /.*ogc\\/tiles\\/v1\\/collections\\/cite:RoadSegments\\/tiles\\?.*/ && @.rel == 'alternate')].type"),
                hasItems("application/x-yaml"));

        //        // test the describedBy template
        //        assertEquals(
        //
        // "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite:RoadSegments/tiles/{tileMatrixSetId}/metadata?f=application%2Fjson",
        //                readSingle(
        //                        json,
        //                        "$.links[?(@.rel=='describedBy' &&
        // @.type=='application/json')].href"));
        //        assertEquals(
        //                Boolean.TRUE,
        //                readSingle(
        //                        json,
        //                        "$.links[?(@.rel=='describedBy' &&
        // @.type=='application/json')].templated"));

        checkRoadSegmentsTileMatrix(json, Tileset.DataType.vector);
    }

    @Test
    public void testTileJSONSingleLayer() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/tiles/v1/collections/cite:RoadSegments/tiles/EPSG:4326/metadata?f=application%2Fjson",
                        200);
        assertThat(doc.read("name"), equalTo("cite:RoadSegments"));
        assertThat(doc.read("scheme"), equalTo("xyz"));
        assertThat(
                readSingle(doc, "tiles"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite%3ARoadSegments/tiles/EPSG:4326/{z}/{y}/{x}?f=application%2Fvnd.mapbox-vector-tile"));
        assertThat(doc.read("minzoom"), equalTo(0));
        assertThat(doc.read("maxzoom"), equalTo(21));
        assertThat(doc.read("center"), equalTo(Arrays.asList(0d, 0d, 0d)));
        assertThat(doc.read("bounds"), equalTo(Arrays.asList(-0.0042, -0.0024, 0.0042, 0.0024)));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'RoadSegments')].fields"),
                allOf(hasEntry("FID", "string"), hasEntry("NAME", "string")));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'RoadSegments')].geometry_type"),
                equalTo("line"));
    }

    @Test
    @SuppressWarnings("unchecked") // matcher varargs
    public void testTileJSONLayerGroup() throws Exception {
        DocumentContext doc =
                getAsJSONPath(
                        "/ogc/tiles/v1/collections/"
                                + NATURE_GROUP
                                + "/tiles/EPSG:900913/metadata?f=application%2Fjson",
                        200);
        assertThat(doc.read("name"), equalTo("nature"));
        assertThat(doc.read("scheme"), equalTo("xyz"));
        assertThat(
                readSingle(doc, "tiles"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/tiles/v1/collections/nature/tiles/EPSG:900913/{z}/{y}/{x}?f=application%2Fvnd.mapbox-vector-tile"));
        assertThat(
                doc.read("center"),
                hasItems(closeTo(0d, 1e-6), closeTo(0d, 1e-6), closeTo(0d, 1e-6)));
        assertThat(doc.read("bounds"), equalTo(Arrays.asList(-180.0, -90.0, 180.0, 90.0)));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'Lakes')].fields"),
                allOf(hasEntry("FID", "string"), hasEntry("NAME", "string")));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'Lakes')].geometry_type"),
                equalTo("polygon"));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'Forests')].fields"),
                allOf(hasEntry("FID", "string"), hasEntry("NAME", "string")));
        assertThat(
                readSingle(doc, "vector_layers[?(@.id == 'Forests')].geometry_type"),
                equalTo("polygon"));
    }

    public void checkRoadSegmentsTileMatrix(DocumentContext json, Tileset.DataType dataType) {
        // check the tile matrices
        assertEquals(Integer.valueOf(2), json.read("$.tilesets.size()"));
        String matrixDefinition =
                "http://localhost:8080/geoserver/ogc/tiles/v1/tileMatrixSets/EPSG%3A4326";
        assertEquals(matrixDefinition, json.read("$.tilesets[0].tileMatrixSetURI"));
        assertEquals(matrixDefinition, json.read("$.tilesets[0].tileMatrixSetDefinition"));
        assertEquals(dataType.toString(), json.read("$.tilesets[0].dataType"));
        String tilesetLink =
                readSingle(json, "$.tilesets[0].links[?(@.type == 'application/json')].href");
        if (dataType == Tileset.DataType.map) {
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite:RoadSegments/map/tiles/EPSG:4326?f=application%2Fjson",
                    tilesetLink);
        } else if (dataType == Tileset.DataType.vector) {
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite:RoadSegments/tiles/EPSG:4326?f=application%2Fjson",
                    tilesetLink);
        }
    }
}

/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.tiles;

import static org.geoserver.ogcapi.v1.tiles.TiledCollectionDocument.REL_TILESETS_MAP;
import static org.geoserver.ogcapi.v1.tiles.TiledCollectionDocument.REL_TILESETS_VECTOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.geoserver.data.test.MockData;
import org.hamcrest.Matchers;
import org.junit.Test;

public class CollectionTest extends TilesTestSupport {

    @Test
    public void testRoadsCollectionJson() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("ogc/tiles/v1/collections/" + roadSegments, 200);

        testRoadsCollectionJson(json);
    }

    @Test
    public void testRoadsCollectionHTML() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        getAsJSoup("ogc/tiles/v1/collections/" + roadSegments + "?f=text/html");
        // TODO: add ids in the elemnets and check contents using jSoup
    }

    @Test
    public void testOnlyMapLinks() throws Exception {
        // this one only has rendered formats associated
        String lakesId = getLayerId(MockData.LAKES);
        DocumentContext json = getAsJSONPath("ogc/tiles/v1/collections/" + lakesId, 200);

        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite:Lakes/map/tiles?f=application%2Fjson",
                readSingle(json, "$.links[?(@.rel=='" + REL_TILESETS_MAP + "' && @.type=='application/json')].href"));
    }

    @Test
    public void testOnlyDataLinks() throws Exception {
        // this one only has rendered formats associated
        String forestsId = getLayerId(MockData.FORESTS);

        DocumentContext json = getAsJSONPath("ogc/tiles/v1/collections/" + forestsId, 200);

        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite:Forests/tiles?f=application%2Fjson",
                readSingle(
                        json, "$.links[?(@.rel=='" + REL_TILESETS_VECTOR + "' && @.type=='application/json')].href"));
    }

    private void testRoadsCollectionJson(DocumentContext json) {
        assertEquals("cite:RoadSegments", json.read("$.id", String.class));
        assertEquals("RoadSegments", json.read("$.title", String.class));
        assertEquals(-0.0042, json.read("$.extent.spatial.bbox[0][0]", Double.class), 0d);
        assertEquals(-0.0024, json.read("$.extent.spatial.bbox[0][1]", Double.class), 0d);
        assertEquals(0.0042, json.read("$.extent.spatial.bbox[0][2]", Double.class), 0d);
        assertEquals(0.0024, json.read("$.extent.spatial.bbox[0][3]", Double.class), 0d);
        assertEquals("http://www.opengis.net/def/crs/OGC/1.3/CRS84", json.read("$.extent.spatial.crs", String.class));

        // check the tiles link (both data and map tiles)
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite:RoadSegments/map/tiles?f=application%2Fjson",
                readSingle(json, "$.links[?(@.rel=='" + REL_TILESETS_MAP + "' && @.type=='application/json')].href"));
        // check the tiles link (both data and map tiles)
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite:RoadSegments/tiles?f=application%2Fjson",
                readSingle(
                        json, "$.links[?(@.rel=='" + REL_TILESETS_VECTOR + "' && @.type=='application/json')].href"));

        // styles
        assertEquals(Integer.valueOf(2), json.read("$.styles.size()"));
        assertEquals("RoadSegments", json.read("$.styles[0].id"));
        assertEquals("Default Styler", json.read("$.styles[0].title"));

        assertEquals("generic", json.read("$.styles[1].id"));
        assertEquals("Generic", json.read("$.styles[1].title"));

        // queryables
        String queryablesLink =
                readSingle(json, "$.links[?(@.rel=='queryables' && @.type=='application/schema+json')].href");
        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/v1/collections/cite:RoadSegments/queryables?f=application%2Fschema%2Bjson",
                queryablesLink);
        String queryablesTitle =
                readSingle(json, "$.links[?(@.rel=='queryables' && @.type=='application/schema+json')].title");
        assertEquals("Collection queryables as application/schema+json", queryablesTitle);
    }

    @Test
    public void testRoadsCollectionYaml() throws Exception {
        String yaml =
                getAsString("ogc/tiles/v1/collections/" + getLayerId(MockData.ROAD_SEGMENTS) + "?f=application/yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testRoadsCollectionJson(json);
    }

    @Test
    public void testBluemarble() throws Exception {
        String roadSegments = getLayerId(MockData.TASMANIA_BM);
        DocumentContext json = getAsJSONPath("ogc/tiles/v1/collections/" + roadSegments, 200);

        assertEquals("wcs:BlueMarble", json.read("$.id", String.class));
        assertEquals("BlueMarble", json.read("$.title", String.class));
        assertEquals(146.5, json.read("$.extent.spatial.bbox[0][0]", Double.class), 0.1d);
        assertEquals(-44.5, json.read("$.extent.spatial.bbox[0][1]", Double.class), 0.1d);
        assertEquals(148, json.read("$.extent.spatial.bbox[0][2]", Double.class), 0.1d);
        assertEquals(-43, json.read("$.extent.spatial.bbox[0][3]", Double.class), 0.1d);
        assertEquals("http://www.opengis.net/def/crs/OGC/1.3/CRS84", json.read("$.extent.spatial.crs", String.class));

        // check the tiles link (only map tiles for the time being)
        List<String> tilesLinks =
                json.read("$.links[?(@.rel=='" + REL_TILESETS_MAP + "' && @.type=='application/json')].href");
        assertEquals(1, tilesLinks.size());
        assertThat(
                tilesLinks,
                Matchers.containsInAnyOrder(
                        "http://localhost:8080/geoserver/ogc/tiles/v1/collections/wcs:BlueMarble/map/tiles?f=application%2Fjson"));

        // styles
        assertEquals(Integer.valueOf(1), json.read("$.styles.size()"));
        assertEquals("raster", json.read("$.styles[0].id"));

        // no queryable links
        List<String> items = json.read("$.links[?(@.rel=='queryables' && @.type=='application/json')].href");
        assertThat(items, Matchers.empty());
    }
}

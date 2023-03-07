/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features.tiled;

import static org.geoserver.ogcapi.v1.tiles.TiledCollectionDocument.REL_TILESETS_VECTOR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.MockData;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class CollectionTest extends TiledFeaturesTestSupport {

    @Test
    public void testCollectionJson() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("ogc/features/v1/collections/" + roadSegments, 200);

        assertEquals("cite:RoadSegments", json.read("$.id", String.class));
        assertEquals("RoadSegments", json.read("$.title", String.class));

        // check the tiles link has been added
        assertThat(
                readSingle(
                        json,
                        "links[?(@.rel=='"
                                + REL_TILESETS_VECTOR
                                + "' && @.type=='application/json')].href"),
                equalTo(
                        "http://localhost:8080/geoserver/ogc/features/v1/collections/cite:RoadSegments/tiles?f=application%2Fjson"));
    }

    @Test
    public void testCollectionHtml() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        Document doc = getAsJSoup("ogc/features/v1/collections/" + roadSegments + "?f=html");
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite:RoadSegments/tiles?f=text%2Fhtml",
                doc.select("#cite__RoadSegments_tiles").attr("href"));
    }

    @Test
    public void testUntiledCollectionHtml() throws Exception {
        String basicPolygons = getLayerId(MockData.BASIC_POLYGONS);
        Document doc = getAsJSoup("ogc/features/v1/collections/" + basicPolygons + "?f=html");
        assertThat(doc.select("#cite__RoadSegments_tiles"), empty());
    }
}

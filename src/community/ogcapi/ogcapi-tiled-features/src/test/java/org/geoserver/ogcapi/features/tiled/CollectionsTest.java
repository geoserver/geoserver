/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.features.tiled;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.MockData;
import org.geoserver.ogcapi.tiles.TiledCollectionDocument;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class CollectionsTest extends TiledFeaturesTestSupport {

    @Test
    public void testCollectionJson() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("ogc/features/collections", 200);

        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/collections/cite%3ARoadSegments/tiles?f=application%2Fjson",
                readSingle(
                        json,
                        "$.collections[?(@.id == 'cite:RoadSegments')].links[?(@.rel=='"
                                + TiledCollectionDocument.REL_TILESETS_VECTOR
                                + "' && @.type=='application/json')].href"));
    }

    @Test
    public void testCollectionsHtml() throws Exception {
        Document doc = getAsJSoup("ogc/features/collections/?f=html");
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/collections/cite%3ARoadSegments/tiles?f=text%2Fhtml",
                doc.select("#cite__RoadSegments_tiles").attr("href"));
        assertThat(
                "http://localhost:8080/geoserver/ogc/features/collections/cite%3ARoadSegments/tiles?f=text%2Fhtml",
                doc.select("#cite__BasicPolygons_tiles"), empty());
    }
}

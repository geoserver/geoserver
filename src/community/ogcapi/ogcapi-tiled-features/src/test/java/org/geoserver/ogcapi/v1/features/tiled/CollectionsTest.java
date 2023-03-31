/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features.tiled;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.ogcapi.v1.tiles.TiledCollectionDocument;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class CollectionsTest extends TiledFeaturesTestSupport {

    @Test
    public void testCollectionJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1/collections", 200);

        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite:RoadSegments/tiles?f=application%2Fjson",
                readSingle(
                        json,
                        "$.collections[?(@.id == 'cite:RoadSegments')].links[?(@.rel=='"
                                + TiledCollectionDocument.REL_TILESETS_VECTOR
                                + "' && @.type=='application/json')].href"));
    }

    @Test
    public void testCollectionsHtml() throws Exception {
        Document doc = getAsJSoup("ogc/features/v1/collections/?f=html");
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite:RoadSegments/tiles?f=text%2Fhtml",
                doc.select("#cite__RoadSegments_tiles").attr("href"));
        assertThat(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite:RoadSegments/tiles?f=text%2Fhtml",
                doc.select("#cite__BasicPolygons_tiles"), empty());
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.tiles;

import static org.geoserver.ogcapi.tiles.TiledCollectionDocument.REL_TILESETS_MAP;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.geoserver.data.test.MockData;
import org.junit.Test;

public class GetStylesTest extends TilesTestSupport {

    @Test
    public void testMapLinks() throws Exception {
        // this one only has rendered formats associated
        String lakesId = getLayerId(MockData.LAKES);
        DocumentContext json = getAsJSONPath("ogc/tiles/collections/" + lakesId + "/styles", 200);

        assertEquals(
                "http://localhost:8080/geoserver/ogc/tiles/collections/cite%3ALakes/styles/map/tiles?f=application%2Fjson",
                readSingle(
                        json,
                        "styles[0].links[?(@.rel=='"
                                + REL_TILESETS_MAP
                                + "' && @.type=='application/json')].href"));
    }

    @Test
    public void testOnlyDataLinks() throws Exception {
        // this one only has vector tiles associated, should not have a styles
        String forestsId = getLayerId(MockData.FORESTS);
        DocumentContext json = getAsJSONPath("ogc/tiles/collections/" + forestsId + "/styles", 200);

        // no tile links should be available from here
        assertEquals(
                0,
                json.read("styles[*].links[?(@.rel=='" + REL_TILESETS_MAP + "')]", List.class)
                        .size());
    }
}

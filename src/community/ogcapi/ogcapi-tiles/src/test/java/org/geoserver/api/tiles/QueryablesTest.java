/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.tiles;

import static org.geoserver.data.test.MockData.ROAD_SEGMENTS;
import static org.geoserver.data.test.MockData.TASMANIA_BM;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;

public class QueryablesTest extends TilesTestSupport {

    @Test
    public void queryablesOnCoverage() throws Exception {
        // queryables are not supported for coverages
        DocumentContext json =
                getAsJSONPath(
                        "ogc/tiles/collections/" + getLayerId(TASMANIA_BM) + "/queryables", 404);
        assertEquals(
                "Collection 'wcs:BlueMarble' cannot be filtered, no queryables available",
                json.read("description"));
    }

    @Test
    public void queryablesOnGroup() throws Exception {
        // queryables are not supported for coverages
        DocumentContext json =
                getAsJSONPath("ogc/tiles/collections/" + NATURE_GROUP + "/queryables", 404);
        assertEquals(
                "Collection '" + NATURE_GROUP + "' cannot be filtered, no queryables available",
                json.read("description"));
    }

    @Test
    public void queryablesOnRoadSegements() throws Exception {
        DocumentContext json =
                getAsJSONPath(
                        "ogc/tiles/collections/" + getLayerId(ROAD_SEGMENTS) + "/queryables", 200);
        assertEquals("geometry", readSingle(json, "queryables[?(@.id=='the_geom')].type"));
        assertEquals("string", readSingle(json, "queryables[?(@.id=='FID')].type"));
        assertEquals("string", readSingle(json, "queryables[?(@.id=='NAME')].type"));
    }
}

package org.geoserver.printing;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.geoserver.test.GeoServerTestSupport;

public class SmokeTest extends GeoServerTestSupport {
    public void testServiceExists() throws Exception {
        JSON json = getAsJSON("/pdf/info.json");

        assertTrue(json instanceof JSONObject);
        JSONObject obj = (JSONObject) json;
        assertTrue(obj.containsKey("scales"));
        assertTrue(obj.containsKey("dpis"));
        assertTrue(obj.containsKey("layouts"));
        assertTrue(obj.containsKey("printURL"));
        assertTrue(obj.containsKey("createURL"));
    }
}

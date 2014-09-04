/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.printing;

import static org.junit.Assert.*;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class SmokeTest extends GeoServerSystemTestSupport  {
    
    @Test
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

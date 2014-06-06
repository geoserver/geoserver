/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.resource;

import org.opengeo.gsr.JsonSchemaTest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class LayerListResourceTest extends ResourceTest {
    private final String query(String service, String params) {
        return baseURL + service + "/MapServer/layers" + params;
    }
    
    @Test
    public void testStreamsQuery() throws Exception {
        String result = getAsString(query("cite", "?f=json"));
        assertTrue("Streams output must validate: " + result, JsonSchemaTest.validateJSON(result, "/gsr-ms/1.0/allLayersAndTables.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue(json.has("tables"));
        assertTrue(json.has("layers"));
        assertTrue(json.get("tables") instanceof JSONArray);
        assertTrue(json.get("layers") instanceof JSONArray);
        JSONArray tables = json.getJSONArray("tables");
        for (Object object : tables) {
            JSONObject table = (JSONObject) object;
            assertFalse("Table " + table + " should not have an extent", table.has("extent"));
        }
        
        JSONArray layers = json.getJSONArray("layers");
        for (Object object : layers) {
            JSONObject layer = (JSONObject) object;
            assertTrue("Layer " + layer + " should have an extent", layer.has("extent"));
        }
    }
}

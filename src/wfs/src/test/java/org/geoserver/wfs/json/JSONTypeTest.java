/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class JSONTypeTest {

    @Test
    public void testMimeType() {

        // MimeType
        Assert.assertNotSame(JSONType.json, JSONType.jsonp);
        Assert.assertTrue(JSONType.isJsonMimeType(JSONType.json));

        // enable JsonP programmatically
        JSONType.setJsonpEnabled(true);
        // check jsonp is enabled
        Assert.assertTrue(JSONType.useJsonp(JSONType.jsonp));

        // disable JsonP
        JSONType.setJsonpEnabled(false);
        Assert.assertFalse(JSONType.useJsonp(JSONType.jsonp));
    }

    @Test
    public void testJSONType() {
        // ENUM type
        JSONType json = JSONType.JSON;
        Assert.assertEquals(JSONType.JSON, json);
        JSONType jsonp = JSONType.JSONP;
        Assert.assertEquals(JSONType.JSONP, jsonp);

        Assert.assertEquals(JSONType.JSON, JSONType.getJSONType(JSONType.json));
        Assert.assertEquals(JSONType.JSONP, JSONType.getJSONType(JSONType.jsonp));
    }

    @Test
    public void testCallbackFunction() {
        Map<String, Map<String, String>> kvp = new HashMap<>();

        Assert.assertEquals(JSONType.CALLBACK_FUNCTION, JSONType.getCallbackFunction(kvp));

        Map<String, String> formatOpts = new HashMap<>();
        kvp.put("FORMAT_OPTIONS", formatOpts);

        Assert.assertEquals(JSONType.CALLBACK_FUNCTION, JSONType.getCallbackFunction(kvp));

        formatOpts.put(JSONType.CALLBACK_FUNCTION_KEY, "functionName");

        Assert.assertEquals("functionName", JSONType.getCallbackFunction(kvp));
    }
}

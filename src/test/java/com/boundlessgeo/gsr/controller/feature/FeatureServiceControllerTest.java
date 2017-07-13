/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.controller.feature;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.boundlessgeo.gsr.JsonSchemaTest;
import com.boundlessgeo.gsr.controller.ControllerTest;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class FeatureServiceControllerTest extends ControllerTest {
    private String query(String service, String params) {
        return getBaseURL() + service + "/FeatureServer" + params;
    }

    private String queryServiceUrl() {
        return getBaseURL() + "cite" + "/FeatureServer/query" + "?f=json";
    }

    @Test
    public void testBasicQuery() throws Exception {
        String result = getAsString(query("cite", "?f=json"));
        assertFalse(result.isEmpty());
        System.out.println(result);
        // TODO: Can't validate since ids are not integers.
        assertTrue(result + " ;Root controller validates", JsonSchemaTest.validateJSON(result, "/gsr-fs/1.0/root.json"));
    }

    @Test
    public void testQuery() throws Exception {
        JSON result = getAsJSON(queryServiceUrl());
        System.out.println(result.toString());
        JSONObject object = (JSONObject) result;
        JSONArray layers = (JSONArray) object.get("layers");
        assertTrue(layers.size() > 0);
    }
}

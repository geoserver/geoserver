/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.controller.feature;

import static org.junit.Assert.assertEquals;
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
        assertFalse(object.has("error"));
        JSONArray layers = (JSONArray) object.get("layers");
        assertTrue(layers.size() > 0);
    }

    //TODO: Debug into cite, find something that gives different # results for each of the below:
    @Test
    public void testQueryByObjectId() throws Exception {
        JSON result = getAsJSON(query("cdf", "/3/query?f=json"
                + "&objectIds=0,1,2,3,4,5,6,7,8,9"));
        System.out.println(result.toString());
        JSONObject object = (JSONObject) result;
        assertFalse(object.has("error"));
        JSONArray layers = (JSONArray) object.get("features");
        assertEquals(10, layers.size());
    }
    @Test
    public void testQueryByWhere() throws Exception {
        JSON result = getAsJSON(query("cdf", "/3/query?f=json&where=\"id\" LIKE ' lfbt%25'"));
        System.out.println(result.toString());
        JSONObject object = (JSONObject) result;
        assertFalse(object.has("error"));
        JSONArray layers = (JSONArray) object.get("features");
        assertEquals(6, layers.size());
    }
    @Test
    public void testQueryByObjectIdAndWhere() throws Exception {
        JSON result = getAsJSON(query("cdf", "/3/query?f=json&where=\"id\" LIKE ' lfbt%25'"
                + "&objectIds=0,1,2,3,4,5,6,7,8,9"));
        System.out.println(result.toString());
        JSONObject object = (JSONObject) result;
        assertFalse(object.has("error"));
        JSONArray layers = (JSONArray) object.get("features");
        assertEquals(2, layers.size());
    }
}

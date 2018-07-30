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
    public void testQueryWhereObjectId() throws Exception {
        JSON result = getAsJSON(query("cdf", "/3/query?f=json"
                + "&where=objectid=0"));
        System.out.println(result.toString());
        JSONObject object = (JSONObject) result;
        assertFalse(object.has("error"));
        JSONArray layers = (JSONArray) object.get("features");
        assertEquals(1, layers.size());
    }
    @Test
    public void testQueryWhereOrObjectIds() throws Exception {
        JSON result = getAsJSON(query("cdf", "/3/query?f=json"
                + "&where=objectid=0 or objectid=1"));
        System.out.println(result.toString());
        JSONObject object = (JSONObject) result;
        assertFalse(object.has("error"));
        JSONArray layers = (JSONArray) object.get("features");
        assertEquals(2, layers.size());
    }

    @Test
    public void testQueryWhereAndObjectIds() throws Exception {
        JSON result = getAsJSON(query("cdf", "/3/query?f=json"
                + "&where=objectid=0 and objectid=1"));
        System.out.println(result.toString());
        JSONObject object = (JSONObject) result;
        assertFalse(object.has("error"));
        JSONArray layers = (JSONArray) object.get("features");
        assertEquals(0, layers.size());
    }

    @Test
    public void testQueryWhereInObjectIds() throws Exception {
        JSON result = getAsJSON(query("cdf", "/3/query?f=json"
                + "&where=objectid IN ('0','1','2')"));
        System.out.println(result.toString());
        JSONObject object = (JSONObject) result;
        assertFalse(object.has("error"));
        JSONArray layers = (JSONArray) object.get("features");
        assertEquals(3, layers.size());
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

    @Test
    public void testFeaturesNative() throws Exception {
        JSON result = getAsJSON(query("cdf", "/3/query?f=json"
                + "&objectIds=0"));
        System.out.println(result.toString());
        JSONObject object = (JSONObject) result;
        assertFalse(object.has("error"));
        assertFalse(object.has("translate"));
        JSONArray layers = (JSONArray) object.get("features");
        assertEquals(1, layers.size());

        JSONObject geometry = layers.getJSONObject(0).getJSONObject("geometry");
        assertEquals("4326", geometry.getJSONObject("spatialReference").getString("wkid"));
        assertEquals(-92.99955, geometry.getDouble("x"), 0.0000001);
        assertEquals(4.524015, geometry.getDouble("y"), 0.0000001);
    }

    @Test
    public void testFeaturesReprojected() throws Exception {
        JSON result = getAsJSON(query("cdf", "/3/query?f=json"
                + "&objectIds=0&outSR=102100"));
        System.out.println(result.toString());
        JSONObject object = (JSONObject) result;
        assertFalse(object.has("error"));
        assertFalse(object.has("translate"));
        JSONArray layers = (JSONArray) object.get("features");
        assertEquals(1, layers.size());

        JSONObject geometry = layers.getJSONObject(0).getJSONObject("geometry");
        assertEquals("102100", geometry.getJSONObject("spatialReference").getString("wkid"));
        assertEquals(-10352662.0, geometry.getDouble("x"), 0.001);
        assertEquals(504135.16, geometry.getDouble("y"), 0.001);
    }

    @Test
    public void testFeaturesQuantized() throws Exception {
        JSON result = getAsJSON(query("cdf", "/3/query?f=json&objectIds=0&outSR=102100" +
                "&quantizationParameters={" +
                    "\"mode\":\"view\"," +
                    "\"originPosition\":\"upperLeft\"," +
                    "\"tolerance\":1000," +
                    "\"extent\":{" +
                        "\"xmin\":-100.0," +
                        "\"ymin\":0.0," +
                        "\"xmax\":-80.0," +
                        "\"ymax\":10.0," +
                        "\"spatialReference\":{\"wkid\":4326,\"latestWkid\":4326}" +
                    "}" +
                "}"));

        System.out.println(result.toString());
        JSONObject object = (JSONObject) result;
        assertFalse(object.has("error"));

        assertTrue(object.has("transform"));
        JSONObject transform = object.getJSONObject("transform");
        assertEquals(1000.0, transform.getJSONArray("scale").getDouble(0), 0.01);
        assertEquals(-11131949, transform.getJSONArray("translate").getDouble(0), 0.01);
        assertEquals(1118890.0, transform.getJSONArray("translate").getDouble(1), 0.01);

        JSONArray layers = (JSONArray) object.get("features");
        assertEquals(1, layers.size());

        JSONObject geometry = layers.getJSONObject(0).getJSONObject("geometry");
        assertEquals("102100", geometry.getJSONObject("spatialReference").getString("wkid"));
        assertEquals(779L, geometry.getLong("x"));
        assertEquals(615L, geometry.getLong("y"));
    }

}

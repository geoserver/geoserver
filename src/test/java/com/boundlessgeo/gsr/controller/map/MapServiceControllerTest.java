/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.controller.map;

import static org.junit.Assert.*;

import org.junit.Test;

import com.boundlessgeo.gsr.JsonSchemaTest;
import com.boundlessgeo.gsr.controller.ControllerTest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class MapServiceControllerTest extends ControllerTest {
    private String query(String service, String params) {
        return getBaseURL() + service + "/MapServer" + params;
    }

    @Test
    public void testBasicQuery() throws Exception {
        String result = getAsString(query("cite", "?f=json&format=jpeg"));
        assertFalse(result.isEmpty());
        // TODO: Can't validate since ids are not integers.
         assertTrue(result + " ;Root controller validates", JsonSchemaTest.validateJSON(result, "/gsr-ms/1.0/root.json"));
    }

    @Test
    public void testNotFoundException() throws Exception {
        JSONObject result = (JSONObject) getAsJSON(query("cte", "?f=json"));
        JSONObject error = result.getJSONObject("error");
        assertEquals(500, error.getInt("code"));
    }

    @Test
    public void testLayerGet() throws Exception {
        JSONObject result = (JSONObject) getAsJSON(getBaseURL() + "cite/MapServer/0");
        System.out.println(result.toString());
    }

    @Test
    public void testIdentify() throws Exception {
        JSONObject result = (JSONObject) getAsJSON(getBaseURL() + "/cite/MapServer/identify?f=json"
            + "&geometryType=esriGeometryPoint&geometry={x: 0, y: 0}"
            + "&layers=all:0&imageDisplay=718,610,96&mapExtent=-126.175461,11.400420,-65.525810,"
            + "62.927282&tolerance=10 ");
        System.out.println(result.toString());
        assertFalse(result.has("error"));
        assertTrue("Result validates against schema", JsonSchemaTest.validateJSON(result, "/gsr-ms/1.0/identify.json"));

        assertEquals(2, result.getJSONArray("results").size());
        assertEquals("4326", result.getJSONArray("results").getJSONObject(0)
                .getJSONObject("geometry").getJSONObject("spatialReference").getString("wkid"));
    }

    @Test
    public void testIdentifyWkid() throws Exception {
        //test cite; expect wkid 4326
        JSONObject result = (JSONObject) getAsJSON(getBaseURL() + "/cite/MapServer/identify?f=json"
                + "&geometryType=esriGeometryPoint&geometry={x: 0, y: 0}"
                + "&layers=all:0&imageDisplay=718,610,96&mapExtent=-126.175461,11.400420,-65.525810,"
                + "62.927282&tolerance=10 ");
        assertFalse(result.has("error"));

        assertEquals(2, result.getJSONArray("results").size());
        assertEquals("4326", result.getJSONArray("results").getJSONObject(0)
                .getJSONObject("geometry").getJSONObject("spatialReference").getString("wkid"));

        result = (JSONObject) getAsJSON(getBaseURL() + "/cdf/MapServer/identify?f=json"
                + "&geometryType=esriGeometryPoint&geometry={x: 500050, y: 500050}&sr=32615"
                + "&layers=all:0&imageDisplay=718,610,96&mapExtent=-126.175461,11.400420,-65.525810,"
                + "62.927282&tolerance=10 ");
        assertFalse(result.has("error"));

        assertEquals(99, result.getJSONArray("results").size());
        assertEquals("32615", result.getJSONArray("results").getJSONObject(0)
                .getJSONObject("geometry").getJSONObject("spatialReference").getString("wkid"));
    }

    @Test
    public void testFind() throws Exception {
        JSONObject result = (JSONObject) getAsJSON(
            getBaseURL() + "/cite/MapServer/find?f=json&searchText=Ash&layers=8");
        System.out.println(result.toString());
        JSONArray results = (JSONArray) result.get("results");
        assertTrue("Results should have one element", results.size() == 1);
    }
}

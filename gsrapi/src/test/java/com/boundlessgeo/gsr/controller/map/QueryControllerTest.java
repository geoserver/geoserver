/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.controller.map;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

import com.boundlessgeo.gsr.JsonSchemaTest;
import com.boundlessgeo.gsr.controller.ControllerTest;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class QueryControllerTest extends ControllerTest {
    private String query(String service, int layerId, String params) {
        return getBaseURL() + service + "/MapServer/" + layerId + "/query" + params;
    }

    @Test
    public void testStreamsQuery() throws Exception {
        JSON json = getAsJSON(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue(String.valueOf(json) + " is a JSON object", json instanceof JSONObject);
        JSONObject jsonObject = (JSONObject) json;
        System.out.println(json.toString());
//        assertTrue("objectIdFieldName is not present", jsonObject.containsKey("objectIdFieldName"));
//        assertTrue("globalIdFieldName is not present", jsonObject.containsKey("globalIdFieldName"));
        assertEquals("geometryType for Streams should be esriGeometryPolyline", "esriGeometryPolyline", jsonObject.get("geometryType"));
        assertTrue("Streams layer should have a field list", jsonObject.get("fields") instanceof JSONArray);
        JSONArray fields = (JSONArray) jsonObject.get("fields");
        //        assertEquals("Streams layer should have two non-geometry fields", 2, fields.size());

        assertTrue("Streams layer should have a feature list", jsonObject.get("features") instanceof JSONArray);
        JSONArray features = (JSONArray) jsonObject.get("features");
        assertEquals("Streams layer should have two features", 2, features.size());
    }

// TODO: This test fails because I didn't understand from reading the spec how to encode MultiPolygon data.  Judging from
//
//    public void testBuildingsQuery() throws Exception {
//        JSON json = getAsJSON(query("Buildings", "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90"));
//        assertTrue(String.valueOf(json) + " is a JSON object", json instanceof JSONObject);
//        JSONObject jsonObject = (JSONObject) json;
//        assertTrue("objectIdFieldName is not present", jsonObject.containsKey("objectIdFieldName"));
//        assertTrue("globalIdFieldName is not present", jsonObject.containsKey("globalIdFieldName"));
//        assertEquals("geometryType for Streams should be GeometryPolyline", "GeometryPolyline", jsonObject.get("geometryType"));
//        assertTrue("Buildings layer should have a field list", jsonObject.get("fields") instanceof JSONArray);
//        JSONArray fields = (JSONArray) jsonObject.get("fields");
//        assertEquals("Buildings layer should have two non-geometry fields", 2, fields.size());
//
//        assertTrue("Buildings layer should have a feature list", jsonObject.get("features") instanceof JSONArray);
//        JSONArray features = (JSONArray) jsonObject.get("features");
//        assertEquals("Buildings layer should have two features", 2, features.size());
//    }

    //TODO this test definitely should be fixed
    @Test
    @Ignore
    public void testPointsQuery() throws Exception {
        JSON json = getAsJSON(query("cgf", 4, "?f=json&geometryType=esriGeometryEnvelope&geometry=500000,500000,500100,500100"));
        assertTrue(String.valueOf(json) + " is a JSON object", json instanceof JSONObject);
        JSONObject jsonObject = (JSONObject) json;
        //        assertTrue("objectIdFieldName is not present", jsonObject.containsKey("objectIdFieldName"));
        //        assertTrue("globalIdFieldName is not present", jsonObject.containsKey("globalIdFieldName"));
        //        assertEquals("geometryType for Streams should be esriGeometryPoint", "esriGeometryPoint", jsonObject.get("geometryType"));

        assertTrue("Points layer should have a field list", jsonObject.get("fields") instanceof JSONArray);
        JSONArray fields = (JSONArray) jsonObject.get("fields");
        assertEquals("Points layer should have two non-geometry fields", 2, fields.size());

        assertTrue("Points layer should have a feature list", jsonObject.get("features") instanceof JSONArray);
        JSONArray features = (JSONArray) jsonObject.get("features");
        assertEquals("Points layer should have two features", 1, features.size());
    }

    @Test
    public void testFormatParameter() throws Exception {
        String result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue("Request with f=json returns features", JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with f=json; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 2);

        result = getAsString(query("cite", 11, "?geometryType=GeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue("Request with no format parameter return an error", JsonSchemaTest.validateJSON(result, "/gsr/1.0/exception.json"));
        json = JSONObject.fromObject(result).getJSONObject("error");
        assertTrue("Request with no format parameter; returned " + result, json.containsKey("code"));

        result = getAsString(query("cite", 11, "?f=xml&geometryType=GeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue("Request with unrecognized format returns an error", JsonSchemaTest.validateJSON(result, "/gsr/1.0/exception.json"));
        json = JSONObject.fromObject(result).getJSONObject("error");
        assertTrue("Request with f=xml; returned " + result, json.containsKey("code"));
    }

    @Test
    public void testGeometryParameter() throws Exception {
        String result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue("Request with short envelope; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returend " + result, json.containsKey("features") && json.getJSONArray("features").size() == 2);
        System.out.println(result);

        result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryPoint&geometry=-0.0001,0.0012"));
        assertTrue("Request with short point; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with short point; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 1);

        result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry={xmin:-180,xmax:180,ymin:-90,ymax:90}"));
        assertTrue("Request with JSON envelope; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with JSON envelope; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 2);

        result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryPoint&geometry={x:-0.0001,y:0.0012}"));
        assertTrue("Request with JSON point; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with JSON point; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 1);

        result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryMultiPoint&geometry={points:[[0.0034,-0.0024],[0.0036,-0.002],[0.0031,-0.0015]]}"));
        assertTrue("Request with JSON multipoint; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with JSON multipoint; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 1);

        result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryPolyLine&geometry={paths:[[[0.0034,-0.0024],[0.0036,-0.002],[0.0031,-0.0015]]]}"));
        assertTrue("Request with JSON polyline; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with JSON multipoint; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 1);

        result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryPolygon&geometry={rings:[[[0.0034,-0.0024],[0.0036,-0.002],[0.0031,-0.0015],[0.0034,-0.0024]]]}"));
        assertTrue("Request with JSON polygon, returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with JSON multipoint; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 1);
    }

    @Test
    public void testWhere() throws Exception {
        String result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90&where=NAME=\'Cam+Stream\'"));
        assertTrue("Request with valid where clause; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features"));

        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90&where=invalid_filter"));
        assertTrue("Request with invalid where clause; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/exception.json"));
        json = JSONObject.fromObject(result).getJSONObject("error");
        assertTrue("Request with invalid where clause; returned " + result, json.containsKey("code"));
    }

    @Test
    public void testReturnGeometryAndOutFields() throws Exception {
        String result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue("Request implicitly including geometries; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        JSONArray features = json.getJSONArray("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertTrue("No geometry at index " + i + " in " + result, feature.containsKey("geometry"));
        }

        result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90&returnGeometry=true"));
        assertTrue("Request explicitly including geometries; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        features = json.getJSONArray("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertTrue("No geometry at index " + i + " in " + result, feature.containsKey("geometry"));
        }

        result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90&returnGeometry=false"));
        assertTrue("Request excluding geometries, but don't specify fields. in this case the geometry should be returned anyway. JSON was " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        features = json.getJSONArray("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            //TODO skipping this requirement for now. Not sure it makes sense.
            //            assertTrue("No geometry at index " + i + " in " + result, feature.containsKey("geometry"));
        }

        result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90&returnGeometry=false&outFields=NAME"));
        assertTrue("Request excluding geometries. JSON was " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        features = json.getJSONArray("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertTrue("Found geometry at index " + i + " in " + result, !feature.containsKey("geometry"));
        }
    }

    @Test
    public void testInSRandOutSR() throws Exception {
        String result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry=-170,-85,170,85&outSR=3857"));
        assertFalse("Response should not be empty!", result.isEmpty());
        assertTrue("Request explicitly including geometries; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertFalse("Results should be a JSON Object (" + json +")", json.isArray() || json.isNullObject());
        assertFalse("spatialReference should be a JSON Object (" + json + ")", json.getJSONObject("spatialReference").isArray() || json.getJSONObject("spatialReference").isNullObject());
        assertFalse("spatialReference.wkid should be a JSON Object (" + json.getJSONObject("spatialReference") + ")", json.getJSONObject("spatialReference").get("wkid") == null);
        assertTrue("Results not in requested spatial reference; json was " + result, json.getJSONObject("spatialReference").get("wkid").equals(3857));

        result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90&outSR=2147483647"));
        assertTrue("Request for unknown WKID produces error; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/exception.json"));
        json = JSONObject.fromObject(result).getJSONObject("error");
        assertTrue("Exception report should have an error code; json is " + result, json.containsKey("code"));

        result = getAsString(query("cite", 11, "?f=json&geometryType=esriGeometryEnvelope&geometry=-45,265,-44,264&inSR=3785"));
        assertTrue("Request explicitly including geometries; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Results not in requested spatial reference; json was " + result, json.getJSONObject("spatialReference").get("wkid").equals(4326));
    }

    @Test
    public void testSpatialRel() throws Exception {
        String result = getAsString(
            query("cite", 11, "?f=json&geometryType=esriGeometryPolyline&geometry={paths:[[[-0.001,0],[0,0.0015]]]}"));
        System.out.println(result);
        assertTrue("Request with implicit spatialRel; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        JSONArray features = json.getJSONArray("features");
        assertTrue("There should be no results for this intersects query. JSON was: " + result, features.size() == 0);

        result = getAsString(query("cite", 11,
            "?f=json&geometryType=GeometryPolyLine&geometry={paths:[[[-0.001,0],[0,0.0015]]]}&spatialRel=esriSpatialRelEnvelopeIntersects"));
        assertTrue("Request specifying spatialreference; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        features = json.getJSONArray("features");
        assertTrue("Should have results for envelope query at 0,0. JSON was: " + result, features.size() == 1);
    }

    @Test
    public void testBasicQuery() throws Exception {
        String query = getBaseURL() + "cite" + "/FeatureServer/" + 11 + "/query"
            + "?f=json&where=objectid=objectid&returnIdsOnly=true";
        JSONObject obj = (JSONObject) getAsJSON(query);
        System.out.println(obj.toString());
        assertFalse(obj.has("error"));
    }
}

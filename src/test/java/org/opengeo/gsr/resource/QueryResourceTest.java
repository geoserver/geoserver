package org.opengeo.gsr.resource;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class QueryResourceTest extends ResourceTest {
    private final String query(String service, String layer, String params) {
        return baseURL + service + "/MapServer/" + layer + "/query" + params;
    }
    
    public void testStreamsQuery() throws Exception {
//        fail(getAsString(query("cite", "Streams", "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90")));
        JSON json = getAsJSON(query("cite", "Streams", "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue(String.valueOf(json) + " is a JSON object", json instanceof JSONObject);
        JSONObject jsonObject = (JSONObject) json;
        assertTrue("objectIdFieldName is not present", jsonObject.containsKey("objectIdFieldName"));
        assertTrue("globalIdFieldName is not present", jsonObject.containsKey("globalIdFieldName"));
        assertEquals("geometryType for Streams should be GeometryPolyline", "GeometryPolyline", jsonObject.get("geometryType"));
        assertTrue("Streams layer should have a field list", jsonObject.get("fields") instanceof JSONArray);
        JSONArray fields = (JSONArray) jsonObject.get("fields");
        assertEquals("Streams layer should have two non-geometry fields", 2, fields.size());
        
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
    
    public void testPointsQuery() throws Exception {
        JSON json = getAsJSON(query("cgf", "Points", "?f=json&geometryType=GeometryEnvelope&geometry=500000,500000,500100,500100"));
        assertTrue(String.valueOf(json) + " is a JSON object", json instanceof JSONObject);
        JSONObject jsonObject = (JSONObject) json;
        assertTrue("objectIdFieldName is not present", jsonObject.containsKey("objectIdFieldName"));
        assertTrue("globalIdFieldName is not present", jsonObject.containsKey("globalIdFieldName"));
        assertEquals("geometryType for Streams should be GeometryPolyline", "GeometryPoint", jsonObject.get("geometryType"));
        assertTrue("Points layer should have a field list", jsonObject.get("fields") instanceof JSONArray);
        JSONArray fields = (JSONArray) jsonObject.get("fields");
        assertEquals("Points layer should have two non-geometry fields", 1, fields.size());
        
        assertTrue("Points layer should have a feature list", jsonObject.get("features") instanceof JSONArray);
        JSONArray features = (JSONArray) jsonObject.get("features");
        assertEquals("Points layer should have two features", 1, features.size());
    }
}

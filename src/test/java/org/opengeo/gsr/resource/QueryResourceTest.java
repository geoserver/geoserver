package org.opengeo.gsr.resource;

import org.opengeo.gsr.JsonSchemaTest;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import static org.junit.Assert.*;

public class QueryResourceTest extends ResourceTest {
    private final String query(String service, int layerId, String params) {
        return baseURL + service + "/MapServer/" + layerId + "/query" + params;
    }
    
    public void testStreamsQuery() throws Exception {
        JSON json = getAsJSON(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90"));
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
        JSON json = getAsJSON(query("cgf", 4, "?f=json&geometryType=GeometryEnvelope&geometry=500000,500000,500100,500100"));
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
    
    public void testFormatParameter() throws Exception {
        String result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue("Request with f=json returns features", JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with f=json; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 2);
        
        result = getAsString(query("cite", 11, "?geometryType=GeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue("Request with no format parameter return an error", JsonSchemaTest.validateJSON(result, "/gsr/1.0/exception.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with no format parameter; returned " + result, json.containsKey("code"));
        
        result = getAsString(query("cite", 11, "?f=xml&geometryType=GeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue("Request with unrecognized format returns an error", JsonSchemaTest.validateJSON(result, "/gsr/1.0/exception.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with f=xml; returned " + result, json.containsKey("code"));
    }
    
    public void testGeometryParameter() throws Exception {
        String result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue("Request with short envelope; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returend " + result, json.containsKey("features") && json.getJSONArray("features").size() == 2);
        
        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryPoint&geometry=-0.0001,0.0012"));
        assertTrue("Request with short point; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with short point; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 1);
        
        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry={minx:-180,maxx:180,miny:-90,maxy:90}"));
        assertTrue("Request with JSON envelope; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with JSON envelope; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 2);
        
        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryPoint&geometry={x:-0.0001,y:0.0012}"));
        assertTrue("Request with JSON point; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with JSON point; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 1);

        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryMultiPoint&geometry={points:[[0.0034,-0.0024],[0.0036,-0.002],[0.0031,-0.0015]]}"));
        assertTrue("Request with JSON multipoint; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with JSON multipoint; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 1);
        
        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryPolyLine&geometry={paths:[[[0.0034,-0.0024],[0.0036,-0.002],[0.0031,-0.0015]]]}"));
        assertTrue("Request with JSON polyline; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with JSON multipoint; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 1);
        
        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryPolygon&geometry={rings:[[[0.0034,-0.0024],[0.0036,-0.002],[0.0031,-0.0015],[0.0034,-0.0024]]]}"));
        assertTrue("Request with JSON polygon, returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with JSON multipoint; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 1);
    }
    
    public void testWhere() throws Exception {
        String result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90&where=NAME=\'Cam+Stream\'"));
        assertTrue("Request with valid where clause; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Request with short envelope; returned " + result, json.containsKey("features") && json.getJSONArray("features").size() == 1);
        
        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90&where=invalid_filter"));
        assertTrue("Request with invalid where clause; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/exception.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Request with invalid where clause; returned " + result, json.containsKey("code"));
    }
    
    public void testReturnGeometryAndOutFields() throws Exception {
        String result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90"));
        assertTrue("Request implicitly including geometries; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        JSONArray features = json.getJSONArray("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertTrue("No geometry at index " + i + " in " + result, feature.containsKey("geometry"));   
        }
        
        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90&returnGeometry=true"));
        assertTrue("Request explicitly including geometries; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        features = json.getJSONArray("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertTrue("No geometry at index " + i + " in " + result, feature.containsKey("geometry"));   
        }

        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90&returnGeometry=false"));
        assertTrue("Request excluding geometries, but don't specify fields. in this case the geometry should be returned anyway. JSON was " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        features = json.getJSONArray("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertTrue("No geometry at index " + i + " in " + result, feature.containsKey("geometry"));   
        }
        
        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90&returnGeometry=false&outFields=NAME"));
        assertTrue("Request excluding geometries. JSON was " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        features = json.getJSONArray("features");
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            assertTrue("Found geometry at index " + i + " in " + result, !feature.containsKey("geometry"));   
        }
    }
    
    public void testInSRandOutSR() throws Exception {
        String result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-170,-85,170,85&outSR=3857"));
        assertFalse("Response should not be empty!", result.isEmpty());
        assertTrue("Request explicitly including geometries; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        assertTrue("Results not in requested spatial reference; json was " + result, json.getJSONObject("spatialReference").getInt("wkid") == 3857);
        
        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90&outSR=2147483647"));
        assertTrue("Request for unknown WKID produces error; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/exception.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Exception report should have an error code; json is " + result, json.containsKey("code"));
        
        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryEnvelope&geometry=-45,265,-44,264&inSR=3785"));
        assertTrue("Request explicitly including geometries; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        assertTrue("Results not in requested spatial reference; json was " + result, json.getJSONObject("spatialReference").getInt("wkid") == 4326);
    }
  
// TODO: This is working, but fails validation because GeoServer doesn't only support numeric identifiers.
//    public void testReturnIdsOnly() throws Exception {
//        String result = getAsString(query("cite", "Streams", "?f=json&geometryType=GeometryEnvelope&geometry=-180,-90,180,90&returnIdsOnly=true"));
//        assertTrue("Request for ids only; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureIdSet.json"));
//    }
    
    public void testSpatialRel() throws Exception {
        String result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryPolyLine&geometry={paths:[[[-0.001,0],[0,0.0015]]]}"));
        assertTrue("Request with implicit spatialRel; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        JSONObject json = JSONObject.fromObject(result);
        JSONArray features = json.getJSONArray("features");
        assertTrue("There should be no results for this intersects query. JSON was: " + result, features.size() == 0);
        
        result = getAsString(query("cite", 11, "?f=json&geometryType=GeometryPolyLine&geometry={paths:[[[-0.001,0],[0,0.0015]]]}&spatialRel=SpatialRelEnvelopeIntersects"));
        assertTrue("Request specifying spatialreference; returned " + result, JsonSchemaTest.validateJSON(result, "/gsr/1.0/featureSet.json"));
        json = JSONObject.fromObject(result);
        features = json.getJSONArray("features");
        assertTrue("Should have results for envelope query at 0,0. JSON was: " + result, features.size() == 1);
    }
}

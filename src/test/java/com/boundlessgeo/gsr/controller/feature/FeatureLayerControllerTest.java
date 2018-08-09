/* Copyright (c) 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.controller.feature;

import com.boundlessgeo.gsr.controller.ControllerTest;
import com.boundlessgeo.gsr.model.geometry.SpatialReference;
import com.boundlessgeo.gsr.model.geometry.SpatialReferenceWKID;
import com.boundlessgeo.gsr.model.geometry.SpatialReferenceWKT;
import com.boundlessgeo.gsr.translate.feature.FeatureEncoder;
import com.boundlessgeo.gsr.translate.geometry.SpatialReferenceEncoder;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import net.sf.json.JSONArray;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geotools.feature.FeatureIterator;
import org.junit.Before;
import org.junit.Test;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Test FeatureService {@link com.boundlessgeo.gsr.api.feature.FeatureLayerController} access.
 * @author Jody Garnett (Boundless)
 */
public class FeatureLayerControllerTest extends ControllerTest {

    private String query(String service, String layer, String params) {
        return getBaseURL() + service + "/FeatureServer/" + layer + params;
    }

    @Before
    public void revert() throws IOException {
        //Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        revertLayer(MockData.POINTS);
        revertLayer(MockData.MPOINTS);
        revertLayer(MockData.MLINES);
        revertLayer(MockData.MPOLYGONS);
        revertLayer(MockData.LINES);
        revertLayer(MockData.POLYGONS);
    }

    @Test
    public void testBasicQuery() throws Exception {
        String q = query("cite", "1", "?f=json");
        JSON result = getAsJSON(q);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        Object type = json.get("type");

        assertEquals( "Feature Layer", type );
    }

    @Test
    public void testUpdateFeatures() throws Exception {
        //Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf","Lines");
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        //verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        FeatureIterator iterator = fti.getFeatureSource(null, null).getFeatures().features();
        org.opengis.feature.Feature nativeFeature = iterator.next();
        //geom should be valid, and not yet equal to the new value
        LineString nativeGeom = (LineString) nativeFeature.getDefaultGeometryProperty().getValue();
        assertTrue(Math.abs(nativeGeom.getGeometryN(0).getCoordinates()[0].x - 500050.0) >= 0.1);
        assertTrue(Math.abs(nativeGeom.getGeometryN(0).getCoordinates()[0].y - 499950.0) >= 0.1);

        String q = query("cgf", "0", "/updateFeatures?f=json");

        String body =
                "[\n" +
                        "  {\n" +
                        "  \"geometry\" : {" +
                        "      \"geometryType\":\"esriGeometryPolyline\", " +
                        "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], " +
                        "      \"spatialReference\" : {\"wkid\" : 32615}" +
                        "    },\n" +
                        "    \"attributes\" : {\n" +
                        "      \"objectid\" : 0,\n" +
                        "      \"id\" : \"t0001\",\n" +
                        "    }\n" +
                        "  }\n" +
                        "]";
        JSON result = postAsJSON(q, body, "application/json");
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("updateResults").getJSONObject(0);
        assertEquals( 0, resultObj.getInt("objectId"));
        assertEquals( true, resultObj.getBoolean("success"));

        //verify feature was updated
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        iterator = fti.getFeatureSource(null, null).getFeatures().features();
        nativeFeature = iterator.next();
        //geom should be valid
        nativeGeom = (LineString) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(nativeGeom.getGeometryN(0).getCoordinates()[0].x, 500050.0, 0.1);
        assertEquals(nativeGeom.getGeometryN(0).getCoordinates()[0].y, 499950.0, 0.1);
    }

    @Test
    public void testUpdateFeaturesErrors() throws Exception {
        String q = query("cgf", "0", "/updateFeatures?f=json");

        //no id
        String body =
                "[\n" +
                        "  {\n" +
                        "  \"geometry\" : {" +
                        "      \"geometryType\":\"esriGeometryPolyline\", " +
                        "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], " +
                        "      \"spatialReference\" : {\"wkid\" : 32615}" +
                        "    },\n" +
                        "    \"attributes\" : {\n" +
                        "      \"id\" : \"t0001\",\n" +
                        "    }\n" +
                        "  }\n" +
                        "]";
        JSON result = postAsJSON(q, body, "application/json");

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("updateResults").getJSONObject(0);
        assertEquals( false, resultObj.getBoolean("success"));
        assertEquals(1019, resultObj.getJSONObject("error").getInt("code"));

        //malformed geometry
        body =
                "[\n" +
                        "  {\n" +
                        "  \"geometry\" : {},\n" +
                        "    \"attributes\" : {\n" +
                        "      \"objectid\" : 0,\n" +
                        "      \"id\" : \"t0001\",\n" +
                        "    }\n" +
                        "  }\n" +
                        "]";
        result = postAsJSON(q, body, "application/json");

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        resultObj = json.getJSONArray("updateResults").getJSONObject(0);
        assertEquals( false, resultObj.getBoolean("success"));
        assertEquals(1000, resultObj.getJSONObject("error").getInt("code"));

        //missing attribute
        body =
                "[\n" +
                        "  {\n" +
                        "  \"geometry\" : {" +
                        "      \"geometryType\":\"esriGeometryPolyline\", " +
                        "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], " +
                        "      \"spatialReference\" : {\"wkid\" : 32615}" +
                        "    },\n" +
                        "    \"attributes\" : {\n" +
                        "      \"objectid\" : 0\n" +
                        "    }\n" +
                        "  }\n" +
                        "]";
        result = postAsJSON(q, body, "application/json");
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        resultObj = json.getJSONArray("updateResults").getJSONObject(0);
        assertEquals( 0, resultObj.getInt("objectId"));
        assertEquals( false, resultObj.getBoolean("success"));
        assertEquals(1000, resultObj.getJSONObject("error").getInt("code"));
    }

    /*
     * Get a MultiPoint geom from the Feature Layer endpoint
     * Post it back to the update features endpoint
     * Verify the native geometry is unchanged
     */
    @Test
    public void testUpdateFeaturesRoundTripMPoint() throws Exception {
        //Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf","MPoints");

        //verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        FeatureIterator iterator = fti.getFeatureSource(null, null).getFeatures().features();
        org.opengis.feature.Feature nativeFeature = iterator.next();
        //geom should be valid, and not yet equal to the new value
        MultiPoint nativeGeom = (MultiPoint) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(2, nativeGeom.getNumGeometries());

        //Get cgf:MPolygons, feature 0
        String q = query("cgf", "2", "/0?f=json");
        JSON result = getAsJSON(q);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;
        JSONObject feature = json.getJSONObject("feature");

        JSONArray featureArray = new JSONArray();
        featureArray.add(feature);

        String body = featureArray.toString();

        //do POST
        q = query("cgf", "2", "/updateFeatures?f=json");
        result = postAsJSON(q, body, "application/json");

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("updateResults").getJSONObject(0);

        assertEquals( true, resultObj.getBoolean("success"));
        assertEquals( 0, resultObj.getInt("objectId"));


        //verify feature was updated
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        iterator = fti.getFeatureSource(null, null).getFeatures().features();
        nativeFeature = iterator.next();

        //updated geom should be unchanged
        MultiPoint updatedGeom = (MultiPoint) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(2, nativeGeom.getNumGeometries());
        assertEquals(nativeGeom, updatedGeom);
    }

    /*
     * Get a MultiLine geom from the Feature Layer endpoint
     * Post it back to the update features endpoint
     * Verify the native geometry is unchanged
     */
    @Test
    public void testUpdateFeaturesRoundTripMLine() throws Exception {
        //Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf","MLines");

        //verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        FeatureIterator iterator = fti.getFeatureSource(null, null).getFeatures().features();
        org.opengis.feature.Feature nativeFeature = iterator.next();
        //geom should be valid, and not yet equal to the new value
        MultiLineString nativeGeom = (MultiLineString) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(2, nativeGeom.getNumGeometries());

        //Get cgf:MPolygons, feature 0
        String q = query("cgf", "1", "/0?f=json");
        JSON result = getAsJSON(q);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;
        JSONObject feature = json.getJSONObject("feature");

        JSONArray featureArray = new JSONArray();
        featureArray.add(feature);

        String body = featureArray.toString();

        //do POST
        q = query("cgf", "1", "/updateFeatures?f=json");
        result = postAsJSON(q, body, "application/json");

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("updateResults").getJSONObject(0);

        assertEquals( true, resultObj.getBoolean("success"));
        assertEquals( 0, resultObj.getInt("objectId"));


        //verify feature was updated
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        iterator = fti.getFeatureSource(null, null).getFeatures().features();
        nativeFeature = iterator.next();

        //updated geom should be unchanged
        MultiLineString updatedGeom = (MultiLineString) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(2, nativeGeom.getNumGeometries());
        assertEquals(nativeGeom, updatedGeom);
    }

    /*
     * Get a MultiPolygon geom from the Feature Layer endpoint
     * Post it back to the update features endpoint
     * Verify the native geometry is unchanged
     */
    @Test
    public void testUpdateFeaturesRoundTripMPolygon() throws Exception {
        //Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf","MPolygons");

        //verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        FeatureIterator iterator = fti.getFeatureSource(null, null).getFeatures().features();
        org.opengis.feature.Feature nativeFeature = iterator.next();
        //geom should be valid, and not yet equal to the new value
        MultiPolygon nativeGeom = (MultiPolygon) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(2, nativeGeom.getNumGeometries());

        //Get cgf:MPolygons, feature 0
        String q = query("cgf", "3", "/0?f=json");
        JSON result = getAsJSON(q);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;
        JSONObject feature = json.getJSONObject("feature");

        JSONArray featureArray = new JSONArray();
        featureArray.add(feature);

        String body = featureArray.toString();

        //do POST
        q = query("cgf", "3", "/updateFeatures?f=json");
        result = postAsJSON(q, body, "application/json");

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("updateResults").getJSONObject(0);

        assertEquals( true, resultObj.getBoolean("success"));
        assertEquals( 0, resultObj.getInt("objectId"));


        //verify feature was updated
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        iterator = fti.getFeatureSource(null, null).getFeatures().features();
        nativeFeature = iterator.next();

        //updated geom should be unchanged
        MultiPolygon updatedGeom = (MultiPolygon) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(2, nativeGeom.getNumGeometries());
        assertEquals(nativeGeom, updatedGeom);
    }

    @Test
    public void testAddFeatures() throws Exception {
        //Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf","Lines");

        //verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        FeatureIterator iterator = fti.getFeatureSource(null, null).getFeatures().features();
        org.opengis.feature.Feature nativeFeature = iterator.next();
        //geom should be valid
        LineString nativeGeom = (LineString) nativeFeature.getDefaultGeometryProperty().getValue();
        assertTrue(Math.abs(nativeGeom.getGeometryN(0).getCoordinates()[0].x - 500050.0) >= 0.1);
        assertTrue(Math.abs(nativeGeom.getGeometryN(0).getCoordinates()[0].y - 499950.0) >= 0.1);

        String q = query("cgf", "0", "/addFeatures?f=json");

        String body =
                "[\n" +
                        "  {\n" +
                        "  \"geometry\" : {" +
                        "      \"geometryType\":\"esriGeometryPolyline\", " +
                        "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], " +
                        "      \"spatialReference\" : {\"wkid\" : 32615}" +
                        "    },\n" +
                        "    \"attributes\" : {\n" +
                        "      \"id\" : \"t0002\",\n" +
                        "    }\n" +
                        "  }\n" +
                        "]";
        JSON result = postAsJSON(q, body, "application/json");
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("addResults").getJSONObject(0);
        assertNotSame( 0, resultObj.getInt("objectId"));
        assertEquals( true, resultObj.getBoolean("success"));

        //verify feature was added
        assertEquals(2, fti.getFeatureSource(null, null).getFeatures().size());

        iterator = fti.getFeatureSource(null, null).getFeatures().features();
        nativeFeature = iterator.next();
        org.opengis.feature.Feature nativeFeature2 = iterator.next();
        //geom should be valid
        nativeGeom = (LineString) nativeFeature2.getDefaultGeometryProperty().getValue();
        assertEquals(nativeGeom.getGeometryN(0).getCoordinates()[0].x, 500050.0, 0.1);
        assertEquals(nativeGeom.getGeometryN(0).getCoordinates()[0].y, 499950.0, 0.1);
    }

    @Test
    public void testAddFeaturesErrors() throws Exception {
        String q = query("cgf", "0", "/addFeatures?f=json");

        //malformed geometry
        String body =
                "[\n" +
                        "  {\n" +
                        "  \"geometry\" : {},\n" +
                        "    \"attributes\" : {\n" +
                        "      \"objectid\" : 0,\n" +
                        "      \"id\" : \"t0001\",\n" +
                        "    }\n" +
                        "  }\n" +
                        "]";
        JSON result = postAsJSON(q, body, "application/json");

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("addResults").getJSONObject(0);
        assertEquals( false, resultObj.getBoolean("success"));
        assertEquals(1000, resultObj.getJSONObject("error").getInt("code"));

        //missing attribute
        body =
                "[\n" +
                        "  {\n" +
                        "  \"geometry\" : {" +
                        "      \"geometryType\":\"esriGeometryPolyline\", " +
                        "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], " +
                        "      \"spatialReference\" : {\"wkid\" : 32615}" +
                        "    },\n" +
                        "    \"attributes\" : {\n" +
                        "      \"objectid\" : 0\n" +
                        "    }\n" +
                        "  }\n" +
                        "]";
        result = postAsJSON(q, body, "application/json");
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        resultObj = json.getJSONArray("addResults").getJSONObject(0);
        assertEquals( false, resultObj.getBoolean("success"));
        assertEquals(1000, resultObj.getJSONObject("error").getInt("code"));

        //duplicate objectid, should lead to a new feature with a new objectid
        body =
                "[\n" +
                        "  {\n" +
                        "  \"geometry\" : {" +
                        "      \"geometryType\":\"esriGeometryPolyline\", " +
                        "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], " +
                        "      \"spatialReference\" : {\"wkid\" : 32615}" +
                        "    },\n" +
                        "    \"attributes\" : {\n" +
                        "      \"objectid\" : 0,\n" +
                        "      \"id\" : \"t0001\",\n" +
                        "    }\n" +
                        "  }\n" +
                        "]";
        result = postAsJSON(q, body, "application/json");
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        resultObj = json.getJSONArray("addResults").getJSONObject(0);
        assertEquals( true, resultObj.getBoolean("success"));
        assertNotSame(0,resultObj.get("objectId"));
    }

    @Test
    public void testDeleteFeaturesByObjectId() throws Exception {
        //Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf","Lines");

        //verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        String q = query("cgf", "0", "/deleteFeatures?f=json&objectIds=0");

        String body = "";

        JSON result = postAsJSON(q, body, "application/json");
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("deleteResults").getJSONObject(0);
        assertEquals( true, resultObj.getBoolean("success"));

        //verify feature was delete
        assertEquals(0, fti.getFeatureSource(null, null).getFeatures().size());


    }

    @Test
    public void testDeleteFeaturesByGeometryQuery() throws Exception {
        //Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf","Lines");

        //verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        String q = query("cgf", "0", "/deleteFeatures?f=json" +
                "&geometry=500124,500024,500176,500076" +
                "&geometryType=esriGeometryEnvelope" +
                "&inSR=32615" +
                "&outSR=32615" +
                "&spatialRel=esriSpatialRelEnvelopeIntersects");

        String body = "";

        JSON result = postAsJSON(q, body, "application/json");
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("deleteResults").getJSONObject(0);
        assertEquals( true, resultObj.getBoolean("success"));

        //verify feature was delete
        assertEquals(0, fti.getFeatureSource(null, null).getFeatures().size());


    }

    @Test
    public void testDeleteFeaturesByWhereQuery() throws Exception {
        //Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf","Lines");

        //verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        String q = query("cgf", "0", "/deleteFeatures?f=json" +
                "&where=id in ('Lines.0')" );

        String body = "";

        JSON result = postAsJSON(q, body, "application/json");
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("deleteResults").getJSONObject(0);
        assertEquals( true, resultObj.getBoolean("success"));

        //verify feature was delete
        assertEquals(0, fti.getFeatureSource(null, null).getFeatures().size());


    }
}
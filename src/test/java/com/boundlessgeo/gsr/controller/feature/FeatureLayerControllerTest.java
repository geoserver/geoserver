/* Copyright (c) 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.controller.feature;

import com.boundlessgeo.gsr.controller.ControllerTest;
import com.boundlessgeo.gsr.translate.feature.FeatureEncoder;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
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

        //wrong geometry type
        body =
                "[\n" +
                        "  {\n" +
                        "  \"geometry\" : {" +
                        "      \"geometryType\":\"esriGeometryPoint\", " +
                        "      \"x\" : 500050.0," +
                        "      \"y\" : 499950.0, " +
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

        resultObj = json.getJSONArray("updateResults").getJSONObject(0);
        assertEquals( 0, resultObj.getInt("objectId"));
        assertEquals( false, resultObj.getBoolean("success"));
        assertEquals(1000, resultObj.getJSONObject("error").getInt("code"));

    }
}

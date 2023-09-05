/* Copyright (c) 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.controller.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import javax.xml.namespace.QName;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gsr.controller.ControllerTest;
import org.geotools.feature.FeatureIterator;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Test FeatureService {@link org.geoserver.gsr.api.feature.FeatureLayerController} access.
 *
 * @author Jody Garnett (Boundless)
 */
public class FeatureLayerControllerTest extends ControllerTest {

    static final String RENDER = "render";
    static final String RENDER_PREFIX = "ren";
    static final QName TRIANGLES = new QName(RENDER, "Triangles", RENDER_PREFIX);
    static final QName DIAMONDS = new QName(RENDER, "Diamonds", RENDER_PREFIX);
    static final QName POI = new QName(RENDER, "poi", RENDER_PREFIX);

    @Override
    public void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addStyle(
                "triangle", "triangle.sld", FeatureLayerControllerTest.class, getCatalog());
        testData.addVectorLayer(
                TRIANGLES,
                Collections.singletonMap(SystemTestData.LayerProperty.STYLE, "triangle"),
                "Points.properties",
                SystemTestData.class,
                getCatalog());
        testData.addStyle("diamond", "diamond.sld", FeatureLayerControllerTest.class, getCatalog());
        testData.addVectorLayer(
                DIAMONDS,
                Collections.singletonMap(SystemTestData.LayerProperty.STYLE, "diamond"),
                "Points.properties",
                SystemTestData.class,
                getCatalog());
        testData.addStyle(
                "poi-picture", "poi-picture.sld", FeatureLayerControllerTest.class, getCatalog());
        testData.addVectorLayer(
                POI,
                Collections.singletonMap(SystemTestData.LayerProperty.STYLE, "poi-picture"),
                "poi.properties",
                FeatureLayerControllerTest.class,
                getCatalog());

        // copy the symbol images
        File[] icons =
                new File("src/test/resources/org/geoserver/gsr/controller/feature/")
                        .listFiles(f -> f.getName().endsWith(".png"));
        for (File icon : icons) {
            FileUtils.copyFile(
                    icon, new File(getDataDirectory().getStyles().dir(), icon.getName()));
        }
    }

    private String query(String service, String layer, String params) {
        return getBaseURL() + service + "/FeatureServer/" + layer + params;
    }

    private String serviceQuery(String service, String params) {
        return getBaseURL() + service + "/FeatureServer" + params;
    }

    @Before
    public void revert() throws IOException {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
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

        assertEquals("Feature Layer", type);
    }

    @Test
    public void testUpdateFeatures() throws Exception {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        // verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        FeatureIterator iterator = fti.getFeatureSource(null, null).getFeatures().features();
        org.geotools.api.feature.Feature nativeFeature = iterator.next();
        // geom should be valid, and not yet equal to the new value
        LineString nativeGeom = (LineString) nativeFeature.getDefaultGeometryProperty().getValue();
        assertTrue(Math.abs(nativeGeom.getGeometryN(0).getCoordinates()[0].x - 500050.0) >= 0.1);
        assertTrue(Math.abs(nativeGeom.getGeometryN(0).getCoordinates()[0].y - 499950.0) >= 0.1);

        String q = query("cgf", "0", "/updateFeatures?f=json");

        String body =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 0,\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        JSON result = postUpdatesAsForm(q, body, true, null);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;
        assertTrue(json.has("editMoment"));

        JSONObject resultObj = json.getJSONArray("updateResults").getJSONObject(0);
        assertEquals(0, resultObj.getInt("objectId"));
        assertEquals(true, resultObj.getBoolean("success"));

        // verify feature was updated
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        iterator = fti.getFeatureSource(null, null).getFeatures().features();
        nativeFeature = iterator.next();
        // geom should be valid
        nativeGeom = (LineString) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(nativeGeom.getGeometryN(0).getCoordinates()[0].x, 500050.0, 0.1);
        assertEquals(nativeGeom.getGeometryN(0).getCoordinates()[0].y, 499950.0, 0.1);
    }

    @Test
    public void testUpdateFeaturesErrors() throws Exception {
        String q = query("cgf", "0", "/updateFeatures?f=json");

        // no id
        String body =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        JSON result = postUpdatesAsForm(q, body, false, null);

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("updateResults").getJSONObject(0);
        assertEquals(false, resultObj.getBoolean("success"));
        assertEquals(1019, resultObj.getJSONObject("error").getInt("code"));

        // malformed geometry
        body =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {},\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 0,\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        result = postUpdatesAsForm(q, body, false, null);

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        resultObj = json.getJSONArray("updateResults").getJSONObject(0);
        assertEquals(false, resultObj.getBoolean("success"));
        assertEquals(1000, resultObj.getJSONObject("error").getInt("code"));

        // missing attribute
        body =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 0\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        result = postUpdatesAsForm(q, body, false, null);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        resultObj = json.getJSONArray("updateResults").getJSONObject(0);
        assertEquals(0, resultObj.getInt("objectId"));
        assertEquals(false, resultObj.getBoolean("success"));
        assertEquals(1000, resultObj.getJSONObject("error").getInt("code"));
    }

    /*
     * Get a MultiPoint geom from the Feature Layer endpoint
     * Post it back to the update features endpoint
     * Verify the native geometry is unchanged
     */
    @Test
    public void testUpdateFeaturesRoundTripMPoint() throws Exception {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "MPoints");

        // verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        FeatureIterator iterator = fti.getFeatureSource(null, null).getFeatures().features();
        org.geotools.api.feature.Feature nativeFeature = iterator.next();
        // geom should be valid, and not yet equal to the new value
        MultiPoint nativeGeom = (MultiPoint) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(2, nativeGeom.getNumGeometries());

        // Get cgf:MPolygons, feature 0
        String q = query("cgf", "2", "/0?f=json");
        JSON result = getAsJSON(q);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;
        JSONObject feature = json.getJSONObject("feature");

        JSONArray featureArray = new JSONArray();
        featureArray.add(feature);

        String body = featureArray.toString();

        // do POST
        q = query("cgf", "2", "/updateFeatures?f=json");
        result = postUpdatesAsForm(q, body, false, null);

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("updateResults").getJSONObject(0);

        assertEquals(true, resultObj.getBoolean("success"));
        assertEquals(0, resultObj.getInt("objectId"));

        // verify feature was updated
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        iterator = fti.getFeatureSource(null, null).getFeatures().features();
        nativeFeature = iterator.next();

        // updated geom should be unchanged
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
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "MLines");

        // verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        FeatureIterator iterator = fti.getFeatureSource(null, null).getFeatures().features();
        org.geotools.api.feature.Feature nativeFeature = iterator.next();
        // geom should be valid, and not yet equal to the new value
        MultiLineString nativeGeom =
                (MultiLineString) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(2, nativeGeom.getNumGeometries());

        // Get cgf:MPolygons, feature 0
        String q = query("cgf", "1", "/0?f=json");
        JSON result = getAsJSON(q);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;
        JSONObject feature = json.getJSONObject("feature");

        JSONArray featureArray = new JSONArray();
        featureArray.add(feature);

        String body = featureArray.toString();

        // do POST
        q = query("cgf", "1", "/updateFeatures?f=json");
        result = postUpdatesAsForm(q, body, false, null);

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("updateResults").getJSONObject(0);

        assertEquals(true, resultObj.getBoolean("success"));
        assertEquals(0, resultObj.getInt("objectId"));

        // verify feature was updated
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        iterator = fti.getFeatureSource(null, null).getFeatures().features();
        nativeFeature = iterator.next();

        // updated geom should be unchanged
        MultiLineString updatedGeom =
                (MultiLineString) nativeFeature.getDefaultGeometryProperty().getValue();
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
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "MPolygons");

        // verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        FeatureIterator iterator = fti.getFeatureSource(null, null).getFeatures().features();
        org.geotools.api.feature.Feature nativeFeature = iterator.next();
        // geom should be valid, and not yet equal to the new value
        MultiPolygon nativeGeom =
                (MultiPolygon) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(2, nativeGeom.getNumGeometries());

        // Get cgf:MPolygons, feature 0
        String q = query("cgf", "3", "/0?f=json");
        JSON result = getAsJSON(q);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;
        JSONObject feature = json.getJSONObject("feature");

        JSONArray featureArray = new JSONArray();
        featureArray.add(feature);

        String body = featureArray.toString();

        // do POST
        q = query("cgf", "3", "/updateFeatures?f=json");
        result = postUpdatesAsForm(q, body, false, null);

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("updateResults").getJSONObject(0);

        assertEquals(true, resultObj.getBoolean("success"));
        assertEquals(0, resultObj.getInt("objectId"));

        // verify feature was updated
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        iterator = fti.getFeatureSource(null, null).getFeatures().features();
        nativeFeature = iterator.next();

        // updated geom should be unchanged
        MultiPolygon updatedGeom =
                (MultiPolygon) nativeFeature.getDefaultGeometryProperty().getValue();
        assertEquals(2, nativeGeom.getNumGeometries());
        assertEquals(nativeGeom, updatedGeom);
    }

    @Test
    public void testAddFeatures() throws Exception {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");

        // verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
        FeatureIterator iterator = fti.getFeatureSource(null, null).getFeatures().features();
        org.geotools.api.feature.Feature nativeFeature = iterator.next();
        // geom should be valid
        LineString nativeGeom = (LineString) nativeFeature.getDefaultGeometryProperty().getValue();
        assertTrue(Math.abs(nativeGeom.getGeometryN(0).getCoordinates()[0].x - 500050.0) >= 0.1);
        assertTrue(Math.abs(nativeGeom.getGeometryN(0).getCoordinates()[0].y - 499950.0) >= 0.1);

        String q = query("cgf", "0", "/addFeatures?f=json");

        String body =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"id\" : \"t0002\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        JSON result = postUpdatesAsForm(q, body, false, null);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;
        assertFalse(json.has("editMoment"));

        JSONObject resultObj = json.getJSONArray("addResults").getJSONObject(0);
        assertNotSame(0, resultObj.getInt("objectId"));
        assertEquals(true, resultObj.getBoolean("success"));

        // verify feature was added
        assertEquals(2, fti.getFeatureSource(null, null).getFeatures().size());

        iterator = fti.getFeatureSource(null, null).getFeatures().features();
        nativeFeature = iterator.next();
        org.geotools.api.feature.Feature nativeFeature2 = iterator.next();
        // geom should be valid
        nativeGeom = (LineString) nativeFeature2.getDefaultGeometryProperty().getValue();
        assertEquals(nativeGeom.getGeometryN(0).getCoordinates()[0].x, 500050.0, 0.1);
        assertEquals(nativeGeom.getGeometryN(0).getCoordinates()[0].y, 499950.0, 0.1);
    }

    @Test
    public void testAddFeaturesErrors() throws Exception {
        String q = query("cgf", "0", "/addFeatures?f=json");

        // malformed geometry
        String body =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {},\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 0,\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        JSON result = postUpdatesAsForm(q, body, false, null);

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("addResults").getJSONObject(0);
        assertEquals(false, resultObj.getBoolean("success"));
        assertEquals(1000, resultObj.getJSONObject("error").getInt("code"));

        // missing attribute
        body =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 0\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        result = postUpdatesAsForm(q, body, false, null);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        resultObj = json.getJSONArray("addResults").getJSONObject(0);
        assertEquals(false, resultObj.getBoolean("success"));
        assertEquals(1000, resultObj.getJSONObject("error").getInt("code"));

        // duplicate objectid, should lead to a new feature with a new objectid
        body =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 0,\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        result = postUpdatesAsForm(q, body, false, null);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        json = (JSONObject) result;

        resultObj = json.getJSONArray("addResults").getJSONObject(0);
        assertEquals(true, resultObj.getBoolean("success"));
        assertNotSame(0, resultObj.get("objectId"));
    }

    @Test
    public void testAddFeaturesWithoutRollback() throws Exception {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");

        // verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        String q = query("cgf", "0", "/addFeatures?f=json");

        // 3 features, one invalid
        String body =
                "[\n"
                        +
                        // valid
                        "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 1,\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "    }\n"
                        + "  },\n"
                        +
                        // invalid - missing attribute
                        "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 2\n"
                        + "    }\n"
                        + "  },\n"
                        +
                        // valid
                        "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 3,\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        JSON result = postUpdatesAsForm(q, body, false, false);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONArray addResults = json.getJSONArray("addResults");
        assertEquals(true, addResults.getJSONObject(0).getBoolean("success"));
        assertEquals(false, addResults.getJSONObject(1).getBoolean("success"));
        assertEquals(true, addResults.getJSONObject(2).getBoolean("success"));

        // verify 2 valid features were added
        assertEquals(3, fti.getFeatureSource(null, null).getFeatures().size());
    }

    @Test
    public void testAddFeaturesWithRollback() throws Exception {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");

        // verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        String q = query("cgf", "0", "/addFeatures?f=json");

        // 3 features, one invalid
        String body =
                "[\n"
                        +
                        // valid
                        "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 1,\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "    }\n"
                        + "  },\n"
                        +
                        // invalid - missing attribute
                        "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 2\n"
                        + "    }\n"
                        + "  },\n"
                        +
                        // valid
                        "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 3,\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        JSON result = postUpdatesAsForm(q, body, false, true);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;
        print(json);

        assertTrue(json.has("error"));
        assertEquals("Operation rolled back.", json.getJSONObject("error").getString("message"));

        // verify feature state unchanged
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());
    }

    @Test
    public void testDeleteFeaturesByObjectId() throws Exception {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");

        // verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        String q = query("cgf", "0", "/deleteFeatures?f=json");

        // JSON result = postAsJSON(q, body, "application/json");
        JSON result = postDeletesAsForm(q, "0", null, null, null, null, null, null, false, false);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("deleteResults").getJSONObject(0);
        assertEquals(true, resultObj.getBoolean("success"));

        // verify feature was delete
        assertEquals(0, fti.getFeatureSource(null, null).getFeatures().size());
    }

    @Test
    public void testDeleteFeaturesByGeometryQuery() throws Exception {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");

        // verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        String q = query("cgf", "0", "/deleteFeatures?f=json");

        // JSON result = postAsJSON(q, body, "application/json");
        JSON result =
                postDeletesAsForm(
                        q,
                        null,
                        null,
                        "500124,500024,500176,500076",
                        "esriGeometryEnvelope",
                        "32615",
                        "32615",
                        "esriSpatialRelEnvelopeIntersects",
                        false,
                        false);

        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("deleteResults").getJSONObject(0);
        assertEquals(true, resultObj.getBoolean("success"));

        // verify feature was delete
        assertEquals(0, fti.getFeatureSource(null, null).getFeatures().size());
    }

    @Test
    public void testDeleteFeaturesByWhereQuery() throws Exception {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");

        // verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        String q = query("cgf", "0", "/deleteFeatures?f=json");

        String body = "";

        JSON result =
                postDeletesAsForm(
                        q, null, "id in ('Lines.0')", null, null, null, null, null, false, false);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("deleteResults").getJSONObject(0);
        assertEquals(true, resultObj.getBoolean("success"));

        // verify feature was delete
        assertEquals(0, fti.getFeatureSource(null, null).getFeatures().size());
    }

    @Test
    public void testApplyEditsServiceLayer() throws Exception {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo fti = catalog.getFeatureTypeByName("cgf", "Lines");

        // verify initial feature state
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        String q =
                query(
                        "cgf",
                        "0",
                        "/applyEdits?f=json&rollbackOnFailure=false&returnEditMoment=false");

        String addsBody =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "\"objectid\" : 1535485437364,\n"
                        + "      \"id\" : \"t0002\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        String updatesBody =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "\"objectid\" : 0,\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        String deletesBody = "0";

        JSON result = postServiceLayerEditsAsForm(q, deletesBody, addsBody, updatesBody);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);

        JSONObject json = (JSONObject) result;

        JSONObject resultObj = json.getJSONArray("deleteResults").getJSONObject(0);
        assertEquals(true, resultObj.getBoolean("success"));

        // verify feature was deleted
        assertEquals(1, fti.getFeatureSource(null, null).getFeatures().size());

        assertNotSame(
                "Lines.0",
                fti.getFeatureSource(null, null)
                        .getFeatures()
                        .features()
                        .next()
                        .getIdentifier()
                        .getID());

        System.out.println(
                fti.getFeatureSource(null, null).getFeatures().features().next().getProperty("id"));
        // verify feature was added
        JSONObject resultObj2 = json.getJSONArray("addResults").getJSONObject(0);
        assertEquals(true, resultObj2.getBoolean("success"));
        assertEquals(
                "t0002",
                fti.getFeatureSource(null, null)
                        .getFeatures()
                        .features()
                        .next()
                        .getProperty("id")
                        .getValue());

        JSONObject resultObj3 = json.getJSONArray("updateResults").getJSONObject(0);
        assertEquals(true, resultObj3.getBoolean("success"));

        System.out.println(json);
    }

    @Test
    public void testApplyEditsService() throws Exception {
        // Use 'cgf' workspace - 0 Lines, 1 MLines, 2 MPoints, 3 MPolygons, 4 Points, 5 Polygons,
        Catalog catalog = getCatalog();
        FeatureTypeInfo ftiLines = catalog.getFeatureTypeByName("cgf", "Lines");
        FeatureTypeInfo ftiPoints = catalog.getFeatureTypeByName("cgf", "Points");

        // verify initial feature state
        assertEquals(1, ftiLines.getFeatureSource(null, null).getFeatures().size());
        assertEquals(1, ftiPoints.getFeatureSource(null, null).getFeatures().size());

        String q =
                serviceQuery(
                        "cgf", "/applyEdits?f=json&rollbackOnFailure=false&returnEditMoment=false");

        String addsBodyLine =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"id\" : \"t0002\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        String updatesBodyLine =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPolyline\", "
                        + "      \"paths\" : [[[500050.0, 499950.0],[500150.0, 500050.0]]], "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 0,\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        String deletesBodyLine = "[0]";

        String layerEdit0 =
                "{\"id\":0,"
                        + "\"adds\":"
                        + addsBodyLine
                        + ","
                        + "\"updates\":"
                        + updatesBodyLine
                        + ","
                        + "\"deletes\":"
                        + deletesBodyLine
                        + "}";

        String addsBodyPoint =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPoint\", "
                        + "      \"x\" : 50051, "
                        + "      \"y\" : 50051, "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"id\" : \"t0002\",\n"
                        + "      \"altitude\" : 400,\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        String updatesBodyPoint =
                "[\n"
                        + "  {\n"
                        + "  \"geometry\" : {"
                        + "      \"geometryType\":\"esriGeometryPoint\", "
                        + "      \"x\" : 50051, "
                        + "      \"y\" : 50051, "
                        + "      \"spatialReference\" : {\"wkid\" : 32615}"
                        + "    },\n"
                        + "    \"attributes\" : {\n"
                        + "      \"objectid\" : 0,\n"
                        + "      \"id\" : \"t0001\",\n"
                        + "      \"altitude\" : 350,\n"
                        + "    }\n"
                        + "  }\n"
                        + "]";
        String deletesBodyPoint = "[0]";

        String layerEdit4 =
                "{\"id\":4,"
                        + "\"adds\":"
                        + addsBodyPoint
                        + ","
                        + "\"updates\":"
                        + updatesBodyPoint
                        + ","
                        + "\"deletes\":"
                        + deletesBodyPoint
                        + "}";

        String serviceEdits = "[" + layerEdit0 + "," + layerEdit4 + "]";

        JSON result = postServiceEditsAsForm(q, serviceEdits);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONArray);

        JSONArray json = (JSONArray) result;

        JSONObject resultLineLayer = (JSONObject) json.get(0);
        JSONObject resultLineDelete =
                resultLineLayer.getJSONArray("deleteResults").getJSONObject(0);
        assertEquals(true, resultLineDelete.getBoolean("success"));

        // verify feature was deleted
        assertEquals(1, ftiLines.getFeatureSource(null, null).getFeatures().size());

        assertNotSame(
                "Lines.0",
                ftiLines.getFeatureSource(null, null)
                        .getFeatures()
                        .features()
                        .next()
                        .getIdentifier()
                        .getID());

        System.out.println(
                ftiLines.getFeatureSource(null, null)
                        .getFeatures()
                        .features()
                        .next()
                        .getProperty("id"));
        // verify feature was added
        JSONObject resultLineAdd = resultLineLayer.getJSONArray("addResults").getJSONObject(0);
        assertEquals(true, resultLineAdd.getBoolean("success"));
        assertEquals(
                "t0002",
                ftiLines.getFeatureSource(null, null)
                        .getFeatures()
                        .features()
                        .next()
                        .getProperty("id")
                        .getValue());

        JSONObject resultLineUpdate =
                resultLineLayer.getJSONArray("updateResults").getJSONObject(0);
        assertEquals(true, resultLineUpdate.getBoolean("success"));

        JSONObject resultPointLayer = (JSONObject) json.get(1);
        JSONObject resultPointDelete =
                resultPointLayer.getJSONArray("deleteResults").getJSONObject(0);
        assertEquals(true, resultPointDelete.getBoolean("success"));

        // verify feature was deleted
        assertEquals(1, ftiPoints.getFeatureSource(null, null).getFeatures().size());

        assertNotSame(
                "Points.0",
                ftiLines.getFeatureSource(null, null)
                        .getFeatures()
                        .features()
                        .next()
                        .getIdentifier()
                        .getID());

        System.out.println(
                ftiPoints
                        .getFeatureSource(null, null)
                        .getFeatures()
                        .features()
                        .next()
                        .getProperty("id"));
        // verify feature was added
        JSONObject resultPointAdd = resultPointLayer.getJSONArray("addResults").getJSONObject(0);
        assertEquals(true, resultPointAdd.getBoolean("success"));
        assertEquals(
                "t0002",
                ftiPoints
                        .getFeatureSource(null, null)
                        .getFeatures()
                        .features()
                        .next()
                        .getProperty("id")
                        .getValue());

        JSONObject resultPointUpdate =
                resultPointLayer.getJSONArray("updateResults").getJSONObject(0);
        assertEquals(true, resultPointUpdate.getBoolean("success"));

        System.out.println(json);
    }

    protected JSON postServiceLayerEditsAsForm(
            String path, String deletes, String adds, String updates) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setContentType("application/x-www-form-urlencoded");
        request.addParameter("deletes", deletes);
        request.addParameter("adds", adds);
        request.addParameter("updates", updates);

        return JSONSerializer.toJSON(dispatch(request).getContentAsString());
    }

    protected JSON postServiceEditsAsForm(String path, String edits) throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setContentType("application/x-www-form-urlencoded");
        request.addParameter("edits", edits);
        return JSONSerializer.toJSON(dispatch(request).getContentAsString());
    }

    protected JSON postDeletesAsForm(
            String path,
            String objectIds,
            String where,
            String geometry,
            String geometryType,
            String inSR,
            String outSR,
            String spatialRel,
            Boolean returnEditMoment,
            Boolean rollbackOnFailure)
            throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setContentType("application/x-www-form-urlencoded");
        if (objectIds != null) {
            request.addParameter("objectIds", objectIds);
        }
        if (geometry != null) {
            request.addParameter("geometry", geometry);
        }
        if (geometryType != null) {
            request.addParameter("geometryType", geometryType);
        }
        if (inSR != null) {
            request.addParameter("inSR", inSR);
        }
        if (outSR != null) {
            request.addParameter("outSR", outSR);
        }
        if (spatialRel != null) {
            request.addParameter("spatialRel", spatialRel);
        }
        return JSONSerializer.toJSON(dispatch(request).getContentAsString());
    }

    protected JSON postUpdatesAsForm(
            String path, String features, Boolean returnEditMoment, Boolean rollbackOnFailure)
            throws Exception {
        MockHttpServletRequest request = createRequest(path);
        request.setMethod("POST");
        request.setContentType("application/x-www-form-urlencoded");
        if (features != null) {
            request.addParameter("features", features);
        }
        if (returnEditMoment != null) {
            request.addParameter("returnEditMoment", returnEditMoment.toString());
        }
        if (rollbackOnFailure != null) {
            request.addParameter("rollbackOnFailure", rollbackOnFailure.toString());
        }
        return JSONSerializer.toJSON(dispatch(request).getContentAsString());
    }

    @Test
    public void testTriangle() throws Exception {
        JSONObject json = (JSONObject) getAsJSON(query(TRIANGLES.getPrefix(), "1", ""));
        print(json);
        JSONObject renderer = json.getJSONObject("drawingInfo").getJSONObject("renderer");
        assertEquals("simple", renderer.getString("type"));
        JSONObject symbol = renderer.getJSONObject("symbol");
        assertEquals("esriSMS", symbol.getString("type"));
        assertEquals("esriSMSTriangle", symbol.getString("style"));
        assertEquals(12, symbol.getInt("size"));
    }

    @Test
    public void testTriangleHTML() throws Exception {
        Document document = getAsJSoup(query(TRIANGLES.getPrefix(), "1", "?f=html"));
        // TODO: actually test contents, so far it's just a smoke test
    }

    @Test
    public void testDiamond() throws Exception {
        JSONObject json = (JSONObject) getAsJSON(query(DIAMONDS.getPrefix(), "0", ""));
        print(json);
        JSONObject renderer = json.getJSONObject("drawingInfo").getJSONObject("renderer");
        assertEquals("simple", renderer.getString("type"));
        JSONObject symbol = renderer.getJSONObject("symbol");
        assertEquals("esriSMS", symbol.getString("type"));
        assertEquals("esriSMSDiamond", symbol.getString("style"));
        assertEquals(12, symbol.getInt("size"));
    }

    @Test
    public void testSimpleFillHTML() throws Exception {
        Document document = getAsJSoup(query("cite", "2", "?f=html"));
        // TODO: actually test contents, so far it's just a smoke test
    }

    @Test
    public void testPictureMarkerHTML() throws Exception {
        Document document = getAsJSoup(query(POI.getPrefix(), "2", "?f=html"));
        // TODO: actually test contents, so far it's just a smoke test
    }

    @Test
    public void testSimpleLineHTML() throws Exception {
        // mlines
        Document document = getAsJSoup(query("cgf", "1", "?f=html"));
        // TODO: actually test contents, so far it's just a smoke test
    }
}

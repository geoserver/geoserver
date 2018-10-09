/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.wms_1_1_1.GetFeatureInfoTest;
import org.geotools.util.NumberRange;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class GetFeatureInfoJSONTest extends GetFeatureInfoTest {

    /** Tests JSONP outside of expected polygon */
    @Test
    public void testSimpleJSONP() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.jsonp;

        // JSONP
        JSONType.setJsonpEnabled(true);
        MockHttpServletResponse response = getAsServletResponse(request, "");
        JSONType.setJsonpEnabled(false);

        // MimeType
        assertEquals(JSONType.jsonp, response.getContentType());

        // Check if the character encoding is the one expected
        assertTrue("UTF-8".equals(response.getCharacterEncoding()));

        // Content
        String result = response.getContentAsString();

        assertNotNull(result);

        assertTrue(result.startsWith(JSONType.CALLBACK_FUNCTION));
        assertTrue(result.endsWith(")"));
        assertTrue(result.indexOf("Green Forest") > 0);

        result = result.substring(0, result.length() - 1);
        result = result.substring(JSONType.CALLBACK_FUNCTION.length() + 1, result.length());

        JSONObject rootObject = JSONObject.fromObject(result);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertEquals(aFeature.getString("geometry_name"), "the_geom");
    }

    /** Tests jsonp with custom callback function */
    @Test
    public void testCustomJSONP() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.jsonp
                        + "&format_options="
                        + JSONType.CALLBACK_FUNCTION_KEY
                        + ":custom";
        // JSONP
        JSONType.setJsonpEnabled(true);
        MockHttpServletResponse response = getAsServletResponse(request, "");
        JSONType.setJsonpEnabled(false);

        // MimeType
        assertEquals(JSONType.jsonp, response.getContentType());

        // Check if the character encoding is the one expected
        assertTrue("UTF-8".equals(response.getCharacterEncoding()));

        // Content
        String result = response.getContentAsString();
        // System.out.println(result);
        assertNotNull(result);

        assertTrue(result.startsWith("custom("));
        assertTrue(result.endsWith(")"));
        assertTrue(result.indexOf("Green Forest") > 0);

        result = result.substring(0, result.length() - 1);
        result = result.substring("custom".length() + 1, result.length());

        JSONObject rootObject = JSONObject.fromObject(result);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertEquals(aFeature.getString("geometry_name"), "the_geom");
    }

    /** Tests JSON outside of expected polygon */
    @Test
    public void testSimpleJSON() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.json;

        // JSON
        MockHttpServletResponse response = getAsServletResponse(request, "");

        // MimeType
        assertEquals(JSONType.json, response.getContentType());

        // Check if the character encoding is the one expected
        assertTrue("UTF-8".equals(response.getCharacterEncoding()));

        // Content
        String result = response.getContentAsString();

        assertNotNull(result);

        JSONObject rootObject = JSONObject.fromObject(result);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertEquals(aFeature.getString("geometry_name"), "the_geom");
    }

    @Test
    public void testPropertySelection() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request =
                "wms?service=wms&version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.json
                        + "&propertyName=NAME";

        // JSON
        MockHttpServletResponse response = getAsServletResponse(request, "");

        // MimeType
        assertEquals(JSONType.json, response.getContentType());

        // Check if the character encoding is the one expected
        assertTrue("UTF-8".equals(response.getCharacterEncoding()));

        // Content
        String result = response.getContentAsString();

        assertNotNull(result);

        JSONObject rootObject = JSONObject.fromObject(result);
        // print(rootObject);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertTrue(aFeature.getJSONObject("geometry").isNullObject());
        JSONObject properties = aFeature.getJSONObject("properties");
        assertTrue(properties.getJSONObject("FID").isNullObject());
        assertEquals("Green Forest", properties.get("NAME"));
    }

    @Test
    public void testReprojectedLayer() throws Exception {
        String layer = getLayerId(MockData.MPOLYGONS);

        String request =
                "wms?version=1.1.1&bbox=500525,500025,500575,500050&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.json;

        // JSON
        JSONObject json = (JSONObject) getAsJSON(request);
        JSONObject feature = (JSONObject) json.getJSONArray("features").get(0);
        JSONObject geom = feature.getJSONObject("geometry");
        // unroll the geometry and get the first coordinate
        JSONArray coords =
                geom.getJSONArray("coordinates").getJSONArray(0).getJSONArray(0).getJSONArray(0);
        assertTrue(
                new NumberRange<Double>(Double.class, 500525d, 500575d)
                        .contains((Number) coords.getDouble(0)));
        assertTrue(
                new NumberRange<Double>(Double.class, 500025d, 500050d)
                        .contains((Number) coords.getDouble(1)));
    }

    /** Tests CQL filter */
    @Test
    public void testCQLFilter() throws Exception {
        String layer = getLayerId(MockData.FORESTS);

        String request =
                "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                        + "&request=GetFeatureInfo&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20&x=10&y=10"
                        + "&info_format="
                        + JSONType.json;

        JSONObject json = (JSONObject) getAsJSON(request);
        JSONArray features = json.getJSONArray("features");
        assertTrue(features.size() > 0);

        // Add CQL filter
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(layer);
        try {
            info.setCqlFilter("NAME LIKE 'Red%'");
            getCatalog().save(info);
            json = (JSONObject) getAsJSON(request);
            features = json.getJSONArray("features");
            assertEquals(0, features.size());
        } finally {
            info = getCatalog().getFeatureTypeByName(layer);
            info.setCqlFilter(null);
            getCatalog().save(info);
        }
    }
}

/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import junit.framework.Test;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.data.test.MockData;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.wms_1_1_1.GetFeatureInfoTest;

public class GetFeatureInfoJSONTest extends GetFeatureInfoTest {

    /**
     * Tests JSONP outside of expected polygon
     * 
     * @throws Exception
     */
    public void testSimpleJSONP() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                + "&info_format=text/javascript&request=GetFeatureInfo&layers=" + layer
                + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);

        assertTrue(result.startsWith(JSONType.CALLBACK_FUNCTION));
        assertTrue(result.endsWith(")\n"));
        assertTrue(result.indexOf("Green Forest") > 0);

        result = result.substring(0, result.length() - 2);
        result = result.substring(JSONType.CALLBACK_FUNCTION.length() + 1,
                result.length());

        JSONObject rootObject = JSONObject.fromObject(result);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertEquals(aFeature.getString("geometry_name"), "the_geom");
    }
    
    /**
     * Tests jsonp with custom callback function
     * 
     * @throws Exception
     */
    public void testCustomJSONP() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                + "&info_format=text/javascript&request=GetFeatureInfo&layers=" + layer
                + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10&format_options=callback:custom";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);

        assertTrue(result.startsWith("custom("));
        assertTrue(result.endsWith(")\n"));
        assertTrue(result.indexOf("Green Forest") > 0);

        result = result.substring(0, result.length() - 2);
        result = result.substring("custom".length() + 1,
                result.length());

        JSONObject rootObject = JSONObject.fromObject(result);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertEquals(aFeature.getString("geometry_name"), "the_geom");
    }

    /**
     * Tests JSON outside of expected polygon
     * 
     * @throws Exception
     */
    public void testSimpleJSON() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg"
                + "&info_format=application/json&request=GetFeatureInfo&layers=" + layer
                + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        String result = getAsString(request);
        // System.out.println(result);
        assertNotNull(result);

        JSONObject rootObject = JSONObject.fromObject(result);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertEquals(aFeature.getString("geometry_name"), "the_geom");

    }

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetFeatureInfoJSONTest());
    }

}

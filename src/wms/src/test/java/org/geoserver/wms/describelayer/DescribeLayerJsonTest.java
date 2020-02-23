/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.describelayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.data.test.MockData;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit test suite for {@link JSONDescribeLayerResponse}
 *
 * @author Carlo Cancellieri - GeoSolutions
 * @version $Id$
 */
public class DescribeLayerJsonTest extends WMSTestSupport {

    @Test
    public void testBuild() throws Exception {
        try {
            new JSONDescribeLayerResponse(getWMS(), "fail");
            fail("Should fails");
        } catch (Exception e) {
        }
    }

    /** Tests jsonp with custom callback function */
    @Test
    public void testCustomJSONP() throws Exception {

        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request =
                "wms?version=1.1.1"
                        + "&request=DescribeLayer"
                        + "&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20"
                        + "&outputFormat="
                        + JSONType.jsonp
                        + "&format_options="
                        + JSONType.CALLBACK_FUNCTION_KEY
                        + ":DescribeLayer";

        JSONType.setJsonpEnabled(true);
        String result = getAsString(request);
        JSONType.setJsonpEnabled(false);

        checkJSONPDescribeLayer(result, layer);
    }

    /** Tests JSON */
    @Test
    public void testSimpleJSON() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request =
                "wms?version=1.1.1"
                        + "&request=DescribeLayer"
                        + "&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20"
                        + "&outputFormat="
                        + JSONType.json;

        String result = getAsString(request);
        // System.out.println(result);

        checkJSONDescribeLayer(result, layer);
    }

    /**
     * @param body Accepts:<br>
     *     DescribeLayer(...)<br>
     */
    private void checkJSONPDescribeLayer(String body, String layer) {
        assertNotNull(body);

        assertTrue(body.startsWith("DescribeLayer("));
        assertTrue(body.endsWith(")\n"));
        body = body.substring(0, body.length() - 2);
        body = body.substring("DescribeLayer(".length(), body.length());

        checkJSONDescribeLayer(body, layer);
    }

    /** Tests jsonp with custom callback function */
    @Test
    public void testJSONLayerGroup() throws Exception {

        String layer = NATURE_GROUP;
        String request =
                "wms?version=1.1.1"
                        + "&request=DescribeLayer"
                        + "&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20"
                        + "&outputFormat="
                        + JSONType.json;

        String result = getAsString(request);

        checkJSONDescribeLayerGroup(result, layer);
    }

    private void checkJSONDescribeLayer(String body, String layer) {
        assertNotNull(body);

        JSONObject rootObject = JSONObject.fromObject(body);
        // JSONObject subObject = rootObject.getJSONObject("WMS_DescribeLayerResponse");
        JSONArray layerDescs = rootObject.getJSONArray("layerDescriptions");

        JSONObject layerDesc = layerDescs.getJSONObject(0);

        assertEquals(layerDesc.get("layerName"), layer);
        // assertEquals(layerDesc.get("owsUrl"), "WFS");
        assertEquals(layerDesc.get("owsType"), "WFS");
    }

    private void checkJSONDescribeLayerGroup(String body, String layer) {
        assertNotNull(body);

        JSONObject rootObject = JSONObject.fromObject(body);

        JSONArray layerDescs = rootObject.getJSONArray("layerDescriptions");
        JSONObject layerDesc = layerDescs.getJSONObject(0);
        assertEquals(
                layerDesc.get("layerName"),
                MockData.LAKES.getPrefix() + ":" + MockData.LAKES.getLocalPart());
        assertTrue(layerDesc.get("owsURL").toString().endsWith("geoserver/wfs?"));
        assertEquals(layerDesc.get("owsType"), "WFS");

        layerDesc = layerDescs.getJSONObject(1);
        assertEquals(
                layerDesc.get("layerName"),
                MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart());
        assertTrue(layerDesc.get("owsURL").toString().endsWith("geoserver/wfs?"));
        assertEquals(layerDesc.get("owsType"), "WFS");
    }

    @Test
    public void testJSONDescribeLayerCharset() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request =
                "wms?version=1.1.1"
                        + "&request=DescribeLayer"
                        + "&layers="
                        + layer
                        + "&query_layers="
                        + layer
                        + "&width=20&height=20"
                        + "&outputFormat="
                        + JSONType.json;

        MockHttpServletResponse result = getAsServletResponse(request, "");
        assertTrue("UTF-8".equals(result.getCharacterEncoding()));
    }
}

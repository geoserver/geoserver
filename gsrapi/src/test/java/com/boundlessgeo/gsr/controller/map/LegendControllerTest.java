/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.controller.map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.boundlessgeo.gsr.controller.ControllerTest;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class LegendControllerTest extends ControllerTest {

    @Test
    public void testStreamsLegend() throws Exception {
        JSON result = getAsJSON(getBaseURL() + "cite/MapServer/legend?f=json");
        assertTrue(result instanceof JSONObject);

        JSONObject jObject = (JSONObject) result;

        JSONArray legends = (JSONArray) jObject.get("legends");
        JSONObject firstLegend = (JSONObject) legends.get(0);
        assertNotNull(firstLegend.get("layerId"));

        JSONArray legend = (JSONArray) firstLegend.get("legend");
        JSONObject l = (JSONObject) legend.get(0);
        Object contentType = l.get("contentType");
        assertNotNull(contentType);

        System.out.println(result.toString());
    }
}

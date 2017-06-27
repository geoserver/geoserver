/* Copyright (c) 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.controller.feature;

import com.boundlessgeo.gsr.controller.ControllerTest;
import org.junit.Test;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import static org.junit.Assert.*;

/**
 * Test FeatureService {@link com.boundlessgeo.gsr.api.feature.FeatureLayerController} access.
 * @author Jody Garnett (Boundless)
 */
public class FeatureLayerControllerTest extends ControllerTest {

    private String query(String service, String layer, String params) {
        return getBaseURL() + service + "/FeatureServer/" + layer + params;
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
}

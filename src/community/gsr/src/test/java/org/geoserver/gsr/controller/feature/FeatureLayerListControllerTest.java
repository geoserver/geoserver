/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.controller.feature;

import static org.junit.Assert.assertEquals;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.gsr.controller.ControllerTest;
import org.junit.Test;

public class FeatureLayerListControllerTest extends ControllerTest {
    private String query(String service, String params) {
        return getBaseURL() + service + "/FeatureServer/layers";
    }

    @Test
    public void testBasicQuery() throws Exception {
        String q = query("cite", "?f=json");
        JSON result = getAsJSON(q);
        JSONObject json = (JSONObject) result;
        JSONArray layers = ((JSONObject) result).getJSONArray("layers");
        checkResult(layers, "Feature Layer");
        JSONArray tables = json.getJSONArray("tables");
        checkResult(tables, "Table");
    }

    private void checkResult(JSONArray array, String type) {
        for (int i = 0; i < array.size(); i++) {
            Object typeAttr = array.getJSONObject(i).get("type");
            assertEquals(typeAttr, type);
        }
    }
}

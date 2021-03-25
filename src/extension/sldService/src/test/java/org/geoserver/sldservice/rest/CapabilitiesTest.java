/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.rest.RestBaseController;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class CapabilitiesTest extends SLDServiceBaseTest {

    @Test
    public void testClassifyForFeatureDefault() throws Exception {
        final String restPath =
                RestBaseController.ROOT_PATH + "/sldservice/" + getServiceUrl() + ".json";
        MockHttpServletResponse response = getAsServletResponse(restPath);
        assertEquals(200, response.getStatus());
        JSONObject json = (JSONObject) getAsJSON(restPath, 200);
        JSONObject vector = json.getJSONObject("capabilities").getJSONObject("vector");
        JSONObject raster = json.getJSONObject("capabilities").getJSONObject("raster");
        JSONArray vClassifications = vector.getJSONArray("classifications");
        JSONArray rClassifications = raster.getJSONArray("classifications");
        SldServiceCapabilities capabilities = new SldServiceCapabilities();
        for (int i = 0; i < vClassifications.size(); i++) {
            assertTrue(
                    capabilities
                            .getVectorClassifications()
                            .contains(vClassifications.getString(i)));
        }

        for (int i = 0; i < rClassifications.size(); i++) {
            assertTrue(
                    capabilities
                            .getRasterClassifications()
                            .contains(vClassifications.getString(i)));
        }
    }

    @Override
    protected String getServiceUrl() {
        return "capabilities";
    }
}

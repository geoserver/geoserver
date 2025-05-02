/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ExecutionGetTest extends OGCApiTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @Test
    public void testGetExecutionCoverageValues() throws Exception {
        // call with defaults: sync execution, inline response, json format
        JSONObject json = (JSONObject) getAsJSON("ogc/processes/v1/processes/gs:GetCoveragesValue/execution?name="
                + getLayerId(SystemTestData.TASMANIA_DEM) + "&x=145.220&y=-41.504");
        JSONArray values = json.getJSONArray("values");
        assertEquals(1, values.size());
        assertEquals(298, values.get(0));
    }

    @Test
    public void testGetExecutionInvalidInput() throws Exception {
        // call with defaults: sync execution, inline response, json format
        JSONObject json = (JSONObject) getAsJSON(
                "ogc/processes/v1/processes/gs:GetCoveragesValue/execution?name=notAGrid&x=145.220&y=-41.504");
        print(json);
        assertEquals("NoApplicableCode", json.getString("type"));
        assertThat(json.getString("title"), containsString("Could not find coverage notAGrid"));
    }

    @Test
    public void testInputOutputMimeTypes() throws Exception {
        // call with custom format for input and output
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/processes/v1/processes/JTS:buffer/execution?geom[type]=application/wkt&geom=POINT(0 0)&distance=10&capStyle=Square&response[f]=application/wkt");
        assertEquals(200, response.getStatus());
        assertEquals("application/wkt", response.getContentType());
        assertEquals("POLYGON ((10 10, 10 -10, -10 -10, -10 10, 10 10))", response.getContentAsString());
    }
}

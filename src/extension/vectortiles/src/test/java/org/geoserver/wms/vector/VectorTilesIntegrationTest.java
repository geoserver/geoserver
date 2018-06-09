/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class VectorTilesIntegrationTest extends WMSTestSupport {

    protected DocumentContext getAsJSONPath(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        if (!isQuietTests()) {
            System.out.println(response.getContentAsString());
        }

        assertEquals(expectedHttpCode, response.getStatus());
        assertThat(response.getContentType(), startsWith("application/json"));
        return JsonPath.parse(response.getContentAsString());
    }

    @Test
    public void testSimple() throws Exception {
        String request =
                "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                        + getLayerId(MockData.ROAD_SEGMENTS)
                        + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                        + "&format=application%2Fjson%3Btype%3Dgeojson";
        DocumentContext json = getAsJSONPath(request, 200);
        // all features returned, with a geometry and a name attribute
        assertEquals(5, ((JSONArray) json.read("$.features")).size());
        assertEquals(5, ((JSONArray) json.read("$.features[*].geometry")).size());
        assertEquals(
                3, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Route 5')]")).size());
        assertEquals(
                1,
                ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Main Street')]"))
                        .size());
        assertEquals(
                1,
                ((JSONArray)
                                json.read(
                                        "$.features[?(@.properties.NAME == 'Dirt Road by Green Forest')]"))
                        .size());
    }

    @Test
    public void testCqlFilter() throws Exception {
        String request =
                "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                        + getLayerId(MockData.ROAD_SEGMENTS)
                        + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                        + "&CQL_FILTER=NAME='Main Street'&format=application%2Fjson%3Btype%3Dgeojson";
        DocumentContext json = getAsJSONPath(request, 200);
        // all features returned, with a geometry and a name attribute
        assertEquals(1, ((JSONArray) json.read("$.features")).size());
        assertEquals(1, ((JSONArray) json.read("$.features[*].geometry")).size());
        assertEquals(
                0, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Route 5')]")).size());
        assertEquals(
                1,
                ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Main Street')]"))
                        .size());
        assertEquals(
                0,
                ((JSONArray)
                                json.read(
                                        "$.features[?(@.properties.NAME == 'Dirt Road by Green Forest')]"))
                        .size());
    }

    @Test
    public void testFilterById() throws Exception {
        String request =
                "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                        + getLayerId(MockData.ROAD_SEGMENTS)
                        + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                        + "&featureId=RoadSegments.1107532045091&format=application%2Fjson%3Btype%3Dgeojson";
        DocumentContext json = getAsJSONPath(request, 200);
        // all features returned, with a geometry and a name attribute
        assertEquals(1, ((JSONArray) json.read("$.features")).size());
        assertEquals(1, ((JSONArray) json.read("$.features[*].geometry")).size());
        assertEquals(
                0, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Route 5')]")).size());
        assertEquals(
                0,
                ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Main Street')]"))
                        .size());
        assertEquals(
                1,
                ((JSONArray)
                                json.read(
                                        "$.features[?(@.properties.NAME == 'Dirt Road by Green Forest')]"))
                        .size());
    }
}

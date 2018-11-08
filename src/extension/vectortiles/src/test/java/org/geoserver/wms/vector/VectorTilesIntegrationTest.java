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
import java.util.List;
import net.minidev.json.JSONArray;
import no.ecc.vectortile.VectorTileDecoder;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;
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
    public void testSimpleMVT() throws Exception {
        checkSimpleMVT(MapBoxTileBuilderFactory.MIME_TYPE);
    }

    @Test
    public void testSimpleMVTLegacyMime() throws Exception {
        checkSimpleMVT(MapBoxTileBuilderFactory.LEGACY_MIME_TYPE);
    }

    public void checkSimpleMVT(String mimeType) throws Exception {
        String request =
                "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                        + getLayerId(MockData.ROAD_SEGMENTS)
                        + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                        + "&format="
                        + mimeType;
        MockHttpServletResponse response = getAsServletResponse(request);
        // the standard mime type is returned
        assertEquals(MapBoxTileBuilderFactory.MIME_TYPE, response.getContentType());
        byte[] responseBytes = response.getContentAsByteArray();
        VectorTileDecoder decoder = new VectorTileDecoder();
        List<VectorTileDecoder.Feature> featuresList = decoder.decode(responseBytes).asList();
        assertEquals(5, featuresList.size());
        assertEquals(
                3,
                featuresList
                        .stream()
                        .filter(f -> "Route 5".equals(f.getAttributes().get("NAME")))
                        .count());
        assertEquals(
                1,
                featuresList
                        .stream()
                        .filter(f -> "Main Street".equals(f.getAttributes().get("NAME")))
                        .count());
        assertEquals("Extent should be 12288", 12288, featuresList.get(0).getExtent());
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

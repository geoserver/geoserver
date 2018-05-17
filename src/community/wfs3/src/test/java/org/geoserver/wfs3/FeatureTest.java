/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.MockData;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FeatureTest extends WFS3TestSupport {

    @Test
    public void testGetLayerAsGeoJson() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("wfs3/collections/" + roadSegments + "/items", 200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(5, (int) json.read("features.length()", Integer.class));
        // check self link
        List selfRels = json.read("links[?(@.type == 'application/geo+json')].rel");
        assertEquals(1, selfRels.size());
        assertEquals("self", selfRels.get(0));
        // check alternate link
        List alternatefRels = json.read("links[?(@.type == 'application/json')].rel");
        assertEquals(1, alternatefRels.size());
        assertEquals("alternate", alternatefRels.get(0));
    }

    @Test
    public void testBBoxFilter() throws Exception {
        String roadSegments = getEncodedName(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath("wfs3/collections/" + roadSegments + "/items?bbox=35,0,60,3", 200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f002 and f003
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class).size());
    }

    @Test
    public void testTimeFilter() throws Exception {
        String roadSegments = getEncodedName(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath("wfs3/collections/" + roadSegments + "/items?time=2006-10-25", 200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
    }

    @Test
    public void testTimeRangeFilter() throws Exception {
        String roadSegments = getEncodedName(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "wfs3/collections/" + roadSegments + "/items?time=2006-09-01/2006-10-23",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class).size());
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f003')]", List.class).size());
    }
    
    @Test
    public void testTimeDurationFilter() throws Exception {
        String roadSegments = getEncodedName(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "wfs3/collections/" + roadSegments + "/items?time=2006-09-01/P1M23DT12H31M12S",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class).size());
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f003')]", List.class).size());
    }

    @Test
    public void testCombinedSpaceTimeFilter() throws Exception {
        String roadSegments = getEncodedName(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "wfs3/collections/"
                                + roadSegments
                                + "/items?time=2006-09-01/2006-10-23&bbox=35,0,60,3",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class).size());
    }

    @Test
    public void testSingleFeatureAsGeoJson() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath(
                        "wfs3/collections/" + roadSegments + "/items/RoadSegments.1107532045088",
                        200);
        assertEquals("Feature", json.read("type", String.class));
        // check self link
        String geoJsonLinkPath = "links[?(@.type == 'application/geo+json')]";
        List selfRels = json.read(geoJsonLinkPath + ".rel");
        assertEquals(1, selfRels.size());
        assertEquals("self", selfRels.get(0));
        String href = (String) ((List) json.read(geoJsonLinkPath + "href")).get(0);
        String expected =
                "http://localhost:8080/geoserver/wfs3/collections/cite__RoadSegments"
                        + "/items/RoadSegments.1107532045088?f=application%2Fgeo%2Bjson";
        assertEquals(expected, href);
        // check alternate link
        List alternatefRels = json.read("links[?(@.type == 'application/json')].rel");
        assertEquals(1, alternatefRels.size());
        assertEquals("alternate", alternatefRels.get(0));
    }

    @Test
    public void testLimit() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath("wfs3/collections/" + roadSegments + "/items?limit=3", 200);
        assertEquals(3, (int) json.read("features.length()", Integer.class));
    }

    @Test
    public void testErrorHandling() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath("wfs3/collections/" + roadSegments + "/items?limit=abc", 400);
        assertEquals("InvalidParameterValue", json.read("code"));
        assertThat(
                json.read("description"), both(containsString("COUNT")).and(containsString("abc")));
    }
}

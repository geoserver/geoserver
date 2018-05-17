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
        // check altenate link
        List alternatefRels = json.read("links[?(@.type == 'application/json')].rel");
        assertEquals(1, alternatefRels.size());
        assertEquals("alternate", alternatefRels.get(0));
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
        String expected = "http://localhost:8080/geoserver/wfs3/collections/cite__RoadSegments" +
                "/items/RoadSegments.1107532045088?f=application%2Fgeo%2Bjson";
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

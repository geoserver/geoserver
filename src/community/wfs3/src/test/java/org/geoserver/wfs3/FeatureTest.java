/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import com.jayway.jsonpath.DocumentContext;
import net.sf.json.JSON;
import org.geoserver.data.test.MockData;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FeatureTest extends WFS3TestSupport {

    @Test
    public void testGetLayerAsGeoJson() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("wfs3/collections/" + roadSegments + "/items",
                200);
        assertEquals(5, (int) json.read("features.length()", Integer.class));
    }


    @Test
    public void testLimit() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("wfs3/collections/" + roadSegments +
                "/items?limit=3", 200);
        assertEquals(3, (int) json.read("features.length()", Integer.class));
    }

//    @Test
//    public void testGetSingleFeature() throws Exception {
//        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
//        JSON json = getAsJSON("wfs3/collections/"  + roadSegments + "/RoadSegments.1107532045088");
//        print(json);
//    }

    @Test
    public void testErrorHandling() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("wfs3/collections/" + roadSegments + 
                "/items?limit=abc", 400);
        assertEquals("InvalidParameterValue", json.read("code"));
        assertThat(json.read("description"), both(containsString("COUNT")).and(containsString("abc")));
    }

}

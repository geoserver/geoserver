/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import net.sf.json.JSON;
import org.geoserver.data.test.MockData;
import org.junit.Test;

public class FeatureTest extends WFS3TestSupport {

    @Test
    public void testGetLayerAsGeoJson() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        JSON json = getAsJSON("wfs3/"  + roadSegments);
        print(json);
    }

    @Test
    public void testGetSingleFeature() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        JSON json = getAsJSON("wfs3/"  + roadSegments + "/RoadSegments.1107532045088");
        print(json);
    }

    @Test
    public void testErrorHandling() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        JSON json = getAsJSON("wfs3/"  + roadSegments + "?count=abc");
        print(json);
    }

}

/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertEquals;

import org.geoserver.ogcapi.OGCAPIMediaTypes;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class RFCGeoJSONFeaturesResponseTest extends FeaturesTestSupport {
    @Test
    public void testRFCGeoJSONFeaturesResponseFromWFSQuery() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "wfs?SERVICE=WFS&request=getFeature&typeName=cite:Buildings&version=2.0.0&outputFormat=application/geo%2Bjson");
        assertEquals(200, response.getStatus());
        assertThat(response.getContentType(), startsWith(OGCAPIMediaTypes.GEOJSON_VALUE));
    }
}

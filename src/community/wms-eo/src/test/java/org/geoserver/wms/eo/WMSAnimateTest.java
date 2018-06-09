/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo;

import static org.junit.Assert.assertEquals;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** @author Daniele Romagnoli - GeoSolutions SAS */
public class WMSAnimateTest extends WMSTestSupport {

    @Test
    public void testAnimate() throws Exception {
        String layer = getLayerId(MockData.FORESTS);

        String request =
                "wms/animate?&aparam=BBOX&avalues=0.002\\,-0.002\\,0.001\\,0.001,-0.001\\,-0.001\\,0.002\\,0.002"
                        + "&layers="
                        + layer
                        + "&format=image/gif;subtype=animated&format_options=layout:message";

        MockHttpServletResponse response = getAsServletResponse(request);

        // MimeType and image check
        assertEquals("image/gif", response.getContentType());
        checkImage(response, "image/gif", 512, 256);
    }
}

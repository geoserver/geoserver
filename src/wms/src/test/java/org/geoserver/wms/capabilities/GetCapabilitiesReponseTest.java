/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2007 OpenPlans
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import org.geoserver.wms.WMSTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit test suite for {@link GetCapabilitiesResponse}
 *
 * @author Simone Giannecchini - GeoSolutions
 * @version $Id$
 */
public class GetCapabilitiesReponseTest extends WMSTestSupport {

    /** Tests ContentDisposition */
    @Test
    public void testSimple() throws Exception {
        String request = "wms?version=1.1.1&request=GetCapabilities&service=WMS";
        MockHttpServletResponse result = getAsServletResponse(request);
        Assert.assertTrue(result.containsHeader("content-disposition"));
        Assert.assertEquals(
                "inline; filename=getcapabilities_1.1.1.xml",
                result.getHeader("content-disposition"));

        request = "wms?version=1.3.0&request=GetCapabilities&service=WMS";
        result = getAsServletResponse(request);
        Assert.assertTrue(result.containsHeader("content-disposition"));
        Assert.assertEquals(
                "inline; filename=getcapabilities_1.3.0.xml",
                result.getHeader("content-disposition"));
    }
}

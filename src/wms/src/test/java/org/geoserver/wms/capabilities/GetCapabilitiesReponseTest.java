/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import junit.framework.Test;

import org.geoserver.wms.WMSTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Unit test suite for {@link GetCapabilitiesResponse}
 * 
 * @author Simone Giannecchini - GeoSolutions
 * @version $Id$
 */
public class GetCapabilitiesReponseTest extends WMSTestSupport {
	
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetCapabilitiesReponseTest());
    }
    
    /**
     * Tests ContentDisposition
     * 
     * @throws Exception
     */
    public void testSimple() throws Exception {
        String request = "wms?version=1.1.1&request=GetCapabilities&service=WMS";
        MockHttpServletResponse result = getAsServletResponse(request);
        assertTrue(result.containsHeader("content-disposition"));
        assertEquals("inline; filename=getcapabilities_1.1.1.xml", result.getHeader("content-disposition"));
        
        request = "wms?version=1.3.0&request=GetCapabilities&service=WMS";
        result = getAsServletResponse(request);
        assertTrue(result.containsHeader("content-disposition"));
        assertEquals("inline; filename=getcapabilities_1.3.0.xml", result.getHeader("content-disposition"));

    }
}

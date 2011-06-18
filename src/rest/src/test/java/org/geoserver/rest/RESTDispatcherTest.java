/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.rest;

import org.geoserver.test.GeoServerTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class RESTDispatcherTest extends GeoServerTestSupport {

    public void testException() throws Exception {
        MockHttpServletResponse r = getAsServletResponse("/rest/exception?code=400&message=error");
        assertEquals( 400, r.getStatusCode() );
        assertEquals( "error", r.getOutputStreamContent());
    }
}

/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.junit.Assert.*;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

public class EnviromentInjectionCallbackTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data needed
    }

    @Before
    public void logout() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Test
    public void testNoUser() throws Exception {
        MockHttpServletResponse r = getAsServletResponse(RestBaseController.ROOT_PATH + "/gsuser");
        assertEquals(200, r.getStatus());
        assertTrue(r.getContentType().startsWith("text/plain"));
        assertEquals("USER_NOT_FOUND", r.getContentAsString());
    }

    @Test
    public void testUser() throws Exception {
        login("testUser", "testPassword");
        MockHttpServletResponse r = getAsServletResponse(RestBaseController.ROOT_PATH + "/gsuser");
        assertEquals(200, r.getStatus());
        String contentType = r.getContentType();
        assertTrue(contentType.startsWith("text/plain"));
        assertEquals("testUser", r.getContentAsString());
    }
}

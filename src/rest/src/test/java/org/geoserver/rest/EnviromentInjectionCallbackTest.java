/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
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
import org.springframework.security.core.context.SecurityContextHolder;

import com.mockrunner.mock.web.MockHttpServletResponse;

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
        MockHttpServletResponse r = getAsServletResponse("/rest/gsuser");
        assertEquals(200, r.getStatusCode());
        assertTrue(r.getContentType().startsWith("text/plain"));
        assertEquals("USER_NOT_FOUND", r.getOutputStreamContent());
    }
    
    @Test
    public void testUser() throws Exception {
        login("testUser", "testPassword");
        MockHttpServletResponse r = getAsServletResponse("/rest/gsuser");
        assertEquals(200, r.getStatusCode());
        assertTrue(r.getContentType().startsWith("text/plain"));
        assertEquals("testUser", r.getOutputStreamContent());
    }
}

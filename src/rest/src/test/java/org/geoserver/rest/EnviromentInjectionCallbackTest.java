/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import java.util.Collections;
import java.util.List;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerTestSupport;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class EnviromentInjectionCallbackTest extends GeoServerTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(null);
    }
    
    /**
     * Enable the Spring Security auth filters
     */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList((javax.servlet.Filter) GeoServerExtensions
                .bean("filterChainProxy"));
    }

    public void testUser() throws Exception {
        authenticate("admin", "geoserver");
        MockHttpServletResponse r = getAsServletResponse("/rest/gsuser");
        assertEquals(200, r.getStatusCode());
        assertTrue(r.getContentType().startsWith("text/plain"));
        assertEquals("admin", r.getOutputStreamContent());
    }
}

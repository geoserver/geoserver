/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** @author tw */
public class AdvancedDispatchFilterTest {

    public AdvancedDispatchFilterTest() {}

    /** Test of destroy method, of class AdvancedDispatchFilter. */
    @Test
    public void testPathIsNullNPE() {
        MockHttpServletRequest request = new MyMockRequest("GET", "/test?name=0");
        request.setServerName("localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        AdvancedDispatchFilter instance = new AdvancedDispatchFilter();
        try {
            instance.doFilter(request, response, filterChain);
        } catch (Exception ex) {
            fail("should work");
        }
    }

    /** Need to handle a null result from getPathInfo. */
    @Test
    public void testHandlePathInfoNull() {
        // Ensure null request succeeds
        MockHttpServletRequest mockRequest = new MyMockRequest("GET", null);
        mockRequest.setPathInfo(null);
        assertNull(mockRequest.getPathInfo());

        AdvancedDispatchFilter.AdvancedDispatchHttpRequest advRequest =
                new AdvancedDispatchFilter.AdvancedDispatchHttpRequest(mockRequest);
        assertNull(advRequest.getPathInfo());

        // Ensure non-null servlet path is handled
        advRequest.servletPath = "/bar";
        mockRequest.setPathInfo("/bar/foo");
        assertEquals("/bar/foo", mockRequest.getPathInfo());
        assertEquals("/foo", advRequest.getPathInfo());

        // Ensure non-null request succeeds
        mockRequest.setPathInfo("/foo");
        assertEquals("/foo", mockRequest.getPathInfo());
        assertEquals("/foo", advRequest.getPathInfo());
    }

    /** Necessary due to special filtering out delegates with name MockHttpServletRequest. */
    class MyMockRequest extends MockHttpServletRequest {

        public MyMockRequest(String method, String requestURI) {
            super(method, requestURI);
        }
    }
}

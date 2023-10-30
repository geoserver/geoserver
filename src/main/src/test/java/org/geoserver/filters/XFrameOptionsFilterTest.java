/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import javax.servlet.ServletException;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Simple test to make sure the XFrameOptions filter works and is configurable. */
public class XFrameOptionsFilterTest {

    @Test
    public void doFilter() throws Exception {
        String header = getHeader("X-Frame-Options");
        assertEquals("Expect default XFrameOption to be DENY", "SAMEORIGIN", header);
    }

    @Test
    public void testFilterWithNoSetPolicy() throws IOException, ServletException {
        String currentShouldSetProperty =
                System.getProperty(XFrameOptionsFilter.GEOSERVER_XFRAME_SHOULD_SET_POLICY);
        System.setProperty(XFrameOptionsFilter.GEOSERVER_XFRAME_SHOULD_SET_POLICY, "false");
        String header = getHeader("X-Frame-Options");

        assertNull("Expect default XFrameOption to be null", header);

        if (currentShouldSetProperty != null) {
            System.setProperty(
                    XFrameOptionsFilter.GEOSERVER_XFRAME_SHOULD_SET_POLICY,
                    currentShouldSetProperty);
        }
    }

    @Test
    public void testFilterWithSameOrigin() throws IOException, ServletException {
        String currentShouldSetProperty =
                System.getProperty(XFrameOptionsFilter.GEOSERVER_XFRAME_POLICY);
        System.setProperty(XFrameOptionsFilter.GEOSERVER_XFRAME_POLICY, "DENY");
        String header = getHeader("X-Frame-Options");

        assertEquals("Expect default XFrameOption to be DENY", "DENY", header);

        if (currentShouldSetProperty != null) {
            System.setProperty(
                    XFrameOptionsFilter.GEOSERVER_XFRAME_POLICY, currentShouldSetProperty);
        }
    }

    @Test
    public void testFilterWithoutContentTypeOptions() throws IOException, ServletException {
        String currentShouldSetProperty =
                System.getProperty(XFrameOptionsFilter.GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY);
        System.setProperty(XFrameOptionsFilter.GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY, "false");
        String header = getHeader("X-Content-Type-Options");

        assertNull("Expect X-Content-Type-Options to be null", header);

        if (currentShouldSetProperty != null) {
            System.setProperty(
                    XFrameOptionsFilter.GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY,
                    currentShouldSetProperty);
        }
    }

    @Test
    public void testFilterWithContentTypeOptions() throws IOException, ServletException {
        String currentShouldSetProperty =
                System.getProperty(XFrameOptionsFilter.GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY);
        System.setProperty(XFrameOptionsFilter.GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY, "true");
        String header = getHeader("X-Content-Type-Options");

        assertEquals("Expect X-Content-Type-Options to be nosniff", "nosniff", header);

        if (currentShouldSetProperty != null) {
            System.setProperty(
                    XFrameOptionsFilter.GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY,
                    currentShouldSetProperty);
        }
    }

    private String getHeader(String name) throws IOException, ServletException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "http://www.geoserver.org");
        MockHttpServletResponse response = new MockHttpServletResponse();
        XFrameOptionsFilter filter = new XFrameOptionsFilter();
        MockFilterChain mockChain = new MockFilterChain();

        filter.doFilter(request, response, mockChain);

        return response.getHeader(name);
    }
}

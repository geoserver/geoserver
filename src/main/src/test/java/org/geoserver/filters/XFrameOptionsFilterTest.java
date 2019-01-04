/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import javax.servlet.ServletException;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

/** Simple test to make sure the XFrameOptions filter works and is configurable. */
public class XFrameOptionsFilterTest {

    @Test
    public void doFilter() throws Exception {
        String header = getXStreamHeader();
        assertEquals("Expect default XFrameOption to be DENY", "SAMEORIGIN", header);
    }

    @Test
    public void testFilterWithNoSetPolicy() throws IOException, ServletException {
        String currentShouldSetProperty =
                System.getProperty(XFrameOptionsFilter.GEOSERVER_XFRAME_SHOULD_SET_POLICY);
        System.setProperty(XFrameOptionsFilter.GEOSERVER_XFRAME_SHOULD_SET_POLICY, "false");
        String header = getXStreamHeader();

        assertEquals("Expect default XFrameOption to be null", null, header);

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
        String header = getXStreamHeader();

        assertEquals("Expect default XFrameOption to be DENY", "DENY", header);

        if (currentShouldSetProperty != null) {
            System.setProperty(
                    XFrameOptionsFilter.GEOSERVER_XFRAME_POLICY, currentShouldSetProperty);
        }
    }

    private String getXStreamHeader() throws IOException, ServletException {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "http://www.geoserver.org");
        MockHttpServletResponse response = new MockHttpServletResponse();
        XFrameOptionsFilter filter = new XFrameOptionsFilter();
        MockServletContext context = new MockServletContext();
        MockFilterConfig config = new MockFilterConfig(context);
        MockFilterChain mockChain = new MockFilterChain();

        filter.doFilter(request, response, mockChain);

        return response.getHeader("X-Frame-Options");
    }
}

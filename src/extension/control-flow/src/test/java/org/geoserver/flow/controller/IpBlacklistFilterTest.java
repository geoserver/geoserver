/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class IpBlacklistFilterTest {

    @Test
    public void testFilterIp() throws IOException, ServletException {
        Properties props = new Properties();
        props.put("ip.blacklist", "192.168.1.8,192.168.1.10");
        IpBlacklistFilter filter = new IpBlacklistFilter(props);
        assertNotNull(filter);
        TestServlet testServlet = new TestServlet();
        MockFilterChain filterChain = new MockFilterChain(testServlet, filter);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.8");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filterChain.doFilter(request, response);
        assertFalse(testServlet.wasServiceCalled());
        testServlet.reset();
        filterChain.reset();
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.9");
        response = new MockHttpServletResponse();
        filterChain.doFilter(request, response);
        assertTrue(testServlet.wasServiceCalled());
        testServlet.reset();
        filterChain.reset();
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.10");
        response = new MockHttpServletResponse();
        filterChain.doFilter(request, response);
        assertFalse(testServlet.wasServiceCalled());
        testServlet.reset();
        filterChain.reset();
    }

    static class TestServlet extends HttpServlet {
        /** serialVersionUID */
        private static final long serialVersionUID = 1L;

        private boolean serviceCalled = false;

        public void service(ServletRequest request, ServletResponse response)
                throws ServletException, IOException {
            serviceCalled = true;
        }

        public void reset() {
            serviceCalled = false;
        }

        public boolean wasServiceCalled() {
            return serviceCalled;
        }
    }
}

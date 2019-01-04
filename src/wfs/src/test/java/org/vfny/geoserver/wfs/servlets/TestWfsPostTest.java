/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wfs.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.ows.util.ResponseUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class TestWfsPostTest {

    /** The proxy base url variable */
    static final String PROXY_BASE_URL = "PROXY_BASE_URL";

    @Test
    public void testEscapeXMLReservedChars() throws Exception {
        TestWfsPost servlet = new TestWfsPost();
        MockHttpServletRequest request = buildMockRequest();
        request.addHeader("Host", "localhost:8080");
        request.setQueryString(ResponseUtils.getQueryString("form_hf_0=&url=vjoce<>:garbage"));
        request.setParameter("url", "vjoce<>:garbage");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        // System.out.println(response.getContentAsString());
        // check xml chars have been escaped
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "java.net.MalformedURLException: no protocol: vjoce&lt;&gt;:garbage"));
    }

    @Test
    public void testDisallowOpenProxy() throws Exception {
        TestWfsPost servlet = new TestWfsPost();
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://www.google.com");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        // checking that reqauest is disallowed
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "Invalid url requested, the demo requests should be hitting: http://localhost:8080/geoserver"));
    }

    @Test
    public void testDisallowOpenProxyWithProxyBase() throws Exception {
        TestWfsPost servlet =
                new TestWfsPost() {
                    String getProxyBaseURL() {
                        return "http://geoserver.org/geoserver";
                    }
                };
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://localhost:1234/internalApp");
        request.setMethod("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        // checking that reqauest is disallowed
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "Invalid url requested, the demo requests should be hitting: http://geoserver.org/geoserver"));
    }

    @Test
    public void testValidateURL() throws Exception {
        TestWfsPost servlet = new TestWfsPost();
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://localhost:1234/internalApp");
        request.setMethod("GET");

        try {
            servlet.validateURL(
                    request, "http://localhost:1234/internalApp", "http://geoserver.org/geoserver");
            fail("Requests should be limited by proxyBaseURL");
        } catch (IllegalArgumentException expected) {
            assertTrue(
                    expected.getMessage()
                            .contains(
                                    "Invalid url requested, the demo requests should be hitting: http://geoserver.org/geoserver"));
        }
    }

    @Test
    public void testGetProxyBaseURL() {
        SettingsInfo settings = new SettingsInfoImpl();
        settings.setProxyBaseUrl("https://foo.com/geoserver");

        GeoServerInfo info = new GeoServerInfoImpl();
        info.setSettings(settings);

        GeoServer gs = new GeoServerImpl();
        gs.setGlobal(info);

        TestWfsPost servlet =
                new TestWfsPost() {
                    @Override
                    protected GeoServer getGeoServer() {
                        return gs;
                    }
                };
        assertEquals("https://foo.com/geoserver", servlet.getProxyBaseURL());
    }

    protected static MockHttpServletRequest buildMockRequest() {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("/geoserver/TestWfsPost");
        request.setRequestURI(
                ResponseUtils.stripQueryString(ResponseUtils.appendPath("/geoserver/TestWfsPost")));
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.wfs.servlets;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.ServletException;
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
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class TestWfsPostTest {

    /** The proxy base url variable */
    static final String PROXY_BASE_URL = "PROXY_BASE_URL";

    @Test
    public void testEscapeXMLReservedChars() throws Exception {
        TestWfsPost servlet = buildMockServlet();
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
    public void testDisallowNonHttpProtocols() throws Exception {
        TestWfsPost servlet = buildMockServlet();
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "file:///");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertThat(
                response.getContentAsString(),
                containsString("Invalid url requested; not an HTTP or HTTPS URL: file:///"));
    }

    @Test
    public void testDisallowPathTraversalUnescaped() throws Exception {
        TestWfsPost servlet = buildMockServlet();
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://localhost:8080/geoserver/..");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertThat(
                response.getContentAsString(),
                containsString(
                        "Invalid url requested; illegal sequence &apos;..&apos; in URL: http://localhost:8080/geoserver/.."));
    }

    @Test
    public void testDisallowPathTraversalEscaped() throws Exception {
        TestWfsPost servlet = buildMockServlet();
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://localhost:8080/geoserver/%2E%2E");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertThat(
                response.getContentAsString(),
                containsString(
                        "Invalid url requested; illegal sequence &apos;..&apos; in URL: http://localhost:8080/geoserver/%2E%2E"));
    }

    @Test
    public void testDisallowOpenProxyWrongProtocol() throws Exception {
        TestWfsPost servlet = buildMockServlet();
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "https://localhost:8080/geoserver/wfs");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertThat(
                response.getContentAsString(),
                containsString(
                        "Invalid url requested, the demo requests should be hitting: http://localhost:8080/geoserver"));
    }

    @Test
    public void testDisallowOpenProxyWrongHost() throws Exception {
        TestWfsPost servlet = buildMockServlet();
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://geoserver.org:8080/geoserver/wfs");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertThat(
                response.getContentAsString(),
                containsString(
                        "Invalid url requested, the demo requests should be hitting: http://localhost:8080/geoserver"));
    }

    @Test
    public void testDisallowOpenProxyWrongPort() throws Exception {
        TestWfsPost servlet = buildMockServlet();
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "https://localhost/geoserver/wfs");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertThat(
                response.getContentAsString(),
                containsString(
                        "Invalid url requested, the demo requests should be hitting: http://localhost:8080/geoserver"));
    }

    @Test
    public void testDisallowOpenProxyWrongPath() throws Exception {
        TestWfsPost servlet = buildMockServlet();
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "https://localhost:8080/foo");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertThat(
                response.getContentAsString(),
                containsString(
                        "Invalid url requested, the demo requests should be hitting: http://localhost:8080/geoserver"));
    }

    @Test
    public void testDisallowOpenProxyWithProxyBaseWrongProtocol() throws Exception {
        TestWfsPost servlet = buildMockServlet("https://geoserver.org/geoserver");
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://geoserver.org/geoserver/wfs");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertThat(
                response.getContentAsString(),
                containsString(
                        "Invalid url requested, the demo requests should be hitting: https://geoserver.org/geoserver"));
    }

    @Test
    public void testDisallowOpenProxyWithProxyBaseWrongHost() throws Exception {
        TestWfsPost servlet = buildMockServlet("https://geoserver.org/geoserver");
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "https://localhost/geoserver/wfs");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertThat(
                response.getContentAsString(),
                containsString(
                        "Invalid url requested, the demo requests should be hitting: https://geoserver.org/geoserver"));
    }

    @Test
    public void testDisallowOpenProxyWithProxyBaseWrongPort() throws Exception {
        TestWfsPost servlet = buildMockServlet("https://geoserver.org/geoserver");
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "https://geoserver.org:1234/geoserver/wfs");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertThat(
                response.getContentAsString(),
                containsString(
                        "Invalid url requested, the demo requests should be hitting: https://geoserver.org/geoserver"));
    }

    @Test
    public void testDisallowOpenProxyWithProxyBaseWrongPath() throws Exception {
        TestWfsPost servlet = buildMockServlet("https://geoserver.org/geoserver");
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "https://geoserver.org/foo");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        assertThat(
                response.getContentAsString(),
                containsString(
                        "Invalid url requested, the demo requests should be hitting: https://geoserver.org/geoserver"));
    }

    @Test
    public void testDisallowOpenProxyWithSupersetNameWithProxyBase() throws Exception {
        TestWfsPost servlet = buildMockServlet("http://geoserver.org");
        MockHttpServletRequest request = buildMockRequest();
        request.setParameter("url", "http://geoserver.org.other");
        request.setMethod("GET");

        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        // checking that request is disallowed
        assertTrue(
                response.getContentAsString()
                        .contains(
                                "Invalid url requested, the demo requests should be hitting: http://geoserver.org"));
    }

    @Test
    public void testValidateURL() throws Exception {
        TestWfsPost servlet = buildMockServlet();
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

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    protected static MockHttpServletRequest buildMockRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("/geoserver");
        request.setServletPath("/TestWfsPost");
        request.setRequestURI(
                ResponseUtils.stripQueryString(ResponseUtils.appendPath("/geoserver/TestWfsPost")));
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    protected static TestWfsPost buildMockServlet() throws ServletException {
        return buildMockServlet(null);
    }

    protected static TestWfsPost buildMockServlet(final String proxyBaseUrl)
            throws ServletException {
        TestWfsPost testWfsPost;
        if (proxyBaseUrl == null) {
            testWfsPost = new TestWfsPost();
        } else {
            testWfsPost =
                    new TestWfsPost() {
                        @Override
                        String getProxyBaseURL() {
                            return proxyBaseUrl;
                        }
                    };
        }
        MockServletContext servletContext = new MockServletContext();
        servletContext.setContextPath("/geoserver");
        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        testWfsPost.init(servletConfig);

        return testWfsPost;
    }
}

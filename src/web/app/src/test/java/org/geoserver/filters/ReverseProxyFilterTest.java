/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.filters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;

import junit.framework.TestCase;

import org.apache.wicket.spring.test.ApplicationContextMock;
import org.geoserver.config.GeoServer;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.platform.GeoServerExtensions;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockFilterConfig;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockHttpSession;
import com.mockrunner.mock.web.MockServletContext;

/**
 * @author Gabriel Roldan (TOPP)
 * @version $Id$
 * @since 2.5.x
 * @source $URL:
 *         https://svn.codehaus.org/geoserver/trunk/geoserver/web/src/test/java/org/geoserver/filters
 *         /ReverseProxyFilterTest.java $
 */
public class ReverseProxyFilterTest extends TestCase {

    private static final String DEFAULT_MIME_TYPES_REGEX = "text/html.*,text/css.*,text/javascript.*,application/x-javascript.*";

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    ReverseProxyFilter filter;

    protected void setUp() throws Exception {
        super.setUp();
        filter = new ReverseProxyFilter();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        filter = null;
    }

    protected GeoServer getMockGeoServer(final String proxyBaseUrl) {
        GeoServerImpl config = new GeoServerImpl();
        GeoServerInfoImpl geoserver = new GeoServerInfoImpl(config);
        geoserver.setProxyBaseUrl(proxyBaseUrl);
        config.setGlobal(geoserver);
        return config;
    }

    public void testInit() throws ServletException {
        final String proxyBaseUrl = "https://localhost/geoserver/tools";

        GeoServer geoserver = getMockGeoServer(proxyBaseUrl);
        String mimeTypesInitParam = "*wrong*expression*";

        try {
            ReverseProxyFilter.parsePatterns(geoserver, mimeTypesInitParam);
            fail("expected ServletException with an illegal regular expression to match mime types");
        } catch (ServletException e) {
            assertTrue(true);
        }

        mimeTypesInitParam = DEFAULT_MIME_TYPES_REGEX;

        ReverseProxyFilter.parsePatterns(geoserver, mimeTypesInitParam);
    }

    public void testDoFilterDisabled() throws ServletException, IOException {
        final String proxyBaseUrl = "https://proxy.server:9090/applications/geoserver";
        final String requestBaseUrl = "http://localhost:8080/geoserver";
        final String requestResource = "/www/resource.html";
        final String content = "<a href=\"http://localhost:8080/geoserver/linked.html\">link</a>"
                + LINE_SEPARATOR;
        final String contentType = "text/html";

        String result = testDoFilter(proxyBaseUrl, requestBaseUrl, requestResource, content,
                contentType, false);

        // no translation performed, filter is disabled
        assertEquals(content, result);
    }

    public void testDoFilterExtraProxyContext() throws ServletException, IOException {
        final String proxyBaseUrl = "https://proxy.server:9090/applications/geoserver";
        final String requestBaseUrl = "http://localhost:8080/geoserver";
        final String requestResource = "/www/resource.html";
        final String content = "<a href=\"http://localhost:8080/geoserver/linked.html\">link</a>"
                + LINE_SEPARATOR;
        final String contentType = "text/html";

        String result = testDoFilter(proxyBaseUrl, requestBaseUrl, requestResource, content,
                contentType, true);

        String expected = "<a href=\"https://proxy.server:9090/applications/geoserver/linked.html\">link</a>"
                + LINE_SEPARATOR;

        assertEquals(expected, result);
    }

    public void testDoFilterNonMatchingMime() throws ServletException, IOException {
        final String proxyBaseUrl = "https://proxy.server:9090/applications/geoserver";
        final String requestBaseUrl = "http://localhost:8080/geoserver";
        final String requestResource = "/www/resource.bin";
        final String content = "<a href=\"http://localhost:8080/geoserver/linked.html\">link</a>"
                + LINE_SEPARATOR;
        final String contentType = "application/octect-stream";

        String result = testDoFilter(proxyBaseUrl, requestBaseUrl, requestResource, content,
                contentType, true);

        assertEquals(content, result);
    }

    public void testDoFilterRelativeUrl() throws ServletException, IOException {
        final String proxyBaseUrl = "https://proxy.server:9090/applications/geoserver";
        final String requestBaseUrl = "http://localhost:8080/geoserver";
        final String requestResource = "/resource.js";
        final String content = "var=\"/geoserver/wms?\";";
        final String contentType = "application/x-javascript";

        String result = testDoFilter(proxyBaseUrl, requestBaseUrl, requestResource, content,
                contentType, true);

        final String expected = "var=\"/applications/geoserver/wms?\";" + LINE_SEPARATOR;
        assertEquals(expected, result);
    }

    public void testDoFilterProxyRoot() throws ServletException, IOException {
        final String proxyBaseUrl = "https://proxy.server";
        final String requestBaseUrl = "http://localhost:8080/geoserver";
        final String requestResource = "/resource.js";
        final String content = "<a href=\"http://localhost:8080/geoserver/linked.html\">link</a>"
                + LINE_SEPARATOR + "<a href=\"/geoserver/style.css\"></a>" + LINE_SEPARATOR;
        final String contentType = "text/html; charset=UTF-8";

        String result = testDoFilter(proxyBaseUrl, requestBaseUrl, requestResource, content,
                contentType, true);

        final String expected = "<a href=\"https://proxy.server/linked.html\">link</a>"
                + LINE_SEPARATOR + "<a href=\"/style.css\"></a>" + LINE_SEPARATOR;

        assertEquals(expected, result);
    }

    /**
     * May the content already contain the proxified url, so no translation should be done or it
     * could end up mangled
     */
    public void testDoFilterContentContainsProxifiedUrl() throws ServletException, IOException {
        final String proxyBaseUrl = "https://localhost/geoserver/tools";
        final String requestBaseUrl = "http://localhost:8080/geoserver";
        final String requestResource = "/resource.js";
        final String content = "<input type=text value=\"https://localhost/geoserver/tools/proxified\">link</a>"
                + LINE_SEPARATOR;
        final String contentType = "text/html; charset=UTF-8";

        String result = testDoFilter(proxyBaseUrl, requestBaseUrl, requestResource, content,
                contentType, true);

        assertEquals(content, result);
    }

    /**
     * May the response have produced no content at all?
     */
    public void testDoFilterNoContent() throws ServletException, IOException {
        final String proxyBaseUrl = "https://localhost/geoserver/tools";
        final String requestBaseUrl = "http://localhost:8080/geoserver";
        final String requestResource = "/resource.js";
        final String content = "";
        final String contentType = "text/html; charset=UTF-8";

        String result = testDoFilter(proxyBaseUrl, requestBaseUrl, requestResource, content,
                contentType, true);

        assertEquals(content, result);
    }

    /**
     * @param proxyBaseUrl
     * @param requestBaseUrl
     * @param requestResource
     * @param content
     * @param contentType
     * @throws MalformedURLException
     * @throws ServletException
     * @throws IOException
     */
    private String testDoFilter(final String proxyBaseUrl, final String requestBaseUrl,
            final String requestResource, final String content, final String contentType,
            final boolean filterIsEnabled) throws MalformedURLException, ServletException,
            IOException {

        GeoServer mockGeoServer = getMockGeoServer(proxyBaseUrl);

        ApplicationContextMock context = new ApplicationContextMock();
        context.putBean(mockGeoServer);

        GeoServerExtensions ext = new GeoServerExtensions();
        ext.setApplicationContext(context);

        MockFilterConfig config = new MockFilterConfig() {
            public String getInitParameter(String name) {
                if ("mime-types".equals(name)) {
                    return DEFAULT_MIME_TYPES_REGEX;
                } else if ("enabled".equals(name)) {
                    return String.valueOf(filterIsEnabled);
                }
                return null;
            }
        };
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURL(requestBaseUrl + requestResource);
        URL url = new URL(requestBaseUrl);
        req.setServerName(url.getHost());
        req.setScheme(url.getProtocol());
        req.setServerPort(url.getPort() == -1 ? 80 : url.getPort());
        req.setContextPath(url.getPath());

        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        MockHttpSession session = new MockHttpSession();
        req.setSession(session);

        MockServletContext servletContext = new MockServletContext();
        session.setupServletContext(servletContext);

        filter.init(config);

        // the servlet to call at the end of the chain, just writes the provided content out
        // to the response
        Servlet servlet = new HttpServlet() {
            public void service(ServletRequest req, ServletResponse res) throws ServletException,
                    IOException {
                res.setContentType(contentType);
                PrintWriter writer = res.getWriter();
                BufferedReader reader = new BufferedReader(new StringReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.println(line);
                }
                writer.flush();
            }
        };
        chain.setServlet(servlet);
        filter.doFilter(req, res, chain);

        String result = res.getOutputStreamContent();
        return result;
    }
}

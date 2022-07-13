/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.geoserver.filters.LoggingFilter.REQUEST_LOG_BUFFER_SIZE_DEFAULT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.DelegatingServletInputStream;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class LoggingFilterTest {
    Logger logger;
    private static OutputStream logCapturingStream;
    private static StreamHandler customLogHandler;

    private static String expectedLogPart = "took";
    private static String expectedHeadersLogPart = "Headers:";
    private static String expectedBodyLogPart = "body:";

    @Before
    public void setup() {
        logger = Logger.getLogger("org.geoserver.filters");
        logger.setLevel(Level.INFO);
        logCapturingStream = new ByteArrayOutputStream();
        customLogHandler = new StreamHandler(logCapturingStream, new SimpleFormatter());
        logger.addHandler(customLogHandler);
    }

    @Test
    public void testRequestLoggingDoesNotOccur() throws IOException, ServletException {
        String capturedLog = getLog("false", "true", "true", REQUEST_LOG_BUFFER_SIZE_DEFAULT);
        assertFalse(capturedLog.contains(expectedLogPart));
        assertFalse(capturedLog.contains(expectedHeadersLogPart));
        assertFalse(capturedLog.contains(expectedBodyLogPart));
    }

    @Test
    public void testRequestLoggingBody() throws IOException, ServletException {
        String capturedLog = getLog("true", "true", "false", REQUEST_LOG_BUFFER_SIZE_DEFAULT);
        assertTrue(capturedLog.contains(expectedLogPart));
        assertFalse(capturedLog.contains(expectedHeadersLogPart));
        assertTrue(capturedLog.contains(expectedBodyLogPart));
    }

    @Test
    public void testRequestLoggingBodySizeLimit() throws IOException, ServletException {
        String capturedLog = getLog("true", "true", "false", 10);
        assertTrue(capturedLog.contains(expectedLogPart));
        assertFalse(capturedLog.contains(expectedHeadersLogPart));
        assertTrue(capturedLog.contains(expectedBodyLogPart));
        String body = StringUtils.substringBetween(capturedLog, "body: \n", "\n");
        assertEquals(10, body.length());
    }

    @Test
    public void testRequestLoggingBodyZeroLimit() throws Exception {
        // confirm that zero length turns off body logging
        String capturedLog = getLog("true", "true", "false", 0);
        assertFalse(capturedLog.contains(expectedBodyLogPart));
    }

    @Test
    public void testRequestLoggingHeaders() throws IOException, ServletException {
        String capturedLog = getLog("true", "false", "true", REQUEST_LOG_BUFFER_SIZE_DEFAULT);
        assertTrue(capturedLog.contains(expectedLogPart));
        assertTrue(capturedLog.contains(expectedHeadersLogPart));
        assertFalse(capturedLog.contains(expectedBodyLogPart));
    }

    @Test
    public void testRequestLoggingBodyStreamClose() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");

        String generatedString = RandomStringUtils.randomAlphabetic(10);
        request.setContentType(MediaType.TEXT_PLAIN_VALUE);
        request.setContent(generatedString.getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();

        // Sensitive filter simulating tomcat practice of throwing
        // Stream closed if LoggingFilter closes initial input stream
        GeoServerFilter sensitiveFilter =
                new GeoServerFilter() {
                    @Override
                    public void init(FilterConfig filterConfig) throws ServletException {}

                    @Override
                    public void doFilter(
                            ServletRequest request, ServletResponse response, FilterChain chain)
                            throws IOException, ServletException {
                        HttpServletRequestWrapper wrapper =
                                new HttpServletRequestWrapper((HttpServletRequest) request) {
                                    @Override
                                    public HttpServletRequest getRequest() {
                                        return (HttpServletRequest) super.getRequest();
                                    }

                                    @Override
                                    public ServletInputStream getInputStream() throws IOException {
                                        return new DelegatingServletInputStream(
                                                getRequest().getInputStream()) {
                                            boolean closed = false;

                                            @Override
                                            public int read() throws IOException {
                                                if (closed) {
                                                    throw new IOException("Stream is closed");
                                                }
                                                return super.read();
                                            }

                                            @Override
                                            public void close() throws IOException {
                                                super.close();
                                                closed = true;
                                            }
                                        };
                                    }
                                };
                        chain.doFilter(wrapper, response);
                    }

                    @Override
                    public void destroy() {}
                };

        LoggingFilter loggingFilter = getLoggingFilter("true", "true", "false", 5);

        Servlet servlet =
                new GenericServlet() {
                    @Override
                    public void service(ServletRequest req, ServletResponse res)
                            throws ServletException, IOException {

                        @SuppressWarnings("PMD.CloseResource")
                        ServletInputStream is = req.getInputStream();

                        // force read to end, ensuring we exhaust 5 byte buffer
                        StringBuilder echo = new StringBuilder();
                        int b = is.read();
                        while (b != -1) {
                            echo.append((char) b);
                            b = is.read();
                        }
                        res.setContentType("text/plain");

                        @SuppressWarnings("PMD.CloseResource")
                        ServletOutputStream os = res.getOutputStream();
                        os.print(echo.toString());
                    }
                };

        MockFilterChain chain = new MockFilterChain(servlet, sensitiveFilter, loggingFilter);
        try {
            chain.doFilter(request, response);
        } catch (IOException failure) {
            if (failure.getMessage().equals("Stream is closed")) {
                fail("LoggingFilterTest closed the stream");
            }
            throw failure;
        }
        String capturedLog = getTestCapturedLog();
        assertTrue(capturedLog.contains(expectedBodyLogPart));
    }

    private String getTestCapturedLog() throws IOException {
        customLogHandler.flush();
        return logCapturingStream.toString();
    }

    private String getLog(
            String requestsEnabled,
            String bodiesEnabled,
            String headersEnabled,
            Integer logBufferSize)
            throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        String generatedString = RandomStringUtils.randomAlphabetic(10);
        request.setContentType(MediaType.TEXT_PLAIN_VALUE);
        request.setContent(generatedString.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        LoggingFilter filter =
                getLoggingFilter(requestsEnabled, bodiesEnabled, headersEnabled, logBufferSize);
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        String capturedLog = getTestCapturedLog();
        return capturedLog;
    }

    private LoggingFilter getLoggingFilter(
            String requestsEnabled,
            String bodiesEnabled,
            String headersEnabled,
            Integer logBufferSize) {
        GeoServer geoServer = new GeoServerImpl();
        GeoServerInfo geoServerInfo = mock(GeoServerInfoImpl.class);
        MetadataMap metadata = new MetadataMap();
        metadata.put(LoggingFilter.LOG_REQUESTS_ENABLED, requestsEnabled);
        metadata.put(LoggingFilter.LOG_BODIES_ENABLED, bodiesEnabled);
        metadata.put(LoggingFilter.LOG_HEADERS_ENABLED, headersEnabled);
        expect(geoServerInfo.getXmlPostRequestLogBufferSize()).andReturn(logBufferSize).anyTimes();
        expect(geoServerInfo.getMetadata()).andReturn(metadata).anyTimes();
        expect(geoServerInfo.getClientProperties()).andReturn(new HashMap<>()).anyTimes();
        expect(geoServerInfo.getCoverageAccess())
                .andReturn(new CoverageAccessInfoImpl())
                .anyTimes();
        expect(geoServerInfo.getSettings()).andReturn(new SettingsInfoImpl()).anyTimes();
        replay(geoServerInfo);
        geoServer.setGlobal(geoServerInfo);
        LoggingFilter filter =
                new LoggingFilter(geoServer) {
                    public LoggingFilter setLogger(Logger logger) {
                        this.logger = logger;
                        return this;
                    }
                }.setLogger(logger);
        return filter;
    }
}

/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

public class GZipFilterTest {

    @Test
    public void testRetrieveSameOutputStream() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "http://www.geoserver.org");
        request.addHeader("accept-encoding", "gzip");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("text/plain");

        // run the filter
        GZIPFilter filter = new GZIPFilter();
        MockServletContext context = new MockServletContext();
        MockFilterConfig config = new MockFilterConfig(context);
        config.addInitParameter("compressed-types", "text/plain");
        filter.init(config);

        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException {
                        AlternativesResponseStream alternatives =
                                (AlternativesResponseStream) response.getOutputStream();
                        GZIPResponseStream gzipStream =
                                (GZIPResponseStream) alternatives.getStream();
                        assertThat(
                                gzipStream.delegateStream,
                                CoreMatchers.instanceOf(DelegatingServletOutputStream.class));
                    }
                };
        filter.doFilter(request, response, chain);
    }

    @Test
    public void testGZipRemovesContentLength() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "http://www.geoserver.org");
        request.addHeader("accept-encoding", "gzip");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("text/plain");

        // run the filter
        GZIPFilter filter = new GZIPFilter();

        MockServletContext context = new MockServletContext();
        MockFilterConfig config = new MockFilterConfig(context);
        config.addInitParameter("compressed-types", "text/plain");
        filter.init(config);

        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException {
                        response.setContentLength(1000);
                        AlternativesResponseStream alternatives =
                                (AlternativesResponseStream) response.getOutputStream();

                        ServletOutputStream gzipStream = alternatives.getStream();
                        gzipStream.write(1);
                    }
                };
        filter.doFilter(request, response, chain);
        assertFalse(response.containsHeader("Content-Length"));
    }

    @Test
    public void testNotGZippedMantainsContentLength() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "http://www.geoserver.org");
        request.addHeader("accept-encoding", "gzip");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("text/css");

        // run the filter
        GZIPFilter filter = new GZIPFilter();
        MockServletContext context = new MockServletContext();
        context.setInitParameter("compressed-types", "text/plain");
        MockFilterConfig config = new MockFilterConfig(context);
        filter.init(config);

        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException {
                        response.setContentLength(1000);
                        AlternativesResponseStream alternatives =
                                (AlternativesResponseStream) response.getOutputStream();

                        ServletOutputStream gzipStream = alternatives.getStream();
                        gzipStream.write(1);
                    }
                };
        filter.doFilter(request, response, chain);
        assertTrue(response.containsHeader("Content-Length"));
        assertEquals("1000", response.getHeader("Content-Length"));
    }

    @Test
    public void testFlushAfterClose() throws ServletException, IOException {
        // prepare request, response, and chain
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "http://www.geoserver.org");
        request.addHeader("accept-encoding", "gzip");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("text/plain");

        // run the filter
        GZIPFilter filter = new GZIPFilter();

        MockServletContext context = new MockServletContext();
        MockFilterConfig config = new MockFilterConfig(context);
        config.addInitParameter("compressed-types", "text/plain");
        filter.init(config);

        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException {
                        response.setContentLength(1000);
                        AlternativesResponseStream alternatives =
                                (AlternativesResponseStream) response.getOutputStream();

                        ServletOutputStream gzipStream = alternatives.getStream();
                        gzipStream.write(1);
                        gzipStream.close();
                        // ka-blam! (or not?)
                        gzipStream.flush();
                    }
                };
        filter.doFilter(request, response, chain);
        assertFalse(response.containsHeader("Content-Length"));
    }
}

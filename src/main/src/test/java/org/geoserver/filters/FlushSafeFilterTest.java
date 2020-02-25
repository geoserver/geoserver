/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.junit.Assert.*;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class FlushSafeFilterTest {

    @Test
    public void testRetrieveSameOutputStream() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException {
                        // make sure repeated calls to get output stream give us back the same
                        // output stream,
                        // e.g., that we're not creating a new wrapper each time
                        ServletOutputStream os1 = response.getOutputStream();
                        ServletOutputStream os2 = response.getOutputStream();
                        assertSame(os1, os2);
                        assertTrue(os1 instanceof FlushSafeResponse.FlushSafeServletOutputStream);
                    }
                };

        // run the filter
        FlushSafeFilter filter = new FlushSafeFilter();
        filter.init(new MockFilterConfig());
        filter.doFilter(request, response, chain);
    }

    @Test
    public void testFlushAfterClose() throws ServletException, IOException {
        // prepare request, response, and chain
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response =
                new MockHttpServletResponse() {

                    ServletOutputStream os;

                    @Override
                    public ServletOutputStream getOutputStream() {
                        if (os == null) {
                            final ServletOutputStream wrapped = super.getOutputStream();
                            os =
                                    new ServletOutputStream() {
                                        boolean closed;

                                        @Override
                                        public void write(int b) throws IOException {
                                            wrapped.write(b);
                                        }

                                        @Override
                                        public void close() throws IOException {
                                            closed = true;
                                            wrapped.close();
                                        }

                                        @Override
                                        public void flush() throws IOException {
                                            if (closed) {
                                                // we should never reach this code
                                                throw new RuntimeException(
                                                        "Aaarg, I'm already closed, your JVM shall die now!");
                                            }
                                            wrapped.flush();
                                        }
                                    };
                        }

                        return os;
                    }
                };
        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException {
                        ServletOutputStream os = response.getOutputStream();
                        os.print("Some random text");
                        os.close();
                        // ka-blam! (or not?)
                        os.flush();
                    }
                };

        // run the filter
        FlushSafeFilter filter = new FlushSafeFilter();
        filter.init(new MockFilterConfig());
        filter.doFilter(request, response, chain);

        // if we got here without exception, it's already a good sign. Let's check the output
        assertEquals("Some random text", response.getContentAsString());
    }
}

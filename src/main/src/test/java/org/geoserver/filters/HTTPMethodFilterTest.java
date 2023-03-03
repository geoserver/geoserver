/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Tests of a filter that restricts the HTTP methods that can be used to access GeoServer. */
public class HTTPMethodFilterTest {
    @Test
    public void testGETRequest() throws Exception {
        MockHttpServletResponse response = getResponseByMethod("GET");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testPOSTRequest() throws Exception {
        MockHttpServletResponse response = getResponseByMethod("POST");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testPUTRequest() throws Exception {
        MockHttpServletResponse response = getResponseByMethod("PUT");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDELETERequest() throws Exception {
        MockHttpServletResponse response = getResponseByMethod("DELETE");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testHEADRequest() throws Exception {
        MockHttpServletResponse response = getResponseByMethod("HEAD");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testOPTIONSRequest() throws Exception {
        MockHttpServletResponse response = getResponseByMethod("OPTIONS");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testPATCHRequest() throws Exception {
        MockHttpServletResponse response = getResponseByMethod("PATCH");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testInvalidRequest() throws Exception {
        MockHttpServletResponse response = getResponseByMethod("PROPFIND");
        assertEquals(405, response.getStatus());
        response = getResponseByMethod("ACL");
        assertEquals(405, response.getStatus());
        response = getResponseByMethod("MKCALENDAR");
        assertEquals(405, response.getStatus());
        response = getResponseByMethod("LINK");
        assertEquals(405, response.getStatus());
        response = getResponseByMethod("BREW");
        assertEquals(405, response.getStatus());
        response = getResponseByMethod("WHEN");
        assertEquals(405, response.getStatus());
    }

    private MockHttpServletResponse getResponseByMethod(String method)
            throws ServletException, IOException {
        MockHttpServletRequest request =
                new MockHttpServletRequest(method, "http://www.geoserver.org");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("text/plain");

        // run the filter
        HTTPMethodFilter filter = new HTTPMethodFilter();
        MockFilterChain chain =
                new MockFilterChain() {
                    @Override
                    @SuppressWarnings("PMD.CloseResource")
                    public void doFilter(ServletRequest request, ServletResponse response)
                            throws IOException, ServletException {
                        ServletOutputStream os = response.getOutputStream();
                        os.print("Some random text");
                        os.close();
                        // ka-blam! (or not?)
                        os.flush();
                    }
                };
        filter.doFilter(request, response, chain);
        return response;
    }
}

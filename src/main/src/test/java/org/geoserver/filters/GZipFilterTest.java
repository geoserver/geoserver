package org.geoserver.filters;

import static org.junit.Assert.*;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Test;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockFilterConfig;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.mockrunner.mock.web.MockServletContext;
import com.mockrunner.mock.web.MockServletOutputStream;

public class GZipFilterTest {

    
    @Test
    public void testRetrieveSameOutputStream() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURL("http://www.geoserver.org");
        request.setHeader("accept-encoding", "gzip");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("text/plain");

        // run the filter
        GZIPFilter filter = new GZIPFilter();
        MockFilterConfig config = new MockFilterConfig();
        MockServletContext context = new MockServletContext();
        context.setInitParameter("compressed-types", "text/plain");
        config.setupServletContext(context);
        filter.init(config);

        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException,
                    ServletException {
                AlternativesResponseStream alternatives = (AlternativesResponseStream) response
                        .getOutputStream();
                GZIPResponseStream gzipStream = (GZIPResponseStream) alternatives.getStream();
                GZIPOutputStream os = gzipStream.gzipstream;

                try {
                    Field f = FilterOutputStream.class.getDeclaredField("out");
                    f.setAccessible(true);
                    OutputStream wrapped = (OutputStream) f.get(os);
                    // System.out.println(wrapped);
                    // we are not memory bound
                    assertTrue(wrapped instanceof MockServletOutputStream);
                } catch (Exception e) {
                    // it can happen
                    System.out
                            .println("Failed to retrieve original stream wrapped by the GZIPOutputStream");
                    e.printStackTrace();
                }
            }
        };
        filter.doFilter(request, response, chain);
    }
}

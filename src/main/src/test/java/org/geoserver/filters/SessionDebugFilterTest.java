package org.geoserver.filters;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class SessionDebugFilterTest {

    @Test
    public void testNPEIsNotThrownIfPathInfoIsNull() throws IOException, ServletException {
        // tests that NPE is not thrown when creating session if the pathInfo is null
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        SessionDebugFilter filter = new SessionDebugFilter();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response)
                    throws IOException, ServletException {
                SessionDebugFilter.SessionDebugWrapper debugWrapper = (SessionDebugFilter.SessionDebugWrapper) request;
                assertNull(debugWrapper.getPathInfo());
                HttpSession session = debugWrapper.getSession();
                assertNotNull(session);
            }
        };
        filter.doFilter(request, response, chain);
    }
}

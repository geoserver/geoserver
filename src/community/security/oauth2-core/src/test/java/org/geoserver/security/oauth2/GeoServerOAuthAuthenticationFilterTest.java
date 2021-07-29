package org.geoserver.security.oauth2;

import static org.junit.Assert.assertNull;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

public class GeoServerOAuthAuthenticationFilterTest {

    @Test
    public void testAnonymousAuthenticationIsNotCreate() {
        GeoServerOAuthAuthenticationFilter filter =
                new GeoServerOAuthAuthenticationFilter(null, null, null, null) {
                    @Override
                    protected String getPreAuthenticatedPrincipal(
                            HttpServletRequest request, HttpServletResponse response) {
                        return null;
                    }
                };
        filter.doAuthenticate(new MockHttpServletRequest(), new MockHttpServletResponse());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public class GeoServerRequestHeaderAuthenticationFilterTest {

    @Test
    public void testAuthenticationViaPreAuthChanging() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(new PreAuthenticatedAuthenticationToken("testadmin", null));
        SecurityContextHolder.setContext(sc);
        GeoServerRequestHeaderAuthenticationFilter toTest =
                new GeoServerRequestHeaderAuthenticationFilter();
        toTest.setPrincipalHeaderAttribute("sec-username");
        request.addHeader("sec-username", "testuser");
        toTest.setSecurityManager(
                new GeoServerSecurityManager(new GeoServerDataDirectory(new File("/tmp"))));
        toTest.setRoleSource(
                PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.Header);

        toTest.doFilter(request, response, filterChain);

        assertEquals(
                "testuser",
                SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
    }

    @Test
    public void testAuthenticationViaPreAuthNoHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(new PreAuthenticatedAuthenticationToken("testadmin", null));
        SecurityContextHolder.setContext(sc);
        GeoServerRequestHeaderAuthenticationFilter toTest =
                new GeoServerRequestHeaderAuthenticationFilter();
        toTest.setPrincipalHeaderAttribute("sec-username");
        toTest.setSecurityManager(
                new GeoServerSecurityManager(new GeoServerDataDirectory(new File("/tmp"))));
        toTest.setRoleSource(
                PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.Header);

        toTest.doFilter(request, response, filterChain);

        // The security context should have been cleared
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

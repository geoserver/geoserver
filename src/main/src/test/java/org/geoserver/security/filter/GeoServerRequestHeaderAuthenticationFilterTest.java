/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.SecurityInterceptorFilterConfig;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityMetadataSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

@SuppressWarnings({"deprecation", "removal"})
public class GeoServerRequestHeaderAuthenticationFilterTest {

    @Test
    public void testAuthenticationViaPreAuthChanging() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(new PreAuthenticatedAuthenticationToken("testadmin", null));
        SecurityContextHolder.setContext(sc);
        GeoServerRequestHeaderAuthenticationFilter toTest = new GeoServerRequestHeaderAuthenticationFilter();
        toTest.setPrincipalHeaderAttribute("sec-username");
        request.addHeader("sec-username", "testuser");
        toTest.setSecurityManager(new GeoServerSecurityManager(new GeoServerDataDirectory(new File("/tmp"))));
        toTest.setRoleSource(PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.Header);
        SecurityInterceptorFilterConfig filterCfg = new SecurityInterceptorFilterConfig();
        filterCfg.setName("custom");
        filterCfg.setClassName(AuthCapturingFilter.class.getName());
        filterCfg.setSecurityMetadataSource("geoserverMetadataSource");
        toTest.doFilter(request, response, filterChain);

        assertEquals(
                "testuser",
                SecurityContextHolder.getContext()
                        .getAuthentication()
                        .getPrincipal()
                        .toString());
    }

    static class AuthCapturingFilter extends GeoServerSecurityFilter implements GeoServerAuthenticationFilter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            request.setAttribute("auth", auth);
            chain.doFilter(request, response);
        }

        @Override
        public boolean applicableForHtml() {
            return true;
        }

        @Override
        public boolean applicableForServices() {
            return true;
        }
    }

    @Test
    public void testAuthenticationWithNullAttributes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        GeoServerSecurityInterceptorFilter toTest2 = new GeoServerSecurityInterceptorFilter();
        toTest2.setSecurityManager(new GeoServerSecurityManager(new GeoServerDataDirectory(new File("/tmp"))));
        toTest2.setSecurityManager(new GeoServerSecurityManager(new GeoServerDataDirectory(new File("/tmp"))));
        SecurityInterceptorFilterConfig filterCfg = new SecurityInterceptorFilterConfig();
        filterCfg.setName("custom");
        filterCfg.setClassName(AuthCapturingFilter.class.getName());
        filterCfg.setSecurityMetadataSource("geoserverMetadataSource");
        SecurityMetadataSource metadataSource = new SecurityMetadataSource() {
            @Override
            public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
                return null;
            }

            @Override
            public Collection<ConfigAttribute> getAllConfigAttributes() {
                return null;
            }

            @Override
            public boolean supports(Class<?> clazz) {
                return false;
            }
        };
        toTest2.initializeFromConfig(filterCfg, metadataSource);
        try {
            toTest2.doFilter(request, response, filterChain);
            fail("Expected AccessDeniedException because the attributes are empty");
        } catch (AccessDeniedException e) {
            assertEquals("Access Denied", e.getMessage());
        }
    }

    @Test
    public void testAuthenticationViaPreAuthNoHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        SecurityContext sc = new SecurityContextImpl();
        sc.setAuthentication(new PreAuthenticatedAuthenticationToken("testadmin", null));
        SecurityContextHolder.setContext(sc);
        GeoServerRequestHeaderAuthenticationFilter toTest = new GeoServerRequestHeaderAuthenticationFilter();
        toTest.setPrincipalHeaderAttribute("sec-username");
        toTest.setSecurityManager(new GeoServerSecurityManager(new GeoServerDataDirectory(new File("/tmp"))));
        toTest.setRoleSource(PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource.Header);

        toTest.doFilter(request, response, filterChain);

        // The security context should have been cleared
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}

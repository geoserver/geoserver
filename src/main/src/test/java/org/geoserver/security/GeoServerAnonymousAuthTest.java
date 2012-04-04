package org.geoserver.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.SecurityManagerConfig;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mockrunner.mock.web.MockFilterChain;
import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class GeoServerAnonymousAuthTest extends GeoServerSecurityTestSupport {

    public void testFilterChainWithEnabled() throws Exception {
        MockHttpServletRequest request = createRequest("/foo");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        GeoServerSecurityFilterChainProxy filterChainProxy = 
            GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
        filterChainProxy.doFilter(request, response, chain);
        
        chain.addFilter(filterChainProxy);
        chain.addFilter(new DummyFilter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                assertNotNull(auth);
                assertTrue(auth instanceof AnonymousAuthenticationToken);
            }
        });
        chain.doFilter(request, response);
    }

    public void testFilterChainWithDisabled() throws Exception {
        disableAnonymousAuth();

        MockHttpServletRequest request = createRequest("/foo");
        
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        
        GeoServerSecurityFilterChainProxy filterChainProxy = 
            GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
        filterChainProxy.doFilter(request, response, chain);
        
        chain.addFilter(filterChainProxy);
        chain.addFilter(new DummyFilter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                assertNull(auth);
            }
        });
        chain.doFilter(request, response);
    }

//    public void testAuthProviderWithEnabled() throws Exception {
//        GeoServerSecurityManager secMgr = getSecurityManager();
//        secMgr.authenticate(new AnonymousAuthenticationToken("geoserver", "anonymousUser",
//            (List) Arrays.asList(new GrantedAuthorityImpl("ROLE_ANONYMOUS"))));
//    }

    public void testAuthProviderWithDisabledEnabled() throws Exception {
        disableAnonymousAuth();
        GeoServerSecurityManager secMgr = getSecurityManager();
        
        try {
            secMgr.authenticate(new AnonymousAuthenticationToken("geoserver", "anonymousUser",
                (List) Arrays.asList(new GrantedAuthorityImpl("ROLE_ANONYMOUS"))));
            fail();
        }
        catch(ProviderNotFoundException pnfe) {
        }
    }

    void disableAnonymousAuth() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        SecurityManagerConfig cfg = secMgr.getSecurityConfig();
        cfg.setAnonymousAuth(false);
        cfg.setConfigPasswordEncrypterName(getPlainTextPasswordEncoder().getName());
        secMgr.saveSecurityConfig(cfg);
    }

    class DummyFilter implements Filter {
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
        }
        @Override
        public void destroy() {
        }
    }
}

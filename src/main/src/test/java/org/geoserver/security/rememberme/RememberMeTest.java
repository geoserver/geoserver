package org.geoserver.security.rememberme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;

import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.test.GeoServerTestSupport;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class RememberMeTest extends GeoServerTestSupport {


    @Override
    protected void setUpInternal() throws Exception {
        //setup a custom filter to capture authentication

        SecurityNamedServiceConfig filterCfg = new BaseSecurityNamedServiceConfig();
        filterCfg.setName("custom");
        filterCfg.setClassName(AuthCapturingFilter.class.getName());

        GeoServerSecurityManager secMgr = getSecurityManager();
        secMgr.saveFilter(filterCfg);

        SecurityManagerConfig cfg = secMgr.getSecurityConfig();
        cfg.getFilterChain().insertAfter("/web/**", filterCfg.getName(), GeoServerSecurityFilterChain.REMEMBER_ME_FILTER);
        
//        cfg.getFilterChain().put("/web/**", Arrays.asList(
//            new FilterChainEntry(filterCfg.getName(), Position.AFTER, 
//                GeoServerSecurityFilterChain.REMEMBER_ME_FILTER)));
        
        secMgr.saveSecurityConfig(cfg);
    }

    @Override
    protected String[] getSpringContextLocations() {
        List<String> list = new ArrayList<String>(Arrays.asList(super.getSpringContextLocations()));
        list.add(getClass().getResource(getClass().getSimpleName() + "-context.xml").toString());
        return list.toArray(new String[list.size()]);
    }

    static class AuthCapturingFilter extends GeoServerSecurityFilter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            request.setAttribute("auth", auth);
            chain.doFilter(request, response);
        }
    }

    static class SecurityProvider extends GeoServerSecurityProvider {
        @Override
        public Class<? extends GeoServerSecurityFilter> getFilterClass() {
            return AuthCapturingFilter.class;
        }

        @Override
        public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
            return new AuthCapturingFilter();
        }
    }

    @Override
    protected List<Filter> getFilters() {
        return Arrays.asList((javax.servlet.Filter)
            applicationContext.getBean(GeoServerSecurityFilterChainProxy.class));
    }

    public void testRememberMeLogin() throws Exception {
        
        MockHttpServletRequest request = createRequest("/j_spring_security_check");
        request.setupAddParameter("username", "admin");
        request.setupAddParameter("password", "geoserver");
        request.setMethod("POST");
        MockHttpServletResponse response = dispatch(request);
        assertLoginOk(response);
        assertEquals(0, response.getCookies().size());

        request = createRequest("/j_spring_security_check");
        request.setupAddParameter("username", "admin");
        request.setupAddParameter("password", "geoserver");
        request.setupAddParameter("_spring_security_remember_me", "yes");
        request.setMethod("POST");
        response = dispatch(request);
        assertLoginOk(response);
        assertEquals(1, response.getCookies().size());

        Cookie cookie = (Cookie) response.getCookies().get(0);

        request = createRequest("/web/");
        response = dispatch(request);
        assertNull(request.getAttribute("auth"));
        
        request = createRequest("/web/");
        request.addCookie(cookie);
        response = dispatch(request);
        assertTrue(request.getAttribute("auth") instanceof RememberMeAuthenticationToken);
    }

    public void testRememberMeOtherUserGroupService() throws Exception {
        // TODO Justin, this should work now
        
        //need to implement this test, at the moment we don't have a way to mock up new users 
        // in a memory user group service... 
        /*
        SecurityUserGoupServiceConfig memCfg = new MemoryUserGroupServiceConfigImpl();
        memCfg.setName("memory");
        memCfg.setClassName(MemoryUserGroupService.class.getName());
        memCfg.setPasswordEncoderName(GeoserverPlainTextPasswordEncoder.BeanName);
        memCfg.setPasswordPolicyName(PasswordValidator.DEFAULT_NAME);

        GeoServerSecurityManager secMgr = getSecurityManager();
        secMgr.saveUserGroupService(memCfg);
        
        GeoserverUserGroupService ug = secMgr.loadUserGroupService("memory");
        GeoserverUser user = ug.createUserObject("foo", "bar", true);
        ug.createStore().addUser(user);
            
        user = ug.getUserByUsername("foo");
        assertNotNull(user);
        */
    }

    void assertLoginOk(MockHttpServletResponse resp) {
        assertEquals("/geoserver/", resp.getHeader("Location"));
    }

    void assertLoginFailed(MockHttpServletResponse resp) {
        assertTrue(resp.getHeader("Location").endsWith("GeoServerLoginPage&error=true"));
    }
}

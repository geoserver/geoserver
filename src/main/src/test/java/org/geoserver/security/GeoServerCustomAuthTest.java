package org.geoserver.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.test.GeoServerTestSupport;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class GeoServerCustomAuthTest extends GeoServerSecurityTestSupport {

    @Override
    protected String[] getSpringContextLocations() {
        List<String> list = new ArrayList<String>(Arrays.asList(super.getSpringContextLocations()));
        list.add(getClass().getResource(getClass().getSimpleName() + "-context.xml").toString());
        return list.toArray(new String[list.size()]);
    }

    public void testInactive() throws Exception {
        UsernamePasswordAuthenticationToken upAuth = 
            new UsernamePasswordAuthenticationToken("foo", "bar");
        try {
            getSecurityManager().authenticate(upAuth);
            fail();
        }
        catch(BadCredentialsException e) {}
    }

    public void testActive() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        UsernamePasswordAuthenticationProviderConfig config = 
                new UsernamePasswordAuthenticationProviderConfig();
        config.setName("custom");
        config.setClassName(AuthProvider.class.getName());
        secMgr.saveAuthenticationProvider(config);
        

        SecurityManagerConfig mgrConfig = secMgr.getSecurityConfig();
        mgrConfig.getAuthProviderNames().add("custom");
        mgrConfig.setConfigPasswordEncrypterName(getPlainTextPasswordEncoder().getName());
        secMgr.saveSecurityConfig(mgrConfig);

        Authentication auth = new UsernamePasswordAuthenticationToken("foo", "bar");
        auth = getSecurityManager().authenticate(auth);
        assertTrue(auth.isAuthenticated());
    }
    
    static class SecurityProvider extends GeoServerSecurityProvider {
        @Override
        public Class<? extends GeoServerAuthenticationProvider> getAuthenticationProviderClass() {
            return AuthProvider.class;
        }
        @Override
        public GeoServerAuthenticationProvider createAuthenticationProvider(
                SecurityNamedServiceConfig config) {
            return new AuthProvider();
        }
    }

    static class AuthProvider extends GeoServerAuthenticationProvider {

        @Override
        public Authentication authenticate(Authentication authentication, HttpServletRequest request)
                throws AuthenticationException {
            if (authentication instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken up = 
                    (UsernamePasswordAuthenticationToken)authentication;
                if ("foo".equals(up.getPrincipal()) && "bar".equals(up.getCredentials())) {
                    authentication = new UsernamePasswordAuthenticationToken("foo", "bar", 
                        (List) Collections.emptyList());
                }
            }
            return authentication;
        }

        @Override
        public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
            return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
        }
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

@Category(SystemTest.class)
public class GeoServerCustomAuthTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                getClass().getResource(getClass().getSimpleName() + "-context.xml").toString());
    }

    @Test
    public void testInactive() throws Exception {
        UsernamePasswordAuthenticationToken upAuth =
                new UsernamePasswordAuthenticationToken("foo", "bar");
        try {
            getSecurityManager().authenticationManager().authenticate(upAuth);
        } catch (BadCredentialsException e) {
        } catch (ProviderNotFoundException e) {
        }
    }

    @Test
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
        auth = getSecurityManager().authenticationManager().authenticate(auth);
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
        public Authentication authenticate(
                Authentication authentication, HttpServletRequest request)
                throws AuthenticationException {
            if (authentication instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken up =
                        (UsernamePasswordAuthenticationToken) authentication;
                if ("foo".equals(up.getPrincipal()) && "bar".equals(up.getCredentials())) {
                    authentication =
                            new UsernamePasswordAuthenticationToken(
                                    "foo", "bar", Collections.<GrantedAuthority>emptyList());
                }
            }
            return authentication;
        }

        @Override
        public boolean supports(
                Class<? extends Object> authentication, HttpServletRequest request) {
            return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
        }
    }
}

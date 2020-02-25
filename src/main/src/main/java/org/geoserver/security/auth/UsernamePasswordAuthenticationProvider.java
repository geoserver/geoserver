/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.filter.GeoServerWebAuthenticationDetails;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.password.GeoServerMultiplexingPasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authentication provider that delegates to a {@link GeoServerUserGroupService}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class UsernamePasswordAuthenticationProvider extends GeoServerAuthenticationProvider {

    /** auth provider to delegate to */
    DaoAuthenticationProvider authProvider;

    String userGroupServiceName;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        UsernamePasswordAuthenticationProviderConfig upAuthConfig =
                (UsernamePasswordAuthenticationProviderConfig) config;

        GeoServerUserGroupService ugService =
                getSecurityManager().loadUserGroupService(upAuthConfig.getUserGroupServiceName());
        if (ugService == null) {
            throw new IllegalArgumentException(
                    "Unable to load user group service " + upAuthConfig.getUserGroupServiceName());
        }
        userGroupServiceName = upAuthConfig.getUserGroupServiceName();

        // create delegate auth provider
        authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(ugService);

        // set up the password encoder
        // multiplex password encoder actually allows us to handle all types of passwords for
        // decoding purposes, regardless of whatever the current one used by the user group service
        // is
        authProvider.setPasswordEncoder(
                new GeoServerMultiplexingPasswordEncoder(getSecurityManager(), ugService));

        try {
            authProvider.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
        return authProvider.supports(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request)
            throws AuthenticationException {
        UsernamePasswordAuthenticationToken auth = null;
        try {
            auth = (UsernamePasswordAuthenticationToken) authProvider.authenticate(authentication);
        } catch (AuthenticationException ex) {
            log(ex);
            return null; // pass request to next provider in the chain
        }
        if (auth == null) {
            return null;
        }

        if (auth.getDetails() instanceof GeoServerWebAuthenticationDetails) {
            ((GeoServerWebAuthenticationDetails) auth.getDetails())
                    .setUserGroupServiceName(userGroupServiceName);
        }
        if (auth.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE) == false) {
            List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
            roles.addAll(auth.getAuthorities());
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);
            UsernamePasswordAuthenticationToken newAuth =
                    new UsernamePasswordAuthenticationToken(
                            auth.getPrincipal(), auth.getCredentials(), roles);
            newAuth.setDetails(auth.getDetails());
            return newAuth;
        }
        return auth;
    }
}

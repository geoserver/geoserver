/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Authentication provider that wraps a regular {@link AuthenticationProvider} in the 
 * {@link GeoServerAuthenticationProvider} interface. 
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class DelegatingAuthenticationProvider extends GeoServerAuthenticationProvider {

    AuthenticationProvider authProvider;

    public DelegatingAuthenticationProvider(AuthenticationProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public boolean supports(Class<? extends Object> authentication, HttpServletRequest request) {
        return authProvider.supports(authentication);
    }

    @Override
    public Authentication authenticate(Authentication authentication, HttpServletRequest request)
            throws AuthenticationException {
        return authProvider.authenticate(authentication);
    }

}

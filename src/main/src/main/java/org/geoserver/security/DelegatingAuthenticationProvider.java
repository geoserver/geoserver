/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import javax.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Authentication provider that wraps a regular {@link AuthenticationProvider} in the {@link
 * GeoServerAuthenticationProvider} interface.
 *
 * @author Justin Deoliveira, OpenGeo
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
    public final Authentication authenticate(
            Authentication authentication, HttpServletRequest request) {
        try {
            return doAuthenticate(authentication, request);
        } catch (AuthenticationException ex) {
            log(ex);

            // pass request to next provider in the chain
            return null;
        }
    }

    /**
     * Does the actual authentication.
     *
     * <p>Subclasses should override this method, the default implementation simply delegages to the
     * underlying {@link AuthenticationProvider#authenticate(Authentication)}.
     *
     * <p>This method does not need to worry about handling any {@link AuthenticationException},
     * they should be thrown back.
     */
    protected Authentication doAuthenticate(
            Authentication authentication, HttpServletRequest request)
            throws AuthenticationException {
        return authProvider.authenticate(authentication);
    }
}

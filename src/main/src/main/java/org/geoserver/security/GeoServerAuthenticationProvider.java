/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import javax.servlet.http.HttpServletRequest;

import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Extension of {@link AuthenticationProvider} for the geoserver security subsystem.
 * <p>
 * Instances of this class are provided by {@link GeoServerSecurityProvider}. Authentication 
 * providers are configured via {@link SecurityManagerConfig#getAuthProviderNames()}.
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 * 
 */
public abstract class GeoServerAuthenticationProvider extends AbstractGeoServerSecurityService 
    implements AuthenticationProvider {

    public static String DEFAULT_NAME = "default";

    @Override
    public final boolean supports(Class<? extends Object> authentication) {
        return supports(authentication, request());
    }

    /**
     * Same function as {@link #supports(Class)} but is provided with the current request object.
     */
    public abstract boolean supports(Class<? extends Object> authentication, HttpServletRequest request);

    @Override
    public final Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        return authenticate(authentication, request());
    }

    /**
     * Same function as {@link #authenticate(Authentication)} but is provided with the current 
     * request object.
     */
    public abstract Authentication authenticate(Authentication authentication, HttpServletRequest request)
            throws AuthenticationException;

    /**
     * The current request.
     */
    HttpServletRequest request() {
        return GeoServerSecurityFilterChainProxy.REQUEST.get();
    }
}

/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.springframework.security.core.Authentication;

/**
 * Authorizer that allows access if the user has authenticated.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AuthenticatedComponentAuthorizer implements ComponentAuthorizer {

    @Override
    public boolean isAccessAllowed(Class<?> componentClass, Authentication authentication) {
        if (GeoServerSecurityFilterChainProxy.isSecurityEnabledForCurrentRequest() == false)
            return true;
        return authentication != null && authentication.isAuthenticated();
    }
}

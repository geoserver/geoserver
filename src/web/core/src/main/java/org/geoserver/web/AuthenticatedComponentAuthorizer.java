/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.springframework.security.core.Authentication;

/**
 * Authorizer that allows access if the user has authenticated.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class AuthenticatedComponentAuthorizer implements ComponentAuthorizer {

    @Override
    public boolean isAccessAllowed(Class componentClass, Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

}

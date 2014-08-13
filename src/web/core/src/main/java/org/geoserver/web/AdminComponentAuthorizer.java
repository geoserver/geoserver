/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authorizer that only allows access to the admin.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class AdminComponentAuthorizer implements ComponentAuthorizer {

    public boolean isAccessAllowed(Class componentClass, Authentication authentication) {
        return getSecurityManager().checkAuthenticationForAdminRole(authentication);
    }

    protected GeoServerSecurityManager getSecurityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }
}

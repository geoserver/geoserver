/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.web.AdminComponentAuthorizer;
import org.springframework.security.core.Authentication;

public class GroupAdminComponentAuthorizer extends AdminComponentAuthorizer {

    @Override
    public boolean isAccessAllowed(Class componentClass, Authentication authentication) {
        // full admin implies group admin
        if (super.isAccessAllowed(componentClass, authentication)) {
            return true;
        }

        return getSecurityManager()
                .checkAuthenticationForRole(authentication, GeoServerRole.GROUP_ADMIN_ROLE);
    }
}

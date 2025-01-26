/* (c) 2014 - 2024 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer;
import org.springframework.security.core.Authentication;

/**
 * Authorizer that allows access if the user has admin rights to any workspace.
 *
 * @author Justin Deoliveira, OpenGeo
 * @see WorkspaceAdminAuthorizer#isWorkspaceAdmin(Authentication)
 */
@SuppressWarnings("serial")
public class WorkspaceAdminComponentAuthorizer extends AdminComponentAuthorizer {

    @Override
    public boolean isAccessAllowed(Class<?> componentClass, Authentication authentication) {

        // if full admin grant access
        if (super.isAccessAllowed(componentClass, authentication)) {
            return true;
        }

        return getWorkspaceAdminAuthorizer().isWorkspaceAdmin(authentication);
    }

    WorkspaceAdminAuthorizer getWorkspaceAdminAuthorizer() {
        return WorkspaceAdminAuthorizer.get().orElseThrow();
    }
}

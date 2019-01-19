/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WorkspaceAccessLimits;
import org.springframework.security.core.Authentication;

/**
 * Authorizer that allows access if the user has admin rights to any workspace.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class WorkspaceAdminComponentAuthorizer extends AdminComponentAuthorizer {
    @Override
    public boolean isAccessAllowed(Class<?> componentClass, Authentication authentication) {

        // if full admin grant access
        if (super.isAccessAllowed(componentClass, authentication)) {
            return true;
        }

        // if not authenticated deny access
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // TODO: we should cache this result somehow
        if (isWorkspaceAdmin(authentication)) {
            return true;
        }

        return false;
    }

    /** Check if the current user has any admin privilege on at least one workspace. */
    boolean isWorkspaceAdmin(Authentication authentication) {

        Catalog catalog = getSecurityManager().getCatalog();

        // the secure catalog builds and owns the ResourceAccessManager
        SecureCatalogImpl secureCatalog =
                GeoServerApplication.get().getBeanOfType(SecureCatalogImpl.class);
        ResourceAccessManager manager = secureCatalog.getResourceAccessManager();

        if (manager != null) {
            for (WorkspaceInfo workspace : catalog.getWorkspaces()) {
                WorkspaceAccessLimits accessLimits =
                        manager.getAccessLimits(authentication, workspace);
                if (accessLimits != null && accessLimits.isAdminable()) {
                    return true;
                }
            }
        }

        return false;
    }
}

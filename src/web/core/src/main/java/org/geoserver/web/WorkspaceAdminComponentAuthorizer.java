/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.security.AdminRequest;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Authorizer that allows access if the user has admin rights to any workspace. 
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class WorkspaceAdminComponentAuthorizer extends AdminComponentAuthorizer {

    @Override
    public boolean isAccessAllowed(Class componentClass,
            Authentication authentication) {

        //if full admin grant access
        if (super.isAccessAllowed(componentClass, authentication)) {
            return true;
        }

        //if not authenticated deny access
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        //TODO: we should cache this result somehow

        List<String> roles = lookupWorkspaceAdminRoles();
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if (roles.contains(auth.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    List<String> lookupWorkspaceAdminRoles() {
        List<String> roles = new ArrayList<String>();
        DataAccessRuleDAO dao = DataAccessRuleDAO.get();
        for (DataAccessRule rule : dao.getRules()) {
            if (rule.getAccessMode() == AccessMode.ADMIN) {
                roles.addAll(rule.getRoles());
            }
        }
        return roles;
    }
}

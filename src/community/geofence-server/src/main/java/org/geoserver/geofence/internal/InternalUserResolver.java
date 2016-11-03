/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.geofence.spi.UserResolver;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.RoleCalculator;
import org.geotools.util.logging.Logging;

/**
 * 
 * Links GeoServer users/roles to internal Geofence server
 * 
 * @author Niels Charlier
 *
 */
public class InternalUserResolver implements UserResolver {
    
    private Logger logger = Logging.getLogger(InternalUserResolver.class);
    
    protected GeoServerSecurityManager securityManager;
    
    public InternalUserResolver(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public boolean existsUser(String username) {
        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Getting Roles for User [" + username + "]");
        }
        
        try {
            for (String serviceName : securityManager.listUserGroupServices()) {
                
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Checking UserGroupService [" + serviceName + "]");
                }
                
                final GeoServerUserGroupService userGroupService = securityManager.loadUserGroupService(serviceName);
                if (userGroupService.getUserByUsername(username) != null) {

                    if(logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "UserGroupService [" + serviceName + "] matching for User [" + username + "]");
                    }

                    return true;
                }
            }
            
            for (String roleServiceName : securityManager.listRoleServices()) {
                
                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Checking RoleService [" + roleServiceName + "]");
                }
                
                final GeoServerRoleService roleService = securityManager.loadRoleService(roleServiceName);
                if (roleService.getRolesForUser(username) != null && 
                        !roleService.getRolesForUser(username).isEmpty()) {
                    
                    if(logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "RoleService [" + roleServiceName + "] matching for User [" + username + "]");
                    }

                    return true;
                }
            }
            
            for (String roleServiceName : securityManager.listRoleServices()) {
                SortedSet<GeoServerRole> userRoles = securityManager.loadRoleService(roleServiceName).getRolesForUser(username);
                if (userRoles != null && !userRoles.isEmpty()) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        
        logger.log(Level.WARNING, "GeoFence was not able to find any matching user on Security Context amd Services.");
        
        return false;
    }

    @Override
    public boolean existsRole(String rolename) {
        try {
            
            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Checking Role [" + rolename + "] on ActiveRoleService [" + securityManager.getActiveRoleService() + "]");
            }
                    
            return securityManager.getActiveRoleService().getRoleByName(rolename) != null;
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Set<String> getRoles(String username) {
        try {
            SortedSet<GeoServerRole> roleSet = securityManager.getActiveRoleService().getRolesForUser(username);
            SortedSet<String> stringSet = new TreeSet<String>();
            
            for (GeoServerRole role : roleSet) {

                if(logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Checking Role [" + role + "] on ActiveRoleService [" + securityManager.getActiveRoleService() + "]");
                }

                stringSet.add(role.getAuthority());
            }

            try {
                // Search for derived roles, the ones assigned through groups
                for (String serviceName : securityManager.listUserGroupServices()) {
                    
                    if(logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Checking UserGroupService [" + serviceName + "]");
                    }
                    
                    final GeoServerUserGroupService userGroupService = securityManager.loadUserGroupService(serviceName);
                    if (userGroupService.getUserByUsername(username) != null) {
    
                        RoleCalculator calc = new RoleCalculator(userGroupService, securityManager.getActiveRoleService());
                        
                        if(logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "UserGroupService [" + serviceName + "] matching for User [" + username + "]");
                        }
                        
                        GeoServerUser user = userGroupService.getUserByUsername(username);
                        for (GeoServerUserGroup group : userGroupService.getGroupsForUser(user)) {
                            if (group.isEnabled()) {
                                for (GeoServerRole role : calc.calculateRoles(group)) {
                                    stringSet.add(role.getAuthority());
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }

            if(logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Matching Roles [" + stringSet + "] for User [" + username + "]");
            }

            return stringSet;
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return Collections.emptySet();
        }
    }

}

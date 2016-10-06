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
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
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
        try {
            for (String serviceName : securityManager.listUserGroupServices()) {
                if (securityManager.loadUserGroupService(serviceName).getUserByUsername(username) != null) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean existsRole(String rolename) {
        try {
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
                stringSet.add(role.getAuthority());
            }
            
            return stringSet;
            
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return Collections.emptySet();
        }
    }

}

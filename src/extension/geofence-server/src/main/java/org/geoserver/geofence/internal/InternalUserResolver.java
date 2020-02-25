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
import org.geoserver.security.GeoServerSecurityService;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.impl.RoleCalculator;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

/**
 * Links GeoServer users/roles to internal Geofence server
 *
 * @author Niels Charlier
 */
@PropertySource("classpath*:application.properties")
public class InternalUserResolver implements UserResolver {

    public static final String DEFAULT_USER_GROUP_SERVICE_KEY =
            "org.geoserver.rest.DefaultUserGroupServiceName";

    private Logger logger = Logging.getLogger(InternalUserResolver.class);

    @Autowired private Environment env;

    @Value("${org.geoserver.rest.DefaultUserGroupServiceName}")
    private String DEFAULT_ROLE_SERVICE_NAME;

    private GeoServerSecurityService defaultSecurityService;

    protected GeoServerSecurityManager securityManager;

    private String getDefaultServiceName() {
        String defaultServiceName = System.getProperty(DEFAULT_USER_GROUP_SERVICE_KEY);
        if (defaultServiceName != null) {
            return defaultServiceName;
        }
        defaultServiceName =
                defaultServiceName == null
                        ? env.getProperty(DEFAULT_USER_GROUP_SERVICE_KEY)
                        : DEFAULT_ROLE_SERVICE_NAME;
        return defaultServiceName == null ? DEFAULT_ROLE_SERVICE_NAME : defaultServiceName;
    }

    public GeoServerSecurityService getDefaultSecurityService() throws IOException {
        if (defaultSecurityService != null) {
            return defaultSecurityService;
        }

        for (String serviceName : securityManager.listUserGroupServices()) {
            if (serviceName.equals(getDefaultServiceName())) {
                final GeoServerUserGroupService userGroupService =
                        securityManager.loadUserGroupService(serviceName);
                defaultSecurityService = userGroupService;
                return userGroupService;
            }
        }

        for (String roleServiceName : securityManager.listRoleServices()) {
            if (roleServiceName.equals(getDefaultServiceName())) {
                final GeoServerRoleService roleService =
                        securityManager.loadRoleService(roleServiceName);
                defaultSecurityService = roleService;
                return roleService;
            }
        }

        defaultSecurityService = securityManager.getActiveRoleService();
        return defaultSecurityService;
    }

    public InternalUserResolver(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean existsUser(String username) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Getting Roles for User [" + username + "]");
        }

        try {
            for (String serviceName : securityManager.listUserGroupServices()) {

                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Checking UserGroupService [" + serviceName + "]");
                }

                final GeoServerUserGroupService userGroupService =
                        securityManager.loadUserGroupService(serviceName);
                if (userGroupService.getUserByUsername(username) != null) {

                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(
                                Level.FINE,
                                "UserGroupService ["
                                        + serviceName
                                        + "] matching for User ["
                                        + username
                                        + "]");
                    }

                    return true;
                }
            }

            for (String roleServiceName : securityManager.listRoleServices()) {

                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Checking RoleService [" + roleServiceName + "]");
                }

                final GeoServerRoleService roleService =
                        securityManager.loadRoleService(roleServiceName);
                if (roleService.getRolesForUser(username) != null
                        && !roleService.getRolesForUser(username).isEmpty()) {

                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(
                                Level.FINE,
                                "RoleService ["
                                        + roleServiceName
                                        + "] matching for User ["
                                        + username
                                        + "]");
                    }

                    return true;
                }
            }

            for (String roleServiceName : securityManager.listRoleServices()) {
                SortedSet<GeoServerRole> userRoles =
                        securityManager.loadRoleService(roleServiceName).getRolesForUser(username);
                if (userRoles != null && !userRoles.isEmpty()) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }

        logger.log(
                Level.FINER,
                "GeoFence was not able to find any matching user on Security Context or Services.");

        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean existsRole(String rolename) {
        try {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(
                        Level.FINE,
                        "Checking Role ["
                                + rolename
                                + "] on ActiveRoleService ["
                                + getDefaultSecurityService()
                                + "]");
            }

            if (getDefaultSecurityService() instanceof GeoServerRoleService) {
                if (((GeoServerRoleService) getDefaultSecurityService()).getRoleByName(rolename)
                        != null) {
                    return true;
                }
            }

            return securityManager.getActiveRoleService().getRoleByName(rolename) != null;
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Set<String> getRoles(String username) {
        try {
            SortedSet<GeoServerRole> roleSet = new TreeSet<GeoServerRole>();
            SortedSet<String> stringSet = new TreeSet<String>();
            if (getDefaultSecurityService() instanceof GeoServerRoleService) {
                roleSet =
                        ((GeoServerRoleService) getDefaultSecurityService())
                                .getRolesForUser(username);
            }

            for (GeoServerRole role : roleSet) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(
                            Level.FINE,
                            "Checking Role ["
                                    + role
                                    + "] on ActiveRoleService ["
                                    + getDefaultSecurityService()
                                    + "]");
                }

                stringSet.add(role.getAuthority());
            }

            roleSet = securityManager.getActiveRoleService().getRolesForUser(username);

            for (GeoServerRole role : roleSet) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(
                            Level.FINE,
                            "Checking Role ["
                                    + role
                                    + "] on ActiveRoleService ["
                                    + securityManager.getActiveRoleService()
                                    + "]");
                }

                stringSet.add(role.getAuthority());
            }

            try {
                // Search for derived roles, the ones assigned through groups
                for (String serviceName : securityManager.listUserGroupServices()) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Checking UserGroupService [" + serviceName + "]");
                    }

                    final GeoServerUserGroupService userGroupService =
                            securityManager.loadUserGroupService(serviceName);
                    if (userGroupService.getUserByUsername(username) != null) {

                        RoleCalculator calc = null;
                        if (getDefaultSecurityService() instanceof GeoServerRoleService) {
                            calc =
                                    new RoleCalculator(
                                            userGroupService,
                                            (GeoServerRoleService) getDefaultSecurityService());
                        }

                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(
                                    Level.FINE,
                                    "UserGroupService ["
                                            + serviceName
                                            + "] matching for User ["
                                            + username
                                            + "]");
                        }

                        GeoServerUser user = userGroupService.getUserByUsername(username);
                        if (calc != null) {
                            for (GeoServerUserGroup group :
                                    userGroupService.getGroupsForUser(user)) {
                                if (group.isEnabled()) {
                                    for (GeoServerRole role : calc.calculateRoles(group)) {
                                        stringSet.add(role.getAuthority());
                                    }
                                }
                            }
                        }

                        calc =
                                new RoleCalculator(
                                        userGroupService, securityManager.getActiveRoleService());
                        if (calc != null) {
                            for (GeoServerUserGroup group :
                                    userGroupService.getGroupsForUser(user)) {
                                if (group.isEnabled()) {
                                    for (GeoServerRole role : calc.calculateRoles(group)) {
                                        stringSet.add(role.getAuthority());
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.log(
                        Level.FINE,
                        "Matching Roles [" + stringSet + "] for User [" + username + "]");
            }

            return stringSet;
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return Collections.emptySet();
        }
    }
}

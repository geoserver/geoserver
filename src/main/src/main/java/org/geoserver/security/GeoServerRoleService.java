/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import org.geoserver.security.event.RoleLoadedEvent;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.GeoServerRole;

/**
 * A class implementing this interface is capable of reading role assignments from a backend.
 *
 * @author christian
 */
public interface GeoServerRoleService extends GeoServerSecurityService {

    /**
     * Creates the granted authority store associated with this service, or null if creating a store
     * is not supported.
     *
     * <p>Implementations that do not support a store should ensure that {@link #canCreateStore()}
     * returns <code>false</code>.
     */
    GeoServerRoleStore createStore() throws IOException;

    /** Register for notifications on load */
    void registerRoleLoadedListener(RoleLoadedListener listener);

    /** Unregister for notifications on load */
    void unregisterRoleLoadedListener(RoleLoadedListener listener);

    /**
     * Get group names for a {@link GeoServerRole} object Hierarchical roles are not considered
     *
     * @return collection which cannot be modified
     */
    SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException;

    /**
     * Get user names for a {@link GeoServerRole} object Hierarchical roles are not considered
     *
     * @return collection which cannot be modified
     */
    SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException;

    /**
     * Get the roles for the user Hierarchical roles are not considered
     *
     * @return a collection which cannot be modified
     */
    SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException;

    /**
     * Get the roles for the group Hierarchical roles are not considered
     *
     * @return a collection which cannot be modified
     */
    SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException;

    /**
     * Get the list of roles currently known by users (implementations must provide the admin role
     * "ROLE_ADMINISTRATOR")
     *
     * @return a collection which cannot be modified
     */
    SortedSet<GeoServerRole> getRoles() throws IOException;

    /**
     * returns a role name -> parent role name mapping for the all {@link GeoServerRole} objects.
     *
     * <p>This method should be used by clients if they have to build a tree structure
     *
     * @return a collection which cannot be modified
     */
    Map<String, String> getParentMappings() throws IOException;

    /**
     * Creates a {@link GeoServerRole} object . Implementations can use their special classes
     * derived from {@link GeoServerRole}
     */
    GeoServerRole createRoleObject(String role) throws IOException;

    /**
     * Get the parent {@link GeoServerRole} object
     *
     * @return the parent role or null
     */
    GeoServerRole getParentRole(GeoServerRole role) throws IOException;

    /**
     * Loads a {@link GeoServerRole} by name
     *
     * @throws null if the role is not found
     */
    GeoServerRole getRoleByName(String role) throws IOException;

    /** load from backend store. On success, a {@link RoleLoadedEvent} should must be triggered */
    void load() throws IOException;

    /**
     * This is a callback for personalized roles Example: Role employee has a property
     * "employeeNumber", which has no value or a default value. "employeeNumber" is also called a
     * role parameter in this context.
     *
     * <p>A user "harry" has assigned the role employee and has a user property "empNr" with the
     * value 4711
     *
     * <p>Now, this method should create a {@link Properties} object containing the the property
     * "employeeNumber" with the value 4711.
     *
     * <p>A GIS example could be a BBOX for specific user to restrict his access to the wms service
     *
     * @param roleName the name of the role
     * @param roleParams the params for the role from {@link GeoServerRoleService}
     * @param userName the user name
     * @param userProps the properties of the user from {@link GeoServerUserGroupService}
     * @return null for no personalization, the personalized properties otherwise
     */
    Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps)
            throws IOException;

    /**
     * @return the local role having the same privileges as {@link GeoserverRole#ADMIN_ROLE} or
     *     <code>null</code> if no such role exists
     */
    GeoServerRole getAdminRole();

    /**
     * @return the local role having the same privileges {@link GeoServerRole#GROUP_ADMIN_ROLE} or
     *     <code>null</code> if no such role exists
     */
    GeoServerRole getGroupAdminRole();

    /** @return the number of roles */
    int getRoleCount() throws IOException;
}

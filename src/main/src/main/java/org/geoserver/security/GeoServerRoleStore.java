/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import org.geoserver.security.impl.GeoServerRole;

/**
 * A class implementing this interface is capable of storing roles to a backend. The store always
 * operates on a {@link GeoServerRoleService} object.
 *
 * @author christian
 */
public interface GeoServerRoleStore extends GeoServerRoleService {

    /** Initializes itself from a service for future store modifications concerning this service */
    void initializeFromService(GeoServerRoleService service) throws IOException;

    /** discards all entries */
    void clear() throws IOException;

    /** Adds a role */
    void addRole(GeoServerRole role) throws IOException;

    /** Updates a role */
    void updateRole(GeoServerRole role) throws IOException;

    /** Removes the specified {@link GeoServerRole} role */
    boolean removeRole(GeoServerRole role) throws IOException;

    /** Associates a role with a group. */
    void associateRoleToGroup(GeoServerRole role, String groupname) throws IOException;

    /** Disassociates a role from a group. */
    void disAssociateRoleFromGroup(GeoServerRole role, String groupname) throws IOException;

    /** Associates a role with a user, */
    void associateRoleToUser(GeoServerRole role, String username) throws IOException;

    /** Disassociates a role from a user. */
    void disAssociateRoleFromUser(GeoServerRole role, String username) throws IOException;

    /**
     * Synchronizes all changes with the backend store. On success, the associated service object
     * should be reloaded
     */
    abstract void store() throws IOException;

    /**
     * returns true if there are pending modifications not written to the backend store
     *
     * @return true/false
     */
    boolean isModified();

    /**
     * Sets the parent role, the method must check if parentRole is not equal to role and if
     * parentRole is not contained in the descendants of role
     *
     * <p>This code sequence will do the job <code>
     *   RoleHierarchyHelper helper = new RoleHierarchyHelper(getParentMappings());
     *   if (helper.isValidParent(role.getAuthority(),
     *           parentRole==null ? null : parentRole.getAuthority())==false)
     *       throw new IOException(parentRole.getAuthority() +
     *               " is not a valid parent for " + role.getAuthority());
     * </code>
     *
     * @param parentRole may be null to remove a parent
     */
    void setParentRole(GeoServerRole role, GeoServerRole parentRole) throws IOException;
}

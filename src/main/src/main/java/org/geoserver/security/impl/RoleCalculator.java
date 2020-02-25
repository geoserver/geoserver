/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerUserGroupService;

/**
 * Helper Object for role calculations
 *
 * @author christian
 */
public class RoleCalculator {

    protected GeoServerRoleService roleService;
    protected GeoServerUserGroupService userGroupService;

    /** Constructor */
    public RoleCalculator(GeoServerRoleService roleService) {
        this(null, roleService);
    }

    /** Constructor */
    public RoleCalculator(
            GeoServerUserGroupService userGroupService, GeoServerRoleService roleService) {
        this.userGroupService = userGroupService;
        this.roleService = roleService;
        assertRoleServiceNotNull();
    }

    public void setRoleService(GeoServerRoleService service) {
        roleService = service;
        assertRoleServiceNotNull();
    }

    public GeoServerRoleService getRoleService() {
        return roleService;
    }

    public void setUserGroupService(GeoServerUserGroupService service) {
        userGroupService = service;
    }

    public GeoServerUserGroupService getUserGroupService() {
        return userGroupService;
    }

    /** Check if the role service is not null */
    protected void assertRoleServiceNotNull() {
        if (roleService == null) {
            throw new RuntimeException("role service Service is null");
        }
    }

    /** Convenience method for {@link #calculateRoles(GeoServerUser)} */
    public SortedSet<GeoServerRole> calculateRoles(String username) throws IOException {
        return calculateRoles(new GeoServerUser(username));
    }

    /**
     * Calculate the {@link GeoServerRole} objects for a user
     *
     * <p>The algorithm
     *
     * <p>get the roles directly assigned to the user get the groups of the user if a {@link
     * GeoServerUserGroupService} service is given, for each "enabled" group, add the roles of the
     * group
     *
     * <p>for earch role search for ancestor roles and add them to the set
     *
     * <p>After role calculation has finished, personalize each role with role attributes if
     * necessary
     *
     * <p>If the user has the admin role of the active role service, {@link
     * GeoServerRole#ADMIN_ROLE} is also included in the set.
     */
    public SortedSet<GeoServerRole> calculateRoles(GeoServerUser user) throws IOException {

        Set<GeoServerRole> set1 = new HashSet<GeoServerRole>();

        // alle roles for the user
        set1.addAll(getRoleService().getRolesForUser(user.getUsername()));
        addInheritedRoles(set1);

        // add all roles for enabled groups
        if (getUserGroupService() != null) {
            for (GeoServerUserGroup group : getUserGroupService().getGroupsForUser(user)) {
                if (group.isEnabled()) set1.addAll(calculateRoles(group));
            }
        }

        // personalize roles
        SortedSet<GeoServerRole> set2 = personalizeRoles(user, set1);

        // add mapped system roles
        addMappedSystemRoles(set2);

        return set2;
    }

    public void addMappedSystemRoles(Collection<GeoServerRole> set) {
        // if the user has the admin role of the role service the
        // GeoserverRole.ADMIN_ROLE must also be in the set
        GeoServerRole adminRole = roleService.getAdminRole();
        if (adminRole != null && set.contains(adminRole)) {
            set.add(GeoServerRole.ADMIN_ROLE);
        }

        // if the user has the group admin role of the role service the
        // GeoserverRole.ADMIN_ROLE must also be in the set
        GeoServerRole groupAdminRole = roleService.getGroupAdminRole();
        if (groupAdminRole != null && set.contains(groupAdminRole)) {
            set.add(GeoServerRole.GROUP_ADMIN_ROLE);
        }
    }

    /** Collects the ascendents for a {@link GeoServerRole} object */
    protected void addParentRole(GeoServerRole role, Collection<GeoServerRole> inherited)
            throws IOException {
        GeoServerRole parentRole = getRoleService().getParentRole(role);
        if (parentRole == null) return; // end of recursion

        if (inherited.contains(parentRole)) return; // end of recursion

        inherited.add(parentRole);
        // recursion
        addParentRole(parentRole, inherited);
    }

    /** Calculate the {@link GeoServerRole} objects for a group including inherited roles */
    public SortedSet<GeoServerRole> calculateRoles(GeoServerUserGroup group) throws IOException {

        SortedSet<GeoServerRole> roles = new TreeSet<GeoServerRole>();
        roles.addAll(getRoleService().getRolesForGroup(group.getGroupname()));
        addInheritedRoles(roles);
        return roles;
    }

    /** Adds inherited roles to a role set */
    public void addInheritedRoles(Collection<GeoServerRole> coll) throws IOException {
        Set<GeoServerRole> inherited = new HashSet<GeoServerRole>();
        for (GeoServerRole role : coll) addParentRole(role, inherited);
        coll.addAll(inherited);
    }

    /**
     * Takes the role set for a user and personalizes the roles (matching user properties and role
     * parameters)
     */
    public SortedSet<GeoServerRole> personalizeRoles(
            GeoServerUser user, Collection<GeoServerRole> roles) throws IOException {
        SortedSet<GeoServerRole> set = new TreeSet<GeoServerRole>();
        for (GeoServerRole role : roles) {
            Properties personalizedProps =
                    getRoleService()
                            .personalizeRoleParams(
                                    role.getAuthority(), role.getProperties(),
                                    user.getUsername(), user.getProperties());
            if (personalizedProps == null) {
                set.add(role);
            } else { // create personalized role
                GeoServerRole pRole = getRoleService().createRoleObject(role.getAuthority());
                pRole.setUserName(user.getUsername());
                for (Object key : personalizedProps.keySet())
                    pRole.getProperties().put(key, personalizedProps.get(key));
                set.add(pRole);
            }
        }
        return set;
    }
}

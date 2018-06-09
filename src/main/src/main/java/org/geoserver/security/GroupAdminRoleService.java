/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import org.geoserver.security.impl.GeoServerRole;

/**
 * Role service wrapper that filters contents based on an authenticated group administrator.
 *
 * <p>Given a group administrator this wrapper will filter out those groups which the administrator
 * does not have administrative access to.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GroupAdminRoleService extends AuthorizingRoleService {

    /** groups the user admins, lazily calculated */
    List<String> groups;

    public GroupAdminRoleService(GeoServerRoleService delegate, List<String> groups) {
        super(delegate);
        this.groups = groups;
    }

    public boolean canCreateStore() {
        return false;
    }

    public GeoServerRoleStore createStore() throws IOException {
        return null;
    }

    @Override
    protected SortedSet<String> filterGroups(
            GeoServerRole role, SortedSet<String> groupNamesForRole) {
        // include only those groups which the user is admin for
        for (Iterator<String> it = groupNamesForRole.iterator(); it.hasNext(); ) {
            if (filterGroup(it.next())) {
                it.remove();
            }
        }
        return groupNamesForRole;
    }

    @Override
    protected boolean filterGroup(String groupname) {
        return !groups.contains(groupname);
    }

    @Override
    protected SortedSet<String> filterUsers(
            GeoServerRole role, SortedSet<String> userNamesForRole) {
        return userNamesForRole;
    }

    @Override
    protected boolean filterUser(String username) {
        return false;
    }

    @Override
    protected SortedSet<GeoServerRole> filterUserRoles(
            String username, SortedSet<GeoServerRole> rolesForUser) {
        return rolesForUser;
    }

    @Override
    protected SortedSet<GeoServerRole> filterGroupRoles(
            String groupname, SortedSet<GeoServerRole> rolesForGroup) {
        return rolesForGroup;
    }

    @Override
    protected SortedSet<GeoServerRole> filterRoles(SortedSet<GeoServerRole> roles) {
        roles.remove(delegate.getAdminRole());
        return roles;
    }

    @Override
    protected Map<String, String> filterParentMappings(Map<String, String> parentMappings) {
        return parentMappings;
    }

    @Override
    protected GeoServerRole filterRole(GeoServerRole role) {
        if (role == delegate.getAdminRole()) {
            return null;
        }
        return null;
    }
}

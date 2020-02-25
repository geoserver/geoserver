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
import java.util.TreeSet;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.GeoServerRole;

/**
 * Role service wrapper that filters contents from the underlying role service.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class AuthorizingRoleService implements GeoServerRoleStore {

    protected GeoServerRoleService delegate;

    public GeoServerRoleService getDelegate() {
        return delegate;
    }

    protected AuthorizingRoleService(GeoServerRoleService delegate) {
        this.delegate = delegate;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        delegate.initializeFromConfig(config);
    }

    @Override
    public boolean canCreateStore() {
        return delegate.canCreateStore();
    }

    @Override
    public GeoServerRoleStore createStore() throws IOException {
        try {
            return getClass()
                    .getConstructor(GeoServerRoleService.class)
                    .newInstance(delegate.createStore());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        delegate.setSecurityManager(securityManager);
    }

    @Override
    public GeoServerSecurityManager getSecurityManager() {
        return delegate.getSecurityManager();
    }

    @Override
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        delegate.registerRoleLoadedListener(listener);
    }

    @Override
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        delegate.unregisterRoleLoadedListener(listener);
    }

    @Override
    public void load() throws IOException {
        delegate.load();
    }

    @Override
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        return filterGroups(role, new TreeSet(delegate.getGroupNamesForRole(role)));
    }

    @Override
    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        return filterUsers(role, new TreeSet(delegate.getUserNamesForRole(role)));
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        return filterUserRoles(username, new TreeSet(delegate.getRolesForUser(username)));
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        return filterGroupRoles(groupname, new TreeSet(delegate.getRolesForGroup(groupname)));
    }

    @Override
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        return filterRoles(new TreeSet(delegate.getRoles()));
    }

    @Override
    public Map<String, String> getParentMappings() throws IOException {
        return filterParentMappings(delegate.getParentMappings());
    }

    @Override
    public GeoServerRole createRoleObject(String role) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        return filterRole(delegate.getParentRole(role));
    }

    @Override
    public GeoServerRole getRoleByName(String role) throws IOException {
        return filterRole(delegate.getRoleByName(role));
    }

    @Override
    public Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps)
            throws IOException {
        return delegate.personalizeRoleParams(roleName, roleParams, userName, userProps);
    }

    @Override
    public GeoServerRole getAdminRole() {
        return filterRole(delegate.getAdminRole());
    }

    @Override
    public GeoServerRole getGroupAdminRole() {
        return filterRole(delegate.getGroupAdminRole());
    }

    @Override
    public int getRoleCount() throws IOException {
        // can't optimize since we might be filtering out roles,
        // TODO: give the subclass the choice
        return getRoles().size();
    }

    //
    // GeoServerRoleStore methods
    //

    protected GeoServerRoleStore delegateAsStore() {
        return (GeoServerRoleStore) delegate;
    }

    @Override
    public void initializeFromService(GeoServerRoleService service) throws IOException {
        delegateAsStore().initializeFromService(((AuthorizingRoleService) service).getDelegate());
    }

    @Override
    public void store() throws IOException {
        delegateAsStore().store();
    }

    @Override
    public boolean isModified() {
        return delegateAsStore().isModified();
    }

    @Override
    public void clear() throws IOException {
        delegateAsStore().clear();
    }

    @Override
    public void addRole(GeoServerRole role) throws IOException {
        if (filterRole(role) != null) {
            delegateAsStore().addRole(role);
        }
    }

    @Override
    public void updateRole(GeoServerRole role) throws IOException {
        if (filterRole(role) != null) {
            delegateAsStore().updateRole(role);
        }
    }

    @Override
    public boolean removeRole(GeoServerRole role) throws IOException {
        if (filterRole(role) != null) {
            return delegateAsStore().removeRole(role);
        }
        return false;
    }

    @Override
    public void associateRoleToGroup(GeoServerRole role, String groupname) throws IOException {
        if (filterRole(role) != null && !filterGroup(groupname)) {
            delegateAsStore().associateRoleToGroup(role, groupname);
        }
    }

    @Override
    public void disAssociateRoleFromGroup(GeoServerRole role, String groupname) throws IOException {
        if (filterRole(role) != null && !filterGroup(groupname)) {
            delegateAsStore().disAssociateRoleFromGroup(role, groupname);
        }
    }

    @Override
    public void associateRoleToUser(GeoServerRole role, String username) throws IOException {
        if (filterRole(role) != null && !filterUser(username)) {
            delegateAsStore().associateRoleToUser(role, username);
        }
    }

    @Override
    public void disAssociateRoleFromUser(GeoServerRole role, String username) throws IOException {
        if (filterRole(role) != null && !filterUser(username)) {
            delegateAsStore().disAssociateRoleFromUser(role, username);
        }
    }

    @Override
    public void setParentRole(GeoServerRole role, GeoServerRole parentRole) throws IOException {
        if (filterRole(role) != null && filterRole(parentRole) != null) {
            delegateAsStore().setParentRole(role, parentRole);
        }
    }

    protected abstract SortedSet<String> filterGroups(
            GeoServerRole role, SortedSet<String> groupNamesForRole);

    protected abstract boolean filterGroup(String groupname);

    protected abstract SortedSet<String> filterUsers(
            GeoServerRole role, SortedSet<String> userNamesForRole);

    protected abstract boolean filterUser(String username);

    protected abstract SortedSet<GeoServerRole> filterUserRoles(
            String username, SortedSet<GeoServerRole> rolesForUser);

    protected abstract SortedSet<GeoServerRole> filterGroupRoles(
            String groupname, SortedSet<GeoServerRole> rolesForGroup);

    protected abstract SortedSet<GeoServerRole> filterRoles(SortedSet<GeoServerRole> roles);

    protected abstract Map<String, String> filterParentMappings(Map<String, String> parentMappings);

    protected abstract GeoServerRole filterRole(GeoServerRole parentRole);
}

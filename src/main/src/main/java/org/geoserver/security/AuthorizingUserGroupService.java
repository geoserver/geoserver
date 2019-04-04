/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.UserGroupLoadedListener;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * User group service wrapper that filters contents from the underlying user group service.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class AuthorizingUserGroupService implements GeoServerUserGroupStore {

    protected GeoServerUserGroupService delegate;

    protected AuthorizingUserGroupService(GeoServerUserGroupService delegate) {
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
    public GeoServerUserGroupStore createStore() throws IOException {
        try {
            return getClass()
                    .getConstructor(GeoServerUserGroupService.class)
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
    public void registerUserGroupLoadedListener(UserGroupLoadedListener listener) {
        delegate.registerUserGroupLoadedListener(listener);
    }

    @Override
    public void unregisterUserGroupLoadedListener(UserGroupLoadedListener listener) {
        delegate.unregisterUserGroupLoadedListener(listener);
    }

    @Override
    public String getPasswordEncoderName() {
        return delegate.getPasswordEncoderName();
    }

    @Override
    public String getPasswordValidatorName() {
        return delegate.getPasswordValidatorName();
    }

    @Override
    public void load() throws IOException {
        delegate.load();
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
        return filterUser((GeoServerUser) delegate.loadUserByUsername(username));
    }

    @Override
    public GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException {
        return filterGroup(delegate.getGroupByGroupname(groupname));
    }

    @Override
    public GeoServerUser getUserByUsername(String username) throws IOException {
        return filterUser(delegate.getUserByUsername(username));
    }

    @Override
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        return filterUser(delegate.createUserObject(username, password, isEnabled));
    }

    @Override
    public GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled)
            throws IOException {
        return filterGroup(delegate.createGroupObject(groupname, isEnabled));
    }

    @Override
    public SortedSet<GeoServerUser> getUsers() throws IOException {
        return filterUsers(new TreeSet<GeoServerUser>(delegate.getUsers()));
    }

    @Override
    public SortedSet<GeoServerUserGroup> getUserGroups() throws IOException {
        return filterGroups(new TreeSet<GeoServerUserGroup>(delegate.getUserGroups()));
    }

    @Override
    public SortedSet<GeoServerUser> getUsersForGroup(GeoServerUserGroup group) throws IOException {
        return filterUsers(new TreeSet<GeoServerUser>(delegate.getUsersForGroup(group)));
    }

    @Override
    public SortedSet<GeoServerUserGroup> getGroupsForUser(GeoServerUser user) throws IOException {
        return filterGroups(new TreeSet<GeoServerUserGroup>(delegate.getGroupsForUser(user)));
    }

    @Override
    public int getUserCount() throws IOException {
        return getUsers().size();
    }

    @Override
    public int getGroupCount() throws IOException {
        return getUserGroups().size();
    }

    //
    // GeoServerUserGroupStore interface
    //

    protected GeoServerUserGroupStore delegateAsStore() {
        return (GeoServerUserGroupStore) delegate;
    }

    @Override
    public void initializeFromService(GeoServerUserGroupService service) throws IOException {
        delegateAsStore().initializeFromService(service);
    }

    @Override
    public void clear() throws IOException {
        delegateAsStore().clear();
    }

    @Override
    public void store() throws IOException {
        delegateAsStore().store();
    }

    @Override
    public void addUser(GeoServerUser user) throws IOException, PasswordPolicyException {
        if (filterUser(user) != null) {
            delegateAsStore().addUser(user);
        }
    }

    @Override
    public void updateUser(GeoServerUser user) throws IOException, PasswordPolicyException {
        if (filterUser(user) != null) {
            delegateAsStore().updateUser(user);
        }
    }

    @Override
    public boolean removeUser(GeoServerUser user) throws IOException {
        if (filterUser(user) != null) {
            return delegateAsStore().removeUser(user);
        }
        return false;
    }

    @Override
    public void addGroup(GeoServerUserGroup group) throws IOException {
        if (filterGroup(group) != null) {
            delegateAsStore().addGroup(group);
        }
    }

    @Override
    public void updateGroup(GeoServerUserGroup group) throws IOException {
        if (filterGroup(group) != null) {
            delegateAsStore().updateGroup(group);
        }
    }

    @Override
    public boolean removeGroup(GeoServerUserGroup group) throws IOException {
        if (filterGroup(group) != null) {
            return delegateAsStore().removeGroup(group);
        }
        return false;
    }

    @Override
    public void associateUserToGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {
        // TODO: should probably throw exception if trying to add to filtered group
        if (filterUser(user) != null && filterGroup(group) != null) {
            delegateAsStore().associateUserToGroup(user, group);
        }
    }

    @Override
    public void disAssociateUserFromGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {
        // TODO: should probably throw exception if trying to add to filtered group
        if (filterUser(user) != null && filterGroup(group) != null) {
            delegateAsStore().disAssociateUserFromGroup(user, group);
        }
    }

    @Override
    public boolean isModified() {
        return delegateAsStore().isModified();
    }

    @Override
    public SortedSet<GeoServerUser> getUsersHavingProperty(String propname) throws IOException {
        return filterUsers(new TreeSet<GeoServerUser>(delegate.getUsersHavingProperty(propname)));
    }

    @Override
    public int getUserCountHavingProperty(String propname) throws IOException {
        return getUsersHavingProperty(propname).size();
    }

    @Override
    public SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) throws IOException {
        return filterUsers(
                new TreeSet<GeoServerUser>(delegate.getUsersNotHavingProperty(propname)));
    }

    @Override
    public int getUserCountNotHavingProperty(String propname) throws IOException {
        return getUsersNotHavingProperty(propname).size();
    }

    @Override
    public SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        return filterUsers(
                new TreeSet<GeoServerUser>(
                        delegate.getUsersHavingPropertyValue(propname, propvalue)));
    }

    @Override
    public int getUserCountHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        return getUsersHavingPropertyValue(propname, propvalue).size();
    }

    protected abstract GeoServerUser filterUser(GeoServerUser user);

    protected abstract GeoServerUserGroup filterGroup(GeoServerUserGroup group);

    protected SortedSet<GeoServerUser> filterUsers(SortedSet<GeoServerUser> users) {
        for (Iterator<GeoServerUser> it = users.iterator(); it.hasNext(); ) {
            if (filterUser(it.next()) == null) {
                it.remove();
            }
        }
        return users;
    }

    protected SortedSet<GeoServerUserGroup> filterGroups(SortedSet<GeoServerUserGroup> groups) {
        for (Iterator<GeoServerUserGroup> it = groups.iterator(); it.hasNext(); ) {
            if (filterGroup(it.next()) == null) {
                it.remove();
            }
        }
        return groups;
    }
}

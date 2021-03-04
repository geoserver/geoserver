/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.validation;

import java.io.IOException;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;

/**
 * This class is a validation wrapper for {@link GeoServerUserGroupStore}
 *
 * <p>Usage: <code>
 * GeoserverUserGroupStore valStore = new UserGroupStoreValidationWrapper(store);
 * valStore.addUser(..);
 * valStore.store()
 * </code> Since the {@link GeoServerUserGroupStore} interface does not allow to throw {@link
 * UserGroupServiceException} objects directly, these objects a wrapped into an IOException. Use
 * {@link IOException#getCause()} to get the proper exception.
 *
 * @author christian
 */
public class UserGroupStoreValidationWrapper extends UserGroupServiceValidationWrapper
        implements GeoServerUserGroupStore {

    /** Creates a wrapper object. */
    public UserGroupStoreValidationWrapper(GeoServerUserGroupStore store) {
        super(store);
    }

    GeoServerUserGroupStore getStore() {
        return (GeoServerUserGroupStore) service;
    }

    @Override
    public void initializeFromService(GeoServerUserGroupService service) throws IOException {
        getStore().initializeFromService(service);
    }

    @Override
    public void clear() throws IOException {
        getStore().clear();
    }

    @Override
    public void addUser(GeoServerUser user) throws IOException, PasswordPolicyException {
        checkNotExistingUserName(user.getUsername());
        getStore().addUser(user);
    }

    @Override
    public void updateUser(GeoServerUser user) throws IOException, PasswordPolicyException {
        checkExistingUserName(user.getUsername());
        getStore().updateUser(user);
    }

    @Override
    public boolean removeUser(GeoServerUser user) throws IOException {
        return getStore().removeUser(user);
    }

    @Override
    public void addGroup(GeoServerUserGroup group) throws IOException {
        checkNotExistingGroupName(group.getGroupname());
        getStore().addGroup(group);
    }

    @Override
    public void updateGroup(GeoServerUserGroup group) throws IOException {
        checkExistingGroupName(group.getGroupname());
        getStore().updateGroup(group);
    }

    @Override
    public boolean removeGroup(GeoServerUserGroup group) throws IOException {
        return getStore().removeGroup(group);
    }

    @Override
    public void store() throws IOException {
        getStore().store();
    }

    @Override
    public void associateUserToGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {
        checkExistingUserName(user.getUsername());
        checkExistingGroupName(group.getGroupname());
        getStore().associateUserToGroup(user, group);
    }

    @Override
    public void disAssociateUserFromGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {
        checkExistingUserName(user.getUsername());
        checkExistingGroupName(group.getGroupname());
        getStore().disAssociateUserFromGroup(user, group);
    }

    @Override
    public boolean isModified() {
        return getStore().isModified();
    }
}

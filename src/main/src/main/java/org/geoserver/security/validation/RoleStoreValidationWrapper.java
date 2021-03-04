/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.validation;

import java.io.IOException;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.impl.GeoServerRole;

/**
 * This class is a validation wrapper for {@link GeoServerRoleStore}
 *
 * <p>Usage: <code>
 * GeoserverRoleStore valStore = new RoleStoreValidationWrapper(store);
 * valStore.addRole(..);
 * valStore.store()
 * </code> Since the {@link GeoServerRoleStore} interface does not allow to throw {@link
 * RoleServiceException} objects directly, these objects a wrapped into an IOException. Use {@link
 * IOException#getCause()} to get the proper exception.
 *
 * @author christian
 */
public class RoleStoreValidationWrapper extends RoleServiceValidationWrapper
        implements GeoServerRoleStore {

    /** @see RoleServiceValidationWrapper */
    public RoleStoreValidationWrapper(
            GeoServerRoleStore store,
            boolean checkAgainstRules,
            GeoServerUserGroupService... services) {
        super(store, checkAgainstRules, services);
    }

    /** @see RoleServiceValidationWrapper */
    public RoleStoreValidationWrapper(
            GeoServerRoleStore store, GeoServerUserGroupService... services) {
        super(store, services);
    }

    GeoServerRoleStore getStore() {
        return (GeoServerRoleStore) service;
    }

    @Override
    public void initializeFromService(GeoServerRoleService aService) throws IOException {
        getStore().initializeFromService(aService);
    }

    @Override
    public void clear() throws IOException {
        getStore().clear();
    }

    @Override
    public void addRole(GeoServerRole role) throws IOException {
        checkReservedNames(role.getAuthority());
        checkNotExistingRoleName(role.getAuthority());
        checkNotExistingInOtherServices(role.getAuthority());
        getStore().addRole(role);
    }

    @Override
    public void updateRole(GeoServerRole role) throws IOException {
        checkExistingRoleName(role.getAuthority());
        getStore().updateRole(role);
    }

    @Override
    public boolean removeRole(GeoServerRole role) throws IOException {
        checkRoleIsMapped(role);
        checkRoleIsUsed(role);
        return getStore().removeRole(role);
    }

    @Override
    public void associateRoleToGroup(GeoServerRole role, String groupname) throws IOException {
        checkExistingRoleName(role.getAuthority());
        checkValidGroupName(groupname);
        getStore().associateRoleToGroup(role, groupname);
    }

    @Override
    public void disAssociateRoleFromGroup(GeoServerRole role, String groupname) throws IOException {
        checkExistingRoleName(role.getAuthority());
        checkValidGroupName(groupname);
        getStore().disAssociateRoleFromGroup(role, groupname);
    }

    @Override
    public void associateRoleToUser(GeoServerRole role, String username) throws IOException {
        checkExistingRoleName(role.getAuthority());
        checkValidUserName(username);
        getStore().associateRoleToUser(role, username);
    }

    @Override
    public void disAssociateRoleFromUser(GeoServerRole role, String username) throws IOException {
        checkExistingRoleName(role.getAuthority());
        checkValidUserName(username);
        getStore().disAssociateRoleFromUser(role, username);
    }

    @Override
    public void store() throws IOException {
        getStore().store();
    }

    @Override
    public boolean isModified() {
        return getStore().isModified();
    }

    @Override
    public void setParentRole(GeoServerRole role, GeoServerRole parentRole) throws IOException {
        checkExistingRoleName(role.getAuthority());
        if (parentRole != null) checkExistingRoleName(parentRole.getAuthority());
        getStore().setParentRole(role, parentRole);
    }
}

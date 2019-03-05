/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.validation.RoleStoreValidationWrapper;
import org.geoserver.security.validation.UserGroupStoreValidationWrapper;

/** Allows editing an existing user */
public class EditUserPage extends AbstractUserPage {

    public EditUserPage(String userGroupServiceName, GeoServerUser user) {
        super(userGroupServiceName, user);
        get("form:username").setEnabled(false);
    }

    @Override
    protected void onFormSubmit(GeoServerUser user) throws IOException, PasswordPolicyException {

        GeoServerUserGroupService ugService = getUserGroupService(ugServiceName);
        GeoServerUserGroupStore ugStore = null;
        try {
            if (ugService.canCreateStore()) {
                ugStore = new UserGroupStoreValidationWrapper(ugService.createStore());

                Set<GeoServerUserGroup> orig = ugStore.getGroupsForUser(user);
                Set<GeoServerUserGroup> add = new HashSet<GeoServerUserGroup>();
                Set<GeoServerUserGroup> remove = new HashSet<GeoServerUserGroup>();
                userGroupPalette.diff(orig, add, remove);

                ugStore.updateUser(user);

                for (GeoServerUserGroup g : add) ugStore.associateUserToGroup(user, g);
                for (GeoServerUserGroup g : remove) ugStore.disAssociateUserFromGroup(user, g);

                ugStore.store();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, ex.getLocalizedMessage(), ex);
            try {
                if (ugStore != null) ugStore.load();
            } catch (IOException ex2) {
            }
            throw ex;
        } catch (PasswordPolicyException ex) {
            try {
                ugStore.load();
            } catch (IOException ex2) {
            }
            throw ex;
        }

        GeoServerRoleStore roleStore = null;
        try {
            if (hasRoleStore(getSecurityManager().getActiveRoleService().getName())) {
                roleStore = getRoleStore(getSecurityManager().getActiveRoleService().getName());
                roleStore = new RoleStoreValidationWrapper(roleStore);

                Set<GeoServerRole> orig = roleStore.getRolesForUser(user.getUsername());
                Set<GeoServerRole> add = new HashSet<GeoServerRole>();
                Set<GeoServerRole> remove = new HashSet<GeoServerRole>();
                rolePalette.diff(orig, add, remove);

                for (GeoServerRole role : add) {
                    roleStore.associateRoleToUser(role, user.getUsername());
                }
                for (GeoServerRole role : remove) {
                    roleStore.disAssociateRoleFromUser(role, user.getUsername());
                }
                roleStore.store();
            }
        } catch (IOException ex) {
            try {
                if (roleStore != null) roleStore.load();
            } catch (IOException ex2) {
            }
            throw ex;
        }
    }
}

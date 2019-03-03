/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import java.io.IOException;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;
import org.geoserver.security.validation.RoleStoreValidationWrapper;
import org.geoserver.security.validation.UserGroupStoreValidationWrapper;

/** Allows creation of a new user in users.properties */
public class NewUserPage extends AbstractUserPage {

    public NewUserPage(String userGroupServiceName) {
        super(userGroupServiceName, new GeoServerUser(""));

        if (hasUserGroupStore(userGroupServiceName) == false) {
            throw new IllegalStateException("New user not possible for read only service");
        }
    }

    @Override
    protected void onFormSubmit(GeoServerUser user) throws IOException, PasswordPolicyException {
        GeoServerUserGroupStore ugStore =
                new UserGroupStoreValidationWrapper(getUserGroupStore(ugServiceName));
        try {
            ugStore.addUser(user);

            for (GeoServerUserGroup group : userGroupPalette.getSelectedGroups()) {
                ugStore.associateUserToGroup(user, group);
            }
            ugStore.store();

        } catch (IOException ex) {
            try {
                ugStore.load();
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

        GeoServerRoleStore gaStore = null;
        try {
            if (hasRoleStore(getSecurityManager().getActiveRoleService().getName())) {
                gaStore = getRoleStore(getSecurityManager().getActiveRoleService().getName());
                gaStore = new RoleStoreValidationWrapper(gaStore);
                for (GeoServerRole role : rolePalette.getSelectedRoles()) {
                    gaStore.associateRoleToUser(role, user.getUsername());
                }
                gaStore.store();
            }
        } catch (IOException ex) {
            try {
                if (gaStore != null) gaStore.load();
            } catch (IOException ex2) {
            }
            throw ex;
        }
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.group;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.form.Form;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.RoleStoreValidationWrapper;
import org.geoserver.security.validation.UserGroupStoreValidationWrapper;
import org.geoserver.security.web.user.UserListProvider;
import org.geoserver.security.web.user.UserTablePanel;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class EditGroupPage extends AbstractGroupPage {

    public EditGroupPage(String userGroupServiceName, final GeoServerUserGroup group) {
        super(userGroupServiceName, group.copy()); // copy before passing into parent

        // name not changeable on edit
        get("form:groupname").setEnabled(false);

        ((Form) get("form"))
                .add(
                        new UserTablePanel(
                                        "users",
                                        userGroupServiceName,
                                        new GeoServerDataProvider<GeoServerUser>() {
                                            @Override
                                            protected List<
                                                            GeoServerDataProvider.Property<
                                                                    GeoServerUser>>
                                                    getProperties() {
                                                return Arrays.asList(UserListProvider.USERNAME);
                                            }

                                            @Override
                                            protected List<GeoServerUser> getItems() {
                                                GeoServerUserGroupService ugService =
                                                        getUserGroupService(
                                                                EditGroupPage.this
                                                                        .userGroupServiceName);
                                                try {
                                                    return new ArrayList<GeoServerUser>(
                                                            ugService.getUsersForGroup(group));
                                                } catch (IOException e) {
                                                    throw new WicketRuntimeException(e);
                                                }
                                            }
                                        })
                                .setFilterable(false));
    }

    @Override
    protected void onFormSubmit(GeoServerUserGroup group) throws IOException {
        GeoServerUserGroupStore store = null;
        try {
            if (hasUserGroupStore(userGroupServiceName)) {
                store =
                        new UserGroupStoreValidationWrapper(
                                getUserGroupStore(userGroupServiceName));
                store.updateGroup(group);
                store.store();
            }
        } catch (IOException ex) {
            try {
                // try to reload the store
                if (store != null) store.load();
            } catch (IOException ex2) {
            }
            throw ex;
        }

        GeoServerRoleStore gaStore = null;
        try {
            if (hasRoleStore(getSecurityManager().getActiveRoleService().getName())) {
                gaStore = getRoleStore(getSecurityManager().getActiveRoleService().getName());
                gaStore = new RoleStoreValidationWrapper(gaStore);

                Set<GeoServerRole> orig = gaStore.getRolesForGroup(group.getGroupname());
                Set<GeoServerRole> add = new HashSet<GeoServerRole>();
                Set<GeoServerRole> remove = new HashSet<GeoServerRole>();
                rolePalette.diff(orig, add, remove);

                for (GeoServerRole role : add)
                    gaStore.associateRoleToGroup(role, group.getGroupname());
                for (GeoServerRole role : remove)
                    gaStore.disAssociateRoleFromGroup(role, group.getGroupname());
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

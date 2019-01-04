/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.user;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.validation.RoleStoreValidationWrapper;
import org.geoserver.security.validation.UserGroupStoreValidationWrapper;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

public class SelectionUserRemovalLink extends AjaxLink<Object> {

    /** */
    private static final long serialVersionUID = 1L;

    GeoServerTablePanel<GeoServerUser> users;

    GeoServerDialog dialog;
    boolean disassociateRoles;
    ConfirmRemovalUserPanel removePanel;
    GeoServerDialog.DialogDelegate delegate;
    String userGroupsServiceName;

    public SelectionUserRemovalLink(
            String userGroupsServiceName,
            String id,
            GeoServerTablePanel<GeoServerUser> users,
            GeoServerDialog dialog,
            boolean disassociateRoles) {
        super(id);
        this.users = users;
        this.dialog = dialog;
        this.disassociateRoles = disassociateRoles;
        this.userGroupsServiceName = userGroupsServiceName;
    }
    // return new ConfirmRemovalPanel<GeoserverUserGroup>(id,"username", selection) {
    // //return new ConfirmRemovalPanel<GeoserverUserGroup>(id,"username", selection) {

    @Override
    public void onClick(AjaxRequestTarget target) {
        final List<GeoServerUser> selection = users.getSelection();
        if (selection.size() == 0) return;

        dialog.setTitle(new ParamResourceModel("confirmRemoval", this));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        dialog.showOkCancel(
                target,
                delegate =
                        new GeoServerDialog.DialogDelegate() {
                            protected Component getContents(String id) {
                                // show a confirmation panel for all the objects we have to remove
                                Model<Boolean> model =
                                        new Model<Boolean>(
                                                SelectionUserRemovalLink.this.disassociateRoles);
                                return removePanel =
                                        new ConfirmRemovalUserPanel(id, model, selection) {
                                            @Override
                                            protected IModel<String> canRemove(GeoServerUser user) {
                                                return SelectionUserRemovalLink.this.canRemove(
                                                        user);
                                            }
                                        };
                            }

                            protected boolean onSubmit(
                                    AjaxRequestTarget target, Component contents) {
                                // cascade delete the whole selection

                                GeoServerUserGroupStore ugStore = null;
                                try {
                                    GeoServerUserGroupService ugService =
                                            GeoServerApplication.get()
                                                    .getSecurityManager()
                                                    .loadUserGroupService(userGroupsServiceName);
                                    ugStore =
                                            new UserGroupStoreValidationWrapper(
                                                    ugService.createStore());
                                    for (GeoServerUser user : removePanel.getRoots()) {
                                        ugStore.removeUser(user);
                                    }
                                    ugStore.store();
                                } catch (IOException ex) {
                                    try {
                                        ugStore.load();
                                    } catch (IOException ex2) {
                                    }
                                    ;
                                    throw new RuntimeException(ex);
                                }

                                GeoServerRoleStore gaStore = null;
                                if (disassociateRoles) {
                                    try {
                                        gaStore =
                                                GeoServerApplication.get()
                                                        .getSecurityManager()
                                                        .getActiveRoleService()
                                                        .createStore();
                                        gaStore = new RoleStoreValidationWrapper(gaStore);

                                        for (GeoServerUser user : removePanel.getRoots()) {
                                            List<GeoServerRole> list =
                                                    new ArrayList<GeoServerRole>();
                                            list.addAll(
                                                    gaStore.getRolesForUser(user.getUsername()));
                                            for (GeoServerRole role : list)
                                                gaStore.disAssociateRoleFromUser(
                                                        role, user.getUsername());
                                        }
                                        gaStore.store();
                                    } catch (IOException ex) {
                                        try {
                                            gaStore.load();
                                        } catch (IOException ex2) {
                                        }
                                        ;
                                        throw new RuntimeException(ex);
                                    }
                                }

                                // the deletion will have changed what we see in the page
                                // so better clear out the selection
                                users.clearSelection();
                                return true;
                            }

                            @Override
                            public void onClose(AjaxRequestTarget target) {
                                // if the selection has been cleared out it's sign a deletion
                                // occurred, so refresh the table
                                if (users.getSelection().size() == 0) {
                                    setEnabled(false);
                                    target.add(SelectionUserRemovalLink.this);
                                    target.add(users);
                                }
                            }
                        });
    }

    protected StringResourceModel canRemove(GeoServerUser user) {
        return null;
    }
}

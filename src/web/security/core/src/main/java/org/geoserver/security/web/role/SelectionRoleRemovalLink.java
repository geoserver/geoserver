/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.io.IOException;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.validation.AbstractSecurityException;
import org.geoserver.security.validation.RoleServiceValidationWrapper;
import org.geoserver.security.validation.RoleStoreValidationWrapper;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

public class SelectionRoleRemovalLink extends AjaxLink<Object> {

    private static final long serialVersionUID = 1L;

    GeoServerTablePanel<GeoServerRole> roles;
    GeoServerDialog dialog;
    GeoServerDialog.DialogDelegate delegate;
    ConfirmRemovalRolePanel removePanel;
    String roleServiceName;

    public SelectionRoleRemovalLink(
            String roleServiceName,
            String id,
            GeoServerTablePanel<GeoServerRole> roles,
            GeoServerDialog dialog) {
        super(id);
        this.roles = roles;
        this.dialog = dialog;
        this.roleServiceName = roleServiceName;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        final List<GeoServerRole> selection = roles.getSelection();
        if (selection.size() == 0) return;

        dialog.setTitle(new ParamResourceModel("confirmRemoval", this));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        dialog.showOkCancel(
                target,
                delegate =
                        new GeoServerDialog.DialogDelegate() {
                            private static final long serialVersionUID = 1L;

                            protected Component getContents(String id) {
                                // show a confirmation panel for all the objects we have to remove
                                return removePanel =
                                        new ConfirmRemovalRolePanel(id, selection) {
                                            private static final long serialVersionUID = 1L;

                                            @Override
                                            protected IModel<String> canRemove(GeoServerRole role) {
                                                return SelectionRoleRemovalLink.this.canRemove(
                                                        role);
                                            }
                                        };
                            }

                            protected boolean onSubmit(
                                    AjaxRequestTarget target, Component contents) {
                                // cascade delete the whole selection

                                GeoServerRoleStore gaStore = null;
                                try {
                                    GeoServerRoleService gaService =
                                            GeoServerApplication.get()
                                                    .getSecurityManager()
                                                    .loadRoleService(roleServiceName);
                                    gaStore =
                                            new RoleStoreValidationWrapper(gaService.createStore());
                                    for (GeoServerRole role : removePanel.getRoots()) {
                                        gaStore.removeRole(role);
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
                                // the deletion will have changed what we see in the page
                                // so better clear out the selection
                                roles.clearSelection();
                                return true;
                            }

                            @Override
                            public void onClose(AjaxRequestTarget target) {
                                // if the selection has been cleared out it's sign a deletion
                                // occurred, so refresh the table
                                if (roles.getSelection().size() == 0) {
                                    setEnabled(false);
                                    target.add(SelectionRoleRemovalLink.this);
                                    target.add(roles);
                                }
                            }
                        });
    }

    protected IModel<String> canRemove(GeoServerRole role) {

        GeoServerRoleService gaService = null;
        try {
            gaService =
                    GeoServerApplication.get()
                            .getSecurityManager()
                            .loadRoleService(roleServiceName);
            boolean isActive =
                    GeoServerApplication.get()
                            .getSecurityManager()
                            .getActiveRoleService()
                            .getName()
                            .equals(roleServiceName);
            RoleServiceValidationWrapper valService =
                    new RoleServiceValidationWrapper(gaService, isActive);
            valService.checkRoleIsMapped(role);
            valService.checkRoleIsUsed(role);
        } catch (IOException e) {
            if (e.getCause() instanceof AbstractSecurityException) {
                return new Model(e.getCause().getMessage());
            } else {
                throw new RuntimeException(e);
            }
        }

        return null;
    }
}

package org.geoserver.web.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.core.userdetails.User;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.impl.GeoserverUserDao;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

public class SelectionUserRemovalLink extends AjaxLink {

    GeoServerTablePanel<User> users;

    GeoServerDialog dialog;

    public SelectionUserRemovalLink(String id, GeoServerTablePanel<User> users,
            GeoServerDialog dialog) {
        super(id);
        this.users = users;
        this.dialog = dialog;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        final List<User> selection = users.getSelection();
        if (selection.size() == 0)
            return;

        dialog.setTitle(new ParamResourceModel("confirmRemoval", this));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
            protected Component getContents(String id) {
                // show a confirmation panel for all the objects we have to remove
                return new ConfirmRemovalUserPanel(id, selection) {
                    @Override
                    protected StringResourceModel canRemove(User user) {
                        return SelectionUserRemovalLink.this.canRemove(user);
                    }
                };
            }

            protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                // cascade delete the whole selection
                GeoserverUserDao dao = GeoserverUserDao.get();
                for (User user : selection) {
                    if(!user.getUsername().equals("admin"))
                        dao.removeUser(user.getUsername());
                }
                try {
                    dao.storeUsers();
                } catch (IOException e) {
                    e.printStackTrace();
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
                    target.addComponent(SelectionUserRemovalLink.this);
                    target.addComponent(users);
                }
            }

        });

    }

    protected StringResourceModel canRemove(User user) {
        return null;
    }

}

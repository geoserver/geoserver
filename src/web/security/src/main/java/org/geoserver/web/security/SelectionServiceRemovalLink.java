package org.geoserver.web.security;

import java.io.IOException;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

public class SelectionServiceRemovalLink extends AjaxLink {

    GeoServerTablePanel<ServiceAccessRule> services;

    GeoServerDialog dialog;

    public SelectionServiceRemovalLink(String id, GeoServerTablePanel<ServiceAccessRule> services,
            GeoServerDialog dialog) {
        super(id);
        this.services = services;
        this.dialog = dialog;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        final List<ServiceAccessRule> selection = services.getSelection();
        if (selection.size() == 0)
            return;

        dialog.setTitle(new ParamResourceModel("confirmRemoval", this));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
            protected Component getContents(String id) {
                // show a confirmation panel for all the objects we have to remove
                return new ConfirmRemovalServicePanel(id, selection) {
                    @Override
                    protected StringResourceModel canRemove(ServiceAccessRule service) {
                        return SelectionServiceRemovalLink.this.canRemove(service);
                    }
                };
            }

            protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                // cascade delete the whole selection
                ServiceAccessRuleDAO dao = ServiceAccessRuleDAO.get();
                for (ServiceAccessRule service : selection) {
                    dao.removeRule(service);
                }
                try {
                    dao.storeRules();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // the deletion will have changed what we see in the page
                // so better clear out the selection
                services.clearSelection();
                return true;
            }

            @Override
            public void onClose(AjaxRequestTarget target) {
                // if the selection has been cleared out it's sign a deletion
                // occurred, so refresh the table
                if (services.getSelection().size() == 0) {
                    setEnabled(false);
                    target.addComponent(SelectionServiceRemovalLink.this);
                    target.addComponent(services);
                }
            }

        });

    }

    protected StringResourceModel canRemove(ServiceAccessRule service) {
        return null;
    }

}

/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.data;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;

public class SelectionDataRuleRemovalLink extends AjaxLink<Object> {

    static final Logger LOGGER = Logging.getLogger(SelectionDataRuleRemovalLink.class);

    GeoServerTablePanel<DataAccessRule> rules;
    GeoServerDialog dialog;
    ConfirmRemovalDataAccessRulePanel removePanel;
    GeoServerDialog.DialogDelegate delegate;

    public SelectionDataRuleRemovalLink(String id, GeoServerTablePanel<DataAccessRule> rules, GeoServerDialog dialog) {
        super(id);
        this.rules = rules;
        this.dialog = dialog;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        final List<DataAccessRule> selection = rules.getSelection();
        if (selection.isEmpty()) return;

        dialog.setTitle(new ParamResourceModel("confirmRemoval", this));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        dialog.showOkCancel(
                target,
                delegate = new GeoServerDialog.DialogDelegate() {
                    @Override
                    protected Component getContents(String id) {
                        // show a confirmation panel for all the objects we have to remove
                        return removePanel = new ConfirmRemovalDataAccessRulePanel(id, selection) {
                            @Override
                            protected IModel<String> canRemove(DataAccessRule data) {
                                return SelectionDataRuleRemovalLink.this.canRemove(data);
                            }
                        };
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        // cascade delete the whole selection
                        DataAccessRuleDAO dao = DataAccessRuleDAO.get();
                        for (DataAccessRule rule : removePanel.getRoots()) {
                            dao.removeRule(rule);
                        }
                        try {
                            dao.storeRules();
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "", e);
                        }

                        // the deletion will have changed what we see in the page
                        // so better clear out the selection
                        rules.clearSelection();
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        // if the selection has been cleared out it's sign a deletion
                        // occurred, so refresh the table
                        if (rules.getSelection().isEmpty()) {
                            setEnabled(false);
                            target.add(SelectionDataRuleRemovalLink.this);
                            target.add(rules);
                        }
                    }
                });
    }

    protected StringResourceModel canRemove(DataAccessRule data) {
        return null;
    }
}

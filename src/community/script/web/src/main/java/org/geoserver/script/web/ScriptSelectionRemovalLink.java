/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.web;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.geoserver.platform.resource.Resource;
import org.geoserver.script.ScriptType;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

public class ScriptSelectionRemovalLink extends AjaxLink<Object> {

    private static final long serialVersionUID = 8837711212090575436L;

    GeoServerTablePanel<Script> tablePanel;

    GeoServerDialog dialog;

    public ScriptSelectionRemovalLink(
            String id, GeoServerTablePanel<Script> tablePanel, GeoServerDialog dialog) {
        super(id);
        this.tablePanel = tablePanel;
        this.dialog = dialog;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        // see if the user selected anything
        final List<Script> selection = tablePanel.getSelection();
        if (selection.size() == 0) return;

        dialog.setTitle(new ParamResourceModel("confirmRemoval", this));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        dialog.showOkCancel(
                target,
                new GeoServerDialog.DialogDelegate() {
                    private static final long serialVersionUID = 9062725459934129182L;

                    protected Component getContents(String id) {
                        // show a confirmation panel for all the objects we have to remove
                        return new Label(id, "Do you want to delete these scripts?");
                    }

                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        for (Script script : selection) {
                            Resource file = script.getResource();
                            file.delete();
                            if (script.getType().equalsIgnoreCase(ScriptType.APP.getLabel())) {
                                file.parent().delete();
                            }
                        }

                        // the deletion will have changed what we see in the page
                        // so better clear out the selection
                        tablePanel.clearSelection();
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        // if the selection has been cleared out it's sign a deletion
                        // occurred, so refresh the table
                        if (tablePanel.getSelection().size() == 0) {
                            setEnabled(false);
                            target.add(ScriptSelectionRemovalLink.this);
                            target.add(tablePanel);
                        }
                    }
                });
    }
}

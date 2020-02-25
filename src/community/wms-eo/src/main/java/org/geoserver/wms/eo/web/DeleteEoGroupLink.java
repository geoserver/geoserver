/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.ConfirmRemovalPanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.wms.eo.EoCatalogBuilder;
import org.geotools.util.logging.Logging;

/**
 * Wicket link to delete an EO layer group.
 *
 * @author Davide Savazzi - geo-solutions.it
 */
class DeleteEoGroupLink extends AjaxLink {

    private LayerGroupTablePanel groupTable;
    private GeoServerDialog dialog;
    private static final Logger LOGGER = Logging.getLogger(DeleteEoGroupLink.class);

    public DeleteEoGroupLink(String id, LayerGroupTablePanel groupTable, GeoServerDialog dialog) {
        super(id);
        this.groupTable = groupTable;
        this.dialog = dialog;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        // see if the user selected anything
        final List<LayerGroupInfo> selection = groupTable.getSelection();
        if (selection.size() == 0) {
            return;
        }

        dialog.setTitle(new ParamResourceModel("confirmRemoval", this));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        dialog.showOkCancel(
                target,
                new GeoServerDialog.DialogDelegate() {
                    @Override
                    protected Component getContents(String id) {
                        // show a confirmation panel for all the objects we have to remove
                        return new ConfirmRemovalPanel(id, selection);
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        // cascade delete the whole selection
                        EoCatalogBuilder builder =
                                new EoCatalogBuilder(GeoServerApplication.get().getCatalog());
                        for (LayerGroupInfo group : selection) {
                            delete(builder, group);
                        }

                        // the deletion will have changed what we see in the page
                        // so better clear out the selection
                        groupTable.clearSelection();
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        // if the selection has been cleared out it's sign a deletion
                        // occurred, so refresh the table
                        if (groupTable.getSelection().size() == 0) {
                            setEnabled(false);
                            target.add(groupTable);
                            for (AbstractLink link : groupTable.getSelectionLinks()) {
                                target.add(link);
                            }
                        }
                    }
                });
    }

    private void delete(EoCatalogBuilder builder, LayerGroupInfo group) {
        try {
            builder.delete(group);
        } catch (RuntimeException e) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, e.getMessage(), e);
            }

            // TODO how to notify?
        }
    }
}

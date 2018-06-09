/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * A reusable cascading, multiple removal link. Assumes the presence of a table panel filled with
 * catalog objects and a {@link GeoServerDialog} to be used for reporting the objects that will be
 * affected by the removal
 */
@SuppressWarnings("serial")
public class SelectionRemovalLink extends AjaxLink<Void> {

    GeoServerTablePanel<? extends CatalogInfo> catalogObjects;
    GeoServerDialog dialog;

    public SelectionRemovalLink(
            String id,
            GeoServerTablePanel<? extends CatalogInfo> catalogObjects,
            GeoServerDialog dialog) {
        super(id);
        this.catalogObjects = catalogObjects;
        this.dialog = dialog;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        // see if the user selected anything
        final List<? extends CatalogInfo> selection = catalogObjects.getSelection();
        if (selection.size() == 0) return;

        dialog.setTitle(new ParamResourceModel("confirmRemoval", this));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        dialog.showOkCancel(
                target,
                new GeoServerDialog.DialogDelegate() {
                    protected Component getContents(String id) {
                        // show a confirmation panel for all the objects we have to remove
                        return new ConfirmRemovalPanel(id, selection) {
                            @Override
                            protected StringResourceModel canRemove(CatalogInfo info) {
                                return SelectionRemovalLink.this.canRemove(info);
                            }
                        };
                    }

                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        // cascade delete the whole selection
                        Catalog catalog = GeoServerApplication.get().getCatalog();
                        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(catalog);
                        for (CatalogInfo ci : selection) {
                            ci.accept(visitor);
                        }

                        // the deletion will have changed what we see in the page
                        // so better clear out the selection
                        catalogObjects.clearSelection();
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        // if the selection has been cleared out it's sign a deletion
                        // occurred, so refresh the table
                        if (catalogObjects.getSelection().size() == 0) {
                            setEnabled(false);
                            target.add(SelectionRemovalLink.this);
                            target.add(catalogObjects);
                        }
                    }
                });
    }

    /**
     * Determines if a catalog object can be removed or not.
     *
     * <p>This method returns non-null in cases where the object should not be be removed. The
     * return value should be a description or reason of why the object can not be removed.
     *
     * @param info The object to be removed.
     * @return A message stating why the object can not be removed, or null to indicate that it can
     *     be removed.
     */
    protected StringResourceModel canRemove(CatalogInfo info) {
        return null;
    }
}

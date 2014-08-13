/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.TJSCascadeDeleteVisitor;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalogObject;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;

import java.util.List;

/**
 * A reusable cascading, multiple removal link. Assumes the presence of a table
 * panel filled with catalog objects and a {@link GeoServerDialog} to be used
 * for reporting the objects that will be affected by the removal
 */
@SuppressWarnings("serial")
public class TJSSelectionRemovalLink extends AjaxLink {

    GeoServerTablePanel<? extends TJSCatalogObject> catalogObjects;
    GeoServerDialog dialog;

    public TJSSelectionRemovalLink(String id, GeoServerTablePanel<? extends TJSCatalogObject> catalogObjects, GeoServerDialog dialog) {
        super(id);
        this.catalogObjects = catalogObjects;
        this.dialog = dialog;
    }

    @Override
    public void onClick(AjaxRequestTarget target) {
        // see if the user selected anything
        final List<? extends TJSCatalogObject> selection = catalogObjects.getSelection();
        if (selection.size() == 0)
            return;

        dialog.setTitle(new ParamResourceModel("confirmRemoval", this));

        // if there is something to cancel, let's warn the user about what
        // could go wrong, and if the user accepts, let's delete what's needed
        dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
            protected Component getContents(String id) {
                // show a confirmation panel for all the objects we have to remove
                return new TJSConfirmRemovalPanel(id, selection) {
                    @Override
                    protected StringResourceModel canRemove(TJSCatalogObject info) {
                        return TJSSelectionRemovalLink.this.canRemove(info);
                    }
                };
            }

            protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                // cascade delete the whole selection
                TJSCatalog catalog = TJSExtension.getTJSCatalog();
                TJSCascadeDeleteVisitor visitor = new TJSCascadeDeleteVisitor(catalog);
                for (TJSCatalogObject ci : selection) {
                    ci.accept(visitor);
                }

                // the deletion will have changed what we see in the page
                // so better clear out the selection
                catalogObjects.clearSelection();

                //persistir los cambios, Alvaro Javier Fuentes Suarez, 11:46 p.m. 1/8/13
                catalog.save();

                return true;
            }

            @Override
            public void onClose(AjaxRequestTarget target) {
                // if the selection has been cleared out it's sign a deletion
                // occurred, so refresh the table
                if (catalogObjects.getSelection().size() == 0) {
                    setEnabled(false);
                    target.addComponent(TJSSelectionRemovalLink.this);
                    target.addComponent(catalogObjects);
                }
            }

        });

    }

    /**
     * Determines if a catalog object can be removed or not.
     * <p>
     * This method returns non-null in cases where the object should not be be
     * removed. The return value should be a description or reason of why the
     * object can not be removed.
     * </p>
     *
     * @param info The object to be removed.
     * @return A message stating why the object can not be removed, or null to
     *         indicate that it can be removed.
     */
    protected StringResourceModel canRemove(TJSCatalogObject info) {
        return null;
    }
}

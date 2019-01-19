/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.util.logging.Level;
import javax.management.RuntimeErrorException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.data.layer.NewLayerPage;
import org.opengis.coverage.grid.Format;

/**
 * Supports coverage store configuration
 *
 * @author Andrea Aime
 * @author Gabriel Roldan
 */
public class CoverageStoreNewPage extends AbstractCoverageStorePage {

    /**
     * @param coverageFactoryName the {@link Format#getName() name} of the format to create a new
     *     raster coverage for
     */
    public CoverageStoreNewPage(final String coverageFactoryName) {
        Catalog catalog = getCatalog();
        final WorkspaceInfo workspace = catalog.getDefaultWorkspace();
        CoverageStoreInfo store = catalog.getFactory().createCoverageStore();
        store.setWorkspace(workspace);
        store.setType(coverageFactoryName);
        store.setEnabled(true);
        store.setURL("file:data/example.extension");

        initUI(store);
    }

    @Override
    protected void onSave(final CoverageStoreInfo info, AjaxRequestTarget target)
            throws IllegalArgumentException {
        final Catalog catalog = getCatalog();

        /*
         * Try saving a copy of it so if the process fails somehow the original "info" does not end
         * up with an id set
         */
        CoverageStoreInfo expandedStore = getCatalog().getResourcePool().clone(info, true);
        CoverageStoreInfo savedStore = catalog.getFactory().createCoverageStore();

        // GR: this shouldn't fail, the Catalog.save(StoreInfo) API does not declare any action in
        // case
        // of a failure!... strange, why a save can't fail?
        // Still, be cautious and wrap it in a try/catch block so the page does not blow up
        try {
            // GeoServer Env substitution; validate first
            catalog.validate(expandedStore, false).throwIfInvalid();

            // GeoServer Env substitution; force to *AVOID* resolving env placeholders...
            savedStore = catalog.getResourcePool().clone(info, false);
            // ... and save
            catalog.save(savedStore);
        } catch (RuntimeException e) {
            LOGGER.log(Level.INFO, "Adding the store for " + info.getURL(), e);
            throw new IllegalArgumentException(
                    "The coverage store could not be saved. Failure message: " + e.getMessage());
        }

        onSuccessfulSave(info, catalog, savedStore);
    }

    protected void onSuccessfulSave(
            final CoverageStoreInfo info, final Catalog catalog, CoverageStoreInfo savedStore) {
        // the StoreInfo save succeeded... try to present the list of coverages (well, _the_
        // coverage while the getotools coverage api does not allow for more than one
        NewLayerPage layerChooserPage;
        try {
            catalog.getResourcePool().clone(savedStore, true);
            // The ID is assigned by the catalog and therefore cannot be cloned
            layerChooserPage = new NewLayerPage(savedStore.getId());
        } catch (RuntimeException e) {
            LOGGER.log(Level.INFO, "Getting list of coverages for saved store " + info.getURL(), e);
            // doh, can't present the list of coverages, means saving the StoreInfo is meaningless.
            try { // be extra cautious
                catalog.remove(savedStore);
            } catch (RuntimeErrorException shouldNotHappen) {
                LOGGER.log(Level.WARNING, "Can't remove CoverageStoreInfo after adding it!", e);
            }
            // tell the caller why we failed...
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        setResponsePage(layerChooserPage);
    }
}

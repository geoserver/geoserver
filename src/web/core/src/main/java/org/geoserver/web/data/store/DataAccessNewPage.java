/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.io.IOException;
import java.util.logging.Level;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.data.layer.NewLayerPage;
import org.geotools.data.DataAccess;
import org.geotools.data.util.NullProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * Provides a form to configure a new geotools {@link DataAccess}
 *
 * @author Gabriel Roldan
 */
public class DataAccessNewPage extends AbstractDataAccessPage {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new datastore configuration page to create a new datastore of the given type
     *
     * @param dataStoreFactDisplayName the type of datastore to create, given by its factory display
     *     name
     */
    public DataAccessNewPage(final String dataStoreFactDisplayName) {
        super();

        final WorkspaceInfo defaultWs = getCatalog().getDefaultWorkspace();
        if (defaultWs == null) {
            throw new IllegalStateException("No default Workspace configured");
        }
        final NamespaceInfo defaultNs = getCatalog().getDefaultNamespace();
        if (defaultNs == null) {
            throw new IllegalStateException("No default Namespace configured");
        }

        // Param[] parametersInfo = dsFact.getParametersInfo();
        // for (int i = 0; i < parametersInfo.length; i++) {
        // Serializable value;
        // final Param param = parametersInfo[i];
        // if (param.sample == null || param.sample instanceof Serializable) {
        // value = (Serializable) param.sample;
        // } else {
        // value = String.valueOf(param.sample);
        // }
        // }

        DataStoreInfo info = getCatalog().getFactory().createDataStore();
        info.setWorkspace(defaultWs);
        info.setEnabled(true);
        info.setType(dataStoreFactDisplayName);

        initUI(info);
    }

    /**
     * Callback method called when the submit button have been pressed and the parameters validation
     * has succeed.
     *
     * @see AbstractDataAccessPage#onSaveDataStore(Form)
     */
    @Override
    protected final void onSaveDataStore(final DataStoreInfo info, AjaxRequestTarget target)
            throws IllegalArgumentException {
        if (!storeEditPanel.onSave()) {
            return;
        }

        final Catalog catalog = getCatalog();

        // Cloning into "expandedStore" through the super class "clone" method
        DataStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);

        DataAccess<? extends FeatureType, ? extends Feature> dataStore;
        try {
            // REVISIT: this may need to be done after saving the DataStoreInfo
            dataStore = expandedStore.getDataStore(new NullProgressListener());
            dataStore.dispose();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error obtaining new data store", e);
            String message = e.getMessage();
            if (message == null && e.getCause() != null) {
                message = e.getCause().getMessage();
            }
            throw new IllegalArgumentException(
                    "Error creating data store, check the parameters. Error message: " + message);
        }

        DataStoreInfo savedStore = catalog.getResourcePool().clone(info, true);
        try {
            // GeoServer Env substitution; validate first
            catalog.validate(savedStore, true).throwIfInvalid();

            // save a copy, so if NewLayerPage fails we can keep on editing this one without being
            // proxied

            // GeoServer Env substitution; force to *AVOID* resolving env placeholders...
            savedStore = catalog.getResourcePool().clone(info, false);
            // ...and save
            catalog.add(savedStore);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error adding data store to catalog", e);
            String message = e.getMessage();
            if (message == null && e.getCause() != null) {
                message = e.getCause().getMessage();
            }

            throw new IllegalArgumentException(
                    "Error creating data store with the provided parameters: " + message);
        }

        final NewLayerPage newLayerPage;
        try {
            // The ID is assigned by the catalog and therefore cannot be cloned
            newLayerPage = new NewLayerPage(savedStore.getId());
        } catch (RuntimeException e) {
            try {
                catalog.remove(expandedStore);
                catalog.remove(savedStore);
            } catch (Exception removeEx) {
                LOGGER.log(Level.WARNING, "Error removing just added datastore!", e);
            }
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        setResponsePage(newLayerPage);
    }
}

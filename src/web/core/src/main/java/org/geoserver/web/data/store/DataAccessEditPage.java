/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.data.DataAccess;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * Provides a form to edit a geotools {@link DataAccess} that already exists in the {@link Catalog}
 *
 * @author Gabriel Roldan
 */
public class DataAccessEditPage extends AbstractDataAccessPage implements Serializable {

    public static final String STORE_NAME = "storeName";
    public static final String WS_NAME = "wsName";

    /** Dialog to ask for save confirmation in case the store can't be reached */
    private GeoServerDialog dialog;

    /** Uses a "name" parameter to locate the datastore */
    public DataAccessEditPage(PageParameters parameters) {
        String wsName = parameters.get(WS_NAME).toOptionalString();
        String storeName = parameters.get(STORE_NAME).toString();
        DataStoreInfo dsi = getCatalog().getDataStoreByName(wsName, storeName);

        if (dsi == null) {
            getSession()
                    .error(
                            new ParamResourceModel(
                                            "DataAccessEditPage.notFound", this, wsName, storeName)
                                    .getString());
            doReturn(StorePage.class);
            return;
        }

        try {
            initUI(dsi);
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
            doReturn(StorePage.class);
            return;
        }
    }

    /**
     * Creates a new datastore configuration page to edit the properties of the given data store
     *
     * @param dataStoreInfoId the datastore id to modify, as per {@link DataStoreInfo#getId()}
     */
    public DataAccessEditPage(final String dataStoreInfoId) throws IllegalArgumentException {
        final Catalog catalog = getCatalog();
        final DataStoreInfo dataStoreInfo = catalog.getDataStore(dataStoreInfoId);

        if (null == dataStoreInfo) {
            throw new IllegalArgumentException("DataStore " + dataStoreInfoId + " not found");
        }

        initUI(dataStoreInfo);
    }

    /** Creates a new edit page directly from a store object. */
    public DataAccessEditPage(DataStoreInfo store) {
        initUI(store);
    }

    protected void initUI(final DataStoreInfo dataStoreInfo) {
        // the confirm dialog
        dialog = new GeoServerDialog("dialog");
        add(dialog);

        super.initUI(dataStoreInfo);

        if (dataStoreInfo.getId() != null) {
            // null id means detached from catalog, don't bother with uniqueness check
            final String wsId = dataStoreInfo.getWorkspace().getId();
            workspacePanel
                    .getFormComponent()
                    .add(
                            new CheckExistingResourcesInWorkspaceValidator(
                                    dataStoreInfo.getId(), wsId));
        }
    }

    /**
     * Callback method called when the submit button have been hit and the parameters validation has
     * succeed.
     *
     * @see AbstractDataAccessPage#onSaveDataStore(Form)
     */
    protected final void onSaveDataStore(
            final DataStoreInfo info, final AjaxRequestTarget requestTarget) {

        if (!storeEditPanel.onSave()) {
            return;
        }

        final Catalog catalog = getCatalog();
        final ResourcePool resourcePool = catalog.getResourcePool();
        resourcePool.clear(info);

        if (info.isEnabled()) {
            // store's enabled, check availability
            DataAccess<? extends FeatureType, ? extends Feature> dataStore;
            try {
                dataStore = catalog.getResourcePool().getDataStore(info);
                LOGGER.finer(
                        "connection parameters verified for store "
                                + info.getName()
                                + ". Got a "
                                + dataStore.getClass().getName());
                doSaveStore(info);
                doReturn(StorePage.class);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error obtaining datastore with the modified values", e);
                confirmSaveOnConnectionFailure(info, requestTarget, e);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Error obtaining datastore with the modified values", e);
                confirmSaveOnConnectionFailure(info, requestTarget, e);
            }
        } else {
            // store's disabled, no need to check the connection parameters
            doSaveStore(info);
            doReturn(StorePage.class);
        }
    }

    @SuppressWarnings("serial")
    private void confirmSaveOnConnectionFailure(
            final DataStoreInfo info,
            final AjaxRequestTarget requestTarget,
            final Exception error) {

        getCatalog().getResourcePool().clear(info);

        final String exceptionMessage;
        {
            String message = error.getMessage();
            if (message == null && error.getCause() != null) {
                message = error.getCause().getMessage();
            }
            exceptionMessage = message;
        }

        dialog.showOkCancel(
                requestTarget,
                new GeoServerDialog.DialogDelegate() {

                    boolean accepted = false;

                    @Override
                    protected Component getContents(String id) {
                        return new StoreConnectionFailedInformationPanel(
                                id, info.getName(), exceptionMessage);
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        doSaveStore(info);
                        accepted = true;
                        return true;
                    }

                    @Override
                    protected boolean onCancel(AjaxRequestTarget target) {
                        return true;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        if (accepted) {
                            doReturn(StorePage.class);
                        }
                    }
                });
    }

    /**
     * Performs the save of the store.
     *
     * <p>This method may be subclasses to provide custom save functionality.
     */
    protected void doSaveStore(final DataStoreInfo info) {
        try {
            final Catalog catalog = getCatalog();

            // The namespace may have changed, in which case we need to update the store resources
            NamespaceInfo namespace = catalog.getNamespaceByPrefix(info.getWorkspace().getName());
            List<FeatureTypeInfo> configuredResources =
                    catalog.getResourcesByStore(info, FeatureTypeInfo.class);
            for (FeatureTypeInfo alreadyConfigured : configuredResources) {
                alreadyConfigured.setNamespace(namespace);
            }

            ResourcePool resourcePool = catalog.getResourcePool();
            resourcePool.clear(info);

            DataStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);

            // Cloning into "expandedStore" through the super class "clone" method
            catalog.validate(expandedStore, false).throwIfInvalid();

            catalog.save(info);
            // save the resources after saving the store
            for (FeatureTypeInfo alreadyConfigured : configuredResources) {
                catalog.save(alreadyConfigured);
            }
            LOGGER.finer("Saved store " + info.getName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error saving data store to catalog", e);
            throw new IllegalArgumentException("Error saving data store:" + e.getMessage());
        }
    }
}

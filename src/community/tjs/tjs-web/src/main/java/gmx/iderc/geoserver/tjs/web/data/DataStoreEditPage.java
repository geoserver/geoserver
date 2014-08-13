/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.data;

import gmx.iderc.geoserver.tjs.catalog.DataStoreInfo;
import gmx.iderc.geoserver.tjs.catalog.TJSCatalog;
import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.web.data.store.StoreConnectionFailedInformationPanel;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.NullProgressListener;

import java.io.Serializable;
import java.util.logging.Level;

public class DataStoreEditPage extends AbstractDataStorePage implements Serializable {

    public static final String STORE_NAME = "name";
//    public static final String WS_NAME = "wsName";

    /**
     * Dialog to ask for save confirmation in case the store can't be reached
     */
    private GeoServerDialog dialog;

    /**
     * Uses a "name" parameter to locate the datastore
     *
     * @param parameters
     */
    public DataStoreEditPage(PageParameters parameters) {
//        String wsName = parameters.getString(WS_NAME);
        String storeName = parameters.getString(STORE_NAME);
        DataStoreInfo dsi = getTJSCatalog().getDataStoreByName(storeName);

        if (dsi == null) {
            error(new ParamResourceModel("DataStoreEditPage.notFound", this, "", storeName).getString());
            setResponsePage(DataStorePage.class);
            return;
        }

        try {
            initUI(dsi);
        } catch (IllegalArgumentException e) {
            error(e.getMessage());
            setResponsePage(DataStorePage.class);
            return;
        }
    }

    /**
     * Creates a new datastore configuration page to edit the properties of the given data store
     *
     * @param dataStoreName the datastore id to modify, as per {@link DataStoreInfo#getId()}
     */
    public DataStoreEditPage(final String dataStoreName) throws IllegalArgumentException {
        final TJSCatalog catalog = getTJSCatalog();
        final DataStoreInfo dataStoreInfo = catalog.getDataStoreByName(dataStoreName);

        if (null == dataStoreInfo) {
            throw new IllegalArgumentException("DataStore " + dataStoreName + " not found");
        }

        initUI(dataStoreInfo);
    }

    @Override
    protected void initUI(final DataStoreInfo dataStoreInfo) {
        // the confirm dialog
        dialog = new GeoServerDialog("dialog");
        add(dialog);

        super.initUI(dataStoreInfo);

//        final String wsId = dataStoreInfo.getWorkspace().getId();
//        workspacePanel.getFormComponent().add(
//                new CheckExistingResourcesInWorkspaceValidator(dataStoreInfo.getId(), wsId));
    }

    protected final void onSaveDataStore(final DataStoreInfo info,
                                         final AjaxRequestTarget requestTarget) {

        final TJSCatalog catalog = getTJSCatalog();
//        final ResourcePool resourcePool = catalog.getResourcePool();
//        resourcePool.clear(info);

        if (info.getEnabled()) {
            // store's enabled, check availability
            TJSDataStore dataStore;
            try {
//                dataStore = catalog.getResourcePool().getDataStore(info);
                dataStore = catalog.getDataStore(info.getName()).getTJSDataStore(new NullProgressListener());
                LOGGER.finer("connection parameters verified for store " + info.getName()
                                     + ". Got a " + dataStore.getClass().getName());
                doSaveStore(info);
                setResponsePage(DataStorePage.class);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error obtaining datastore with the modified values", e);
                confirmSaveOnConnectionFailure(info, requestTarget, e);
            }
        } else {
            // store's disabled, no need to check the connection parameters
            doSaveStore(info);
            setResponsePage(DataStorePage.class);
        }
    }

    @SuppressWarnings("serial")
    private void confirmSaveOnConnectionFailure(final DataStoreInfo info,
                                                final AjaxRequestTarget requestTarget, final Exception error) {

//        getCatalog().getResourcePool().clear(info);
//        getCatalog().clear(info);

        final String exceptionMessage;
        {
            String message = error.getMessage();
            if (message == null && error.getCause() != null) {
                message = error.getCause().getMessage();
            }
            exceptionMessage = message;
        }

        dialog.showOkCancel(requestTarget, new GeoServerDialog.DialogDelegate() {

            boolean accepted = false;

            @Override
            protected Component getContents(String id) {
                return new StoreConnectionFailedInformationPanel(id, info.getName(),
                                                                        exceptionMessage);
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
                    setResponsePage(DataStorePage.class);
                }
            }
        });
    }

    private void doSaveStore(final DataStoreInfo info) {
        try {
            final TJSCatalog catalog = getTJSCatalog();

            // The namespace may have changed, in which case we need to update the store resources
//            NamespaceInfo namespace = catalog.getNamespaceByPrefix(info.getWorkspace().getName());
//            List<FeatureTypeInfo> configuredResources = catalog.getResourcesByStore(info,
//                    FeatureTypeInfo.class);
//            for (FeatureTypeInfo alreadyConfigured : configuredResources) {
//                alreadyConfigured.setNamespace(namespace);
//            }

//            ResourcePool resourcePool = catalog.getResourcePool();
//            resourcePool.clear(info);
            catalog.save(info);
            // save the resources after saving the store
//            for (FeatureTypeInfo alreadyConfigured : configuredResources) {
//                catalog.save(alreadyConfigured);
//            }
            LOGGER.finer("Saved store " + info.getName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error saving data store to catalog", e);
            throw new IllegalArgumentException("Error saving data store:" + e.getMessage());
        }
    }

}

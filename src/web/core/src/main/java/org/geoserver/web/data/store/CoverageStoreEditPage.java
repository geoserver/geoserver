/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.util.factory.GeoTools;
import org.opengis.coverage.grid.GridCoverageReader;

/**
 * Supports coverage store configuration
 *
 * @author Andrea Aime
 */
public class CoverageStoreEditPage extends AbstractCoverageStorePage {

    public static final String STORE_NAME = "storeName";
    public static final String WS_NAME = "wsName";

    /** Dialog to ask for save confirmation in case the store can't be reached */
    private GeoServerDialog dialog;

    /** Uses a "name" parameter to locate the datastore */
    public CoverageStoreEditPage(PageParameters parameters) {
        String wsName = parameters.get(WS_NAME).toOptionalString();
        String storeName = parameters.get(STORE_NAME).toString();
        CoverageStoreInfo csi = getCatalog().getCoverageStoreByName(wsName, storeName);

        if (csi == null) {
            getSession()
                    .error(
                            new ParamResourceModel(
                                            "CoverageStoreEditPage.notFound",
                                            this,
                                            storeName,
                                            wsName)
                                    .getString());
            doReturn(StorePage.class);
            return;
        }

        initUI(csi);
    }

    /** @param storeId the store id */
    public CoverageStoreEditPage(final String storeId) throws IllegalArgumentException {
        Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getCoverageStore(storeId);
        if (store == null) {
            throw new IllegalArgumentException("Cannot find coverage store " + storeId);
        }

        initUI(store);
    }

    /** Creates a new edit page directly from a store object. */
    public CoverageStoreEditPage(CoverageStoreInfo store) throws IllegalArgumentException {
        initUI(store);
    }

    @Override
    void initUI(CoverageStoreInfo store) {
        dialog = new GeoServerDialog("dialog");
        add(dialog);

        super.initUI(store);

        if (store.getId() != null) {
            // store id == null means the store is not part of catalog, forgo uniqueness check
            String workspaceId = store.getWorkspace().getId();
            workspacePanel
                    .getFormComponent()
                    .add(
                            new CheckExistingResourcesInWorkspaceValidator(
                                    store.getId(), workspaceId));
        }
    }

    @Override
    protected final void onSave(final CoverageStoreInfo info, final AjaxRequestTarget requestTarget)
            throws IllegalArgumentException {

        if (null == info.getType()) {
            throw new IllegalArgumentException("Coverage type has not been set");
        }

        final Catalog catalog = getCatalog();
        final ResourcePool resourcePool = catalog.getResourcePool();
        resourcePool.clear(info);

        // Map<String, Serializable> connectionParameters = info.getConnectionParameters();

        if (info.isEnabled()) {
            // store's enabled, make sure it works
            LOGGER.finer(
                    "Store "
                            + info.getName()
                            + " is enabled, verifying factory availability "
                            + "before saving it...");
            AbstractGridFormat gridFormat = resourcePool.getGridCoverageFormat(info);

            if (gridFormat == null) {
                throw new IllegalArgumentException(
                        "No grid format found capable of connecting to the provided URL."
                                + " To save the store disable it, and check the required libraries are in place");
            }
            try {
                // get the reader through ResourcePool so it resolves relative URL's for us
                GridCoverageReader reader =
                        resourcePool.getGridCoverageReader(info, GeoTools.getDefaultHints());
                LOGGER.info(
                        "Connection to store "
                                + info.getName()
                                + " validated. Got a "
                                + reader.getClass().getName()
                                + ". Saving store");
                doSaveStore(info);
                doReturn(StorePage.class);
            } catch (IOException e) {
                confirmSaveOnConnectionFailure(info, requestTarget, e);
            } catch (RuntimeException e) {
                confirmSaveOnConnectionFailure(info, requestTarget, e);
            }
        } else {
            // store's disabled, no need to check for availability
            doSaveStore(info);
            doReturn(StorePage.class);
        }
    }

    @SuppressWarnings("serial")
    private void confirmSaveOnConnectionFailure(
            final CoverageStoreInfo info,
            final AjaxRequestTarget requestTarget,
            final Exception error) {
        final String exceptionMessage = error.getMessage();

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
    protected void doSaveStore(final CoverageStoreInfo info) {
        try {
            Catalog catalog = getCatalog();

            final String prefix = info.getWorkspace().getName();
            final NamespaceInfo namespace = catalog.getNamespaceByPrefix(prefix);

            List<CoverageInfo> alreadyConfigured;
            alreadyConfigured = catalog.getResourcesByStore(info, CoverageInfo.class);

            for (CoverageInfo coverage : alreadyConfigured) {
                coverage.setNamespace(namespace);
            }

            ResourcePool resourcePool = catalog.getResourcePool();
            resourcePool.clear(info);

            // Cloning into "expandedStore" through the super class "clone" method
            CoverageStoreInfo expandedStore = resourcePool.clone(info, true);
            catalog.validate(expandedStore, false).throwIfInvalid();

            catalog.save(info);

            for (CoverageInfo coverage : alreadyConfigured) {
                catalog.save(coverage);
            }
            LOGGER.finer("Saved store " + info.getName());
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Saving the store for " + info.getURL(), e);
            throw new IllegalArgumentException("Unable to save the store: " + e.getMessage());
        }
    }
}

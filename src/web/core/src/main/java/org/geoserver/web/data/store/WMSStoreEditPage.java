/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geotools.ows.wms.WebMapServer;

public class WMSStoreEditPage extends AbstractWMSStorePage {

    public static final String STORE_NAME = "storeName";
    public static final String WS_NAME = "wsName";

    /** Uses a "name" parameter to locate the datastore */
    public WMSStoreEditPage(PageParameters parameters) {
        String wsName = parameters.get(WS_NAME).toOptionalString();
        String storeName = parameters.get(STORE_NAME).toString();
        WMSStoreInfo store = getCatalog().getStoreByName(wsName, storeName, WMSStoreInfo.class);
        initUI(store);
    }

    /** Creates a new edit page directly from a store object. */
    public WMSStoreEditPage(WMSStoreInfo store) {
        initUI(store);
    }

    @Override
    protected void onSave(WMSStoreInfo info, AjaxRequestTarget target) throws IllegalArgumentException {
        if (!info.isEnabled()) {
            doSaveStore(info);
        } else {
            try {
                // try to see if we can connect
                getCatalog().getResourcePool().clear(info);
                // do not call info.getWebMapServer cause it ends up calling
                // resourcepool.getWebMapServer with the unproxied instance (old values)
                // info.getWebMapServer(null).getCapabilities();
                WebMapServer webMapServer = getCatalog().getResourcePool().getWebMapServer(info);
                webMapServer.getCapabilities();
                doSaveStore(info);
            } catch (Exception e) {
                confirmSaveOnConnectionFailure(info, target, e);
            }
        }
    }

    /**
     * Performs the save of the store.
     *
     * <p>This method may be subclasses to provide custom save functionality.
     */
    protected void doSaveStore(WMSStoreInfo info) {
        Catalog catalog = getCatalog();

        // Cloning into "expandedStore" through the super class "clone" method
        WMSStoreInfo expandedStore = catalog.getResourcePool().clone(info, true);

        getCatalog().validate(expandedStore, false).throwIfInvalid();

        getCatalog().save(info);
        doReturn(StorePage.class);
    }

    @SuppressWarnings("serial")
    private void confirmSaveOnConnectionFailure(
            final WMSStoreInfo info, final AjaxRequestTarget requestTarget, final Exception error) {

        getCatalog().getResourcePool().clear(info);

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
                return new StoreConnectionFailedInformationPanel(id, info.getName(), exceptionMessage);
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
}

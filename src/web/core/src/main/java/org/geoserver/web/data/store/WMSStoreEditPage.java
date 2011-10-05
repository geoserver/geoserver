/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.store;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geotools.data.wms.WebMapServer;

public class WMSStoreEditPage extends AbstractWMSStorePage {
    
    public static final String STORE_NAME = "storeName";
    public static final String WS_NAME = "wsName";
    
    /**
     * Uses a "name" parameter to locate the datastore
     * @param parameters
     */
    public WMSStoreEditPage(PageParameters parameters) {
        String wsName = parameters.getString(WS_NAME);
        String storeName = parameters.getString(STORE_NAME);
        WMSStoreInfo store = getCatalog().getStoreByName(wsName, storeName, WMSStoreInfo.class);
        initUI(store);
    }

    @Override
    protected void onSave(WMSStoreInfo info, AjaxRequestTarget target)
            throws IllegalArgumentException {
        if(!info.isEnabled()) {
            doSaveStore(info);
        } else {
            try {
                // try to see if we can connect
                getCatalog().getResourcePool().clear(info);
                // do not call info.getWebMapServer cause it ends up calling
                // resourcepool.getWebMapServer with the unproxied instance (old values)
                //info.getWebMapServer(null).getCapabilities();
                WebMapServer webMapServer = getCatalog().getResourcePool().getWebMapServer(info);
                webMapServer.getCapabilities();
                doSaveStore(info);
            } catch(Exception e) {
                confirmSaveOnConnectionFailure(info, target, e);
            }
        }

    }
    
    private void doSaveStore(WMSStoreInfo info) {
        getCatalog().save(info);
        setResponsePage(StorePage.class);
    }

    @SuppressWarnings("serial")
    private void confirmSaveOnConnectionFailure(final WMSStoreInfo info,
            final AjaxRequestTarget requestTarget, final Exception error) {

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
                    setResponsePage(StorePage.class);
                }
            }
        });
    }

}

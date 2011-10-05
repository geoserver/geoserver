/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;

import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.WMSStoreInfo;
import org.geotools.data.wms.WebMapServer;
import org.opengis.util.ProgressListener;

@SuppressWarnings("serial")
public class WMSStoreInfoImpl extends StoreInfoImpl implements WMSStoreInfo {

    String capabilitiesURL;
    private String user;
    private String password;
    private int maxConnections;

    protected WMSStoreInfoImpl() {
    }
    
    public WMSStoreInfoImpl(CatalogImpl catalog) {
        super(catalog);
    }

    public String getCapabilitiesURL() {
        return capabilitiesURL;
    }

    public void setCapabilitiesURL(String capabilitiesURL) {
        this.capabilitiesURL = capabilitiesURL;
    }

    @Override
    public String getUsername() {
        return user;
    }

    @Override
    public void setUsername(String user) {
        this.user = user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public int getMaxConnections() {
        return maxConnections;
    }

    @Override
    public void setMaxConnections(int maxConcurrentConnections) {
        this.maxConnections = maxConcurrentConnections;        
    }

    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    public WebMapServer getWebMapServer(ProgressListener listener) throws IOException {
        return getCatalog().getResourcePool().getWebMapServer(this);
    }

}

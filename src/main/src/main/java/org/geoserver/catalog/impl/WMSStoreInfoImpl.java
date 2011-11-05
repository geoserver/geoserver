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

    public static final int DEFAULT_MAX_CONNECTIONS = 6;

    public static final int DEFAULT_CONNECT_TIMEOUT = 30;

    public static final int DEFAULT_READ_TIMEOUT = 60;

    String capabilitiesURL;

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

    //@Override
    public String getUsername() {
        return getMetadata().get("user", String.class);
    }

    //@Override
    public void setUsername(String user) {
        getMetadata().put("user", user);
    }

    //@Override
    public String getPassword() {
        return getMetadata().get("password", String.class);
    }

    //@Override
    public void setPassword(String password) {
        getMetadata().put("password", password);
    }

    //@Override
    public int getMaxConnections() {
        Integer maxconnections = getMetadata().get("maxConnections", Integer.class);
        return maxconnections == null? DEFAULT_MAX_CONNECTIONS : maxconnections;
    }

    //@Override
    public void setMaxConnections(int maxConcurrentConnections) {
        getMetadata().put("maxConnections", Integer.valueOf(maxConcurrentConnections));        
    }

    public int getReadTimeout() {
        Integer readTimeout = getMetadata().get("readTimeout", Integer.class);
        return readTimeout == null? DEFAULT_READ_TIMEOUT : readTimeout;
    }

    public void setReadTimeout(int timeoutSeconds) {
        getMetadata().put("readTimeout", Integer.valueOf(timeoutSeconds));        
    }

    public int getConnectTimeout() {
        Integer connectTimeout = getMetadata().get("connectTimeout", Integer.class);
        return connectTimeout == null? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
    }

    public void setConnectTimeout(int timeoutSeconds) {
        getMetadata().put("connectTimeout", Integer.valueOf(timeoutSeconds));        
    }

    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    public WebMapServer getWebMapServer(ProgressListener listener) throws IOException {
        return getCatalog().getResourcePool().getWebMapServer(this);
    }

}

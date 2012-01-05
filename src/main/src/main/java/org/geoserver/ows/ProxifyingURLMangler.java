/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Map;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;

/**
 * A URL mangler that replaces the base URL with the proxied one
 */
public class ProxifyingURLMangler implements URLMangler {

    GeoServer geoServer;

    public ProxifyingURLMangler(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public void mangleURL(StringBuilder baseURL, StringBuilder path, Map<String, String> kvp,
            URLType type) {
        
        //first check the system property
        String proxyBase = GeoServerExtensions.getProperty("PROXY_BASE_URL");
        if (proxyBase == null) {
            //if no system property fall back to configuration
            proxyBase = geoServer.getSettings().getProxyBaseUrl();
        }

        // perform the replacement if the proxy base is set
        if (proxyBase != null && proxyBase.trim().length() > 0) {
            baseURL.setLength(0);
            baseURL.append(proxyBase);
        }
    }

}

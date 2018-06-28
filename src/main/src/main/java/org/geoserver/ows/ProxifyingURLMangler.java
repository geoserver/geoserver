/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.vfny.geoserver.util.Requests;

/** A URL mangler that replaces the base URL with the proxied one */
public class ProxifyingURLMangler implements URLMangler {

    GeoServer geoServer;
    public static String USEHEADERS_PARAM = "USEHEADERS_PROXYURL";

    public ProxifyingURLMangler(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {
        baseURL =
                this.geoServer.getGlobal().isUseHeadersProxyURL().booleanValue() == true
                        ? this.mangleURLHeaders(baseURL)
                        : this.mangleURLFixedURL(
                                baseURL,
                                GeoServerExtensions.getProperty(Requests.PROXY_PARAM),
                                geoServer.getSettings().getProxyBaseUrl());
    }

    /**
     * Mangle URL using the PROXY_BASE_URL
     *
     * @param baseURL baseURL to mangle
     * @param proxyURLSys Proxy URL fron system property
     * @param proxyURLConf Proxy URL from configuraiton
     * @return mangled proxy URL
     */
    private StringBuilder mangleURLFixedURL(
            StringBuilder baseURL, String proxyURLSys, String proxyURLConf) {

        // first check the system property, then fall back to configuration
        String proxyBase = (proxyURLSys != null) ? proxyURLSys : proxyURLConf;

        // perform the replacement if the proxy base is set,
        // otherwise return the baseURL unchanged
        if (proxyBase != null && proxyBase.trim().length() > 0) {
            baseURL.setLength(0);
            baseURL.append(proxyBase);
        }

        return baseURL;
    }

    /** Mangle URL using request headers */
    private StringBuilder mangleURLHeaders(StringBuilder baseURL) {

        // Request owsRequest = (Request) Dispatcher.REQUEST.get();
        // owsRequest.httpRequest.getHeaderNames();

        // first check the system property
        String proxyBase = GeoServerExtensions.getProperty("PROXY_BASE_URL");
        if (proxyBase == null) {
            // if no system property fall back to configuration
            proxyBase = geoServer.getSettings().getProxyBaseUrl();
        }

        // perform the replacement if the proxy base is set
        if (proxyBase != null && proxyBase.trim().length() > 0) {
            baseURL.setLength(0);
            baseURL.append(proxyBase);
        }

        return baseURL;
    }
}

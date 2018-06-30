/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.*;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.vfny.geoserver.util.Requests;

/** A URL mangler that replaces the base URL with the proxied one */
public class ProxifyingURLMangler implements URLMangler {

    GeoServer geoServer;
    private static Logger LOGGER = Logging.getLogger(ProxifyingURLMangler.class);
    public static String USEHEADERS_PARAM = "USEHEADERS_PROXYURL";
    public static String TEMPLATE_SEPARATOR = " ";
    public static String TEMPLATE_PREFIX = "${";
    public static String TEMPLATE_POSTFIX = "}";
    public static String TEMPLATE_HEADERPREFIX = "headers";

    public static final String FORWARDED_HEADER = "X-Forwarded";
    public static final String FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    public static final String FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    public static final String FORWARDED_PATH_HEADER = "X-Forwarded-Path";
    public static final String HOST_HEADER = "Host";

    public ProxifyingURLMangler(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    public void mangleURL(
            StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {

        // first check the system property, then fall back to configuration
        String proxyBase =
                (GeoServerExtensions.getProperty(Requests.PROXY_PARAM) != null)
                        ? GeoServerExtensions.getProperty(Requests.PROXY_PARAM)
                        : this.geoServer.getSettings().getProxyBaseUrl();

        // Mangles the URL base in different ways based on a flag
        // (for two reasons: a) speed; b) to make the admin aware of
        // possible security liabilities)
        baseURL =
                (this.geoServer.getGlobal().isUseHeadersProxyURL() == true && proxyBase != null)
                        ? this.mangleURLHeaders(baseURL, proxyBase)
                        : this.mangleURLFixedURL(baseURL, proxyBase);
    }

    /**
     * Mangle URL using the PROXY_BASE_URL
     *
     * @param baseURL URL to mangle
     * @param proxyBase Proxy base URL as taken from configuration
     * @return mangled proxy URL
     */
    private StringBuilder mangleURLFixedURL(StringBuilder baseURL, String proxyBase) {

        // perform the replacement if the proxy base is set,
        // otherwise return the baseURL unchanged
        if (proxyBase != null && proxyBase.trim().length() > 0) {
            baseURL.setLength(0);
            baseURL.append(proxyBase);
        }

        return baseURL;
    }

    /**
     * Mangle URL using request headers
     *
     * @param baseURL URL to mangle
     * @param proxyBase Proxy base URL as taken from configuration
     * @return mangled proxy URL
     */
    private StringBuilder mangleURLHeaders(StringBuilder baseURL, String proxyBase) {

        // If the proxy base URL does not contain templates, fall back to
        // the fixed URL cse
        if (!proxyBase.contains(TEMPLATE_PREFIX)) {
            return this.mangleURLFixedURL(baseURL, proxyBase);
        }

        // Cycles through templates in the proxy base URL until one is competely matched
        Map<String, String> headers = this.compileHeadersMap();
        for (String template : Arrays.asList(proxyBase.split(TEMPLATE_SEPARATOR))) {
            String candidate = QuickTemplate.replaceVariables(template, headers);
            if (!candidate.contains(TEMPLATE_PREFIX)) {
                baseURL.setLength(0);
                baseURL.append(candidate);
                break;
            }
        }

        return baseURL;
    }

    /** Compile Map of header templates and actual header values */
    private Map<String, String> compileHeadersMap() {

        Map<String, String> headers = new HashMap<String, String>();

        HttpServletRequest owsRequest = Dispatcher.REQUEST.get().getHttpRequest();
        List<String> headerNames =
                new ArrayList<String>(
                        Arrays.asList(
                                FORWARDED_HEADER,
                                FORWARDED_HOST_HEADER,
                                FORWARDED_PROTO_HEADER,
                                HOST_HEADER,
                                FORWARDED_PATH_HEADER));
        headerNames.forEach(
                (headerName) -> {
                    if (owsRequest.getHeader(headerName) != null) {
                        headers.put(
                                String.format(
                                        "%s%s%s", TEMPLATE_PREFIX, headerName, TEMPLATE_POSTFIX),
                                owsRequest.getHeader(headerName));
                    }
                });

        return headers;
    }
}

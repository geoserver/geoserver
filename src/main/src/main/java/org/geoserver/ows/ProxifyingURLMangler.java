/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.vfny.geoserver.util.Requests;

/** A URL mangler that replaces the base URL with the proxied one */
public class ProxifyingURLMangler implements URLMangler {

    public enum Headers {
        FORWARDED("Forwarded"),
        FORWARDED_PROTO("X-Forwarded-Proto"),
        FORWARDED_HOST("X-Forwarded-Host"),
        FORWARDED_PATH("X-Forwarded-Path"),
        FORWARDED_FOR("X-Forwarded-For"),
        HOST("Host");

        private String header;

        Headers(String h) {
            this.header = h;
        }

        public String asString() {
            return header;
        }
    }

    public enum ForwardedComponents {
        FOR("for"),
        BY("by"),
        PROTO("proto"),
        HOST("host"),
        PATH("path");

        private String comp;

        ForwardedComponents(String c) {
            this.comp = c;
        }

        public String asString() {
            return comp;
        }
    }

    GeoServer geoServer;

    public static String TEMPLATE_SEPARATOR = " ";
    public static String TEMPLATE_PREFIX = "${";
    public static String TEMPLATE_POSTFIX = "}";

    public static Map<String, Pattern> FORWARDED_PATTERNS = new HashMap<String, Pattern>();

    {
        Arrays.asList(ForwardedComponents.values())
                .forEach(
                        (comp) -> {
                            FORWARDED_PATTERNS.put(
                                    comp.asString(),
                                    Pattern.compile(
                                            String.format(
                                                    "(.*)%s=([^;^ ]+)(.*)", comp.asString())));
                        });
    }

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
        if (this.geoServer.getGlobal().isUseHeadersProxyURL() == true && proxyBase != null) {
            this.mangleURLHeaders(baseURL, proxyBase);
        } else {
            this.mangleURLFixedURL(baseURL, proxyBase);
        }
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

        // If the request is not an OWS request, does not proxy the URL
        if (Dispatcher.REQUEST.get() == null) {
            return baseURL;
        }

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

    /**
     * Compile Map of header templates and actual header values
     *
     * @return map of header names and values
     */
    private Map<String, String> compileHeadersMap() {

        Map<String, String> headers = new HashMap<String, String>();

        HttpServletRequest owsRequest = Dispatcher.REQUEST.get().getHttpRequest();
        Arrays.asList(Headers.values())
                .forEach(
                        (header) -> {
                            if (owsRequest.getHeader(header.asString()) != null) {
                                if (header == Headers.FORWARDED) {
                                    FORWARDED_PATTERNS.forEach(
                                            (comp, pattern) -> {
                                                Matcher m =
                                                        pattern.matcher(
                                                                owsRequest.getHeader(
                                                                        header.asString()));
                                                if (m.matches()) {
                                                    headers.put(
                                                            String.format(
                                                                    "%s%s%s",
                                                                    TEMPLATE_PREFIX,
                                                                    Headers.FORWARDED.asString()
                                                                            + "."
                                                                            + comp,
                                                                    TEMPLATE_POSTFIX),
                                                            m.group(2));
                                                }
                                            });
                                } else {
                                    headers.put(
                                            String.format(
                                                    "%s%s%s",
                                                    TEMPLATE_PREFIX,
                                                    header.asString(),
                                                    TEMPLATE_POSTFIX),
                                            owsRequest.getHeader(header.asString()));
                                }
                            }
                        });

        return headers;
    }
}

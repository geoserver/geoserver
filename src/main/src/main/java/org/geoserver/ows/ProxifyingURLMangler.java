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
import org.apache.commons.lang3.StringUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerEnvironment;
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

    public static Map<String, Pattern> FORWARDED_PATTERNS = new HashMap<>();

    {
        Arrays.asList(ForwardedComponents.values()).forEach((comp) -> {
            FORWARDED_PATTERNS.put(comp.asString(), Pattern.compile("(.*)%s=([^;^ ]+)(.*)".formatted(comp.asString())));
        });
    }

    public ProxifyingURLMangler(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    @Override
    public void mangleURL(StringBuilder baseURL, StringBuilder path, Map<String, String> kvp, URLType type) {

        // first check the system property, then fall back to configuration
        String proxyBase = (GeoServerExtensions.getProperty(Requests.PROXY_PARAM) != null)
                ? GeoServerExtensions.getProperty(Requests.PROXY_PARAM)
                : this.geoServer.getSettings().getProxyBaseUrl();

        // resolve parameters values if parametrization is activated
        proxyBase = resolveParametrization(proxyBase);

        // Mangles the URL base in different ways based on a flag
        // (for two reasons: a) speed; b) to make the admin aware of
        // possible security liabilities)
        boolean doMangleHeaders = resolveDoMangleHeaders();
        if (proxyBase != null && doMangleHeaders) {
            this.mangleURLHeaders(baseURL, proxyBase);
        } else {
            this.mangleURLFixedURL(baseURL, proxyBase);
        }
    }

    private boolean resolveDoMangleHeaders() {
        if (isUseHeadersSystemPropertyEnabled()) return true;
        Boolean wsAwareFlag = geoServer.getSettings().isUseHeadersProxyURL();
        Boolean resultFlag = wsAwareFlag != null
                ? wsAwareFlag
                : geoServer.getGlobal().getSettings().isUseHeadersProxyURL();
        if (resultFlag != null) return resultFlag;
        return false;
    }

    /**
     * Check if the PROXY_BASE_URL_HEADERS system property is set to use headers for proxying.
     *
     * @return true if the system property is set to use headers for proxying
     */
    private boolean isUseHeadersSystemPropertyEnabled() {
        String useHeadersProxyURL = GeoServerExtensions.getProperty(Requests.PROXY_HEADER_PARAM);
        return useHeadersProxyURL != null && "true".equalsIgnoreCase(useHeadersProxyURL.trim());
    }

    /** Resolve parameters values in the provided String if GeoServer parametrization is activated. */
    private String resolveParametrization(String proxyBase) {
        if (GeoServerEnvironment.allowEnvParametrization() && StringUtils.isNotBlank(proxyBase)) {
            GeoServerEnvironment gsEnvironment = GeoServerExtensions.bean(GeoServerEnvironment.class);
            proxyBase = (String) gsEnvironment.resolveValue(proxyBase);
        }
        return proxyBase;
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
        if (proxyBase != null && !proxyBase.trim().isEmpty()) {
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

    /**
     * Compile Map of header templates and actual header values
     *
     * @return map of header names and values
     */
    private Map<String, String> compileHeadersMap() {
        Map<String, String> headers = new HashMap<>();
        Arrays.asList(Headers.values()).forEach(h -> collectHeader(headers, h.asString()));

        return headers;
    }

    private void collectHeader(Map<String, String> headers, String headerName) {
        String headerValue = HTTPHeadersCollector.getHeader(headerName);
        if (headerValue != null) {
            if (headerName.equals(Headers.FORWARDED.asString())) {
                collectForwardedHeaders(headers, headerValue);
            } else {
                headers.put(toTemplate(headerName), headerValue);
            }
        }
    }

    private void collectForwardedHeaders(Map<String, String> headers, String headerValue) {
        FORWARDED_PATTERNS.forEach((comp, pattern) -> {
            Matcher m = pattern.matcher(headerValue);
            if (m.matches()) {
                String key = toTemplate(Headers.FORWARDED.asString() + "." + comp);
                headers.put(key, m.group(2));
            }
        });
    }

    private String toTemplate(String header) {
        return "%s%s%s".formatted(TEMPLATE_PREFIX, header, TEMPLATE_POSTFIX);
    }
}

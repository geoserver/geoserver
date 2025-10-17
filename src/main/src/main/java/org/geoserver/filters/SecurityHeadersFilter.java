/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import com.google.common.net.HttpHeaders;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.csp.CSPHeaderDAO;
import org.geotools.util.logging.Logging;

/**
 * Simple filter to set various security related HTTP response headers. The properties to control these headers can be
 * set via command line -D arg, web.xml init or environment variable.<br>
 * <br>
 * Two properties control the X-Frame-Options header to prevent click jacking attacks:<br>
 * - geoserver.xframe.shouldSetPolicy: controls whether to set the X-Frame-Options header. Default is true.<br>
 * - geoserver.xframe.policy: controls the value of the X-Frame-Options header. Default is SAMEORIGIN. Valid options are
 * DENY, SAMEORIGIN and ALLOW-FROM [uri] (ALLOW-FROM is unsupported in modern browsers and will be treated as if the
 * header was not set at all)<br>
 * <br>
 * One property controls the X-Content-Type-Options header to prevent cross-site scripting attacks that depend on
 * content sniffing:<br>
 * - geoserver.xContentType.shouldSetPolicy: controls whether to set the X-Content-Type-Options header. Default is true.
 * <br>
 * <br>
 * Two properties control the X-XSS-Protection header:<br>
 * - geoserver.xXssProtection.shouldSetPolicy: controls whether to set the X-XSS-Protection header. Default is false.
 * <br>
 * - geoserver.xXssProtection.policy: controls the value of the X-XSS-Protection header. Default is 0. Valid options are
 * 0, 1, 1; mode=block<br>
 * <br>
 * Two properties control the Strict-Transport-Security header to reduce the possibility of man-in-the-middle attacks:
 * <br>
 * - geoserver.hsts.shouldSetPolicy: controls whether to set the Strict-Transport-Security header. This header will only
 * be added to HTTPS requests. Default is false.<br>
 * - geoserver.hsts.policy: controls the value of the Strict-Transport-Security header. Default is "max-age=31536000 ;
 * includeSubDomains". Valid options can change the max-age to the desired age in seconds and can omit the
 * includeSubDomains directive.<br>
 * <br>
 * The Content-Security-Policy header to prevent cross-site scripting attacks is set based on a separate configuration
 * file.
 */
public class SecurityHeadersFilter implements Filter {

    private static final Logger LOGGER = Logging.getLogger(SecurityHeadersFilter.class);

    private static final String DEFAULT_HSTS_POLICY = "max-age=31536000 ; includeSubDomains";
    private static final String DEFAULT_FRAME_POLICY = "SAMEORIGIN";
    private static final String DEFAULT_XXSS_POLICY = "0";

    /** The system property to set whether the Strict-Transport-Security should be set */
    public static final String GEOSERVER_HSTS_SHOULD_SET_POLICY = "geoserver.hsts.shouldSetPolicy";

    /** The system property for the value of the Strict-Transport-Security header */
    public static final String GEOSERVER_HSTS_POLICY = "geoserver.hsts.policy";

    /** The system property to set whether the X-Frame-Options header should be set */
    public static final String GEOSERVER_XFRAME_SHOULD_SET_POLICY = "geoserver.xframe.shouldSetPolicy";

    /** The system property for the value of the X-Frame-Options header */
    public static final String GEOSERVER_XFRAME_POLICY = "geoserver.xframe.policy";

    /** The system property to set whether the X-Content-Type-Options header should be set */
    public static final String GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY = "geoserver.xContentType.shouldSetPolicy";

    /** The system property to set whether the X-XSS-Protection header should be set */
    public static final String GEOSERVER_XXSS_PROTECTION_SHOULD_SET_POLICY = "geoserver.xXssProtection.shouldSetPolicy";

    /** The system property for the value of the X-XSS-Protection header */
    public static final String GEOSERVER_XXSS_PROTECTION_POLICY = "geoserver.xXssProtection.policy";

    private volatile Map<String, Object> cache = null;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        Map<String, Object> map = getCache();
        if (request.isSecure() && (Boolean) map.get(GEOSERVER_HSTS_SHOULD_SET_POLICY)) {
            httpResponse.setHeader(HttpHeaders.STRICT_TRANSPORT_SECURITY, (String) map.get(GEOSERVER_HSTS_POLICY));
        }
        if ((Boolean) map.get(GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY)) {
            // there is no other valid value for this header
            httpResponse.setHeader(HttpHeaders.X_CONTENT_TYPE_OPTIONS, "nosniff");
        }
        if ((Boolean) map.get(GEOSERVER_XFRAME_SHOULD_SET_POLICY)) {
            httpResponse.setHeader(HttpHeaders.X_FRAME_OPTIONS, (String) map.get(GEOSERVER_XFRAME_POLICY));
        }
        if ((Boolean) map.get(GEOSERVER_XXSS_PROTECTION_SHOULD_SET_POLICY)) {
            httpResponse.setHeader(HttpHeaders.X_XSS_PROTECTION, (String) map.get(GEOSERVER_XXSS_PROTECTION_POLICY));
        }

        try {
            response = GeoServerExtensions.bean(CSPHeaderDAO.class)
                    .setContentSecurityPolicy((HttpServletRequest) request, httpResponse);
            chain.doFilter(request, response);
        } finally {
            CSPHeaderDAO.removeProxyPolicy();
        }
    }

    @Override
    public void destroy() {}

    private Map<String, Object> getCache() {
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
                    cache = initializeCache();
                }
            }
        }
        return cache;
    }

    private static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getStringProperty(key, null);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    private static String getStringProperty(String key, String defaultValue) {
        String value = GeoServerExtensions.getProperty(key);
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    private static Map<String, Object> initializeCache() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(GEOSERVER_HSTS_SHOULD_SET_POLICY, getBooleanProperty(GEOSERVER_HSTS_SHOULD_SET_POLICY, false));
        map.put(GEOSERVER_HSTS_POLICY, getStringProperty(GEOSERVER_HSTS_POLICY, DEFAULT_HSTS_POLICY));
        map.put(
                GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY,
                getBooleanProperty(GEOSERVER_XCONTENT_TYPE_SHOULD_SET_POLICY, true));
        map.put(GEOSERVER_XFRAME_SHOULD_SET_POLICY, getBooleanProperty(GEOSERVER_XFRAME_SHOULD_SET_POLICY, true));
        map.put(GEOSERVER_XFRAME_POLICY, getStringProperty(GEOSERVER_XFRAME_POLICY, DEFAULT_FRAME_POLICY));
        map.put(
                GEOSERVER_XXSS_PROTECTION_SHOULD_SET_POLICY,
                getBooleanProperty(GEOSERVER_XXSS_PROTECTION_SHOULD_SET_POLICY, false));
        map.put(
                GEOSERVER_XXSS_PROTECTION_POLICY,
                getStringProperty(GEOSERVER_XXSS_PROTECTION_POLICY, DEFAULT_XXSS_POLICY));
        LOGGER.fine(() -> "Security HTTP response header settings: \n "
                + map.entrySet().stream()
                        .map(e -> e.getKey() + " = " + e.getValue())
                        .collect(Collectors.joining("\n ")));
        return Collections.unmodifiableMap(map);
    }
}

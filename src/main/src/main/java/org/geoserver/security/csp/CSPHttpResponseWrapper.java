/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY;
import static com.google.common.net.HttpHeaders.CONTENT_SECURITY_POLICY_REPORT_ONLY;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.util.logging.Logging;

/**
 * {@link HttpServletResponse} wrapper that merges the Content-Security-Policy headers from GeoServer and Wicket 9+.
 * This is primarily intended to merge the frame-ancestors directive that GeoServer will set by default but Wicket does
 * not set by default when Wicket replaces the header set by GeoServer.
 */
public class CSPHttpResponseWrapper extends HttpServletResponseWrapper {

    /** The CSPHttpResponseWrapper class logger. */
    private static final Logger LOGGER = Logging.getLogger(CSPHttpResponseWrapper.class);

    /** The CSP configuration */
    private final CSPConfiguration config;

    /**
     * @param response the response object to wrap
     * @param config the CSP configuration
     */
    public CSPHttpResponseWrapper(HttpServletResponse response, CSPConfiguration config) {
        super(response);
        this.config = config;
    }

    /**
     * Override this method to handle other components (e.g., Wicket) setting their own Content-Security-Policy or
     * Content-Security-Policy-Report-Only header.
     *
     * @param name the header name
     * @param value the header value
     */
    @Override
    public void setHeader(String name, String value) {
        if (CONTENT_SECURITY_POLICY.equalsIgnoreCase(name)
                || CONTENT_SECURITY_POLICY_REPORT_ONLY.equalsIgnoreCase(name)) {
            setContentSecurityPolicy(name, value);
        } else {
            super.setHeader(name, value);
        }
    }

    /**
     * This method will allow Wicket 9+ and other modules to completely replace the Content-Security-Policy header set
     * by GeoServer if that is enabled in the configuration. Otherwise, non-fetch directives from the GeoServer CSP that
     * are not included in the new CSP will be consolidated and appended to the CSP of the HTTP response.
     *
     * @param newName the header name
     * @param newValue the header value
     */
    private void setContentSecurityPolicy(String newName, String newValue) {
        if (!this.config.isEnabled()) {
            // if CSP is disabled and override is true set the CSP header; otherwise ignore it
            if (this.config.isAllowOverride()) {
                super.setHeader(newName, newValue);
            }
            return;
        }
        String oldName = this.config.isReportOnly() ? CONTENT_SECURITY_POLICY_REPORT_ONLY : CONTENT_SECURITY_POLICY;
        String oldValue = getHeader(oldName);
        if (this.config.isAllowOverride()) {
            if (oldValue != null && !newName.equalsIgnoreCase(oldName)) {
                super.setHeader(oldName, null);
            }
            String name = CONTENT_SECURITY_POLICY.equalsIgnoreCase(newName)
                    ? CONTENT_SECURITY_POLICY
                    : CONTENT_SECURITY_POLICY_REPORT_ONLY;
            LOGGER.fine(
                    () -> "Overriding header:\n Old" + oldName + ": " + oldValue + "\n New" + name + ": " + newValue);
            super.setHeader(name, newValue);
        } else {
            String merged = getMergedHeader(oldValue, newValue);
            LOGGER.fine(() -> "Merging "
                    + oldName
                    + " header:\n Old: "
                    + oldValue
                    + "\n New: "
                    + newValue
                    + "\n Merged: "
                    + merged);
            super.setHeader(oldName, merged);
        }
    }

    /**
     * Checks if the directive is not included in the new CSP and is not a fetch directive.
     *
     * @param value the new Content-Security-Policy header value
     * @param directive the directive to check
     * @return the directive to merge or null
     */
    private static String getMergedDirective(String value, String directive) {
        int index = directive.indexOf(' ');
        String name = index < 0 ? directive : directive.substring(0, index);
        return (!name.contains("-src") && !value.contains(name)) ? directive : null;
    }

    /**
     * Appends non-fetch directives from the old CSP that are not already included in the new CSP.
     *
     * @param oldValue the old Content-Security-Policy header value
     * @param newValue the new Content-Security-Policy header value
     * @return the merged Content-Security-Policy header value
     */
    private static String getMergedHeader(String oldValue, String newValue) {
        if (oldValue != null) {
            String toMerge = Arrays.stream(oldValue.split(","))
                    .map(s -> s.split(";"))
                    .flatMap(Arrays::stream)
                    .map(String::trim)
                    .map(s -> getMergedDirective(newValue, s))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("; "));
            if (!toMerge.isEmpty()) {
                return newValue + (newValue.endsWith(";") ? "" : ";") + ", " + toMerge + ';';
            }
        }
        return newValue;
    }
}

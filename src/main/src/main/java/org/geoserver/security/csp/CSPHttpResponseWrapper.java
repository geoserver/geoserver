/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import com.google.common.net.HttpHeaders;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import org.geotools.util.logging.Logging;

/**
 * {@link HttpServletResponse} wrapper that merges the Content-Security-Policy headers from
 * GeoServer and Wicket 9+. This is primarily intended to merge the frame-ancestors directive that
 * GeoServer will set by default but Wicket does not set by default when Wicket replaces the header
 * set by GeoServer.
 */
public class CSPHttpResponseWrapper extends HttpServletResponseWrapper {

    /** The CSPHttpResponseWrapper class logger. */
    private static final Logger LOGGER = Logging.getLogger(CSPHttpResponseWrapper.class);

    /** @param response the response object to wrap */
    public CSPHttpResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    /**
     * Override this method to prevent Wicket 9+ from completely replacing the
     * Content-Security-Policy header set by GeoServer. Non-fetch directives from GeoServer CSP that
     * are not included in the new CSP will be consolidated and appended to the CSP of the HTTP
     * response.
     *
     * @param name the header name
     * @param value the header value
     */
    @Override
    public void setHeader(String name, String value) {
        if (!HttpHeaders.CONTENT_SECURITY_POLICY.equalsIgnoreCase(name)) {
            super.setHeader(name, value);
            return;
        }
        // there should always be an existing header value if this wrapper was created
        String oldValue = getHeader(name);
        String toMerge =
                Arrays.stream(oldValue.split(","))
                        .map(s -> s.split(";"))
                        .flatMap(Arrays::stream)
                        .map(String::trim)
                        .map(s -> getMergedDirective(value, s))
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("; "));
        String newValue = value + (toMerge.isEmpty() ? "" : (", " + toMerge + ';'));
        LOGGER.fine(
                () ->
                        "Merging Content-Security-Policy header:\n Old: "
                                + oldValue
                                + "\n New: "
                                + value
                                + "\n Merged: "
                                + newValue);
        super.setHeader(HttpHeaders.CONTENT_SECURITY_POLICY, newValue);
    }

    /**
     * Checks if the directive is not included in the new CSP value and is not a fetch directive.
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
}

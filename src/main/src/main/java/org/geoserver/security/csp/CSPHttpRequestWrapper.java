/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.csp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

/** {@link HttpServletRequest} wrapper that provides access to the CSP configuration. */
public class CSPHttpRequestWrapper extends HttpServletRequestWrapper {

    /** The CSP configuration */
    private final CSPConfiguration config;

    /**
     * @param request the request object to wrap
     * @param config the CSP configuration
     */
    public CSPHttpRequestWrapper(HttpServletRequest request, CSPConfiguration config) {
        super(request);
        this.config = config;
    }

    /** @return the CSP configuration */
    public CSPConfiguration getConfig() {
        return this.config;
    }
}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Fail-closed authentication filter used as a stand-in when a real authentication filter could not be loaded (for
 * example because it was created by a community plugin that is no longer installed).
 *
 * <p>It denies every request with {@code 403 Forbidden}. The security subsystem injects it into a request filter chain
 * only when that chain has lost its last authentication filter and has no security interceptor to otherwise enforce
 * access control, so that a previously protected chain does not silently become open.
 */
public class DisabledSecurityFilter extends GeoServerSecurityFilter implements GeoServerAuthenticationFilter {

    static final String DENY_MESSAGE =
            "Access denied: this request matched a security filter chain whose authentication filter is "
                    + "disabled because it could not be loaded. An administrator must reconfigure or remove "
                    + "the offending security filter.";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, DENY_MESSAGE);
    }

    @Override
    public boolean applicableForHtml() {
        return true;
    }

    @Override
    public boolean applicableForServices() {
        return true;
    }
}

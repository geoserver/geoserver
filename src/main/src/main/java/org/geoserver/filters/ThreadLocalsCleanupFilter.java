/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.geotools.referencing.CRS;

public class ThreadLocalsCleanupFilter implements Filter {

    @Override
    public void destroy() {
        CRS.cleanupThreadLocals();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            CRS.cleanupThreadLocals();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        CRS.cleanupThreadLocals();
    }
}

/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.geotools.referencing.CRS;

public class ThreadLocalsCleanupFilter implements Filter {

    public void destroy() {
        CRS.cleanupThreadLocals();
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            CRS.cleanupThreadLocals();
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        CRS.cleanupThreadLocals();
    }
    
}

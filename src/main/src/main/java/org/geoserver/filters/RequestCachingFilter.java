/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.geoserver.catalog.impl.RequestCachingFacade;

/** Opens the {@link RequestCachingFacade} cache for the duration of a request. */
public class RequestCachingFilter implements GeoServerFilter {

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // a forward runs the filter again on the same thread: the outer request keeps the cache
        boolean opened = RequestCachingFacade.open();
        try {
            chain.doFilter(request, response);
        } finally {
            if (opened) {
                RequestCachingFacade.close();
            }
        }
    }

    @Override
    public void destroy() {}
}

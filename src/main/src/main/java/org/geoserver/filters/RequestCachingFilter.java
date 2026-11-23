/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.geoserver.catalog.impl.RequestCachingFacade;

/** Opens the {@link RequestCachingFacade} cache for the duration of a request. */
public class RequestCachingFilter implements GeoServerFilter {

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
}

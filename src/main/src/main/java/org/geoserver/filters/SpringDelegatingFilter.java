/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import java.io.IOException;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;

/**
 * A composite servlet filter that loaders delegate filters from a spring context, rather then from
 * the traditional web.xml. This class allows for servlet filters to be dynamically contributed via
 * a spring context.
 *
 * <p>It will pick up any filter declared in the Spring context that implements the {@link
 * GeoServerFilter} interface. Ordering of the filters won't normally be predictable, but as with
 * all other extension points it's possible to force a specific ordering by having filters implement
 * the {@link ExtensionPriority} interface
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class SpringDelegatingFilter implements Filter {

    List<GeoServerFilter> filters;

    public void init(FilterConfig filterConfig) throws ServletException {
        filters = GeoServerExtensions.extensions(GeoServerFilter.class);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        new Chain(chain).doFilter(request, response);
    }

    public void destroy() {
        if (filters != null) {
            for (Filter f : filters) {
                f.destroy();
            }
            filters = null;
        }
    }

    class Chain implements FilterChain {

        FilterChain delegate;
        int filter = 0;

        public Chain(FilterChain chain) {
            this.delegate = chain;
        }

        public void doFilter(ServletRequest request, ServletResponse response)
                throws IOException, ServletException {

            if (filter < filters.size()) {
                filters.get(filter++).doFilter(request, response, this);
            } else {
                // resume the actual chain
                delegate.doFilter(request, response);
            }
        }
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
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
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet filter making sure we cannot end up calling flush() on the response output stream after
 * close() has been called (https://osgeo-org.atlassian.net/browse/GEOS-5985)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FlushSafeFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // if we are dealing with an HTTP response, wrap it so that flush cannot
        // be called after close, which makes Tomcat APR runtime crash the JVM
        // (https://osgeo-org.atlassian.net/browse/GEOS-5985)
        if (response instanceof HttpServletResponse) {
            HttpServletResponse hr = (HttpServletResponse) response;
            response = new FlushSafeResponse(hr);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // nothing to do
    }
}

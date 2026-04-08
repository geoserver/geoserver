/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * A servlet filter that allows for advanced dispatching.
 *
 * <p>This fiter allows for a single mapping from web.xml for all requests to the spring dispatcher. It creates a
 * wrapper around the servlet request object that "fakes" the serveltPath property to make it look like the mapping was
 * created in web.xml when in actuality it was created in spring.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AdvancedDispatchFilter implements Filter {

    @Override
    public void destroy() {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest servletRequest) {
            request = new AdvancedDispatchHttpRequest(servletRequest);
        }
        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    static class AdvancedDispatchHttpRequest extends HttpServletRequestWrapper {

        String servletPath = null;

        public AdvancedDispatchHttpRequest(HttpServletRequest delegate) {
            super(delegate);

            if (delegate.getClass().getSimpleName().endsWith("MockHttpServletRequest")) {
                return;
            }

            String path = delegate.getPathInfo();

            if (path == null) {
                return;
            }

            int slash = path.indexOf('/', 1);
            if (slash > -1) {
                this.servletPath = path.substring(0, slash);
            } else {
                this.servletPath = path;
            }

            int question = this.servletPath.indexOf('?');
            if (question > -1) {
                this.servletPath = this.servletPath.substring(0, question);
            }
        }

        @Override
        public String getPathInfo() {
            HttpServletRequest delegate = (HttpServletRequest) getRequest();
            if (servletPath != null
                    && delegate.getPathInfo() != null
                    && delegate.getPathInfo().startsWith(servletPath))
                return delegate.getPathInfo().substring(servletPath.length());
            else return delegate.getPathInfo();
        }

        @Override
        public String getServletPath() {
            return servletPath != null ? servletPath : ((HttpServletRequest) getRequest()).getServletPath();
        }
    }
}

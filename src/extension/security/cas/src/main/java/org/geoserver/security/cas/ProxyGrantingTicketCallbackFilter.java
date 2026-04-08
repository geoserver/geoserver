/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.apereo.cas.client.proxy.ProxyGrantingTicketStorage;
import org.apereo.cas.client.util.WebUtils;
import org.springframework.beans.factory.BeanNameAware;

/**
 * a singleton {@link Filter} object receiving callbacks for proxy granting tickets from a cas server
 *
 * @author christian
 */
public class ProxyGrantingTicketCallbackFilter implements Filter, BeanNameAware {

    String name;
    ProxyGrantingTicketStorage pgtStorageFilter;

    public ProxyGrantingTicketCallbackFilter(ProxyGrantingTicketStorage pgtStorageFilter) {
        this.pgtStorageFilter = pgtStorageFilter;
    }

    public String getName() {
        return name;
    }

    @Override
    public void setBeanName(String name) {
        this.name = name;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    /** extract a proxy granting tickets and store it in the global {@link ProxyGrantingTicketStorage} object */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        WebUtils.readAndRespondToProxyReceptorRequest(
                (HttpServletRequest) request, (HttpServletResponse) response, pgtStorageFilter);
    }

    @Override
    public void destroy() {}
}

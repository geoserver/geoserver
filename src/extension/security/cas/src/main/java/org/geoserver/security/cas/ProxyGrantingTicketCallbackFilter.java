/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.util.CommonUtils;
import org.springframework.beans.factory.BeanNameAware;

/**
 * a singleton {@link Filter} object receiving callbacks for proxy granting tickets from a cas
 * server
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

    /**
     * extract a proxy granting tickets and store it in the global {@link
     * ProxyGrantingTicketStorage} object
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        CommonUtils.readAndRespondToProxyReceptorRequest(
                (HttpServletRequest) request, (HttpServletResponse) response, pgtStorageFilter);
    }

    @Override
    public void destroy() {}
}

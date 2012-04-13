/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
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

import org.geoserver.platform.GeoServerExtensions;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.util.CommonUtils;

/**
 * a singleton {@link Filter} object receiving
 * callbacks for proxy granting tickets from a cas
 * server 
 * 
 * @author christian
 *
 */
public class ProxyGrantingTicketCallbackFilter implements Filter{
    
    
    static public ProxyGrantingTicketCallbackFilter get() {        
        return GeoServerExtensions.bean(ProxyGrantingTicketCallbackFilter.class);
    }
    
    static public ProxyGrantingTicketStorage getPGTStorage() {                
        return GeoServerExtensions.bean(ProxyGrantingTicketStorage.class);
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /** 
     * extract a proxy granting tickets and store it in the global
     * {@link ProxyGrantingTicketStorage} object
     * 
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        CommonUtils.readAndRespondToProxyReceptorRequest(
                (HttpServletRequest)request,
                (HttpServletResponse) response,
                ProxyGrantingTicketCallbackFilter.getPGTStorage());
        return;
    }

    @Override
    public void destroy() {
    }


}

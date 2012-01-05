/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.geoserver.filters.GeoServerFilter;
import org.geoserver.platform.ExtensionPriority;
import org.springframework.security.userdetails.UserDetailsService;

/**
 * A dispatcher callback checking for an <code>authkey</code> parameter in the request. If the
 * parameter is found is it run against a {@link AuthenticationKeyMapper} and against the specified
 * {@link UserDetailsService} to lookup a user, if the process fails the request will be rejected as
 * non authenticated
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class AuthenticationKeyFilter implements GeoServerFilter, ExtensionPriority {

    AuthenticationKeyManager manager;

    public AuthenticationKeyFilter(AuthenticationKeyManager manager) {
        this.manager = manager;
    }

    public int getPriority() {
        // we need to run before anything else, 
        return ExtensionPriority.HIGHEST;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do here
        
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // see if we have the authkey property available (we have to do a case insensitive match)
        String key = null;
        Enumeration<String> paramNames = request.getParameterNames();
        while(paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            if(KeyAuthenticationToken.KEY.equalsIgnoreCase(name)) {
                key = request.getParameter(name);
                break;
            }
        }
        if (key != null) {
            // found it, map it to a user name
            manager.authenticate(key);
        }

        chain.doFilter(request, response);
    }

    public void destroy() {
        // nothing to do here either
        
    }

}

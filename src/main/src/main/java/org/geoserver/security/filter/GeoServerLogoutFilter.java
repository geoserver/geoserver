/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security.filter;

import java.io.IOException;

import javax.servlet.ServletException;

import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * User name / password authentication filter
 * 
 * 
 * @author christian
 * 
 */
public class GeoServerLogoutFilter extends GeoServerCompositeFilter {

    public static final String URL_AFTER_LOGOUT="/web/";
    public static final String URL_FOR_LOGOUT= "/j_spring_security_logout";
    

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        RememberMeServices rms = securityManager.getRememberMeService(); 

        // add logout filter
        LogoutFilter logoutFilter = new LogoutFilter(URL_AFTER_LOGOUT, (LogoutHandler) rms,
                new SecurityContextLogoutHandler());
        logoutFilter.setFilterProcessesUrl(URL_FOR_LOGOUT);
        
        try {
            logoutFilter.afterPropertiesSet();
        } catch (ServletException e1) {
            throw new IOException(e1);
        }
        getNestedFilters().add(logoutFilter);

    }
}

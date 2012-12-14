/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;

import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;

/**
 * Named RemeberMe Authentication Filter
 * 
 * @author mcr
 *
 */
public class GeoServerRememberMeAuthenticationFilter extends GeoServerCompositeFilter 
    implements GeoServerAuthenticationFilter {

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        
//       not needed at the moment        
//        RememberMeAuthenticationFilterConfig authConfig = 
//                (RememberMeAuthenticationFilterConfig) config;
        
        RememberMeAuthenticationFilter filter = new RememberMeAuthenticationFilter(
                getSecurityManager(),getSecurityManager().getRememberMeService());
        filter.afterPropertiesSet();
        getNestedFilters().add(filter);        
    }

    /**
     * @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForHtml()
     */
    @Override
    public boolean applicableForHtml() {
        return true;
    }


    /**
     * @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForServices()
     */
    @Override
    public boolean applicableForServices() {
        return false;
    }

}

/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;

import javax.servlet.ServletException;

import org.geoserver.security.config.SecurityContextPersistenceFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

/**
 * Security context persitence filter 
 * 
 * @author mcr
 *
 */
public class GeoServerSecurityContextPersistenceFilter extends GeoServerCompositeFilter {
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        
        
        SecurityContextPersistenceFilterConfig pConfig = 
                (SecurityContextPersistenceFilterConfig) config;
                
        HttpSessionSecurityContextRepository repo = new HttpSessionSecurityContextRepository();
        SecurityContextPersistenceFilter filter = new SecurityContextPersistenceFilter(repo);
        repo.setAllowSessionCreation(pConfig.isAllowSessionCreation());        
        filter.setForceEagerSessionCreation(false);

        try {
            filter.afterPropertiesSet();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        getNestedFilters().add(filter);        
    }
    
}

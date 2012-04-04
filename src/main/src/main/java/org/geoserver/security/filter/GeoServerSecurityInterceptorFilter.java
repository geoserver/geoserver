/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.SecurityInterceptorFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;

/**
 * Security interceptor filter
 * 
 * @author mcr
 *
 */
public class GeoServerSecurityInterceptorFilter extends GeoServerCompositeFilter {
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        
        
        SecurityInterceptorFilterConfig siConfig = 
                (SecurityInterceptorFilterConfig) config;
        
        FilterSecurityInterceptor filter = new FilterSecurityInterceptor();

        filter.setAuthenticationManager(getSecurityManager());
        
        List<AccessDecisionVoter> voters = new ArrayList<AccessDecisionVoter>();
        voters.add(new RoleVoter());
        voters.add(new AuthenticatedVoter());
        AffirmativeBased accessDecisionManager = new AffirmativeBased(voters); 
        accessDecisionManager.setAllowIfAllAbstainDecisions(siConfig.isAllowIfAllAbstainDecisions());
        filter.setAccessDecisionManager(accessDecisionManager);        
        
        // TODO, Justin, is this correct
        filter.setSecurityMetadataSource((FilterInvocationSecurityMetadataSource)
                    GeoServerExtensions.bean(siConfig.getSecurityMetadataSource()));
        try {
            filter.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException();
        }
        getNestedFilters().add(filter);        
    }
    
}

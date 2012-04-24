/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AbstractFilterProvider;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;

/**
 * Security provider for CAS
 * 
 * @author mcr
 */
public class GeoServerCasProxiedAuthenticationProvider extends AbstractFilterProvider {

    protected ProxyGrantingTicketStorage pgtStorageFilter;

    public GeoServerCasProxiedAuthenticationProvider(ProxyGrantingTicketStorage pgtStorageFilter) {
        this.pgtStorageFilter = pgtStorageFilter;
    }

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("casProxiedAuthentication", GeoServerCasProxiedAuthenticationFilter.class);
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerCasProxiedAuthenticationFilter.class;
    }

    @Override
    public GeoServerSecurityFilter createFilter(SecurityNamedServiceConfig config) {
        return new GeoServerCasProxiedAuthenticationFilter(pgtStorageFilter);
    }
    
    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new CasFilterConfigValidator(securityManager);
    }


}

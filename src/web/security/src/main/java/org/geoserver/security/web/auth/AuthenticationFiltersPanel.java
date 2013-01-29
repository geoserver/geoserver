/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geoserver.security.web.SecurityNamedServicesPanel;

public class AuthenticationFiltersPanel extends SecurityNamedServicesPanel<SecurityAuthFilterConfig>{

    public AuthenticationFiltersPanel(String id) {
        super(id, new AuthenticationFiltersProvider());
    }

    @Override
    protected Class getServiceClass() {
        return GeoServerAuthenticationFilter.class;
    }

    @Override
    protected void validateRemoveConfig(SecurityAuthFilterConfig config)
            throws SecurityConfigException {
        SecurityConfigValidator.getConfigurationValiator(GeoServerSecurityFilter.class, 
            config.getClassName()).validateRemoveFilter(config);
    }

    @Override
    protected void removeConfig(SecurityAuthFilterConfig config)
            throws Exception {
        getSecurityManager().removeFilter(config);
    }

}

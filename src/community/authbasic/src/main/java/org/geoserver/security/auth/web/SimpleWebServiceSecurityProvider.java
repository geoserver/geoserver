/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth.web;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.filter.GeoServerUserNamePasswordAuthenticationFilter;
import org.geoserver.security.validation.SecurityConfigValidator;

public class SimpleWebServiceSecurityProvider extends GeoServerSecurityProvider {

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("webauthservice", SimpleWebAuthenticationConfig.class);
    }

    @Override
    public void init(GeoServerSecurityManager manager) {
        super.init(manager);
    }

    @Override
    public Class<? extends GeoServerAuthenticationProvider> getAuthenticationProviderClass() {

        return SimpleWebServiceAuthenticationProvider.class;
    }

    @Override
    public GeoServerAuthenticationProvider createAuthenticationProvider(
            SecurityNamedServiceConfig config) {

        return new SimpleWebServiceAuthenticationProvider();
    }

    @Override
    public Class<? extends GeoServerSecurityFilter> getFilterClass() {
        return GeoServerUserNamePasswordAuthenticationFilter.class;
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new SimpleWebAuthenticationConfigValidator(securityManager);
    }
}

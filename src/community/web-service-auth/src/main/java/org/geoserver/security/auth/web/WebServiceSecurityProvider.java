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
import org.geoserver.security.validation.SecurityConfigValidator;

public class WebServiceSecurityProvider extends GeoServerSecurityProvider {

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("webauthservice", WebAuthenticationConfig.class);
    }

    @Override
    public void init(GeoServerSecurityManager manager) {
        super.init(manager);
    }

    @Override
    public Class<? extends GeoServerAuthenticationProvider> getAuthenticationProviderClass() {

        return WebServiceAuthenticationProvider.class;
    }

    @Override
    public GeoServerAuthenticationProvider createAuthenticationProvider(
            SecurityNamedServiceConfig config) {

        return new WebServiceAuthenticationProvider();
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new WebAuthenticationConfigValidator(securityManager);
    }
}

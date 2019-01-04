/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.security.validation.SecurityConfigValidator;
import org.geoserver.security.web.SecurityNamedServicesPanel;

/**
 * Panel for providing list of authentication provider configurations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AuthenticationProvidersPanel
        extends SecurityNamedServicesPanel<SecurityAuthProviderConfig> {

    public AuthenticationProvidersPanel(String id) {
        super(id, new AuthenticationProviderProvider());
    }

    @Override
    protected Class getServiceClass() {
        return GeoServerAuthenticationProvider.class;
    }

    @Override
    protected void validateRemoveConfig(SecurityAuthProviderConfig config)
            throws SecurityConfigException {
        SecurityConfigValidator.getConfigurationValiator(
                        GeoServerAuthenticationProvider.class, config.getClassName())
                .validateRemoveAuthProvider(config);
    }

    @Override
    protected void removeConfig(SecurityAuthProviderConfig config) throws Exception {

        getSecurityManager().removeAuthenticationProvider(config);
    }
}

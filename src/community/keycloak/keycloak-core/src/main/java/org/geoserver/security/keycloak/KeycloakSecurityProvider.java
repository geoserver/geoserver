/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import java.io.IOException;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.SecurityConfigValidator;

/** Registers the Keycloak role service with GeoServer's security framework. */
public class KeycloakSecurityProvider extends GeoServerSecurityProvider {

    @Override
    public void configure(XStreamPersister xp) {
        super.configure(xp);
        xp.getXStream().alias("keycloakRoleService", KeycloakRoleServiceConfig.class);
    }

    @Override
    public Class<? extends GeoServerRoleService> getRoleServiceClass() {
        return KeycloakRoleService.class;
    }

    @Override
    public GeoServerRoleService createRoleService(SecurityNamedServiceConfig config) throws IOException {
        return new KeycloakRoleService();
    }

    @Override
    public SecurityConfigValidator createConfigurationValidator(GeoServerSecurityManager securityManager) {
        return new SecurityConfigValidator(securityManager);
    }
}

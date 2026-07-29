/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.keycloak;

import org.geoserver.security.keycloak.KeycloakRoleService;
import org.geoserver.security.keycloak.KeycloakRoleServiceConfig;
import org.geoserver.security.web.role.RoleServicePanelInfo;

/** Registers {@link KeycloakRoleServicePanel} with GeoServer's security UI. */
public class KeycloakRoleServicePanelInfo
        extends RoleServicePanelInfo<KeycloakRoleServiceConfig, KeycloakRoleServicePanel> {

    public KeycloakRoleServicePanelInfo() {
        setComponentClass(KeycloakRoleServicePanel.class);
        setServiceClass(KeycloakRoleService.class);
        setServiceConfigClass(KeycloakRoleServiceConfig.class);
        setPriority(0);
    }
}

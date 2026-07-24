/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import org.geoserver.security.config.SecurityRoleServiceConfig;

/** Configuration for {@link KeycloakRoleService}. */
public class KeycloakRoleServiceConfig extends KeycloakSecurityServiceConfig implements SecurityRoleServiceConfig {

    protected String adminRoleName;
    protected String groupAdminRoleName;
    private String idsOfClientsList;

    public KeycloakRoleServiceConfig() {}

    public KeycloakRoleServiceConfig(KeycloakRoleServiceConfig other) {
        super(other);
        adminRoleName = other.getAdminRoleName();
        groupAdminRoleName = other.getGroupAdminRoleName();
        idsOfClientsList = other.getIdsOfClientsList();
    }

    @Override
    public String getAdminRoleName() {
        return adminRoleName;
    }

    @Override
    public void setAdminRoleName(String name) {
        adminRoleName = name;
    }

    @Override
    public String getGroupAdminRoleName() {
        return groupAdminRoleName;
    }

    @Override
    public void setGroupAdminRoleName(String groupAdminRoleName) {
        this.groupAdminRoleName = groupAdminRoleName;
    }

    public String getIdsOfClientsList() {
        return idsOfClientsList;
    }

    public void setIdsOfClientsList(String idsOfClientsList) {
        this.idsOfClientsList = idsOfClientsList;
    }
}

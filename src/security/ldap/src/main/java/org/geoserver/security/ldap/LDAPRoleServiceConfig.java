/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import org.geoserver.security.config.SecurityRoleServiceConfig;

/**
 * Configuration class for the LDAPRoleService.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 */
public class LDAPRoleServiceConfig extends LDAPBaseSecurityServiceConfig
        implements SecurityRoleServiceConfig {

    private static final long serialVersionUID = -5731528105464984100L;

    public LDAPRoleServiceConfig() {}

    public LDAPRoleServiceConfig(LDAPRoleServiceConfig other) {
        super(other);
    }

    @Override
    public String getAdminRoleName() {
        return getAdminGroup();
    }

    @Override
    public void setAdminRoleName(String adminRoleName) {
        setAdminGroup(adminRoleName);
    }

    @Override
    public String getGroupAdminRoleName() {
        return getGroupAdminGroup();
    }

    @Override
    public void setGroupAdminRoleName(String adminRoleName) {
        setGroupAdminGroup(adminRoleName);
    }
}

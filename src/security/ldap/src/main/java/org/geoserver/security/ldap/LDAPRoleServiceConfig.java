/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
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
 *
 */
public class LDAPRoleServiceConfig extends LDAPBaseSecurityServiceConfig implements SecurityRoleServiceConfig {
    // user complete name and password for authenticated search of roles
    private String user;
    private String password;
    // filter for "all groups/roles extraction"
    private String allGroupsSearchFilter;
    
    public LDAPRoleServiceConfig() {
        
    }
    
    public LDAPRoleServiceConfig(LDAPRoleServiceConfig other) {
        super(other);        
        user = other.getUser();        
        password = other.getPassword();        
        allGroupsSearchFilter = other.getAllGroupsSearchFilter();
    }
    
    public String getUser() {
        return user;
    }
    public void setUser(String userDn) {
        this.user = userDn;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getAllGroupsSearchFilter() {
        return allGroupsSearchFilter;
    }
    public void setAllGroupsSearchFilter(String allGroupsSearchFilter) {
        this.allGroupsSearchFilter = allGroupsSearchFilter;
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

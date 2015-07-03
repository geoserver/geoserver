/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;

/**
 * Basic class for LDAP service related configurations.
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 *
 */
public abstract class LDAPBaseSecurityServiceConfig extends BaseSecurityNamedServiceConfig {
    String serverURL;    
    
    String groupSearchBase;
    String groupSearchFilter;
    
    Boolean useTLS;
    
    // bind to the server before extracting groups
    // some LDAP server require this (e.g. ActiveDirectory)
    Boolean bindBeforeGroupSearch;
    
    String adminGroup;
    String groupAdminGroup;
    
    // extract user data using the given filter (alternative to userDnPattern)
    String userFilter;
      

    public LDAPBaseSecurityServiceConfig() {
    }
    
    public LDAPBaseSecurityServiceConfig(LDAPBaseSecurityServiceConfig other) {
        super(other);
        serverURL = other.getServerURL();        
        groupSearchBase = other.getGroupSearchBase();
        groupSearchFilter= other.getGroupSearchFilter();        
        adminGroup = other.getAdminGroup();
        groupAdminGroup = other.getGroupAdminGroup();
        bindBeforeGroupSearch = other.isBindBeforeGroupSearch();        
        userFilter = other.getUserFilter();
        useTLS = other.isUseTLS();
    }
        
    public String getServerURL() {
        return serverURL;
    }
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }
    

    public String getGroupSearchBase() {
        return groupSearchBase;
    }
    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public String getGroupSearchFilter() {
        return groupSearchFilter;
    }
    public void setGroupSearchFilter(String groupSearchFilter) {
        this.groupSearchFilter = groupSearchFilter;
    }
    
    public void setUseTLS(Boolean useTLS) {
        this.useTLS = useTLS;
    }
    public Boolean isUseTLS() {
        return useTLS;
    }

    public Boolean isBindBeforeGroupSearch() {
        return bindBeforeGroupSearch == null ? false : bindBeforeGroupSearch;
    }
    
    public void setBindBeforeGroupSearch(Boolean bindBeforeGroupSearch) {
        this.bindBeforeGroupSearch = bindBeforeGroupSearch;
    }
    
    public String getAdminGroup() {
        return adminGroup;
    }

    public void setAdminGroup(String adminGroup) {
        this.adminGroup = adminGroup;
    }
        
    public String getGroupAdminGroup() {
        return groupAdminGroup;
    }

    public void setGroupAdminGroup(String groupAdminGroup) {
        this.groupAdminGroup = groupAdminGroup;
    }
    

    public String getUserFilter() {
        return userFilter;
    }

    public void setUserFilter(String userFilter) {
        this.userFilter = userFilter;
    }
}

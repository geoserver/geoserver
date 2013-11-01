/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;

public class LDAPSecurityServiceConfig extends BaseSecurityNamedServiceConfig 
    implements SecurityAuthProviderConfig {

    String serverURL;
    // extract user data using a distinguished name
    String userDnPattern;
    String groupSearchBase;
    String groupSearchFilter;
    String userGroupServiceName;
    Boolean useTLS;
    
    // bind to the server before extracting groups
    // some LDAP server require this (e.g. ActiveDirectory)
    Boolean bindBeforeGroupSearch;
    // extract user data using the given filter (alternative to userDnPattern)
    String userFilter;
    // format username before doing authentication using the given format (used if userFilter
    // is specified)
    String userFormat;
    String adminGroup;
    String groupAdminGroup;

    public LDAPSecurityServiceConfig() {
    }

    public LDAPSecurityServiceConfig(LDAPSecurityServiceConfig other) {
        super(other);
        serverURL = other.getServerURL();
        userDnPattern = other.getUserDnPattern();
        groupSearchBase = other.getGroupSearchBase();
        groupSearchFilter= other.getGroupSearchFilter();
        userGroupServiceName = other.getUserGroupServiceName();
        adminGroup = other.getAdminGroup();
        groupAdminGroup = other.getGroupAdminGroup();
        bindBeforeGroupSearch = other.isBindBeforeGroupSearch();
        userFilter = other.getUserFilter();
        userFormat = other.getUserFormat();
        useTLS = other.isUseTLS();
    }

    public String getServerURL() {
        return serverURL;
    }
    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public String getUserDnPattern() {
        return userDnPattern;
    }
    public void setUserDnPattern(String userDnPattern) {
        this.userDnPattern = userDnPattern;
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

    @Override
    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    @Override
    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
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

    public String getUserFormat() {
        return userFormat;
    }

    public void setUserFormat(String userFormat) {
        this.userFormat = userFormat;
    }
    
    
    
}

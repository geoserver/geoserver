/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;

public class LDAPSecurityServiceConfig extends BaseSecurityNamedServiceConfig 
    implements SecurityAuthProviderConfig {

    String serverURL;
    String userDnPattern;
    String groupSearchBase;
    String groupSearchFilter;
    String userGroupServiceName;
    Boolean useTLS;

    public LDAPSecurityServiceConfig() {
    }

    public LDAPSecurityServiceConfig(LDAPSecurityServiceConfig other) {
        super(other);
        serverURL = other.getServerURL();
        userDnPattern = other.getUserDnPattern();
        groupSearchBase = other.getGroupSearchBase();
        groupSearchFilter= other.getGroupSearchFilter();
        userGroupServiceName = other.getUserGroupServiceName();
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

    @Override
    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    @Override
    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }
}

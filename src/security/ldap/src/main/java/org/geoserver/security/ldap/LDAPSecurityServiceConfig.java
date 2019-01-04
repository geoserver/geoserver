/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import org.geoserver.security.config.SecurityAuthProviderConfig;

public class LDAPSecurityServiceConfig extends LDAPBaseSecurityServiceConfig
        implements SecurityAuthProviderConfig {

    private static final long serialVersionUID = 6093735229286602599L;

    // extract user data using a distinguished name
    String userDnPattern;

    String userGroupServiceName;

    // format username before doing authentication using the given format
    String userFormat;

    public LDAPSecurityServiceConfig() {}

    public LDAPSecurityServiceConfig(LDAPSecurityServiceConfig other) {
        super(other);
        userDnPattern = other.getUserDnPattern();
        userGroupServiceName = other.getUserGroupServiceName();
        userFormat = other.getUserFormat();
    }

    public String getUserFormat() {
        return userFormat;
    }

    public void setUserFormat(String userFormat) {
        this.userFormat = userFormat;
    }

    public String getUserDnPattern() {
        return userDnPattern;
    }

    public void setUserDnPattern(String userDnPattern) {
        this.userDnPattern = userDnPattern;
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

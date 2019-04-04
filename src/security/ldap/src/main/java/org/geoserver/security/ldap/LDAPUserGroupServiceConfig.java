/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import org.geoserver.security.config.SecurityUserGroupServiceConfig;

/** @author Niels Charlier */
public class LDAPUserGroupServiceConfig extends LDAPBaseSecurityServiceConfig
        implements SecurityUserGroupServiceConfig {

    private static final long serialVersionUID = 4699211240178341515L;

    String passwordEncoderName;

    String passwordPolicyName;

    String populatedAttributes;

    @Override
    public String getPasswordEncoderName() {
        return passwordEncoderName;
    }

    @Override
    public void setPasswordEncoderName(String passwordEncoderName) {
        this.passwordEncoderName = passwordEncoderName;
    }

    @Override
    public String getPasswordPolicyName() {
        return passwordPolicyName;
    }

    @Override
    public void setPasswordPolicyName(String passwordPolicyName) {
        this.passwordPolicyName = passwordPolicyName;
    }

    public String getPopulatedAttributes() {
        return populatedAttributes;
    }

    public void setPopulatedAttributes(String populatedAttributes) {
        this.populatedAttributes = populatedAttributes;
    }
}

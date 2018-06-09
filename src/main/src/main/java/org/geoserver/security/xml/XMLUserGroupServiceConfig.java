/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.xml;

import org.geoserver.security.config.SecurityUserGroupServiceConfig;

public class XMLUserGroupServiceConfig extends XMLSecurityServiceConfig
        implements SecurityUserGroupServiceConfig {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    String passwordEncoderName;
    String passwordPolicyName;

    public XMLUserGroupServiceConfig() {}

    public XMLUserGroupServiceConfig(XMLUserGroupServiceConfig other) {
        super(other);
        passwordEncoderName = other.getPasswordEncoderName();
        passwordPolicyName = other.getPasswordPolicyName();
    }

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
}

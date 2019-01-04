/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config.impl;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;

public class MemoryUserGroupServiceConfigImpl extends BaseSecurityNamedServiceConfig
        implements SecurityUserGroupServiceConfig {

    private static final long serialVersionUID = 1L;
    protected String passwordEncoderName;
    protected String passwordPolicyName;
    protected String toBeEncrypted;

    public MemoryUserGroupServiceConfigImpl() {}

    public MemoryUserGroupServiceConfigImpl(MemoryUserGroupServiceConfigImpl other) {
        super(other);
        passwordEncoderName = other.getPasswordEncoderName();
        passwordPolicyName = other.getPasswordPolicyName();
        toBeEncrypted = other.getToBeEncrypted();
    }

    public String getToBeEncrypted() {
        return toBeEncrypted;
    }

    public void setToBeEncrypted(String toBeEncrypted) {
        this.toBeEncrypted = toBeEncrypted;
    }

    public String getPasswordPolicyName() {
        return passwordPolicyName;
    }

    public void setPasswordPolicyName(String passwordPolicyName) {
        this.passwordPolicyName = passwordPolicyName;
    }

    @Override
    public String getPasswordEncoderName() {
        return passwordEncoderName;
    }

    @Override
    public void setPasswordEncoderName(String name) {
        passwordEncoderName = name;
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;

/**
 * Base class for configuration for {@link MasterPasswordProvider} implementations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MasterPasswordProviderConfig extends BaseSecurityNamedServiceConfig {

    boolean readOnly;

    boolean loginEnabled;

    public MasterPasswordProviderConfig() {}

    public MasterPasswordProviderConfig(MasterPasswordProviderConfig other) {
        super(other);
        this.readOnly = other.isReadOnly();
        this.loginEnabled = other.loginEnabled;
    }

    /** Flag determining if the url is read only and may not be written back to. */
    public boolean isReadOnly() {
        return readOnly;
    }

    /** Sets flag determining if the url is read only and may not be written back to. */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /** Flag determining if the root can login to GeoServer or not. */
    public boolean isLoginEnabled() {
        return loginEnabled;
    }

    /** Sets flag determining if the root can login to GeoServer or not. */
    public void setLoginEnabled(boolean loginEnabled) {
        this.loginEnabled = loginEnabled;
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import org.geoserver.security.config.SecurityConfig;

/**
 * Configuration object for the GeoServer master password.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class MasterPasswordConfig implements SecurityConfig {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    String providerName;

    public MasterPasswordConfig() {
    }

    public MasterPasswordConfig(MasterPasswordConfig other) {
        this.providerName = other.getProviderName();
    }

    /**
     * The name of the master password provider.
     */
    public String getProviderName() {
        return providerName;
    }

    /**
     * Sets the name of the master password provider.
     */
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
}

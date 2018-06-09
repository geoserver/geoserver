/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rememberme;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;

/**
 * Configuration object for remember me services.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class RememberMeServicesConfig extends BaseSecurityNamedServiceConfig {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_KEY = "geoserver";

    String key = DEFAULT_KEY;

    public RememberMeServicesConfig() {}

    public RememberMeServicesConfig(RememberMeServicesConfig other) {
        super(other);
        setKey(other.getKey());
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}

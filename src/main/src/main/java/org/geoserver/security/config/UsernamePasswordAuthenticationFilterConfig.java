/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.filter.GeoServerUserNamePasswordAuthenticationFilter;

/**
 * {@link GeoServerUserNamePasswordAuthenticationFilter} configuration object.
 *
 * @author mcr
 */
public class UsernamePasswordAuthenticationFilterConfig extends SecurityFilterConfig
        implements SecurityAuthFilterConfig {

    public static final String DEFAULT_PASSWORD_PARAM = "password";
    public static final String DEFAULT_USERNAME_PARAM = "username";

    private static final long serialVersionUID = 1L;

    private String passwordParameterName;
    private String usernameParameterName;

    public String getPasswordParameterName() {
        return passwordParameterName;
    }

    public void setPasswordParameterName(String passwordParameterName) {
        this.passwordParameterName = passwordParameterName;
    }

    public String getUsernameParameterName() {
        return usernameParameterName;
    }

    public void setUsernameParameterName(String usernameParameterName) {
        this.usernameParameterName = usernameParameterName;
    }

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }
}

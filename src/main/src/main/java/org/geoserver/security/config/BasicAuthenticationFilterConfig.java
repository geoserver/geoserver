/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;

/**
 * {@link GeoServerBasicAuthenticationFilter} configuration object.
 *
 * <p>If {@link #useRememberMe} is <code>true</code>, the filter registers a successful
 * authentication in the global remember me service.
 *
 * @author mcr
 */
public class BasicAuthenticationFilterConfig extends SecurityFilterConfig
        implements SecurityAuthFilterConfig {

    private static final long serialVersionUID = 1L;
    private boolean useRememberMe;

    public boolean isUseRememberMe() {
        return useRememberMe;
    }

    public void setUseRememberMe(boolean useRememberMe) {
        this.useRememberMe = useRememberMe;
    }

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }
}

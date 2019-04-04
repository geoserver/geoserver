/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.filter.GeoServerDigestAuthenticationFilter;

/**
 * {@link GeoServerDigestAuthenticationFilter} configuration object.
 *
 * <p>The user group service must have a plain text or an encrypting password encoder.
 *
 * <p>The default value for {@link #nonceValiditySeconds} is 300
 *
 * @author mcr
 */
public class DigestAuthenticationFilterConfig extends SecurityFilterConfig
        implements SecurityAuthFilterConfig {

    private static final long serialVersionUID = 1L;
    private String userGroupServiceName;
    private int nonceValiditySeconds = 300;

    public int getNonceValiditySeconds() {
        return nonceValiditySeconds;
    }

    public void setNonceValiditySeconds(int nonceValiditySeconds) {
        this.nonceValiditySeconds = nonceValiditySeconds;
    }

    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }
}

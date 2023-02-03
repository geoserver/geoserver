/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geoserver.authentication.auth;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;

/** @author ETj (etj at geo-solutions.it) */
public class GeoFenceAuthenticationProviderConfig extends BaseSecurityNamedServiceConfig
        implements SecurityAuthProviderConfig {

    public GeoFenceAuthenticationProviderConfig() {}

    public GeoFenceAuthenticationProviderConfig(GeoFenceAuthenticationProviderConfig other) {
        super(other);
    }

    @Override
    public String getUserGroupServiceName() {
        return null;
    }

    @Override
    public void setUserGroupServiceName(String userGroupServiceName) {}
}

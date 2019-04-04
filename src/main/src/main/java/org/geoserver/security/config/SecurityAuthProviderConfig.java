/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerUserGroupService;

/**
 * {@link GeoServerAuthenticationProvider} configuration object.
 *
 * <p>The {@link #getUserGroupServiceName()} may be null if no {@link GeoServerUserGroupService} is
 * needed.
 */
public interface SecurityAuthProviderConfig extends SecurityNamedServiceConfig {

    public String getUserGroupServiceName();

    public void setUserGroupServiceName(String userGroupServiceName);
}

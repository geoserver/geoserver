/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.filter.GeoServerJ2eeAuthenticationFilter;

/**
 * {@link GeoServerJ2eeAuthenticationFilter} configuration object.
 * <p>
 * {@link #roleServiceName} is optional, default is {@link GeoServerSecurityManager#getActiveRoleService()} 
 * </p>
 * @author christian
 *
 */
public class J2eeAuthenticationFilterConfig extends SecurityFilterConfig 
    implements SecurityAuthFilterConfig {

    private static final long serialVersionUID = 1L;

    private String roleServiceName;

    public String getRoleServiceName() {
        return roleServiceName;
    }

    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }

    @Override
    public  boolean providesAuthenticationEntryPoint() {
        return true;
    }
}

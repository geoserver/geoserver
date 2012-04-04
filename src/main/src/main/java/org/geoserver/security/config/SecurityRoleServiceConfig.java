/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.GeoServerRoleService;

/**
 * Interface for {@link GeoServerRoleService} configuration objects.
 *
 * @author christian
 */
public interface SecurityRoleServiceConfig extends SecurityNamedServiceConfig {

    /**
     * The name of the admin role for this service.
     */
    String getAdminRoleName();

    /**
     * Sets the name of the admin role for this service.
     */
    void setAdminRoleName(String adminRoleName);

}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.impl.GeoServerRole;

/**
 * Interface for {@link GeoServerRoleService} configuration objects.
 *
 * @author christian
 */
public interface SecurityRoleServiceConfig extends SecurityNamedServiceConfig {

    /**
     * Optional, the name of the administrator role for this service. The role has the same
     * privileges as {@link GeoServerRole#ADMIN_ROLE}
     */
    String getAdminRoleName();

    /** Sets the name of the administrator role for this service. */
    void setAdminRoleName(String adminRoleName);

    /**
     * Optional, the name of the group administrator role for this service. The role has the same
     * privileges as {@link GeoServerRole#GROUP_ADMIN_ROLE}
     */
    String getGroupAdminRoleName();

    /** Sets the name of the group administrator role for this service. */
    void setGroupAdminRoleName(String adminRoleName);
}

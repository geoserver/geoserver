/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import java.io.Serial;
import org.geoserver.security.impl.GeoServerJ2eeRoleService;

/**
 * Configuration class for {@link GeoServerJ2eeRoleService}
 *
 * @author christian
 */
public class J2eeRoleServiceConfig extends BaseSecurityNamedServiceConfig implements SecurityRoleServiceConfig {
    @Serial
    private static final long serialVersionUID = 1L;

    protected String adminRoleName;
    protected String groupAdminRoleName;

    public J2eeRoleServiceConfig() {}

    public J2eeRoleServiceConfig(J2eeRoleServiceConfig other) {
        super(other);
        adminRoleName = other.getAdminRoleName();
        groupAdminRoleName = other.getGroupAdminRoleName();
    }

    @Override
    public String getAdminRoleName() {
        return adminRoleName;
    }

    @Override
    public void setAdminRoleName(String name) {
        adminRoleName = name;
    }

    @Override
    public String getGroupAdminRoleName() {
        return groupAdminRoleName;
    }

    @Override
    public void setGroupAdminRoleName(String groupAdminRoleName) {
        this.groupAdminRoleName = groupAdminRoleName;
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.filter.GeoServerRoleFilter;

/**
 * Configuration for {@link GeoServerRoleFilter}
 *
 * @author mcr
 */
public class RoleFilterConfig extends SecurityFilterConfig {

    private static final long serialVersionUID = 1L;
    private String httpResponseHeaderAttrForIncludedRoles;
    private String roleConverterName;

    public String getHttpResponseHeaderAttrForIncludedRoles() {
        return httpResponseHeaderAttrForIncludedRoles;
    }

    public void setHttpResponseHeaderAttrForIncludedRoles(
            String httpResponseHeaderAttrForIncludedRoles) {
        this.httpResponseHeaderAttrForIncludedRoles = httpResponseHeaderAttrForIncludedRoles;
    }

    public String getRoleConverterName() {
        return roleConverterName;
    }

    public void setRoleConverterName(String roleConverterName) {
        this.roleConverterName = roleConverterName;
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;

/**
 * {@link GeoServerPreAuthenticatedUserNameFilter} configuration object.
 *
 * <p>{@link #getRoleSource()} determines how to calculate the roles:
 *
 * <ol>
 *   <li>{@link PreAuthenticatedUserNameRoleSource#UserGroupService} - Roles are calculated using
 *       the named user group service {@link #getUserGroupServiceName()}
 *   <li>{@link PreAuthenticatedUserNameRoleSource#RoleService} - Roles are calculated using the
 *       named role service {@link #getRoleServiceName()}. If no role service is given, the default
 *       is {@link GeoServerSecurityManager#getActiveRoleService()}
 *   <li>{@link PreAuthenticatedUserNameRoleSource#Header} - Roles are calculated using the content
 *       of {@link #getRolesHeaderAttribute()} parsed by {@link #getRoleConverterName()}. if no
 *       converter is given, roles are parsed by the default converter {@link
 *       GeoServerRoleConverter}
 *
 * @author christian
 */
public abstract class PreAuthenticatedUserNameFilterConfig extends SecurityFilterConfig
        implements SecurityAuthFilterConfig {

    private RoleSource roleSource;
    private String rolesHeaderAttribute;
    private String userGroupServiceName;
    private String roleConverterName;
    private String roleServiceName;

    /**
     * RoleSource list values common to all PreAuthenticatedUserNameFilterConfig hierarchy.
     *
     * @author Mauro Bartolomeoli (mauro.bartolomeoli@geo-solutions.it)
     */
    public static enum PreAuthenticatedUserNameRoleSource implements RoleSource {
        Header,
        UserGroupService,
        RoleService;

        @Override
        public boolean equals(RoleSource other) {
            return other != null && other.toString().equals(toString());
        }
    };

    private static final long serialVersionUID = 1L;

    public RoleSource getRoleSource() {
        return roleSource;
    }

    public void setRoleSource(RoleSource roleSource) {
        this.roleSource = roleSource;
    }

    public String getRolesHeaderAttribute() {
        return rolesHeaderAttribute;
    }

    public void setRolesHeaderAttribute(String rolesHeaderAttribute) {
        this.rolesHeaderAttribute = rolesHeaderAttribute;
    }

    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    public String getRoleConverterName() {
        return roleConverterName;
    }

    public void setRoleConverterName(String roleConverterName) {
        this.roleConverterName = roleConverterName;
    }

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }

    public String getRoleServiceName() {
        return roleServiceName;
    }

    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }
}

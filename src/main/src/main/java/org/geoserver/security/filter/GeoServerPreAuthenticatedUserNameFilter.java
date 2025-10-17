/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.filter.GeoServerRoleResolvers.RoleResolver;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;

/**
 * J2EE Authentication Filter
 *
 * @author mcr
 */
public abstract class GeoServerPreAuthenticatedUserNameFilter extends GeoServerPreAuthenticationFilter
        implements GeoServerRoleResolvers.ResolverContext {

    private RoleSource roleSource;
    private String rolesHeaderAttribute;
    private String userGroupServiceName;
    private String roleConverterName;
    private String roleServiceName;
    private GeoServerRoleConverter converter;

    protected static final String UserNameAlreadyRetrieved = "org.geoserver.security.filter.usernameAlreadyRetrieved";
    protected static final String UserName = "org.geoserver.security.filter.username";

    @Override
    public RoleSource getRoleSource() {
        return roleSource;
    }

    public void setRoleSource(RoleSource roleSource) {
        this.roleSource = roleSource;
    }

    @Override
    public String getRolesHeaderAttribute() {
        return rolesHeaderAttribute;
    }

    public void setRolesHeaderAttribute(String rolesHeaderAttribute) {
        this.rolesHeaderAttribute = rolesHeaderAttribute;
    }

    @Override
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
    public String getRoleServiceName() {
        return roleServiceName;
    }

    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        PreAuthenticatedUserNameFilterConfig authConfig = (PreAuthenticatedUserNameFilterConfig) config;

        roleSource = authConfig.getRoleSource();
        rolesHeaderAttribute = authConfig.getRolesHeaderAttribute();
        userGroupServiceName = authConfig.getUserGroupServiceName();
        roleConverterName = authConfig.getRoleConverterName();
        roleServiceName = authConfig.getRoleServiceName();

        if (PreAuthenticatedUserNameRoleSource.Header.equals(getRoleSource())) {
            String converterName = authConfig.getRoleConverterName();
            GeoServerRoleConverter lConverter = GeoServerRoleResolvers.loadConverter(converterName);
            setConverter(lConverter);
        }
    }

    @Override
    protected String getPreAuthenticatedPrincipal(HttpServletRequest request) {
        // avoid retrieving the user name more than once
        if (request.getAttribute(UserNameAlreadyRetrieved) != null) return (String) request.getAttribute(UserName);

        String principal = getPreAuthenticatedPrincipalName(request);
        if (principal != null && principal.trim().isEmpty()) principal = null;
        try {
            if (principal != null && PreAuthenticatedUserNameRoleSource.UserGroupService.equals(getRoleSource())) {
                GeoServerUserGroupService service =
                        getSecurityManager().loadUserGroupService(getUserGroupServiceName());
                GeoServerUser u = service.getUserByUsername(principal);
                if (u != null && u.isEnabled() == false) {
                    principal = null;
                    handleDisabledUser(u, request);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        request.setAttribute(UserNameAlreadyRetrieved, Boolean.TRUE);
        if (principal != null) request.setAttribute(UserName, principal);
        return principal;
    }

    protected void handleDisabledUser(GeoServerUser u, HttpServletRequest request) {
        // do nothing
    }

    @Override
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal) throws IOException {
        RoleResolver lResolver = GeoServerRoleResolvers.PRE_AUTH_ROLE_SOURCE_RESOLVER;
        Collection<GeoServerRole> roles = lResolver.convert(new ResolverParam(principal, request, this));
        return roles;
    }

    @Override
    public String getCacheKey(HttpServletRequest request) {

        // caching does not make sense if everything is in the header
        if (PreAuthenticatedUserNameRoleSource.Header.equals(getRoleSource())) return null;
        return super.getCacheKey(request);
    }

    @Override
    public GeoServerRoleConverter getConverter() {
        return converter;
    }

    public void setConverter(GeoServerRoleConverter converter) {
        this.converter = converter;
    }

    protected abstract String getPreAuthenticatedPrincipalName(HttpServletRequest request);
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.RoleCalculator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

/**
 * J2EE Authentication Filter
 *
 * @author mcr
 */
public abstract class GeoServerPreAuthenticatedUserNameFilter
        extends GeoServerPreAuthenticationFilter {

    private RoleSource roleSource;
    private String rolesHeaderAttribute;
    private String userGroupServiceName;
    private String roleConverterName;
    private String roleServiceName;
    private GeoServerRoleConverter converter;

    protected static final String UserNameAlreadyRetrieved =
            "org.geoserver.security.filter.usernameAlreadyRetrieved";
    protected static final String UserName = "org.geoserver.security.filter.username";

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

    public String getRoleServiceName() {
        return roleServiceName;
    }

    public void setRoleServiceName(String roleServiceName) {
        this.roleServiceName = roleServiceName;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        PreAuthenticatedUserNameFilterConfig authConfig =
                (PreAuthenticatedUserNameFilterConfig) config;

        roleSource = authConfig.getRoleSource();
        rolesHeaderAttribute = authConfig.getRolesHeaderAttribute();
        userGroupServiceName = authConfig.getUserGroupServiceName();
        roleConverterName = authConfig.getRoleConverterName();
        roleServiceName = authConfig.getRoleServiceName();

        // TODO, Justin, is this ok ?
        if (PreAuthenticatedUserNameRoleSource.Header.equals(getRoleSource())) {
            String converterName = authConfig.getRoleConverterName();
            if (converterName == null || converterName.length() == 0)
                setConverter(GeoServerExtensions.bean(GeoServerRoleConverter.class));
            else setConverter((GeoServerRoleConverter) GeoServerExtensions.bean(converterName));
        }
    }

    @Override
    protected String getPreAuthenticatedPrincipal(HttpServletRequest request) {
        // avoid retrieving the user name more than once
        if (request.getAttribute(UserNameAlreadyRetrieved) != null)
            return (String) request.getAttribute(UserName);

        String principal = getPreAuthenticatedPrincipalName(request);
        if (principal != null && principal.trim().length() == 0) principal = null;
        try {
            if (principal != null
                    && PreAuthenticatedUserNameRoleSource.UserGroupService.equals(
                            getRoleSource())) {
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
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal)
            throws IOException {

        Collection<GeoServerRole> roles;
        if (PreAuthenticatedUserNameRoleSource.RoleService.equals(getRoleSource())) {
            roles = getRolesFromRoleService(request, principal);
        } else if (PreAuthenticatedUserNameRoleSource.UserGroupService.equals(getRoleSource())) {
            roles = getRolesFromUserGroupService(request, principal);
        } else if (PreAuthenticatedUserNameRoleSource.Header.equals(getRoleSource())) {
            roles = getRolesFromHttpAttribute(request, principal);
        } else {
            throw new RuntimeException("Never should reach this point");
        }

        LOGGER.log(
                Level.FINE,
                "Got roles {0} from {1} for principal {2}",
                new Object[] {roles, getRoleSource(), principal});

        return roles;
    }

    /**
     * Calculates roles from a {@link GeoServerRoleService} The default service is {@link
     * GeoServerSecurityManager#getActiveRoleService()}
     *
     * <p>The result contains all inherited roles, but no personalized roles
     */
    protected Collection<GeoServerRole> getRolesFromRoleService(
            HttpServletRequest request, String principal) throws IOException {
        boolean useActiveService =
                getRoleServiceName() == null || getRoleServiceName().trim().length() == 0;

        GeoServerRoleService service =
                useActiveService
                        ? getSecurityManager().getActiveRoleService()
                        : getSecurityManager().loadRoleService(getRoleServiceName());

        RoleCalculator calc = new RoleCalculator(service);
        return calc.calculateRoles(principal);
    }

    /**
     * Calculates roles using a {@link GeoServerUserGroupService} if the principal is not found, an
     * empty collection is returned
     */
    protected Collection<GeoServerRole> getRolesFromUserGroupService(
            HttpServletRequest request, String principal) throws IOException {
        Collection<GeoServerRole> roles = new ArrayList<GeoServerRole>();

        GeoServerUserGroupService service =
                getSecurityManager().loadUserGroupService(getUserGroupServiceName());
        UserDetails details = null;
        try {
            details = service.loadUserByUsername(principal);
        } catch (UsernameNotFoundException ex) {
            LOGGER.log(
                    Level.WARNING,
                    "User " + principal + " not found in " + getUserGroupServiceName());
        }

        if (details != null) {
            for (GrantedAuthority auth : details.getAuthorities()) roles.add((GeoServerRole) auth);
        }
        return roles;
    }

    /**
     * Calculates roles using the String found in the http header attribute if no role string is
     * found, anempty collection is returned
     *
     * <p>The result contains personalized roles
     */
    protected Collection<GeoServerRole> getRolesFromHttpAttribute(
            HttpServletRequest request, String principal) throws IOException {
        Collection<GeoServerRole> roles = new ArrayList<GeoServerRole>();

        String rolesString = request.getHeader(getRolesHeaderAttribute());
        if (rolesString == null || rolesString.trim().length() == 0) {
            LOGGER.log(Level.WARNING, "No roles in header attribute: " + getRolesHeaderAttribute());
            return roles;
        }

        roles.addAll(getConverter().convertRolesFromString(rolesString, principal));
        LOGGER.log(
                Level.FINE,
                "for principal "
                        + principal
                        + " found roles "
                        + StringUtils.collectionToCommaDelimitedString(roles)
                        + " in header "
                        + getRolesHeaderAttribute());
        return roles;
    }

    @Override
    public String getCacheKey(HttpServletRequest request) {

        // caching does not make sense if everything is in the header
        if (PreAuthenticatedUserNameRoleSource.Header.equals(getRoleSource())) return null;
        return super.getCacheKey(request);
    }

    public GeoServerRoleConverter getConverter() {
        return converter;
    }

    public void setConverter(GeoServerRoleConverter converter) {
        this.converter = converter;
    }

    protected abstract String getPreAuthenticatedPrincipalName(HttpServletRequest request);
}

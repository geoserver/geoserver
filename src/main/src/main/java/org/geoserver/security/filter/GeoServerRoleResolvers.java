/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.exception.GeoServerRuntimException;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleCalculator;
import org.geotools.util.logging.Logging;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

/**
 * Provides {@link RoleResolver}s to obtain {@link GeoServerRole}s for a principal name during authentication. Useful
 * for filters in pre-authentication scenarios.
 *
 * <p>To resolve roles provide a {@link ResolverContext} with the contextual information regarding the sources to
 * inspect and a {@link ResolverParam} with the current principal name and {@link HttpServletRequest}.
 *
 * <p>Typically the {@link #PRE_AUTH_ROLE_SOURCE_RESOLVER} should be used which covers the other resolvers.
 */
public class GeoServerRoleResolvers {

    private static final Logger LOGGER = Logging.getLogger(GeoServerRoleResolvers.class);

    /**
     * Loads the {@link GeoServerRoleConverter} for the given name
     *
     * @param pName optional name
     * @return the converter
     */
    public static GeoServerRoleConverter loadConverter(String pName) {
        GeoServerRoleConverter lConverter;
        if (pName == null || pName.isEmpty()) {
            lConverter = GeoServerExtensions.bean(GeoServerRoleConverter.class);
        } else {
            lConverter = (GeoServerRoleConverter) GeoServerExtensions.bean(pName);
        }
        return lConverter;
    }

    /**
     * Provides the {@link #principal} name and {@link #request} together with the {@link #context} which provides
     * access to further information required for the conversion.
     */
    public static class ResolverParam {
        private final String principal;
        private final HttpServletRequest request;
        private final ResolverContext context;

        /**
         * @param pPrincipal
         * @param pRequest
         * @param pContext
         */
        public ResolverParam(String pPrincipal, HttpServletRequest pRequest, ResolverContext pContext) {
            super();
            principal = pPrincipal;
            request = pRequest;
            context = pContext;
        }

        public GeoServerSecurityManager getSecurityManager() {
            return context.getSecurityManager();
        }

        public String getRoleServiceName() {
            return context.getRoleServiceName();
        }

        public String getUserGroupServiceName() {
            return context.getUserGroupServiceName();
        }

        public String getRolesHeaderAttribute() {
            return context.getRolesHeaderAttribute();
        }

        public GeoServerRoleConverter getConverter() {
            return context.getConverter();
        }

        public RoleSource getRoleSource() {
            return context.getRoleSource();
        }

        /** @return the principal */
        public String getPrincipal() {
            return principal;
        }

        /** @return the request */
        public HttpServletRequest getRequest() {
            return request;
        }

        /** @return the context */
        public ResolverContext getContext() {
            return context;
        }
    }

    /**
     * Provides access to the {@link RoleSource}. It determines which source shall be considered. Also provides the name
     * to be used per source.
     */
    public interface ResolverContext {
        GeoServerSecurityManager getSecurityManager();

        String getRoleServiceName();

        String getUserGroupServiceName();

        String getRolesHeaderAttribute();

        GeoServerRoleConverter getConverter();

        RoleSource getRoleSource();
    }

    /** Default implementation of a {@link ResolverContext}. */
    public static class DefaultResolverContext implements ResolverContext {
        private GeoServerSecurityManager securityManager;
        private String roleServiceName;
        private String userGroupServiceName;
        private String rolesHeaderAttribute;
        private GeoServerRoleConverter converter;
        private RoleSource roleSource;

        /**
         * @param pSecurityManager
         * @param pRoleServiceName
         * @param pUserGroupServiceName
         * @param pRolesHeaderAttribute
         * @param pConverter
         * @param pRoleSource
         */
        public DefaultResolverContext(
                GeoServerSecurityManager pSecurityManager,
                String pRoleServiceName,
                String pUserGroupServiceName,
                String pRolesHeaderAttribute,
                GeoServerRoleConverter pConverter,
                RoleSource pRoleSource) {
            super();
            securityManager = pSecurityManager;
            roleServiceName = pRoleServiceName;
            userGroupServiceName = pUserGroupServiceName;
            rolesHeaderAttribute = pRolesHeaderAttribute;
            converter = pConverter;
            roleSource = pRoleSource;
        }

        /** @return the securityManager */
        @Override
        public GeoServerSecurityManager getSecurityManager() {
            return securityManager;
        }

        /** @return the roleServiceName */
        @Override
        public String getRoleServiceName() {
            return roleServiceName;
        }

        /** @return the userGroupServiceName */
        @Override
        public String getUserGroupServiceName() {
            return userGroupServiceName;
        }

        /** @return the rolesHeaderAttribute */
        @Override
        public String getRolesHeaderAttribute() {
            return rolesHeaderAttribute;
        }

        /** @return the converter */
        @Override
        public GeoServerRoleConverter getConverter() {
            return converter;
        }

        /** @return the roleSource */
        @Override
        public RoleSource getRoleSource() {
            return roleSource;
        }
    }

    /** Contract for resolving the {@link GeoServerRole} for a principal. */
    public interface RoleResolver extends Converter<ResolverParam, Collection<GeoServerRole>> {}

    /**
     * Calculates roles from a {@link GeoServerRoleService} The default service is
     * {@link GeoServerSecurityManager#getActiveRoleService()}
     *
     * <p>The result contains all inherited roles, but no personalized roles
     */
    public static final RoleResolver ROLE_SERVICE_RESOLVER = p -> {
        boolean useActiveService =
                p.getRoleServiceName() == null || p.getRoleServiceName().trim().isEmpty();

        GeoServerRoleService service;
        try {
            service = useActiveService
                    ? p.getSecurityManager().getActiveRoleService()
                    : p.getSecurityManager().loadRoleService(p.getRoleServiceName());
            RoleCalculator calc = new RoleCalculator(service);
            return calc.calculateRoles(p.principal);
        } catch (IOException e) {
            throw new GeoServerRuntimException(
                    "Failed to load roles for user '"
                            + p.principal
                            + "' from roleService '"
                            + p.getRoleServiceName()
                            + "'.",
                    e);
        }
    };

    /**
     * Calculates roles using a {@link GeoServerUserGroupService} if the principal is not found, an empty collection is
     * returned
     */
    public static final RoleResolver USER_GROUP_SERVICE_RESOLVER = p -> {
        Collection<GeoServerRole> roles = new ArrayList<>();

        GeoServerUserGroupService service;
        try {
            service = p.getSecurityManager().loadUserGroupService(p.getUserGroupServiceName());
        } catch (IOException e) {
            throw new GeoServerRuntimException(
                    "Failed to load roles for user '"
                            + p.principal
                            + "' from userGroupService '"
                            + p.getUserGroupServiceName()
                            + "'.",
                    e);
        }
        UserDetails details = null;
        try {
            details = service.loadUserByUsername(p.principal);
        } catch (UsernameNotFoundException ex) {
            LOGGER.log(Level.WARNING, "User " + p.principal + " not found in " + p.getUserGroupServiceName());
        }

        if (details != null) {
            for (GrantedAuthority auth : details.getAuthorities()) roles.add((GeoServerRole) auth);
        }
        return roles;
    };

    /**
     * Calculates roles using the String found in the http header attribute if no role string is found, an empty
     * collection is returned
     *
     * <p>The result contains personalized roles
     */
    public static final RoleResolver HTTP_HEADER_RESOLVER = p -> {
        if (p.getRequest() == null) {
            throw new GeoServerRuntimException("Resolving roles from HTTP headers failed. Request not available.");
        }
        Collection<GeoServerRole> roles = new ArrayList<>();

        String rolesString = p.getRequest().getHeader(p.getRolesHeaderAttribute());
        if (rolesString == null || rolesString.trim().isEmpty()) {
            LOGGER.log(Level.WARNING, "No roles in header attribute: " + p.getRolesHeaderAttribute());
            return roles;
        }

        roles.addAll(p.getConverter().convertRolesFromString(rolesString, p.principal));
        LOGGER.log(
                Level.FINE,
                "for principal "
                        + p.principal
                        + " found roles "
                        + StringUtils.collectionToCommaDelimitedString(roles)
                        + " in header "
                        + p.getRolesHeaderAttribute());
        return roles;
    };

    /** Resolves {@link GeoServerRole}s when the {@link RoleSource} is a {@link PreAuthenticatedUserNameRoleSource}. */
    public static final RoleResolver PRE_AUTH_ROLE_SOURCE_RESOLVER = p -> {
        Collection<GeoServerRole> roles;
        RoleSource rs = p.getRoleSource();

        if (PreAuthenticatedUserNameRoleSource.RoleService.equals(rs)) {
            roles = ROLE_SERVICE_RESOLVER.convert(p);
        } else if (PreAuthenticatedUserNameRoleSource.UserGroupService.equals(rs)) {
            roles = USER_GROUP_SERVICE_RESOLVER.convert(p);
        } else if (PreAuthenticatedUserNameRoleSource.Header.equals(rs)) {
            roles = HTTP_HEADER_RESOLVER.convert(p);
        } else {
            String lMsg = "Couldn't determine roles based on the specified role source %s.";
            throw new RuntimeException(lMsg.formatted(rs));
        }

        String lMsg = "Got roles {0} from {1} for principal {2}";
        LOGGER.log(Level.FINE, lMsg, new Object[] {roles, rs, p.principal});

        return roles;
    };
}

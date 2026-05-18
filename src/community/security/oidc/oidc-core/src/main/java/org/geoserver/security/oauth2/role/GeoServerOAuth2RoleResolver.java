/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.role;

import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.geoserver.security.filter.GeoServerRoleResolvers.PRE_AUTH_ROLE_SOURCE_RESOLVER;
import static org.geoserver.security.impl.GeoServerUser.ADMIN_USERNAME;
import static org.geoserver.security.impl.GeoServerUser.ROOT_USERNAME;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverContext;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.filter.GeoServerRoleResolvers.RoleResolver;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geoserver.security.oauth2.role.provider.keycloak.KeycloakRolesResolver;
import org.geoserver.security.oauth2.role.provider.msgraph.MSGraphRolesResolver;
import org.geoserver.security.oauth2.role.strategy.AccessTokenRoleResolverStrategy;
import org.geoserver.security.oauth2.role.strategy.IdTokenRoleResolverStrategy;
import org.geoserver.security.oauth2.role.strategy.KeycloakRoleResolverStrategy;
import org.geoserver.security.oauth2.role.strategy.MSGraphRoleResolverStrategy;
import org.geoserver.security.oauth2.role.strategy.OpenIdRoleResolverStrategy;
import org.geoserver.security.oauth2.role.strategy.UserInfoRoleResolverStrategy;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Resolves roles for a given user during login with OAuth2 and OpenID Connect.
 *
 * <p>Per-{@link OpenIdRoleSource} resolution is delegated to {@link OpenIdRoleResolverStrategy} implementations (one
 * per source); the resolver itself only dispatches by source, applies the role-converter mapping, appends the
 * {@code ROLE_AUTHENTICATED} marker, and runs {@link RoleCalculator}-driven hierarchy enrichment. Adding a new IdP
 * source = one new strategy class, no edit to this class.
 *
 * <p>Strategies are loaded from two places, in this order: built-in defaults (instantiated in
 * {@link #registerDefaultStrategies()}) and Spring-registered beans of type {@link OpenIdRoleResolverStrategy}
 * (override the defaults if they declare the same {@link OpenIdRoleSource}).
 *
 * @author awaterme
 */
public class GeoServerOAuth2RoleResolver implements RoleResolver {

    private static final Logger LOGGER = Logging.getLogger(GeoServerOAuth2RoleResolver.class);

    /** Param subclass carrying the per-request {@link OAuth2UserRequest}; strategies cast to this. */
    public static final class OAuth2ResolverParam extends ResolverParam {

        private final OAuth2UserRequest userRequest;

        public OAuth2ResolverParam(
                String pPrincipal,
                HttpServletRequest pRequest,
                ResolverContext pContext,
                OAuth2UserRequest pUserRequest) {
            super(pPrincipal, pRequest, pContext);
            userRequest = pUserRequest;
        }

        public OAuth2UserRequest getUserRequest() {
            return userRequest;
        }
    }

    private final GeoServerOAuth2LoginFilterConfig config;
    private final OAuth2RoleConverter roleConverter;
    private final Map<OpenIdRoleSource, OpenIdRoleResolverStrategy> strategies = new EnumMap<>(OpenIdRoleSource.class);

    public GeoServerOAuth2RoleResolver(GeoServerOAuth2LoginFilterConfig pConfig) {
        this.config = pConfig;
        this.roleConverter =
                new OAuth2RoleConverter(config.getRoleConverterString(), config.isOnlyExternalListedRoles());
        registerDefaultStrategies();
        // Spring-registered strategies (e.g. for future IdPs contributed by sibling modules) override defaults.
        for (OpenIdRoleResolverStrategy s : GeoServerExtensions.extensions(OpenIdRoleResolverStrategy.class)) {
            strategies.put(s.getRoleSource(), s);
        }
    }

    /** Built-in strategies — kept independent of Spring so tests without a context still work. */
    private void registerDefaultStrategies() {
        strategies.put(OpenIdRoleSource.IdToken, new IdTokenRoleResolverStrategy());
        strategies.put(OpenIdRoleSource.AccessToken, new AccessTokenRoleResolverStrategy());
        strategies.put(OpenIdRoleSource.UserInfo, new UserInfoRoleResolverStrategy());
        strategies.put(OpenIdRoleSource.MSGraphAPI, new MSGraphRoleResolverStrategy());
        strategies.put(OpenIdRoleSource.KeycloakAPI, new KeycloakRoleResolverStrategy());
    }

    @Override
    public Collection<GeoServerRole> convert(ResolverParam pParam) {
        if (!(pParam instanceof OAuth2ResolverParam oauthParam)) {
            throw new IllegalArgumentException(OAuth2ResolverParam.class.getSimpleName() + " required");
        }
        String principal = pParam.getPrincipal();
        if (ADMIN_USERNAME.equalsIgnoreCase(principal) || ROOT_USERNAME.equalsIgnoreCase(principal)) {
            // Avoid unintentional match with pre-existing administrator
            LOGGER.log(
                    Level.WARNING,
                    "Potentially harmful OAuth2 user '%s' detected. Granting no roles.".formatted(principal));
            return new ArrayList<>();
        }

        Collection<GeoServerRole> result;
        RoleSource rs = pParam.getContext().getRoleSource();
        if (rs == null) {
            LOGGER.log(SEVERE, "Role assignment failed. Role source unspecified.");
            result = new ArrayList<>();
        } else if (rs instanceof OpenIdRoleSource oirs) {
            result = resolveViaStrategy(oirs, oauthParam);
        } else {
            // Standard pre-auth role sources (Header, UserGroupService, RoleService) — handled by the framework
            result = PRE_AUTH_ROLE_SOURCE_RESOLVER.convert(pParam);
        }
        if (result == null) result = new ArrayList<>();

        enrichInheritedAndSystemRoles(result, pParam);

        if (LOGGER.isLoggable(Level.FINE)) {
            String roles = result.stream().map(GeoServerRole::getAuthority).collect(joining(","));
            LOGGER.fine("User '%s' received roles from roleSource=%s: %s".formatted(principal, rs, roles));
        }
        return result;
    }

    /**
     * Looks up the strategy bound to {@code source}, calls it for raw role names, applies the role-converter mapping
     * and appends {@code ROLE_AUTHENTICATED}.
     */
    private Collection<GeoServerRole> resolveViaStrategy(OpenIdRoleSource source, OAuth2ResolverParam param) {
        OpenIdRoleResolverStrategy strategy = strategies.get(source);
        if (strategy == null) {
            LOGGER.log(SEVERE, "No OpenIdRoleResolverStrategy registered for role source: %s".formatted(source));
            return new ArrayList<>();
        }
        List<String> roleNames = strategy.resolveRoleNames(param, config);
        if (roleNames == null) roleNames = new ArrayList<>();
        roleNames = roleConverter.convert(roleNames);
        if (!roleNames.contains("ROLE_AUTHENTICATED")) roleNames.add("ROLE_AUTHENTICATED");
        return roleNames.stream().map(GeoServerRole::new).collect(toList());
    }

    /**
     * Runs hierarchy enrichment via {@link RoleCalculator}. When the filter config names a specific
     * {@link GeoServerRoleService} (via the inherited {@code roleServiceName} field), enrichment uses that service;
     * otherwise it falls back to the security manager's active role service.
     */
    private void enrichInheritedAndSystemRoles(Collection<GeoServerRole> roles, ResolverParam param) {
        GeoServerSecurityManager securityManager = param.getSecurityManager();
        GeoServerRoleService roleService = null;
        String roleServiceName = config == null ? null : config.getRoleServiceName();
        if (roleServiceName != null && !roleServiceName.isBlank()) {
            try {
                roleService = securityManager.loadRoleService(roleServiceName);
            } catch (IOException e) {
                LOGGER.log(
                        SEVERE,
                        "Failed to load configured role service '%s'; falling back to active."
                                .formatted(roleServiceName),
                        e);
            }
        }
        if (roleService == null) roleService = securityManager.getActiveRoleService();
        RoleCalculator calc = new RoleCalculator(roleService);
        try {
            calc.addInheritedRoles(roles);
        } catch (IOException e) {
            LOGGER.log(
                    SEVERE,
                    "Role calculation failed on inherited roles for user '%s'.".formatted(param.getPrincipal()),
                    e);
        }
        calc.addMappedSystemRoles(roles);
        if (!roles.contains(GeoServerRole.AUTHENTICATED_ROLE)) roles.add(GeoServerRole.AUTHENTICATED_ROLE);
    }

    /** Returns the strategy bound to the given role source — primarily for test injection. May return {@code null}. */
    public OpenIdRoleResolverStrategy getStrategy(OpenIdRoleSource source) {
        return strategies.get(source);
    }

    /**
     * @deprecated Use {@code ((UserInfoRoleResolverStrategy) getStrategy(UserInfo)).setUserServiceSupplier(s)}.
     *     Retained as a thin façade so existing tests keep compiling.
     */
    @Deprecated
    public void setUserServiceSupplier(Supplier<OAuth2UserService<OAuth2UserRequest, OAuth2User>> pSupplier) {
        OpenIdRoleResolverStrategy s = strategies.get(OpenIdRoleSource.UserInfo);
        if (s instanceof UserInfoRoleResolverStrategy ui) ui.setUserServiceSupplier(pSupplier);
    }

    /**
     * @deprecated Use {@code ((MSGraphRoleResolverStrategy) getStrategy(MSGraphAPI)).setResolverSupplier(s)}. Retained
     *     as a thin façade so existing tests keep compiling.
     */
    @Deprecated
    public void setMsGraphRolesResolverSupplier(Supplier<MSGraphRolesResolver> pSupplier) {
        OpenIdRoleResolverStrategy s = strategies.get(OpenIdRoleSource.MSGraphAPI);
        if (s instanceof MSGraphRoleResolverStrategy ms) ms.setResolverSupplier(pSupplier);
    }

    /** Test hook for the Keycloak strategy's underlying {@link KeycloakRolesResolver} supplier. */
    public void setKeycloakRolesResolverSupplier(Supplier<KeycloakRolesResolver> pSupplier) {
        OpenIdRoleResolverStrategy s = strategies.get(OpenIdRoleSource.KeycloakAPI);
        if (s instanceof KeycloakRoleResolverStrategy kc) kc.setResolverSupplier(pSupplier);
    }
}

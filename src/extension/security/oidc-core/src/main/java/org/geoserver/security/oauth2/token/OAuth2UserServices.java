/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.token;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.filter.GeoServerRoleResolvers;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverContext;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.UserProfilePropertyNames;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.role.GeoServerOAuth2RoleResolver;
import org.geoserver.security.oauth2.role.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;
import org.geoserver.security.oauth2.user.GeoServerOAuth2User;
import org.geoserver.security.oauth2.user.GeoServerOidcUser;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Provides {@link OAuth2UserService} implementation for OAuth2 and OpenID Connect. Allows for integration with the
 * GeoServer supported user role sources.
 *
 * @author awaterme
 */
public class OAuth2UserServices {

    private static final Logger LOGGER = Logging.getLogger(OAuth2UserServices.class);

    static class GeoServerOAuth2UserService extends OAuth2UserServices
            implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

        private Supplier<OAuth2UserService<OAuth2UserRequest, OAuth2User>> delegateSupplier =
                () -> new DefaultOAuth2UserService();

        public GeoServerOAuth2UserService(
                ResolverContext resolverContext,
                Supplier<HttpServletRequest> requestSupplier,
                GeoServerOAuth2LoginFilterConfig config) {
            super(resolverContext, requestSupplier, config);
        }

        public GeoServerOAuth2UserService(
                ResolverContext resolverContext,
                Supplier<HttpServletRequest> requestSupplier,
                GeoServerOAuth2LoginFilterConfig config,
                Supplier<GeoServerOAuth2RoleResolver> resolverSupplier) {
            super(resolverContext, requestSupplier, config, resolverSupplier);
        }

        @Override
        public OAuth2User loadUser(OAuth2UserRequest pUserRequest) throws OAuth2AuthenticationException {
            OAuth2User lUser = delegateSupplier.get().loadUser(pUserRequest);
            String lUserName = lUser.getName();
            String lUserNameAttributeName = userNameAttributeName(pUserRequest);
            LOGGER.fine(() -> "Building OAuth2 principal for user '" + lUserName + "'.");

            Collection<GeoServerRole> roles = determineRoles(lUserName, pUserRequest);
            Properties properties = resolveProperties(lUserName, lUser.getAttributes());
            LOGGER.fine(() -> "Built OAuth2 principal for user '"
                    + lUserName
                    + "' with "
                    + properties.size()
                    + " resolved properties.");
            return new GeoServerOAuth2User(lUserName, roles, properties, lUser.getAttributes(), lUserNameAttributeName);
        }

        /**
         * Replaces the default delegate used to load the upstream Spring OAuth2 user.
         *
         * <p>Intended for tests and custom wiring.
         */
        public void setDelegateSupplier(Supplier<OAuth2UserService<OAuth2UserRequest, OAuth2User>> delegateSupplier) {
            this.delegateSupplier = delegateSupplier;
        }
    }

    static class GeoServerOidcUserService extends OAuth2UserServices
            implements OAuth2UserService<OidcUserRequest, OidcUser> {

        private Supplier<OAuth2UserService<OidcUserRequest, OidcUser>> delegateSupplier = () -> new OidcUserService();

        public GeoServerOidcUserService(
                ResolverContext resolverContext,
                Supplier<HttpServletRequest> requestSupplier,
                GeoServerOAuth2LoginFilterConfig config) {
            super(resolverContext, requestSupplier, config);
        }

        public GeoServerOidcUserService(
                ResolverContext resolverContext,
                Supplier<HttpServletRequest> requestSupplier,
                GeoServerOAuth2LoginFilterConfig config,
                Supplier<GeoServerOAuth2RoleResolver> resolverSupplier) {
            super(resolverContext, requestSupplier, config, resolverSupplier);
        }

        @Override
        public OidcUser loadUser(OidcUserRequest pUserRequest) throws OAuth2AuthenticationException {
            OidcUser lUser = delegateSupplier.get().loadUser(pUserRequest);
            String lUserName = lUser.getName();
            String lUserNameAttributeName = userNameAttributeName(pUserRequest);
            LOGGER.fine(() -> "Building OIDC principal for user '" + lUserName + "'.");

            Collection<GeoServerRole> roles = determineRoles(lUserName, pUserRequest);
            Map<String, Object> claims = mergeOidcClaims(lUser.getIdToken(), lUser.getUserInfo());
            Properties properties = resolveProperties(lUserName, claims);
            LOGGER.fine(() -> "Built OIDC principal for user '"
                    + lUserName
                    + "' with "
                    + properties.size()
                    + " resolved properties.");
            return new GeoServerOidcUser(
                    lUserName,
                    roles,
                    properties,
                    claims,
                    lUserNameAttributeName,
                    lUser.getIdToken(),
                    lUser.getUserInfo());
        }

        /**
         * Replaces the default delegate used to load the upstream Spring OIDC user.
         *
         * <p>Intended for tests and custom wiring.
         */
        public void setDelegateSupplier(Supplier<OAuth2UserService<OidcUserRequest, OidcUser>> delegateSupplier) {
            this.delegateSupplier = delegateSupplier;
        }
    }

    /**
     * Creates an OIDC user service that enriches the Spring principal with GeoServer roles and properties.
     *
     * @param pResolverContext role and user/group-service lookup context
     * @param pReqSupplier supplier for the current servlet request
     * @param pConfig OAuth2/OIDC filter configuration
     * @return a user service returning principals that implement both {@link OidcUser} and {@link GeoServerUser}
     */
    public static OAuth2UserService<OidcUserRequest, OidcUser> newOidcUserService(
            GeoServerRoleResolvers.ResolverContext pResolverContext,
            Supplier<HttpServletRequest> pReqSupplier,
            GeoServerOAuth2LoginFilterConfig pConfig) {
        GeoServerOidcUserService lService = new GeoServerOidcUserService(pResolverContext, pReqSupplier, pConfig);
        return lService;
    }

    /**
     * Creates an OAuth2 user service that enriches the Spring principal with GeoServer roles and properties.
     *
     * @param pResolverContext role and user/group-service lookup context
     * @param pReqSupplier supplier for the current servlet request
     * @param pConfig OAuth2/OIDC filter configuration
     * @return a user service returning principals that implement both {@link OAuth2User} and {@link GeoServerUser}
     */
    public static OAuth2UserService<OAuth2UserRequest, OAuth2User> newOAuth2UserService(
            GeoServerRoleResolvers.ResolverContext pResolverContext,
            Supplier<HttpServletRequest> pReqSupplier,
            GeoServerOAuth2LoginFilterConfig pConfig) {
        GeoServerOAuth2UserService lService = new GeoServerOAuth2UserService(pResolverContext, pReqSupplier, pConfig);
        return lService;
    }

    protected final GeoServerRoleResolvers.ResolverContext resolverContext;
    protected final Supplier<HttpServletRequest> requestSupplier;
    protected final GeoServerOAuth2LoginFilterConfig config;
    protected final Supplier<GeoServerOAuth2RoleResolver> resolverSupplier;

    public OAuth2UserServices(
            ResolverContext resolverContext,
            Supplier<HttpServletRequest> requestSupplier,
            GeoServerOAuth2LoginFilterConfig config) {
        this(resolverContext, requestSupplier, config, null);
    }

    protected OAuth2UserServices(
            ResolverContext resolverContext,
            Supplier<HttpServletRequest> requestSupplier,
            GeoServerOAuth2LoginFilterConfig config,
            Supplier<GeoServerOAuth2RoleResolver> resolverSupplier) {
        super();
        this.resolverContext = resolverContext;
        this.requestSupplier = requestSupplier;
        this.config = config;
        this.resolverSupplier =
                resolverSupplier == null ? () -> new GeoServerOAuth2RoleResolver(this.config) : resolverSupplier;
    }

    /**
     * Resolves the configured Spring user-name attribute for the current provider.
     *
     * <p>The returned value controls both Spring principal naming and the GeoServer user/group-service lookup key.
     */
    protected String userNameAttributeName(OAuth2UserRequest pUserRequest) {
        // null check performed by delegate already
        String lUserNameAttributeName = pUserRequest
                .getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();
        return lUserNameAttributeName;
    }

    /**
     * Resolves GeoServer roles for the authenticated OAuth2/OIDC principal.
     *
     * <p>This keeps the existing role-resolution pipeline unchanged and only adapts it to the Spring
     * {@link OAuth2UserRequest} context.
     *
     * @param pUserName resolved principal name
     * @param pRequest OAuth2 or OIDC user request
     * @return resolved GeoServer roles, never {@code null}
     */
    protected Collection<GeoServerRole> determineRoles(String pUserName, OAuth2UserRequest pRequest) {
        LOGGER.fine(() -> "Resolving roles for user '" + pUserName + "'.");
        HttpServletRequest lRequest = requestSupplier.get();
        ResolverParam lParam = new OAuth2ResolverParam(pUserName, lRequest, resolverContext, pRequest);
        GeoServerOAuth2RoleResolver lResolver = resolverSupplier.get();
        Collection<GeoServerRole> roles = lResolver.convert(lParam);
        LOGGER.fine(() -> "Resolved " + roles.size() + " roles for user '" + pUserName + "'.");
        return roles;
    }

    /**
     * Resolves GeoServer user properties for the authenticated principal.
     *
     * <p>Properties are loaded from the configured user/group service first and then back-filled from claims only for
     * missing well-known profile fields.
     *
     * @param userName resolved principal name
     * @param claims OAuth2/OIDC claims or attributes used as fallback values
     * @return resolved properties, never {@code null}
     */
    protected Properties resolveProperties(String userName, Map<String, Object> claims) {
        LOGGER.fine(() -> "Resolving user properties for '" + userName + "'.");
        Properties properties = new Properties();
        properties.putAll(loadUserGroupServiceProperties(userName));
        boolean hadUserGroupProperties = !properties.isEmpty();
        fillIfMissing(properties, UserProfilePropertyNames.FIRST_NAME, claims.get("given_name"));
        fillIfMissing(properties, UserProfilePropertyNames.LAST_NAME, claims.get("family_name"));
        fillIfMissing(properties, UserProfilePropertyNames.PREFERRED_USERNAME, claims.get("preferred_username"));
        fillIfMissing(properties, UserProfilePropertyNames.EMAIL, claims.get("email"));
        LOGGER.fine(() -> "Resolved "
                + properties.size()
                + " user properties for '"
                + userName
                + "' using "
                + (hadUserGroupProperties ? "user-group-service and claims fallback." : "claims fallback only."));
        return properties;
    }

    /**
     * Loads user properties from the configured GeoServer user/group service.
     *
     * <p>This lookup is best-effort only. Missing configuration, missing users, or I/O errors must not prevent login.
     *
     * @param userName resolved principal name used as lookup key
     * @return properties loaded from the user/group service, or an empty set when unavailable
     */
    protected Properties loadUserGroupServiceProperties(String userName) {
        Properties properties = new Properties();
        String userGroupServiceName = resolverContext.getUserGroupServiceName();
        if (StringUtils.isBlank(userGroupServiceName)) {
            LOGGER.fine(() -> "No user group service configured for user '" + userName + "'.");
            return properties;
        }

        LOGGER.fine(() -> "Attempting property lookup for user '"
                + userName
                + "' from user group service '"
                + userGroupServiceName
                + "'.");

        try {
            GeoServerUserGroupService service =
                    resolverContext.getSecurityManager().loadUserGroupService(userGroupServiceName);
            if (service == null) {
                LOGGER.fine(() -> "User group service '"
                        + userGroupServiceName
                        + "' could not be loaded for user '"
                        + userName
                        + "'.");
                return properties;
            }
            GeoServerUser user = service.getUserByUsername(userName);
            if (user != null) {
                properties.putAll(user.getProperties());
                LOGGER.fine(() -> "Loaded "
                        + properties.size()
                        + " properties for user '"
                        + userName
                        + "' from user group service '"
                        + userGroupServiceName
                        + "'.");
            } else {
                LOGGER.fine(() ->
                        "User '" + userName + "' not found in user group service '" + userGroupServiceName + "'.");
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    e,
                    () -> "Failed to resolve properties for user '"
                            + userName
                            + "' from user group service '"
                            + userGroupServiceName
                            + "'. Continuing with claim-derived properties only.");
        }

        return properties;
    }

    /**
     * Merges OIDC claims from the ID token and optional user-info response.
     *
     * <p>When both sources provide the same claim, the user-info value wins.
     *
     * @param idToken ID token returned by the provider, may be {@code null}
     * @param userInfo user-info response returned by the provider, may be {@code null}
     * @return merged claims map, never {@code null}
     */
    protected Map<String, Object> mergeOidcClaims(OidcIdToken idToken, OidcUserInfo userInfo) {
        Map<String, Object> claims = new LinkedHashMap<>(idToken == null ? Map.of() : idToken.getClaims());
        if (userInfo != null) claims.putAll(userInfo.getClaims());
        LOGGER.fine(() -> "Merged OIDC claims from "
                + (userInfo == null ? "ID token only" : "ID token and user-info")
                + ", resulting in "
                + claims.size()
                + " claim entries.");
        return claims;
    }

    /**
     * Writes a string property only when it is currently absent and the supplied value is a non-blank string.
     *
     * @param properties target property set
     * @param propertyName property name to populate
     * @param value candidate claim or attribute value
     */
    protected void fillIfMissing(Properties properties, String propertyName, Object value) {
        if (properties.containsKey(propertyName)) return;
        if (!(value instanceof String stringValue) || stringValue.isBlank()) return;
        properties.setProperty(propertyName, stringValue);
    }
}

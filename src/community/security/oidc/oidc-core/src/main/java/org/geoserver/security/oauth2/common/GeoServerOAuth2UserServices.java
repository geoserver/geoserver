/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.logging.Logger;
import org.geoserver.security.filter.GeoServerRoleResolvers;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverContext;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.oauth2.common.GeoServerOAuth2RoleResolver.OAuth2ResolverParam;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Provides {@link OAuth2UserService} implementation for OAuth2 and OpenID Connect. Allows for integration with the
 * GeoServer supported user role sources.
 *
 * @author awaterme
 */
public class GeoServerOAuth2UserServices {

    private static final Logger LOGGER = Logging.getLogger(GeoServerOAuth2UserServices.class);

    static class GeoServerOAuth2UserService extends GeoServerOAuth2UserServices
            implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

        private Supplier<OAuth2UserService<OAuth2UserRequest, OAuth2User>> delegateSupplier =
                () -> new DefaultOAuth2UserService();

        public GeoServerOAuth2UserService(
                ResolverContext resolverContext,
                Supplier<HttpServletRequest> requestSupplier,
                GeoServerOAuth2LoginFilterConfig config) {
            super(resolverContext, requestSupplier, config);
        }

        @Override
        public OAuth2User loadUser(OAuth2UserRequest pUserRequest) throws OAuth2AuthenticationException {
            OAuth2User lUser = delegateSupplier.get().loadUser(pUserRequest);
            String lUserName = lUser.getName();
            String lUserNameAttributeName = userNameAttributeName(pUserRequest);

            Collection<GeoServerRole> roles = determineRoles(lUserName, pUserRequest);
            return new DefaultOAuth2User(roles, lUser.getAttributes(), lUserNameAttributeName);
        }

        public void setDelegateSupplier(Supplier<OAuth2UserService<OAuth2UserRequest, OAuth2User>> delegateSupplier) {
            this.delegateSupplier = delegateSupplier;
        }
    }

    static class GeoServerOidcUserService extends GeoServerOAuth2UserServices
            implements OAuth2UserService<OidcUserRequest, OidcUser> {

        private Supplier<OAuth2UserService<OidcUserRequest, OidcUser>> delegateSupplier = () -> new OidcUserService();

        public GeoServerOidcUserService(
                ResolverContext resolverContext,
                Supplier<HttpServletRequest> requestSupplier,
                GeoServerOAuth2LoginFilterConfig config) {
            super(resolverContext, requestSupplier, config);
        }

        @Override
        public OidcUser loadUser(OidcUserRequest pUserRequest) throws OAuth2AuthenticationException {
            OidcUser lUser = delegateSupplier.get().loadUser(pUserRequest);
            String lUserName = lUser.getName();
            String lUserNameAttributeName = userNameAttributeName(pUserRequest);

            Collection<GeoServerRole> roles = determineRoles(lUserName, pUserRequest);
            return new DefaultOidcUser(roles, lUser.getIdToken(), lUser.getUserInfo(), lUserNameAttributeName);
        }

        public void setDelegateSupplier(Supplier<OAuth2UserService<OidcUserRequest, OidcUser>> delegateSupplier) {
            this.delegateSupplier = delegateSupplier;
        }
    }

    public static OAuth2UserService<OidcUserRequest, OidcUser> newOidcUserService(
            GeoServerRoleResolvers.ResolverContext pResolverContext,
            Supplier<HttpServletRequest> pReqSupplier,
            GeoServerOAuth2LoginFilterConfig pConfig) {
        GeoServerOidcUserService lService = new GeoServerOidcUserService(pResolverContext, pReqSupplier, pConfig);
        return lService;
    }

    public static OAuth2UserService<OAuth2UserRequest, OAuth2User> newOAuth2UserService(
            GeoServerRoleResolvers.ResolverContext pResolverContext,
            Supplier<HttpServletRequest> pReqSupplier,
            GeoServerOAuth2LoginFilterConfig pConfig) {
        GeoServerOAuth2UserService lService = new GeoServerOAuth2UserService(pResolverContext, pReqSupplier, pConfig);
        return lService;
    }

    protected GeoServerRoleResolvers.ResolverContext resolverContext;
    protected Supplier<HttpServletRequest> requestSupplier;
    protected GeoServerOAuth2LoginFilterConfig config;
    protected Supplier<GeoServerOAuth2RoleResolver> resolverSupplier = () -> new GeoServerOAuth2RoleResolver(config);

    public GeoServerOAuth2UserServices(
            ResolverContext resolverContext,
            Supplier<HttpServletRequest> requestSupplier,
            GeoServerOAuth2LoginFilterConfig config) {
        super();
        this.resolverContext = resolverContext;
        this.requestSupplier = requestSupplier;
        this.config = config;
    }

    protected String userNameAttributeName(OAuth2UserRequest pUserRequest) {
        // null check performed by delegate already
        String lUserNameAttributeName = pUserRequest
                .getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();
        return lUserNameAttributeName;
    }

    protected Collection<GeoServerRole> determineRoles(String pUserName, OAuth2UserRequest pRequest) {
        LOGGER.fine("Resolving roles for user '" + pUserName + "'.");
        HttpServletRequest lRequest = requestSupplier.get();
        ResolverParam lParam = new OAuth2ResolverParam(pUserName, lRequest, resolverContext, pRequest);
        GeoServerOAuth2RoleResolver lResolver = resolverSupplier.get();
        Collection<GeoServerRole> roles = lResolver.convert(lParam);
        return roles;
    }

    public void setResolverSupplier(Supplier<GeoServerOAuth2RoleResolver> resolverSupplier) {
        this.resolverSupplier = resolverSupplier;
    }
}

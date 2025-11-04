/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static org.geoserver.security.filter.GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR;
import static org.geoserver.security.oauth2.common.GeoServerOAuth2UserServices.newOAuth2UserService;
import static org.geoserver.security.oauth2.common.GeoServerOAuth2UserServices.newOidcUserService;
import static org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent.disableButtonEvent;
import static org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent.enableButtonEvent;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.filter.GeoServerRoleResolvers;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverContext;
import org.geoserver.security.oauth2.common.ConfidentialLogger;
import org.geoserver.security.oauth2.common.HttpServletRequestSupplier;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginCustomizers.ClientRegistrationCustomizer;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginCustomizers.HttpSecurityCustomizer;
import org.geoserver.security.oauth2.spring.GeoServerAuthorizationRequestCustomizer;
import org.geoserver.security.oauth2.spring.GeoServerOAuth2AccessTokenResponseClient;
import org.geoserver.security.oauth2.spring.GeoServerOidcIdTokenDecoderFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.RequestMatcherRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Builder for {@link GeoServerOAuth2LoginAuthenticationFilter}.
 *
 * <p>Further documentation: Look at {@link GeoServerOAuth2LoginAuthenticationFilter}
 *
 * @see GeoServerOAuth2LoginAuthenticationFilter
 * @author awaterme
 */
public class GeoServerOAuth2LoginAuthenticationFilterBuilder implements GeoServerOAuth2ClientRegistrationId {

    public static final String DEFAULT_AUTHORIZATION_REQUEST_BASE_URI = "web/oauth2/authorization";

    /** Filter types required for GeoServer */
    private static final List<Class<?>> REQ_FILTER_TYPES = asList(
            OAuth2AuthorizationRequestRedirectFilter.class,
            OAuth2LoginAuthenticationFilter.class,
            RequestCacheAwareFilter.class);

    // mandatory
    private GeoServerOAuth2LoginFilterConfig configuration;
    private GeoServerSecurityManager securityManager;
    private HttpSecurity http;
    private ApplicationEventPublisher eventPublisher;
    private GeoServerOidcIdTokenDecoderFactory tokenDecoderFactory;

    private InMemoryClientRegistrationRepository clientRegistrationRepository;
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService;
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService;

    private OAuth2AuthorizedClientService authorizedClientService;
    private OAuth2AuthorizedClientRepository authorizedClientRepository;
    private DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver;
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;
    private LogoutSuccessHandler logoutSuccessHandler;
    private Filter redirectToProviderFilter;

    private Supplier<HttpServletRequest> requestSupplier;
    private GeoServerRoleResolvers.ResolverContext roleResolverContext;
    private boolean closed;

    // Might be used for customizations.
    private HttpSecurityCustomizer httpSecurityCustomizer = (h) -> {};
    private ClientRegistrationCustomizer clientRegistrationCustomizer = (h) -> {};

    public GeoServerOAuth2LoginAuthenticationFilterBuilder() {
        super();
    }

    /**
     * Builds a new filter, setup with the given configuration. Must be called once only.
     *
     * @return a new filter
     */
    public GeoServerOAuth2LoginAuthenticationFilter build() {
        validate();

        GeoServerOAuth2LoginAuthenticationFilter filter = new GeoServerOAuth2LoginAuthenticationFilter();

        if (0 < configuration.getActiveProviderCount()) {
            filter.setLogoutSuccessHandler(getLogoutSuccessHandler());
            List<Filter> lFilters = createNestedFilters();
            filter.setNestedFilters(lFilters);
        }

        ConfidentialLogger.setEnabled(configuration.isOidcAllowUnSecureLogging());
        return filter;
    }

    private void validate() {
        org.springframework.util.Assert.notNull(configuration, "Property 'configuration' must not be null");
        org.springframework.util.Assert.notNull(http, "Property 'http' must not be null");
        org.springframework.util.Assert.notNull(securityManager, "Property 'securityManager' must not be null");
        org.springframework.util.Assert.notNull(eventPublisher, "Property 'eventPublisher' must not be null");
        org.springframework.util.Assert.notNull(tokenDecoderFactory, "Property 'tokenDecoderFactory' must not be null");
        org.springframework.util.Assert.isTrue(!closed, "Builder must not be reused.");
        closed = true;
    }

    private List<Filter> createNestedFilters() {
        try {
            return createFiltersImpl();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create filter.", e);
        }
    }

    private List<Filter> createFiltersImpl() throws Exception {
        // Attention: Singleton (also picked up by Spring) uses this config. If multiple instances
        // of this filter shall be allowed in the future (not planned), adjust this accordingly.
        tokenDecoderFactory.setGeoServerOAuth2LoginFilterConfig(configuration);

        http.oauth2Login(oauthConfig -> {
            oauthConfig.clientRegistrationRepository(getClientRegistrationRepository());
            oauthConfig.authorizedClientRepository(getAuthorizedClientRepository());
            oauthConfig.authorizedClientService(getAuthorizedClientService());

            // Replaced deprecated endpoint DSL with lambda customizers
            oauthConfig.userInfoEndpoint(userInfo -> {
                userInfo.userService(getOauth2UserService());
                userInfo.oidcUserService(getOidcUserService());
            });
            oauthConfig.authorizationEndpoint(
                    authorization -> authorization.authorizationRequestResolver(getAuthorizationRequestResolver()));
            oauthConfig.tokenEndpoint(token -> token.accessTokenResponseClient(getAccessTokenResponseClient()));

            oauthConfig.loginProcessingUrl("/web/login/oauth2/code/*");
        });

        httpSecurityCustomizer.accept(http);

        SecurityFilterChain lChain = http.build();
        List<Filter> lFilters = lChain.getFilters();
        lFilters = lFilters.stream()
                .filter(f -> REQ_FILTER_TYPES.contains(f.getClass()))
                .collect(toList());

        String lAuthEntryPoint = configuration.getAuthenticationEntryPointRedirectUri();
        if (configuration.getEnableRedirectAuthenticationEntryPoint() && lAuthEntryPoint != null) {
            lFilters.add(getRedirectToProviderFilter());
        }
        return lFilters;
    }

    private ResolverContext createRoleResolverContext() {
        GeoServerRoleConverter lConverter = null;
        if (PreAuthenticatedUserNameRoleSource.Header.equals(configuration.getRoleSource())) {
            String converterName = configuration.getRoleConverterName();
            lConverter = GeoServerRoleResolvers.loadConverter(converterName);
        }
        return new GeoServerRoleResolvers.DefaultResolverContext(
                securityManager,
                configuration.getRoleServiceName(),
                configuration.getUserGroupServiceName(),
                configuration.getRolesHeaderAttribute(),
                lConverter,
                configuration.getRoleSource());
    }

    private InMemoryClientRegistrationRepository createClientRegistrationRepository() {
        List<ClientRegistration> lRegistrations = new ArrayList<>();
        if (configuration.isGoogleEnabled()) {
            lRegistrations.add(createGoogleClientRegistration());
            eventPublisher.publishEvent(enableButtonEvent(this, REG_ID_GOOGLE));
        } else {
            eventPublisher.publishEvent(disableButtonEvent(this, REG_ID_GOOGLE));
        }
        if (configuration.isGitHubEnabled()) {
            lRegistrations.add(createGitHubClientRegistration());
            eventPublisher.publishEvent(enableButtonEvent(this, REG_ID_GIT_HUB));
        } else {
            eventPublisher.publishEvent(disableButtonEvent(this, REG_ID_GIT_HUB));
        }
        if (configuration.isMsEnabled()) {
            lRegistrations.add(createMicrosoftClientRegistration());
            eventPublisher.publishEvent(enableButtonEvent(this, REG_ID_MICROSOFT));
        } else {
            eventPublisher.publishEvent(disableButtonEvent(this, REG_ID_MICROSOFT));
        }
        if (configuration.isOidcEnabled()) {
            lRegistrations.add(createCustomProviderRegistration());
            eventPublisher.publishEvent(enableButtonEvent(this, REG_ID_OIDC));
        } else {
            eventPublisher.publishEvent(disableButtonEvent(this, REG_ID_OIDC));
        }
        return new InMemoryClientRegistrationRepository(lRegistrations);
    }

    private OAuth2AuthorizedClientService createAuthorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(getClientRegistrationRepository());
    }

    private OAuth2AuthorizedClientRepository createAuthorizedClientRepository(
            OAuth2AuthorizedClientService authorizedClientService) {
        return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
    }

    private ClientRegistration createGoogleClientRegistration() {
        /*
         * Well known-endpoint:
         * - https://accounts.google.com/.well-known/openid-configuration
         * Documentation:
         * - https://developers.google.com/identity/openid-connect/openid-connect
         * Logout@Google:
         * - seems currently not supported
         * - https://stackoverflow.com/questions/4202161/google-account-logout-and-redirect
         */

        ClientRegistration lReg = CommonOAuth2Provider.GOOGLE
                // registrationId is used in paths (login and authorization)
                .getBuilder(REG_ID_GOOGLE)
                .clientId(configuration.getGoogleClientId())
                .clientSecret(configuration.getGoogleClientSecret())
                .userNameAttributeName(configuration.getGoogleUserNameAttribute())
                .redirectUri(configuration.getGoogleRedirectUri())
                .build();
        clientRegistrationCustomizer.accept(lReg);
        return lReg;
    }

    private ClientRegistration createGitHubClientRegistration() {
        /*
         * GitHub does not support OIDC, but OAuth2.
         *
         * Well known-endpoint:
         * - n/a
         * Documentation:
         * - https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps
         * Further Information:
         * - https://stackoverflow.com/questions/71741596/how-do-i-implement-social-login-with-github-accounts
         * Logout@GitHub:
         * - seems currently not supported
         */

        ClientRegistration lReg = CommonOAuth2Provider.GITHUB
                // registrationId is used in paths (login and authorization)
                .getBuilder(REG_ID_GIT_HUB)
                .clientId(configuration.getGitHubClientId())
                .clientSecret(configuration.getGitHubClientSecret())
                .userNameAttributeName(configuration.getGitHubUserNameAttribute())
                .redirectUri(configuration.getGitHubRedirectUri())
                .build();
        clientRegistrationCustomizer.accept(lReg);
        return lReg;
    }

    private ClientRegistration createMicrosoftClientRegistration() {
        /*
         * Well known-endpoint:
         * - https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration
         */

        String lScopeTxt = configuration.getMsScopes();
        String[] lScopes = ScopeUtils.valueOf(lScopeTxt);
        ClientRegistration lReg = ClientRegistration
                // registrationId is used in paths (login and authorization)
                .withRegistrationId(REG_ID_MICROSOFT)
                .clientId(configuration.getMsClientId())
                .clientSecret(configuration.getMsClientSecret())
                .userNameAttributeName(configuration.getMsUserNameAttribute())
                .redirectUri(configuration.getMsRedirectUri())
                .clientAuthenticationMethod(CLIENT_SECRET_BASIC)
                .authorizationGrantType(AUTHORIZATION_CODE)
                .scope(lScopes)
                .authorizationUri("https://login.microsoftonline.com/common/oauth2/v2.0/authorize")
                .tokenUri("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
                .jwkSetUri("https://login.microsoftonline.com/common/discovery/v2.0/keys")
                .providerConfigurationMetadata(singletonMap(
                        "end_session_endpoint", "https://login.microsoftonline.com/common/oauth2/v2.0/logout"))
                .clientName(REG_ID_MICROSOFT)
                .build();
        clientRegistrationCustomizer.accept(lReg);
        return lReg;
    }

    private ClientRegistration createCustomProviderRegistration() {
        String lScopeTxt = configuration.getOidcScopes();
        String[] lScopes = ScopeUtils.valueOf(lScopeTxt);
        ClientAuthenticationMethod lAuthMethod =
                configuration.isOidcAuthenticationMethodPostSecret() ? CLIENT_SECRET_POST : CLIENT_SECRET_BASIC;

        ClientRegistration lReg = ClientRegistration
                // registrationId is used in paths (login and authorization)
                .withRegistrationId(REG_ID_OIDC)
                .clientId(configuration.getOidcClientId())
                .clientSecret(configuration.getOidcClientSecret())
                .userNameAttributeName(configuration.getOidcUserNameAttribute())
                .redirectUri(configuration.getOidcRedirectUri())
                .clientAuthenticationMethod(lAuthMethod)
                .authorizationGrantType(AUTHORIZATION_CODE)
                .scope(lScopes)
                .authorizationUri(configuration.getOidcAuthorizationUri())
                .tokenUri(configuration.getOidcTokenUri())
                .userInfoUri(configuration.getOidcUserInfoUri())
                .jwkSetUri(configuration.getOidcJwkSetUri())
                .providerConfigurationMetadata(singletonMap("end_session_endpoint", configuration.getOidcLogoutUri()))
                .clientName(REG_ID_OIDC)
                .build();
        clientRegistrationCustomizer.accept(lReg);
        return lReg;
    }

    /** @return the logoutSuccessHandler */
    public LogoutSuccessHandler getLogoutSuccessHandler() {
        if (logoutSuccessHandler == null) {
            OidcClientInitiatedLogoutSuccessHandler lLogoutSuccessHandler =
                    new OidcClientInitiatedLogoutSuccessHandler(getClientRegistrationRepository());
            lLogoutSuccessHandler.setPostLogoutRedirectUri(configuration.getPostLogoutRedirectUri());

            // trivial redirect strategy that just sets the LOGOUT_REDIRECT_ATTR attribute on the request
            // This is handled by the main GS Logout system, which does a redirect.
            lLogoutSuccessHandler.setRedirectStrategy(new RedirectStrategy() {
                @Override
                public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url)
                        throws IOException {
                    request.setAttribute(LOGOUT_REDIRECT_ATTR, url);
                }
            });

            logoutSuccessHandler = lLogoutSuccessHandler;
        }
        return logoutSuccessHandler;
    }

    /** @param pLogoutSuccessHandler the logoutSuccessHandler to set */
    public void setLogoutSuccessHandler(LogoutSuccessHandler pLogoutSuccessHandler) {
        logoutSuccessHandler = pLogoutSuccessHandler;
    }

    /** @return the oAuth2UserService */
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> getOauth2UserService() {
        if (oauth2UserService == null) {
            oauth2UserService = newOAuth2UserService(getRoleResolverContext(), getRequestSupplier(), configuration);
        }
        return oauth2UserService;
    }

    /** @param pOAuth2UserService the oAuth2UserService to set */
    public void setOauth2UserService(OAuth2UserService<OAuth2UserRequest, OAuth2User> pOAuth2UserService) {
        oauth2UserService = pOAuth2UserService;
    }

    /** @return the oidcUserService */
    public OAuth2UserService<OidcUserRequest, OidcUser> getOidcUserService() {
        if (oidcUserService == null) {
            oidcUserService = newOidcUserService(getRoleResolverContext(), getRequestSupplier(), configuration);
        }
        return oidcUserService;
    }

    /** @param pOidcUserService the oidcUserService to set */
    public void setOidcUserService(OAuth2UserService<OidcUserRequest, OidcUser> pOidcUserService) {
        oidcUserService = pOidcUserService;
    }

    /** @return the roleResolverContext */
    public GeoServerRoleResolvers.ResolverContext getRoleResolverContext() {
        if (roleResolverContext == null) {
            roleResolverContext = createRoleResolverContext();
        }
        return roleResolverContext;
    }

    /** @param pRoleResolverContext the roleResolverContext to set */
    public void setRoleResolverContext(GeoServerRoleResolvers.ResolverContext pRoleResolverContext) {
        roleResolverContext = pRoleResolverContext;
    }

    /** @return the requestSupplier */
    public Supplier<HttpServletRequest> getRequestSupplier() {
        if (requestSupplier == null) {
            requestSupplier = new HttpServletRequestSupplier();
        }
        return requestSupplier;
    }

    /** @param pRequestSupplier the requestSupplier to set */
    public void setRequestSupplier(Supplier<HttpServletRequest> pRequestSupplier) {
        requestSupplier = pRequestSupplier;
    }

    /** @return the clientRegistrationRepository */
    public InMemoryClientRegistrationRepository getClientRegistrationRepository() {
        if (clientRegistrationRepository == null) {
            clientRegistrationRepository = createClientRegistrationRepository();
        }
        return clientRegistrationRepository;
    }

    /** @param pClientRegistrationRepository the clientRegistrationRepository to set */
    public void setClientRegistrationRepository(InMemoryClientRegistrationRepository pClientRegistrationRepository) {
        clientRegistrationRepository = pClientRegistrationRepository;
    }

    /** @return the authorizedClientService */
    public OAuth2AuthorizedClientService getAuthorizedClientService() {
        if (authorizedClientService == null) {
            authorizedClientService = createAuthorizedClientService(getClientRegistrationRepository());
        }
        return authorizedClientService;
    }

    /** @param pAuthorizedClientService the authorizedClientService to set */
    public void setAuthorizedClientService(OAuth2AuthorizedClientService pAuthorizedClientService) {
        authorizedClientService = pAuthorizedClientService;
    }

    /** @return the authorizedClientRepository */
    public OAuth2AuthorizedClientRepository getAuthorizedClientRepository() {
        if (authorizedClientRepository == null) {
            authorizedClientRepository = createAuthorizedClientRepository(getAuthorizedClientService());
        }
        return authorizedClientRepository;
    }

    /** @param pAuthorizedClientRepository the authorizedClientRepository to set */
    public void setAuthorizedClientRepository(OAuth2AuthorizedClientRepository pAuthorizedClientRepository) {
        authorizedClientRepository = pAuthorizedClientRepository;
    }

    /** @return the authorizationRequestResolver */
    public DefaultOAuth2AuthorizationRequestResolver getAuthorizationRequestResolver() {
        if (authorizationRequestResolver == null) {
            authorizationRequestResolver = new DefaultOAuth2AuthorizationRequestResolver(
                    getClientRegistrationRepository(), "/" + DEFAULT_AUTHORIZATION_REQUEST_BASE_URI);
            authorizationRequestResolver.setAuthorizationRequestCustomizer(
                    new GeoServerAuthorizationRequestCustomizer(configuration));
        }
        return authorizationRequestResolver;
    }

    /** @param pAuthorizationRequestResolver the authorizationRequestResolver to set */
    public void setAuthorizationRequestResolver(
            DefaultOAuth2AuthorizationRequestResolver pAuthorizationRequestResolver) {
        authorizationRequestResolver = pAuthorizationRequestResolver;
    }

    /** @return the redirectToProviderFilter */
    public Filter getRedirectToProviderFilter() {
        if (redirectToProviderFilter == null) {
            AuthenticationTrustResolver trust = new AuthenticationTrustResolverImpl();
            RequestMatcher lMatcher = r -> {
                Authentication lAuth = SecurityContextHolder.getContext().getAuthentication();
                if (lAuth == null) {
                    return true;
                }
                boolean lFullyAuthenticated = !trust.isAnonymous(lAuth) && !trust.isRememberMe(lAuth);
                return !lFullyAuthenticated;
            };
            redirectToProviderFilter =
                    new RequestMatcherRedirectFilter(lMatcher, configuration.getAuthenticationEntryPointRedirectUri());
        }
        return redirectToProviderFilter;
    }

    /** @return the accessTokenResponseClient */
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> getAccessTokenResponseClient() {
        if (accessTokenResponseClient == null) {
            // Replace deprecated DefaultAuthorizationCodeTokenResponseClient with
            // RestClientAuthorizationCodeTokenResponseClient
            accessTokenResponseClient = new GeoServerOAuth2AccessTokenResponseClient(
                    new RestClientAuthorizationCodeTokenResponseClient(), tokenDecoderFactory);
        }
        return accessTokenResponseClient;
    }

    /** @param pAccessTokenResponseClient the accessTokenResponseClient to set */
    public void setAccessTokenResponseClient(
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> pAccessTokenResponseClient) {
        accessTokenResponseClient = pAccessTokenResponseClient;
    }

    /** @return the httpSecurityCustomizer */
    public Consumer<HttpSecurity> getHttpSecurityCustomizer() {
        return httpSecurityCustomizer;
    }

    /** @param pConfiguration the configuration to set */
    public void setConfiguration(GeoServerOAuth2LoginFilterConfig pConfiguration) {
        configuration = pConfiguration;
    }

    /** @param pHttp the http to set */
    public void setHttp(HttpSecurity pHttp) {
        http = pHttp;
    }

    /** @param pTokenDecoderFactory the tokenDecoderFactory to set */
    public void setTokenDecoderFactory(GeoServerOidcIdTokenDecoderFactory pTokenDecoderFactory) {
        tokenDecoderFactory = pTokenDecoderFactory;
    }

    /** @return the tokenDecoderFactory */
    public GeoServerOidcIdTokenDecoderFactory getTokenDecoderFactory() {
        return tokenDecoderFactory;
    }

    /** @param pSecurityManager the securityManager to set */
    public void setSecurityManager(GeoServerSecurityManager pSecurityManager) {
        securityManager = pSecurityManager;
    }

    /** @param pEventPublisher the eventPublisher to set */
    public void setEventPublisher(ApplicationEventPublisher pEventPublisher) {
        eventPublisher = pEventPublisher;
    }

    /** @param pHttpSecurityCustomizer the httpSecurityCustomizer to set */
    public void setHttpSecurityCustomizer(HttpSecurityCustomizer pHttpSecurityCustomizer) {
        if (pHttpSecurityCustomizer != null) {
            httpSecurityCustomizer = pHttpSecurityCustomizer;
        }
    }

    /** @param pClientRegistrationCustomizer the clientRegistrationCustomizer to set */
    public void setClientRegistrationCustomizer(ClientRegistrationCustomizer pClientRegistrationCustomizer) {
        if (pClientRegistrationCustomizer != null) {
            clientRegistrationCustomizer = pClientRegistrationCustomizer;
        }
    }
}

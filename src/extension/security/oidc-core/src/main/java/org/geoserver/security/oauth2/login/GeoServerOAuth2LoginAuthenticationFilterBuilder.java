/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static java.util.Arrays.asList;
import static org.geoserver.security.oauth2.token.OAuth2UserServices.newOAuth2UserService;
import static org.geoserver.security.oauth2.token.OAuth2UserServices.newOidcUserService;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.geoserver.security.GeoServerRoleConverter;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.filter.GeoServerRoleResolvers;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverContext;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.OAuth2LoginCustomizers.ClientRegistrationCustomizer;
import org.geoserver.security.oauth2.login.OAuth2LoginCustomizers.HttpSecurityCustomizer;
import org.geoserver.security.oauth2.login.builder.ClientRegistrationFactory;
import org.geoserver.security.oauth2.login.builder.HttpSecurityConfigurer;
import org.geoserver.security.oauth2.login.builder.LogoutHandlerFactory;
import org.geoserver.security.oauth2.spring.GeoServerAuthorizationRequestCustomizer;
import org.geoserver.security.oauth2.spring.GeoServerOAuth2AccessTokenResponseClient;
import org.geoserver.security.oauth2.spring.GeoServerOidcIdTokenDecoderFactory;
import org.geoserver.security.oauth2.util.ConfidentialLogger;
import org.geoserver.security.oauth2.util.HttpServletRequestSupplier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.RequestMatcherRedirectFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Builder for {@link GeoServerOAuth2LoginAuthenticationFilter}.
 *
 * <p>Orchestrates three single-responsibility helpers under {@link org.geoserver.security.oauth2.login.builder}:
 *
 * <ul>
 *   <li>{@link ClientRegistrationFactory} — assembles the per-provider Spring {@code ClientRegistration}s and publishes
 *       button enable/disable events
 *   <li>{@link HttpSecurityConfigurer} — wires the Spring {@code HttpSecurity} chain (oauth2Login + optional
 *       oauth2ResourceServer) and returns the filter subset to wrap
 *   <li>{@link LogoutHandlerFactory} — builds the RP-initiated logout handler
 * </ul>
 *
 * <p>This class retains the public setter/getter surface so existing tests and consumers (in particular
 * {@code GeoServerOAuth2LoginAuthenticationProvider}) keep working unchanged.
 *
 * @see GeoServerOAuth2LoginAuthenticationFilter
 * @author awaterme
 */
public class GeoServerOAuth2LoginAuthenticationFilterBuilder implements OAuth2ClientRegistrationId {

    public static final String DEFAULT_AUTHORIZATION_REQUEST_BASE_URI = "web/oauth2/authorization";

    /** Filter types required for GeoServer */
    private static final List<Class<?>> REQ_FILTER_TYPES = asList(
            OAuth2AuthorizationRequestRedirectFilter.class,
            OAuth2LoginAuthenticationFilter.class,
            RequestCacheAwareFilter.class,
            BearerTokenAuthenticationFilter.class);

    // mandatory
    private GeoServerOAuth2LoginFilterConfig configuration;
    private GeoServerSecurityManager securityManager;
    private HttpSecurity http;
    private ApplicationEventPublisher eventPublisher;
    private GeoServerOidcIdTokenDecoderFactory tokenDecoderFactory;

    /**
     * Application-wide registry of every active OAuth2 filter's
     * {@link org.springframework.security.oauth2.client.registration.ClientRegistration}s. Injected by
     * {@link GeoServerOAuth2LoginAuthenticationProvider} so that all OAuth2 filters share the same repository — this is
     * what lets Spring's {@code OAuth2AuthorizationRequestRedirectFilter} inside one filter's sub-chain resolve the
     * scoped registration ID owned by another filter, avoiding the {@code InvalidClientRegistrationIdException} → HTTP
     * 500 cascade that bit the legacy per-filter-private repository design under Spring Security 7.
     */
    private OAuth2ClientRegistrationRegistry clientRegistrationRegistry;

    private ClientRegistrationRepository clientRegistrationRepository;
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

    private HttpSecurityCustomizer httpSecurityCustomizer = (h) -> {};
    private ClientRegistrationCustomizer clientRegistrationCustomizer = (h) -> {};

    public GeoServerOAuth2LoginAuthenticationFilterBuilder() {
        super();
    }

    /**
     * Builds a new filter, set up with the given configuration. Must be called once only.
     *
     * @return a new filter
     */
    public GeoServerOAuth2LoginAuthenticationFilter build() {
        validate();

        GeoServerOAuth2LoginAuthenticationFilter filter = new GeoServerOAuth2LoginAuthenticationFilter();

        if (0 < configuration.getActiveProviderCount()) {
            filter.setLogoutSuccessHandler(getLogoutSuccessHandler());
            filter.setNestedFilters(createNestedFilters());
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
            return new HttpSecurityConfigurer(
                            http, configuration, securityManager, tokenDecoderFactory, REQ_FILTER_TYPES, builderContext)
                    .configure();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create filter.", e);
        }
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

    /** @return the logoutSuccessHandler */
    public LogoutSuccessHandler getLogoutSuccessHandler() {
        if (logoutSuccessHandler == null) {
            logoutSuccessHandler = new LogoutHandlerFactory()
                    .create(getClientRegistrationRepository(), configuration.getPostLogoutRedirectUri());
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
    public ClientRegistrationRepository getClientRegistrationRepository() {
        if (clientRegistrationRepository == null) {
            clientRegistrationRepository = new ClientRegistrationFactory(configuration, clientRegistrationCustomizer)
                    .build(eventPublisher, clientRegistrationRegistry, this);
        }
        return clientRegistrationRepository;
    }

    /** @param pClientRegistrationRepository the clientRegistrationRepository to set */
    public void setClientRegistrationRepository(ClientRegistrationRepository pClientRegistrationRepository) {
        clientRegistrationRepository = pClientRegistrationRepository;
    }

    /** @return the authorizedClientService */
    public OAuth2AuthorizedClientService getAuthorizedClientService() {
        if (authorizedClientService == null) {
            authorizedClientService = new InMemoryOAuth2AuthorizedClientService(getClientRegistrationRepository());
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
            authorizedClientRepository =
                    new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(getAuthorizedClientService());
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
                if (configuration != null
                        && configuration.isEnableResourceServerMode()
                        && BearerAwareSecurityContextRepository.isBearerRequest(r)) {
                    return false;
                }
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

    /**
     * Inject the application-wide {@link OAuth2ClientRegistrationRegistry}. When set, the builder publishes this
     * filter's registrations into the shared registry and returns it from {@link #getClientRegistrationRepository()} so
     * that Spring's redirect / login filters in every OAuth2 filter's sub-chain can resolve scoped registration IDs
     * owned by sibling filters. Required for multi-filter deployments; when {@code null}, the builder falls back to the
     * legacy private per-filter repository.
     */
    public void setClientRegistrationRegistry(OAuth2ClientRegistrationRegistry pRegistry) {
        clientRegistrationRegistry = pRegistry;
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

    /**
     * Adapter exposing this builder as a {@link HttpSecurityConfigurer.BuilderContext} — lets the configurer pull all
     * lazily-initialized inputs without leaking the builder's public surface to it.
     */
    private final HttpSecurityConfigurer.BuilderContext builderContext = new HttpSecurityConfigurer.BuilderContext() {
        @Override
        public ClientRegistrationRepository clientRegistrationRepository() {
            return getClientRegistrationRepository();
        }

        @Override
        public OAuth2AuthorizedClientRepository authorizedClientRepository() {
            return getAuthorizedClientRepository();
        }

        @Override
        public OAuth2AuthorizedClientService authorizedClientService() {
            return getAuthorizedClientService();
        }

        @Override
        public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService() {
            return getOauth2UserService();
        }

        @Override
        public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
            return getOidcUserService();
        }

        @Override
        public DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver() {
            return getAuthorizationRequestResolver();
        }

        @Override
        public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
            return getAccessTokenResponseClient();
        }

        @Override
        public Filter redirectToProviderFilter() {
            return getRedirectToProviderFilter();
        }

        @Override
        public HttpSecurityCustomizer httpSecurityCustomizer() {
            return httpSecurityCustomizer;
        }
    };
}

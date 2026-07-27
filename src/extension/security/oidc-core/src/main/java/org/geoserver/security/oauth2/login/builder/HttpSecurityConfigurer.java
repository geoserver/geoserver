/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login.builder;

import static java.util.stream.Collectors.toList;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_GOOGLE;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_MICROSOFT;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.scopedRegId;

import jakarta.servlet.Filter;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.BearerAwareSecurityContextRepository;
import org.geoserver.security.oauth2.login.GeoServerJwtAudienceValidator;
import org.geoserver.security.oauth2.login.GeoServerOAuth2JwtAuthenticationConverter;
import org.geoserver.security.oauth2.login.OAuth2LoginCustomizers.HttpSecurityCustomizer;
import org.geoserver.security.oauth2.spring.GeoServerOidcIdTokenDecoderFactory;
import org.geoserver.security.oauth2.token.GeoServerOAuth2OpaqueTokenIntrospector;
import org.geoserver.security.oauth2.token.TokenIntrospector;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures the per-filter Spring {@link HttpSecurity} chain — wires up oauth2Login, optional oauth2ResourceServer
 * (hybrid mode for Bearer-token requests), the user/token services and customizer hooks. Returns the subset of Spring
 * filters that GeoServer's composite filter is supposed to wrap.
 *
 * <p>Hybrid resource-server mode (Bearer + Login on the same filter) is auto-enabled only when exactly one provider is
 * enabled. Choice between opaque-token introspection (RFC 7662) and JWT validation is driven by whether an
 * introspection URL is configured.
 *
 * <p>Extracted from {@code GeoServerOAuth2LoginAuthenticationFilterBuilder}. Takes the builder by reference because the
 * Spring HttpSecurity wiring pulls many lazily-initialized inputs from it (user services, authorization request
 * resolver, access-token response client …) — moving each setter chain would require a much bigger interface.
 */
public class HttpSecurityConfigurer {

    /**
     * Filter types the OIDC plugin actually cares about — Spring's HttpSecurity.build() returns a full chain; we keep
     * only these four and wrap them inside {@code GeoServerOAuth2LoginAuthenticationFilter}.
     */
    private final List<Class<?>> reqFilterTypes;

    private final HttpSecurity http;
    private final GeoServerOAuth2LoginFilterConfig config;
    private final GeoServerSecurityManager securityManager;
    private final GeoServerOidcIdTokenDecoderFactory tokenDecoderFactory;
    private final BuilderContext ctx;

    public HttpSecurityConfigurer(
            HttpSecurity http,
            GeoServerOAuth2LoginFilterConfig config,
            GeoServerSecurityManager securityManager,
            GeoServerOidcIdTokenDecoderFactory tokenDecoderFactory,
            List<Class<?>> reqFilterTypes,
            BuilderContext ctx) {
        this.http = http;
        this.config = config;
        this.securityManager = securityManager;
        this.tokenDecoderFactory = tokenDecoderFactory;
        this.reqFilterTypes = reqFilterTypes;
        this.ctx = ctx;
    }

    /**
     * Wires the {@link HttpSecurity} (oauth2Login + optional oauth2ResourceServer), applies the consumer-supplied
     * customizer, and returns the filtered subset of the resulting Spring chain plus the optional
     * "redirect-to-provider" filter when single-provider entry-point redirection is enabled.
     */
    public List<Filter> configure() throws Exception {
        // Each builder instance gets its own prototype-scoped tokenDecoderFactory (see applicationContext.xml).
        tokenDecoderFactory.setGeoServerOAuth2LoginFilterConfig(config);

        http.oauth2Login(oauth -> {
            oauth.clientRegistrationRepository(ctx.clientRegistrationRepository());
            oauth.authorizedClientRepository(ctx.authorizedClientRepository());
            oauth.authorizedClientService(ctx.authorizedClientService());
            oauth.userInfoEndpoint(userInfo -> {
                userInfo.userService(ctx.oauth2UserService());
                userInfo.oidcUserService(ctx.oidcUserService());
            });
            oauth.authorizationEndpoint(req -> req.authorizationRequestResolver(ctx.authorizationRequestResolver()));
            oauth.tokenEndpoint(token -> token.accessTokenResponseClient(ctx.accessTokenResponseClient()));
            oauth.loginProcessingUrl("/web/login/oauth2/code/*");
            // On OAuth2/OIDC authentication failure Spring's default failure handler redirects to "/login?error" —
            // a path with no handler in the GeoServer webapp (no Wicket page mount, no servlet, no static resource).
            // Tomcat's default servlet then serves the bare path back without a Content-Type, which browsers
            // interpret as a downloadable empty file named "login". Redirect to /web/ instead so the Wicket-rendered
            // home page surfaces the standard login form and users can retry without seeing a file-download dialog.
            oauth.failureUrl("/web/");
        });

        // Hybrid mode: accept machine-to-machine Authorization: Bearer requests using the same provider config.
        OpaqueTokenIntrospector opaqueIntrospector = buildOpaqueIntrospectorIfApplicable();
        JwtDecoder jwtDecoder = (opaqueIntrospector == null) ? buildJwtDecoderIfApplicable() : null;

        if (opaqueIntrospector != null) {
            // Bearer-token requests must remain stateless even if a UI login session exists.
            http.securityContext(sc -> sc.securityContextRepository(new BearerAwareSecurityContextRepository()));
            http.oauth2ResourceServer(rs -> rs.opaqueToken(ot -> ot.introspector(opaqueIntrospector)));
        } else if (jwtDecoder != null) {
            http.securityContext(sc -> sc.securityContextRepository(new BearerAwareSecurityContextRepository()));
            GeoServerOAuth2JwtAuthenticationConverter converter =
                    new GeoServerOAuth2JwtAuthenticationConverter(securityManager, config);
            http.oauth2ResourceServer(rs -> rs.jwt(jwt -> {
                jwt.decoder(jwtDecoder);
                jwt.jwtAuthenticationConverter(converter);
            }));
        }

        HttpSecurityCustomizer customizer = ctx.httpSecurityCustomizer();
        if (customizer != null) {
            customizer.accept(http);
        }

        SecurityFilterChain chain = http.build();
        List<Filter> filters = chain.getFilters().stream()
                .filter(f -> reqFilterTypes.contains(f.getClass()))
                .collect(toList());

        String authEntryPoint = config.getAuthenticationEntryPointRedirectUri();
        if (config.getEnableRedirectAuthenticationEntryPoint() && authEntryPoint != null) {
            filters.add(ctx.redirectToProviderFilter());
        }
        return filters;
    }

    /**
     * Builds a {@link JwtDecoder} for resource-server mode when exactly one provider is enabled and a JWKS URI is
     * resolvable, optionally adding an audience validator. Returns null when not applicable (resource-server mode off,
     * no provider, multiple providers, GitHub-only, or no JWKS URI).
     */
    JwtDecoder buildJwtDecoderIfApplicable() {
        if (config == null || !config.isEnableResourceServerMode() || config.getActiveProviderCount() != 1) {
            return null;
        }
        String jwkSetUri = resolveJwkSetUri();
        if (!StringUtils.isNotBlank(jwkSetUri)) {
            return null;
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefault();
        if (config.isValidateTokenAudience()) {
            OAuth2TokenValidator<Jwt> aud = new GeoServerJwtAudienceValidator(
                    config.getValidateTokenAudienceClaimName(), config.getValidateTokenAudienceClaimValue());
            validator = new DelegatingOAuth2TokenValidator<>(validator, aud);
        }
        decoder.setJwtValidator(validator);
        return decoder;
    }

    private String resolveJwkSetUri() {
        if (config.isGoogleEnabled()) {
            ClientRegistration reg = ctx.clientRegistrationRepository()
                    .findByRegistrationId(scopedRegId(config.getName(), REG_ID_GOOGLE));
            return reg == null ? null : reg.getProviderDetails().getJwkSetUri();
        }
        if (config.isMsEnabled()) {
            ClientRegistration reg = ctx.clientRegistrationRepository()
                    .findByRegistrationId(scopedRegId(config.getName(), REG_ID_MICROSOFT));
            return reg == null ? null : reg.getProviderDetails().getJwkSetUri();
        }
        if (config.isOidcEnabled()) {
            return config.getOidcJwkSetUri();
        }
        return null; // GitHub-only or unknown
    }

    /**
     * Builds an {@link OpaqueTokenIntrospector} for resource-server mode when exactly one (custom OIDC) provider is
     * enabled and an introspection endpoint URL is configured. Returns null when not applicable.
     */
    OpaqueTokenIntrospector buildOpaqueIntrospectorIfApplicable() {
        if (config == null || !config.isEnableResourceServerMode() || config.getActiveProviderCount() != 1) {
            return null;
        }
        if (!config.isOidcEnabled()) {
            return null;
        }
        String introspectionUrl = config.getOidcIntrospectionUrl();
        if (!StringUtils.isNotBlank(introspectionUrl)) {
            return null;
        }
        TokenIntrospector delegate = new TokenIntrospector(
                introspectionUrl,
                config.getOidcClientId(),
                config.getOidcClientSecret(),
                config.isOidcAuthenticationMethodPostSecret());
        return new GeoServerOAuth2OpaqueTokenIntrospector(delegate, securityManager, config);
    }

    /**
     * Bag of lazily-initialized inputs the configurer pulls from its enclosing builder. Implemented as an inline
     * lambda-style functional interface so each accessor remains a one-liner in the builder.
     */
    public interface BuilderContext {
        ClientRegistrationRepository clientRegistrationRepository();

        org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository authorizedClientRepository();

        org.springframework.security.oauth2.client.OAuth2AuthorizedClientService authorizedClientService();

        org.springframework.security.oauth2.client.userinfo.OAuth2UserService<
                        org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest,
                        org.springframework.security.oauth2.core.user.OAuth2User>
                oauth2UserService();

        org.springframework.security.oauth2.client.userinfo.OAuth2UserService<
                        org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest,
                        org.springframework.security.oauth2.core.oidc.user.OidcUser>
                oidcUserService();

        org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver
                authorizationRequestResolver();

        org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient<
                        org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest>
                accessTokenResponseClient();

        Filter redirectToProviderFilter();

        HttpSecurityCustomizer httpSecurityCustomizer();
    }
}

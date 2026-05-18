/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login.builder;

import static java.util.Collections.singletonMap;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_GIT_HUB;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_GOOGLE;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_MICROSOFT;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.scopedRegId;
import static org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent.disableButtonEvent;
import static org.geoserver.security.oauth2.login.OAuth2LoginButtonEnablementEvent.enableButtonEvent;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
import static org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.OAuth2ClientRegistrationRegistry;
import org.geoserver.security.oauth2.login.OAuth2LoginCustomizers.ClientRegistrationCustomizer;
import org.geoserver.security.oauth2.login.ScopeUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Assembles the per-filter list of Spring {@link ClientRegistration}s from a {@link GeoServerOAuth2LoginFilterConfig},
 * publishes per-provider button enable/disable events to the application event bus, and returns either the shared
 * registry-backed repository (when one is wired) or a private fallback repository.
 *
 * <p>Extracted from {@code GeoServerOAuth2LoginAuthenticationFilterBuilder} to keep that class focused on filter
 * assembly. Stateless except for the optional {@link ClientRegistrationCustomizer} applied to each built registration.
 */
public class ClientRegistrationFactory {

    private final GeoServerOAuth2LoginFilterConfig config;
    private final ClientRegistrationCustomizer registrationCustomizer;

    public ClientRegistrationFactory(
            GeoServerOAuth2LoginFilterConfig config, ClientRegistrationCustomizer registrationCustomizer) {
        this.config = config;
        this.registrationCustomizer = registrationCustomizer == null ? r -> {} : registrationCustomizer;
    }

    /**
     * Builds the per-filter {@link ClientRegistration}s and publishes the button enablement events. When
     * {@code sharedRegistry} is non-null the registrations are atomically published into it (and that same registry is
     * returned, so every OAuth2 filter shares one repository — Spring Security 7's redirect-resolver throws on unknown
     * registration IDs, which means cross-filter URL routing requires a shared repository). When null, a private
     * {@link InMemoryClientRegistrationRepository} is returned (legacy / minimal-test fallback).
     */
    public ClientRegistrationRepository build(
            ApplicationEventPublisher eventPublisher,
            OAuth2ClientRegistrationRegistry sharedRegistry,
            Object eventSource) {
        String filterName = config.getName();
        List<ClientRegistration> registrations = new ArrayList<>();

        if (config.isGoogleEnabled()) {
            registrations.add(buildGoogleRegistration());
            eventPublisher.publishEvent(
                    enableButtonEvent(eventSource, REG_ID_GOOGLE, scopedRegId(filterName, REG_ID_GOOGLE)));
        } else {
            eventPublisher.publishEvent(
                    disableButtonEvent(eventSource, REG_ID_GOOGLE, scopedRegId(filterName, REG_ID_GOOGLE)));
        }
        if (config.isGitHubEnabled()) {
            registrations.add(buildGitHubRegistration());
            eventPublisher.publishEvent(
                    enableButtonEvent(eventSource, REG_ID_GIT_HUB, scopedRegId(filterName, REG_ID_GIT_HUB)));
        } else {
            eventPublisher.publishEvent(
                    disableButtonEvent(eventSource, REG_ID_GIT_HUB, scopedRegId(filterName, REG_ID_GIT_HUB)));
        }
        if (config.isMsEnabled()) {
            registrations.add(buildMicrosoftRegistration());
            eventPublisher.publishEvent(
                    enableButtonEvent(eventSource, REG_ID_MICROSOFT, scopedRegId(filterName, REG_ID_MICROSOFT)));
        } else {
            eventPublisher.publishEvent(
                    disableButtonEvent(eventSource, REG_ID_MICROSOFT, scopedRegId(filterName, REG_ID_MICROSOFT)));
        }
        if (config.isOidcEnabled()) {
            registrations.add(buildCustomOidcRegistration());
            eventPublisher.publishEvent(
                    enableButtonEvent(eventSource, REG_ID_OIDC, scopedRegId(filterName, REG_ID_OIDC)));
        } else {
            eventPublisher.publishEvent(
                    disableButtonEvent(eventSource, REG_ID_OIDC, scopedRegId(filterName, REG_ID_OIDC)));
        }

        if (sharedRegistry != null) {
            sharedRegistry.replaceFilterRegistrations(filterName, registrations);
            return sharedRegistry;
        }
        return new InMemoryClientRegistrationRepository(registrations);
    }

    /**
     * Google: well-known endpoint at https://accounts.google.com/.well-known/openid-configuration. Google does not
     * support OIDC RP-initiated logout, so no end_session_endpoint is configured.
     */
    private ClientRegistration buildGoogleRegistration() {
        ClientRegistration reg = CommonOAuth2Provider.GOOGLE
                .getBuilder(scopedRegId(config.getName(), REG_ID_GOOGLE))
                .clientId(config.getGoogleClientId())
                .clientSecret(config.getGoogleClientSecret())
                .userNameAttributeName(config.getGoogleUserNameAttribute())
                .redirectUri(config.getGoogleRedirectUri())
                .build();
        registrationCustomizer.accept(reg);
        return reg;
    }

    /**
     * GitHub: OAuth2 only (not OIDC). No RP-initiated logout supported. Docs:
     * https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps
     */
    private ClientRegistration buildGitHubRegistration() {
        ClientRegistration reg = CommonOAuth2Provider.GITHUB
                .getBuilder(scopedRegId(config.getName(), REG_ID_GIT_HUB))
                .clientId(config.getGitHubClientId())
                .clientSecret(config.getGitHubClientSecret())
                .userNameAttributeName(config.getGitHubUserNameAttribute())
                .redirectUri(config.getGitHubRedirectUri())
                .build();
        registrationCustomizer.accept(reg);
        return reg;
    }

    /**
     * Microsoft Azure (multi-tenant /common). Well-known endpoint:
     * https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration
     */
    private ClientRegistration buildMicrosoftRegistration() {
        String[] scopes = ScopeUtils.valueOf(config.getMsScopes());
        ClientRegistration reg = ClientRegistration.withRegistrationId(scopedRegId(config.getName(), REG_ID_MICROSOFT))
                .clientId(config.getMsClientId())
                .clientSecret(config.getMsClientSecret())
                .userNameAttributeName(config.getMsUserNameAttribute())
                .redirectUri(config.getMsRedirectUri())
                .clientAuthenticationMethod(CLIENT_SECRET_BASIC)
                .authorizationGrantType(AUTHORIZATION_CODE)
                .scope(scopes)
                .authorizationUri("https://login.microsoftonline.com/common/oauth2/v2.0/authorize")
                .tokenUri("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                .userInfoUri("https://graph.microsoft.com/oidc/userinfo")
                .jwkSetUri("https://login.microsoftonline.com/common/discovery/v2.0/keys")
                .providerConfigurationMetadata(singletonMap(
                        "end_session_endpoint", "https://login.microsoftonline.com/common/oauth2/v2.0/logout"))
                .clientName(REG_ID_MICROSOFT)
                .build();
        registrationCustomizer.accept(reg);
        return reg;
    }

    /**
     * Custom OIDC provider — all endpoints + scopes + client auth method come from the filter config (typically
     * populated by the "Discover" button hitting the IdP's well-known endpoint).
     */
    private ClientRegistration buildCustomOidcRegistration() {
        String[] scopes = ScopeUtils.valueOf(config.getOidcScopes());
        ClientAuthenticationMethod authMethod =
                config.isOidcAuthenticationMethodPostSecret() ? CLIENT_SECRET_POST : CLIENT_SECRET_BASIC;

        ClientRegistration reg = ClientRegistration.withRegistrationId(scopedRegId(config.getName(), REG_ID_OIDC))
                .clientId(config.getOidcClientId())
                .clientSecret(config.getOidcClientSecret())
                .userNameAttributeName(config.getOidcUserNameAttribute())
                .redirectUri(config.getOidcRedirectUri())
                .clientAuthenticationMethod(authMethod)
                .authorizationGrantType(AUTHORIZATION_CODE)
                .scope(scopes)
                .authorizationUri(config.getOidcAuthorizationUri())
                .tokenUri(config.getOidcTokenUri())
                .userInfoUri(config.getOidcUserInfoUri())
                .jwkSetUri(config.getOidcJwkSetUri())
                .providerConfigurationMetadata(singletonMap("end_session_endpoint", config.getOidcLogoutUri()))
                .clientName(REG_ID_OIDC)
                .build();
        registrationCustomizer.accept(reg);
        return reg;
    }
}

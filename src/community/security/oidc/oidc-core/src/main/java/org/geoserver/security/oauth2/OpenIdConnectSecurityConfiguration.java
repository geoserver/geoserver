/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.util.List;
import org.geoserver.security.oauth2.pkce.PKCERequestEnhancer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

/** OpenId connect specific REST templates for OAuth2 protocol. */
@Configuration(value = "openIdConnectSecurityConfiguration")
@EnableOAuth2Client
class OpenIdConnectSecurityConfiguration extends GeoServerOAuth2SecurityConfiguration {

    private OpenIdConnectFilterConfig config;

    @Override
    @Bean(name = "openIdConnectResource")
    public OAuth2ProtectedResourceDetails geoServerOAuth2Resource() {
        return super.geoServerOAuth2Resource();
    }

    @Override
    protected String getDetailsId() {
        return "openid-connect-oauth2-client";
    }

    /** Must have "session" scope */
    @Override
    @Bean(name = "openIdConnectRestTemplate")
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public OAuth2RestTemplate geoServerOauth2RestTemplate() {
        OAuth2RestTemplate template = super.geoServerOauth2RestTemplate();
        List<HttpMessageConverter<?>> messageConverters = template.getMessageConverters();
        messageConverters.add(new MappingJackson2HttpMessageConverter());

        return template;
    }

    public void setConfiguration(OpenIdConnectFilterConfig config) {
        this.config = config;
    }

    @Override
    protected OAuth2RestTemplate getOAuth2RestTemplate() {
        if (config != null) {
            String jwkUri = config.getJwkURI();
            return new ValidatingOAuth2RestTemplate(
                    geoServerOAuth2Resource(),
                    new DefaultOAuth2ClientContext(getAccessTokenRequest()),
                    jwkUri,
                    config);
        }
        return super.getOAuth2RestTemplate();
    }

    @Override
    @Bean(name = "authorizationAccessTokenProvider")
    @Scope(value = "prototype")
    public AuthorizationCodeAccessTokenProvider authorizationAccessTokenProvider() {
        AuthorizationCodeAccessTokenProvider authorizationCodeAccessTokenProvider =
                super.authorizationAccessTokenProvider();
        if (config != null && config.isUsePKCE()) {
            authorizationCodeAccessTokenProvider.setTokenRequestEnhancer(
                    new PKCERequestEnhancer(config));
        }
        return authorizationCodeAccessTokenProvider;
    }
}

/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.security.oauth2;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

/** OpenId connect specific REST templates for OAuth2 protocol. */
@Configuration(value = "openIdConnectSecurityConfiguration")
@EnableOAuth2Client
class OpenIdConnectSecurityConfiguration extends GeoServerOAuth2SecurityConfiguration {

    @Bean(name = "openIdConnectResource")
    public OAuth2ProtectedResourceDetails geoServerOAuth2Resource() {
        return super.geoServerOAuth2Resource();
    }

    @Override
    protected String getDetailsId() {
        return "openid-connect-oauth2-client";
    }

    /** Must have "session" scope */
    @Bean(name = "openIdConnectRestTemplate")
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public OAuth2RestTemplate geoServerOauth2RestTemplate() {
        OAuth2RestTemplate template = super.geoServerOauth2RestTemplate();
        List<HttpMessageConverter<?>> messageConverters = template.getMessageConverters();
        messageConverters.add(new MappingJackson2HttpMessageConverter());

        return template;
    }
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;

/**
 * Base OAuth2 Configuration Class. Each OAuth2 specific Extension must implement its own {@link OAuth2RestTemplate}
 * 
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public abstract class GeoServerOAuth2SecurityConfiguration implements OAuth2SecurityConfiguration {

    @Autowired
    protected Environment env;

    @Resource
    @Qualifier("accessTokenRequest")
    private AccessTokenRequest accessTokenRequest;

    /**
     * @return the accessTokenRequest
     */
    public AccessTokenRequest getAccessTokenRequest() {
        return accessTokenRequest;
    }

    /**
     * @param accessTokenRequest the accessTokenRequest to set
     */
    public void setAccessTokenRequest(AccessTokenRequest accessTokenRequest) {
        this.accessTokenRequest = accessTokenRequest;
    }

    @Bean
    public OAuth2ProtectedResourceDetails geoServerOAuth2Resource() {
        AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
        details.setId("google-oauth-client");

        details.setGrantType("authorization_code");
        details.setTokenName("authorization_code");
        details.setUseCurrentUri(false);
        details.setAuthenticationScheme(AuthenticationScheme.query);
        details.setClientAuthenticationScheme(AuthenticationScheme.form);

        return details;
    }

    public abstract OAuth2RestTemplate geoServerOauth2RestTemplate();
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenProvider;
import org.springframework.security.oauth2.client.token.AccessTokenProviderChain;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.client.token.grant.implicit.ImplicitAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

/**
 * Google specific REST remplates for OAuth2 protocol.
 * <p>
 * First of all the user must create an API key through the Google API Credentials <br/>
 * See: <string>https://console.developers.google.com/apis/credentials/oauthclient </strong>
 * </p>
 * <p>
 * The procedure will provide a new <b>Client ID</b> and <b>Client Secret</b>
 * </p>
 * <p>
 * Also the user must specify the <b>Authorized redirect URIs</b> pointing to the GeoServer instances <br/>
 * Example:
 * <ul>
 * <li>http://localhost:8080/geoserver</li>
 * <li>http://localhost:8080/geoserver/</li>
 * </ul>
 * </p>
 * <p>
 * The Google OAuth2 Filter Endpoint will automatically redirect the users to an URL like the following one at first login <br/>
 * <br/>
 * <code>
 * https://accounts.google.com/o/oauth2/auth?response_type=code&client_id=my_client_id&redirect_uri=http://localhost:8080/geoserver&scope=https://www.googleapis.com/auth/userinfo.email%20https://www.googleapis.com/auth/userinfo.profile
 * </code>
 * </p>
 * <p>
 * Tipically a correct configuration for the Google OAuth2 Provider is like the following:
 * </p>
 * <ul>
 * <li>Cliend Id: <b>my_client_id</b></li>
 * <li>Cliend Secret: <b>my_client_secret</b></li>
 * <li>Access Token URI: <b>https://accounts.google.com/o/oauth2/token</b></li>
 * <li>User Authorization URI: <b>https://accounts.google.com/o/oauth2/auth</b></li>
 * <li>Redirect URI: <b>http://localhost:8080/geoserver</b></li>
 * <li>Check Token Endpoint URL: <b>https://www.googleapis.com/oauth2/v1/tokeninfo</b></li>
 * <li>Logout URI: <b>https://accounts.google.com/logout</b></li>
 * <li>Scopes: <b>https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/userinfo.profile</b></li>
 * </ul>
 * 
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
@Configuration(value="googleOAuth2SecurityConfiguration")
@EnableOAuth2Client
class GoogleOAuth2SecurityConfiguration extends GeoServerOAuth2SecurityConfiguration {

    @Bean(name="googleOAuth2Resource")
    public OAuth2ProtectedResourceDetails geoServerOAuth2Resource() {
        AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
        details.setId("oauth2-client");

        details.setGrantType("authorization_code");
        details.setTokenName("authorization_code");
        details.setUseCurrentUri(false);
        details.setAuthenticationScheme(AuthenticationScheme.query);
        details.setClientAuthenticationScheme(AuthenticationScheme.form);

        return details;
    }
    
    /**
     * Must have "session" scope
     */
    @Bean(name="googleOauth2RestTemplate")
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public OAuth2RestTemplate geoServerOauth2RestTemplate() {

        OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(geoServerOAuth2Resource(),
                new DefaultOAuth2ClientContext(getAccessTokenRequest()));

        AuthorizationCodeAccessTokenProvider authorizationCodeAccessTokenProvider = new AuthorizationCodeAccessTokenProvider();
        authorizationCodeAccessTokenProvider.setStateMandatory(false);

        AccessTokenProvider accessTokenProviderChain = new AccessTokenProviderChain(
                Arrays.<AccessTokenProvider> asList(authorizationCodeAccessTokenProvider,
                        new ImplicitAccessTokenProvider(),
                        new ResourceOwnerPasswordAccessTokenProvider(),
                        new ClientCredentialsAccessTokenProvider()));

        oAuth2RestTemplate.setAccessTokenProvider(accessTokenProviderChain);

        return oAuth2RestTemplate;
    }

}

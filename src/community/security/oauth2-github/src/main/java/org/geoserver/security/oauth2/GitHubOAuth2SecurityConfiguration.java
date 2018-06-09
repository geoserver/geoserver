/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
 * GitHub specific REST remplates for OAuth2 protocol.
 *
 * <p>GitHub Authorization APIs available at
 * <strong>https://developer.github.com/v3/#authentication</strong> and
 * <strong>https://developer.github.com/v3/oauth/</strong>
 *
 * <p>First of all the user must create an API key through the GitHub API Credentials <br>
 * See: <string>https://github.com/settings/applications/new</strong>
 *
 * <p>The procedure will provide a new <b>Client ID</b> and <b>Client Secret</b>
 *
 * <p>Also the user must specify the <b>Authorization callback URL</b> pointing to the GeoServer
 * instances <br>
 * Example:
 *
 * <ul>
 *   <li>http://localhost:8080/geoserver
 * </ul>
 *
 * <p>The GitHub OAuth2 Filter Endpoint will automatically redirect the users to an URL like the
 * following one at first login <br>
 * <br>
 * <code>
 * https://github.com/login/oauth/authorize?response_type=code&client_id=my_client_id&redirect_uri=http://localhost:8080/geoserver&scope=user
 * </code>
 *
 * <p>Tipically a correct configuration for the GitHub OAuth2 Provider is like the following:
 *
 * <ul>
 *   <li>Cliend Id: <b>my_client_id</b>
 *   <li>Cliend Secret: <b>my_client_secret</b>
 *   <li>Access Token URI: <b>https://github.com/login/oauth/access_token</b>
 *   <li>User Authorization URI: <b>https://github.com/login/oauth/authorize</b>
 *   <li>Redirect URI: <b>http://localhost:8080/geoserver</b>
 *   <li>Check Token Endpoint URL: <b>https://api.github.com/user</b>
 *   <li>Logout URI: <b>https://github.com/logout</b>
 *   <li>Scopes: <b>user</b>
 * </ul>
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
@Configuration(value = "githubOAuth2SecurityConfiguration")
@EnableOAuth2Client
class GitHubOAuth2SecurityConfiguration extends GeoServerOAuth2SecurityConfiguration {

    @Bean(name = "githubOAuth2Resource")
    public OAuth2ProtectedResourceDetails geoServerOAuth2Resource() {
        AuthorizationCodeResourceDetails details = new AuthorizationCodeResourceDetails();
        details.setId("github-oauth2-client");

        details.setGrantType("authorization_code");
        details.setAuthenticationScheme(AuthenticationScheme.header);
        details.setClientAuthenticationScheme(AuthenticationScheme.form);

        return details;
    }

    /** Must have "session" scope */
    @Bean(name = "githubOauth2RestTemplate")
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public OAuth2RestTemplate geoServerOauth2RestTemplate() {

        OAuth2RestTemplate oAuth2RestTemplate =
                new OAuth2RestTemplate(
                        geoServerOAuth2Resource(),
                        new DefaultOAuth2ClientContext(getAccessTokenRequest()));

        AuthorizationCodeAccessTokenProvider authorizationCodeAccessTokenProvider =
                new AuthorizationCodeAccessTokenProvider();
        authorizationCodeAccessTokenProvider.setStateMandatory(false);

        AccessTokenProvider accessTokenProviderChain =
                new AccessTokenProviderChain(
                        Arrays.<AccessTokenProvider>asList(
                                authorizationCodeAccessTokenProvider,
                                new ImplicitAccessTokenProvider(),
                                new ResourceOwnerPasswordAccessTokenProvider(),
                                new ClientCredentialsAccessTokenProvider()));

        oAuth2RestTemplate.setAccessTokenProvider(accessTokenProviderChain);
        List<HttpMessageConverter<?>> messageConverters = oAuth2RestTemplate.getMessageConverters();
        messageConverters.add(new MappingJackson2HttpMessageConverter());

        return oAuth2RestTemplate;
    }
}

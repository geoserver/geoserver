/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

/**
 * Google specific REST remplates for OAuth2 protocol.
 *
 * <p>First of all the user must create an API key through the Google API Credentials <br>
 * See: <string>https://console.developers.google.com/apis/credentials/oauthclient </strong>
 *
 * <p>The procedure will provide a new <b>Client ID</b> and <b>Client Secret</b>
 *
 * <p>Also the user must specify the <b>Authorized redirect URIs</b> pointing to the GeoServer
 * instances <br>
 * Example:
 *
 * <ul>
 *   <li>http://localhost:8080/geoserver
 *   <li>http://localhost:8080/geoserver/
 * </ul>
 *
 * <p>The Google OAuth2 Filter Endpoint will automatically redirect the users to an URL like the
 * following one at first login <br>
 * <br>
 * <code>
 * https://accounts.google.com/o/oauth2/auth?response_type=code&client_id=my_client_id&redirect_uri=http://localhost:8080/geoserver&scope=https://www.googleapis.com/auth/userinfo.email%20https://www.googleapis.com/auth/userinfo.profile
 * </code>
 *
 * <p>Tipically a correct configuration for the Google OAuth2 Provider is like the following:
 *
 * <ul>
 *   <li>Cliend Id: <b>my_client_id</b>
 *   <li>Cliend Secret: <b>my_client_secret</b>
 *   <li>Access Token URI: <b>https://accounts.google.com/o/oauth2/token</b>
 *   <li>User Authorization URI: <b>https://accounts.google.com/o/oauth2/auth</b>
 *   <li>Redirect URI: <b>http://localhost:8080/geoserver</b>
 *   <li>Check Token Endpoint URL: <b>https://www.googleapis.com/oauth2/v1/tokeninfo</b>
 *   <li>Logout URI: <b>https://accounts.google.com/logout</b>
 *   <li>Scopes:
 *       <b>https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/userinfo.profile</b>
 * </ul>
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
@Configuration(value = "googleOAuth2SecurityConfiguration")
@EnableOAuth2Client
class GoogleOAuth2SecurityConfiguration extends GeoServerOAuth2SecurityConfiguration {

    @Bean(name = "googleOAuth2Resource")
    public OAuth2ProtectedResourceDetails geoServerOAuth2Resource() {
        AuthorizationCodeResourceDetails details =
                (AuthorizationCodeResourceDetails) super.geoServerOAuth2Resource();
        details.setTokenName("authorization_code");

        return details;
    }

    /** Must have "session" scope */
    @Bean(name = "googleOauth2RestTemplate")
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public OAuth2RestTemplate geoServerOauth2RestTemplate() {
        return super.geoServerOauth2RestTemplate();
    }
}

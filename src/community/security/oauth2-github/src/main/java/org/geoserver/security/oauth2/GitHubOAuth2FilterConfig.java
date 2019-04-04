/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GitHubOAuth2FilterConfig extends GeoServerOAuth2FilterConfig {

    public GitHubOAuth2FilterConfig() {
        this.accessTokenUri = "https://github.com/login/oauth/access_token";
        this.userAuthorizationUri = "https://github.com/login/oauth/authorize";
        this.redirectUri = "http://localhost:8080/geoserver";
        this.checkTokenEndpointUrl = "https://api.github.com/user";
        this.logoutUri = "https://github.com/logout";
        this.scopes = "user";
        this.enableRedirectAuthenticationEntryPoint = false;
        this.forceAccessTokenUriHttps = true;
        this.forceUserAuthorizationUriHttps = true;
        this.loginEndpoint = "/j_spring_oauth2_github_login";
        this.logoutEndpoint = "/j_spring_oauth2_github_logout";
    };
}

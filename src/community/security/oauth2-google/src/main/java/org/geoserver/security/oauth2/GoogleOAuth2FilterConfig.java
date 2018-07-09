/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GoogleOAuth2FilterConfig extends GeoServerOAuth2FilterConfig {

    public GoogleOAuth2FilterConfig() {
        this.accessTokenUri = "https://accounts.google.com/o/oauth2/token";
        this.userAuthorizationUri = "https://accounts.google.com/o/oauth2/auth";
        this.redirectUri = "http://localhost:8080/geoserver";
        this.checkTokenEndpointUrl = "https://www.googleapis.com/oauth2/v1/tokeninfo";
        this.logoutUri = "https://accounts.google.com/logout";
        this.scopes =
                "https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/userinfo.profile";
        this.enableRedirectAuthenticationEntryPoint = false;
        this.forceAccessTokenUriHttps = true;
        this.forceUserAuthorizationUriHttps = true;
        this.loginEndpoint = "/j_spring_oauth2_google_login";
        this.logoutEndpoint = "/j_spring_oauth2_google_logout";
    }
}

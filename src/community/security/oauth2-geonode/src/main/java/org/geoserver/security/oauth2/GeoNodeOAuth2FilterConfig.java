/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeoNodeOAuth2FilterConfig extends GeoServerOAuth2FilterConfig {

    public GeoNodeOAuth2FilterConfig() {
        // default values
        this.accessTokenUri = "https://geonode_host/o/token/";
        this.userAuthorizationUri = "https://geonode_host_port/o/authorize/";
        this.checkTokenEndpointUrl = "https://geonode_host_port/api/o/v4/tokeninfo/";
        this.logoutUri = "https://geonode_host_port/account/logout/";
        this.scopes = "read,write,groups";
        this.enableRedirectAuthenticationEntryPoint = false;
        this.forceAccessTokenUriHttps = false;
        this.forceUserAuthorizationUriHttps = false;
        this.loginEndpoint = "/j_spring_oauth2_geonode_login";
        this.logoutEndpoint = "/j_spring_oauth2_geonode_logout";
    }
}

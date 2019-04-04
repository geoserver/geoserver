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
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

/**
 * GeoNode specific REST templates for OAuth2 protocol.
 *
 * <p>First of all the user must create an OAuth2 Application throught the GeoNode Admin GUI <br>
 * See: <string>GeoNode docs: Admin Tutorial</strong>
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
 * <p>The GeoNode OAuth2 Filter Endpoint will automatically redirect the users to an URL like the
 * following one at first login <br>
 *
 * <p>Tipically a correct configuration for the GeoNode OAuth2 Provider is like the following one:
 *
 * <ul>
 *   <li>Cliend Id: <b>my_client_id</b>
 *   <li>Cliend Secret: <b>my_client_secret</b>
 *   <li>Access Token URI: <b>https://geonode_host/o/token/</b>
 *   <li>User Authorization URI: <b>https://geonode_host_port/o/authorize/</b>
 *   <li>Redirect URI: <b>http://localhost:8080/geoserver</b>
 *   <li>Check Token Endpoint URL: <b>https://geonode_host_port/api/o/v4/tokeninfo/</b>
 *   <li>Logout URI: <b>https://geonode_host_port/account/logout/</b>
 *   <li>Scopes: <b>read,write,groups</b>
 * </ul>
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
@Configuration(value = "geoNodeOAuth2SecurityConfiguration")
@EnableOAuth2Client
class GeoNodeOAuth2SecurityConfiguration extends GeoServerOAuth2SecurityConfiguration {

    @Bean(name = "geoNodeOAuth2Resource")
    public OAuth2ProtectedResourceDetails geoServerOAuth2Resource() {
        return super.geoServerOAuth2Resource();
    }

    @Override
    protected String getDetailsId() {
        return "geonode-oauth2-client";
    }

    /** Must have "session" scope */
    @Bean(name = "geoNodeOauth2RestTemplate")
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public OAuth2RestTemplate geoServerOauth2RestTemplate() {
        return super.geoServerOauth2RestTemplate();
    }
}

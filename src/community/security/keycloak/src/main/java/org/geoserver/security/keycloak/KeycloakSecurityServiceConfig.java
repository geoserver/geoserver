/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;

/** Extension of {@link SecurityNamedServiceConfig} */
public class KeycloakSecurityServiceConfig extends BaseSecurityNamedServiceConfig {

    private String serverURL;
    private String realm;
    private String clientID;
    private String clientSecret;

    public KeycloakSecurityServiceConfig() {}

    public KeycloakSecurityServiceConfig(KeycloakSecurityServiceConfig other) {
        super(other);
        serverURL = other.getServerURL();
        realm = other.getRealm();
        clientID = other.getClientID();
        clientSecret = other.getClientSecret();
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        String url = serverURL;
        // If the given URL does not start with http or https, add http
        if (!url.matches("^(http|https)://.*$")) {
            url = "http://" + url;
        }
        // Remove trailing / if given
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        this.serverURL = url;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientID() {
        return clientID;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}

package org.geoserver.taskmanager.external.impl;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.http.KeycloakAuthenticator;
import java.net.MalformedURLException;
import java.net.URL;

public class ExternalKeycloakGSImpl extends ExternalGSImpl {

    private String clientId;

    private String clientSecret;

    private String authUrl;

    private String realm;

    private String proxyHost;

    private int proxyPort;

    private KeycloakAuthenticator auth = null;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    @Override
    public GeoServerRESTManager getRESTManager() throws MalformedURLException {
        if (auth == null) {
            auth =
                    new KeycloakAuthenticator(
                            getUsername(),
                            getPassword(),
                            clientId,
                            clientSecret,
                            authUrl,
                            realm,
                            proxyHost,
                            proxyPort);
        }
        return new GeoServerRESTManager(new URL(getUrl()), auth);
    }
}

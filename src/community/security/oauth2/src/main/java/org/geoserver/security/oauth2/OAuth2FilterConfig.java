/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;

/**
 * The GeoServer OAuth2 Filter Configuration. This POJO contains the properties needed to correctly configure the Spring Auth Filter.
 * 
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class OAuth2FilterConfig extends PreAuthenticatedUserNameFilterConfig {

    private static final long serialVersionUID = 3448775031434700860L;

    private String cliendId;

    private String clientSecret;

    private String accessTokenUri;

    private String userAuthorizationUri;

    private String redirectUri = "http://localhost:8080/geoserver";

    private String checkTokenEndpointUrl;

    private String logoutUri;

    private String scopes;

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }

    /**
     * @return the cliendId
     */
    public String getCliendId() {
        return cliendId;
    }

    /**
     * @param cliendId the cliendId to set
     */
    public void setCliendId(String cliendId) {
        this.cliendId = cliendId;
    }

    /**
     * @return the clientSecret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * @param clientSecret the clientSecret to set
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * @return the accessTokenUri
     */
    public String getAccessTokenUri() {
        return accessTokenUri;
    }

    /**
     * @param accessTokenUri the accessTokenUri to set
     */
    public void setAccessTokenUri(String accessTokenUri) {
        this.accessTokenUri = accessTokenUri;
    }

    /**
     * @return the userAuthorizationUri
     */
    public String getUserAuthorizationUri() {
        return userAuthorizationUri;
    }

    /**
     * @param userAuthorizationUri the userAuthorizationUri to set
     */
    public void setUserAuthorizationUri(String userAuthorizationUri) {
        this.userAuthorizationUri = userAuthorizationUri;
    }

    /**
     * @return the redirectUri
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * @param redirectUri the redirectUri to set
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * @return the checkTokenEndpointUrl
     */
    public String getCheckTokenEndpointUrl() {
        return checkTokenEndpointUrl;
    }

    /**
     * @param checkTokenEndpointUrl the checkTokenEndpointUrl to set
     */
    public void setCheckTokenEndpointUrl(String checkTokenEndpointUrl) {
        this.checkTokenEndpointUrl = checkTokenEndpointUrl;
    }

    /**
     * @return the logoutUri
     */
    public String getLogoutUri() {
        return logoutUri;
    }

    /**
     * @param logoutUri the logoutUri to set
     */
    public void setLogoutUri(String logoutUri) {
        this.logoutUri = logoutUri;
    }

    /**
     * @return the scopes
     */
    public String getScopes() {
        return scopes;
    }

    /**
     * @param scopes the scopes to set
     */
    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

}

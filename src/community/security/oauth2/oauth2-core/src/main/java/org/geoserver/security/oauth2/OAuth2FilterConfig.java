/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * The GeoServer OAuth2 Filter Configuration. This POJO contains the properties needed to correctly
 * configure the Spring Auth Filter.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public interface OAuth2FilterConfig {

    /** @return the cliendId */
    public String getCliendId();

    /** @param cliendId the cliendId to set */
    public void setCliendId(String cliendId);

    /** @return the clientSecret */
    public String getClientSecret();

    /** @param clientSecret the clientSecret to set */
    public void setClientSecret(String clientSecret);

    /** @return */
    public Boolean getForceAccessTokenUriHttps();

    /** @param forceAccessTokenUriHttps */
    public void setForceAccessTokenUriHttps(Boolean forceAccessTokenUriHttps);

    /** @return the accessTokenUri */
    public String getAccessTokenUri();

    /** @param accessTokenUri the accessTokenUri to set */
    public void setAccessTokenUri(String accessTokenUri);

    /** @return */
    public Boolean getForceUserAuthorizationUriHttps();

    /** @param forceAccessTokenUriHttps */
    public void setForceUserAuthorizationUriHttps(Boolean forceUserAuthorizationUriHttps);

    /** @return the userAuthorizationUri */
    public String getUserAuthorizationUri();

    /** @param userAuthorizationUri the userAuthorizationUri to set */
    public void setUserAuthorizationUri(String userAuthorizationUri);

    /** @return the redirectUri */
    public String getRedirectUri();

    /** @param redirectUri the redirectUri to set */
    public void setRedirectUri(String redirectUri);

    /** @return the checkTokenEndpointUrl */
    public String getCheckTokenEndpointUrl();

    /** @param checkTokenEndpointUrl the checkTokenEndpointUrl to set */
    public void setCheckTokenEndpointUrl(String checkTokenEndpointUrl);

    /** @return the logoutUri */
    public String getLogoutUri();

    /** @param logoutUri the logoutUri to set */
    public void setLogoutUri(String logoutUri);

    /** @return the scopes */
    public String getScopes();

    /** @param scopes the scopes to set */
    public void setScopes(String scopes);

    /** **THIS MUST** be different for every OAuth2 Plugin */
    public String getLoginEndpoint();

    /** **THIS MUST** be different for every OAuth2 Plugin */
    public String getLogoutEndpoint();

    /** @param loginEndpoint */
    public void setLoginEndpoint(String loginEndpoint);

    /** @param logoutEndpoint */
    public void setLogoutEndpoint(String logoutEndpoint);

    /** @return the enableRedirectAuthenticationEntryPoint */
    public Boolean getEnableRedirectAuthenticationEntryPoint();

    /**
     * @param enableRedirectAuthenticationEntryPoint the enableRedirectAuthenticationEntryPoint to
     *     set
     */
    public void setEnableRedirectAuthenticationEntryPoint(
            Boolean enableRedirectAuthenticationEntryPoint);

    /**
     * Returns filter {@link AuthenticationEntryPoint} actual implementation
     *
     * @return {@link AuthenticationEntryPoint}
     */
    public AuthenticationEntryPoint getAuthenticationEntryPoint();
}

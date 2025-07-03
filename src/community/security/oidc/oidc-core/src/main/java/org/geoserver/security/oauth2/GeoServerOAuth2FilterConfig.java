/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeoServerOAuth2FilterConfig extends PreAuthenticatedUserNameFilterConfig
        implements SecurityAuthFilterConfig, OAuth2FilterConfig {

    private static final long serialVersionUID = -8581346584859849804L;

    protected String cliendId;

    protected String clientSecret;

    protected String accessTokenUri;

    protected String userAuthorizationUri;

    protected String redirectUri = "http://localhost:8080/geoserver";

    protected String checkTokenEndpointUrl;

    protected String logoutUri;

    protected String scopes;

    protected Boolean enableRedirectAuthenticationEntryPoint;

    protected Boolean forceAccessTokenUriHttps;

    protected Boolean forceUserAuthorizationUriHttps;

    protected String loginEndpoint;

    protected String logoutEndpoint;

    /**
     * Add extra logging. NOTE: this might spill confidential information to the log - do not turn
     * on in normal operation!
     */
    boolean allowUnSecureLogging = false;

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return true;
    }

    @Override
    public boolean isAllowUnSecureLogging() {
        return allowUnSecureLogging;
    }

    @Override
    public void setAllowUnSecureLogging(boolean allowUnSecureLogging) {
        this.allowUnSecureLogging = allowUnSecureLogging;
    }

    /** @return the cliendId */
    @Override
    public String getCliendId() {
        return cliendId;
    }

    /** @param cliendId the cliendId to set */
    @Override
    public void setCliendId(String cliendId) {
        this.cliendId = cliendId;
    }

    /** @return the clientSecret */
    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    /** @param clientSecret the clientSecret to set */
    @Override
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /** @return the accessTokenUri */
    @Override
    public String getAccessTokenUri() {
        return accessTokenUri;
    }

    /** @param accessTokenUri the accessTokenUri to set */
    @Override
    public void setAccessTokenUri(String accessTokenUri) {
        this.accessTokenUri = accessTokenUri;
    }

    /** @return the userAuthorizationUri */
    @Override
    public String getUserAuthorizationUri() {
        return userAuthorizationUri;
    }

    /** @param userAuthorizationUri the userAuthorizationUri to set */
    @Override
    public void setUserAuthorizationUri(String userAuthorizationUri) {
        this.userAuthorizationUri = userAuthorizationUri;
    }

    /** @return the redirectUri */
    @Override
    public String getRedirectUri() {
        return redirectUri;
    }

    /** @param redirectUri the redirectUri to set */
    @Override
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /** @return the checkTokenEndpointUrl */
    @Override
    public String getCheckTokenEndpointUrl() {
        return checkTokenEndpointUrl;
    }

    /** @param checkTokenEndpointUrl the checkTokenEndpointUrl to set */
    @Override
    public void setCheckTokenEndpointUrl(String checkTokenEndpointUrl) {
        this.checkTokenEndpointUrl = checkTokenEndpointUrl;
    }

    /** @return the logoutUri */
    @Override
    public String getLogoutUri() {
        return logoutUri;
    }

    /** @param logoutUri the logoutUri to set */
    @Override
    public void setLogoutUri(String logoutUri) {
        this.logoutUri = logoutUri;
    }

    /** @return the scopes */
    @Override
    public String getScopes() {
        return scopes;
    }

    /** @param scopes the scopes to set */
    @Override
    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    /** @return the enableRedirectAuthenticationEntryPoint */
    @Override
    public Boolean getEnableRedirectAuthenticationEntryPoint() {
        return enableRedirectAuthenticationEntryPoint;
    }

    /**
     * @param enableRedirectAuthenticationEntryPoint the enableRedirectAuthenticationEntryPoint to
     *     set
     */
    @Override
    public void setEnableRedirectAuthenticationEntryPoint(
            Boolean enableRedirectAuthenticationEntryPoint) {
        this.enableRedirectAuthenticationEntryPoint = enableRedirectAuthenticationEntryPoint;
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return new AuthenticationEntryPoint() {

            @Override
            public void commence(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    AuthenticationException authException)
                    throws IOException, ServletException {
                final StringBuilder loginUri = buildAuthorizationUrl();

                if (getEnableRedirectAuthenticationEntryPoint()
                        || request.getRequestURI().endsWith(getLoginEndpoint())) {
                    response.sendRedirect(loginUri.toString());
                }
            }
        };
    }

    @Override
    public StringBuilder buildAuthorizationUrl() {
        final StringBuilder loginUri = new StringBuilder(getUserAuthorizationUri());
        loginUri.append("?")
                .append("response_type=code")
                .append("&")
                .append("client_id=")
                .append(getCliendId())
                .append("&")
                .append("scope=")
                .append(getScopes().replace(",", "%20"))
                .append("&")
                .append("redirect_uri=")
                .append(getRedirectUri());
        return loginUri;
    }

    @Override
    public Boolean getForceAccessTokenUriHttps() {
        return forceAccessTokenUriHttps;
    }

    @Override
    public void setForceAccessTokenUriHttps(Boolean forceAccessTokenUriHttps) {
        this.forceAccessTokenUriHttps = forceAccessTokenUriHttps;
    }

    @Override
    public Boolean getForceUserAuthorizationUriHttps() {
        return forceUserAuthorizationUriHttps;
    }

    @Override
    public void setForceUserAuthorizationUriHttps(Boolean forceUserAuthorizationUriHttps) {
        this.forceUserAuthorizationUriHttps = forceUserAuthorizationUriHttps;
    }

    @Override
    public String getLoginEndpoint() {
        return loginEndpoint;
    }

    @Override
    public String getLogoutEndpoint() {
        return logoutEndpoint;
    }

    @Override
    public void setLoginEndpoint(String loginEndpoint) {
        this.loginEndpoint = loginEndpoint;
    }

    @Override
    public void setLogoutEndpoint(String logoutEndpoint) {
        this.logoutEndpoint = logoutEndpoint;
    }
}

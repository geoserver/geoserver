/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.security.oauth2;

import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.RoleSource;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Filter configuration for OpenId Connect. This is completely freeform, so adding only the basic
 * bits in here.
 */
public class OpenIdConnectFilterConfig extends GeoServerOAuth2FilterConfig {

    String principalKey = "email";
    String jwkURI;
    String tokenRolesClaim;
    String responseMode;
    String postLogoutRedirectUri;
    boolean sendClientSecret = false;
    boolean allowBearerTokens = true;

    /** Supports extraction of roles among the token claims */
    public static enum OpenIdRoleSource implements RoleSource {
        IdToken,
        AccessToken,
        MSGraphAPI,
        UserInfo;

        @Override
        public boolean equals(RoleSource other) {
            return other != null && other.toString().equals(toString());
        }
    };

    public OpenIdConnectFilterConfig() {
        this.redirectUri = baseRedirectUri();
        this.postLogoutRedirectUri = baseRedirectUri();
        this.scopes = "user";
        this.enableRedirectAuthenticationEntryPoint = false;
        this.forceAccessTokenUriHttps = true;
        this.forceUserAuthorizationUriHttps = true;
        this.loginEndpoint = "/j_spring_oauth2_openid_connect_login";
        this.logoutEndpoint = "/j_spring_oauth2_openid_connect_logout";
    };

    /**
     * we add "/" at the end since not having it will SOMETIME cause issues. This will either use
     * the proxyBaseURL (if set), or from ServletUriComponentsBuilder.fromCurrentContextPath().
     *
     * @return
     */
    String baseRedirectUri() {
        GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
        String proxbaseUrl = null;
        if (gs != null) proxbaseUrl = gs.getSettings().getProxyBaseUrl();
        if (StringUtils.hasText(proxbaseUrl)) {
            return proxbaseUrl + "/";
        }
        if (RequestContextHolder.getRequestAttributes() != null)
            return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + "/";
        return "http://localhost:8080/geoserver";
    }

    public String getPrincipalKey() {
        return principalKey == null ? "email" : principalKey;
    }

    public void setPrincipalKey(String principalKey) {
        this.principalKey = principalKey;
    }

    public String getJwkURI() {
        return jwkURI;
    }

    public void setJwkURI(String jwkURI) {
        this.jwkURI = jwkURI;
    }

    public String getTokenRolesClaim() {
        return tokenRolesClaim;
    }

    public void setTokenRolesClaim(String tokenRolesClaim) {
        this.tokenRolesClaim = tokenRolesClaim;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public boolean isSendClientSecret() {
        return sendClientSecret;
    }

    public void setSendClientSecret(boolean sendClientSecret) {
        this.sendClientSecret = sendClientSecret;
    }

    public boolean isAllowBearerTokens() {
        return allowBearerTokens;
    }

    public void setAllowBearerTokens(boolean allowBearerTokens) {
        this.allowBearerTokens = allowBearerTokens;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    @Override
    public StringBuilder buildAuthorizationUrl() {
        StringBuilder sb = super.buildAuthorizationUrl();
        String responseMode = getResponseMode();
        if (responseMode != null && !"".equals(responseMode.trim()))
            sb.append("&response_mode=").append(responseMode);
        return sb;
    }

    protected StringBuilder buildEndSessionUrl(final String idToken) {
        final StringBuilder logoutUri = new StringBuilder(getLogoutUri());
        boolean first = true;
        if (idToken != null) first = appendParam(first, "id_token_hint", idToken, logoutUri);
        if (StringUtils.hasText(getPostLogoutRedirectUri()))
            appendParam(first, "post_logout_redirect_uri", getPostLogoutRedirectUri(), logoutUri);
        return logoutUri;
    }

    private boolean appendParam(boolean first, String name, String value, StringBuilder sb) {
        sb.append(first ? "?" : "&").append(name).append("=").append(value);
        return false;
    }
}

/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static java.util.Optional.ofNullable;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilterBuilder.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Filter configuration for OAuth2 and OpenID Connect.
 *
 * @author awaterme
 */
public class GeoServerOAuth2LoginFilterConfig extends PreAuthenticatedUserNameFilterConfig
        implements SecurityAuthFilterConfig, GeoServerOAuth2ClientRegistrationId {

    @Serial
    private static final long serialVersionUID = -8581346584859849804L;

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
    }

    /**
     * Constant used to setup the proxy base in tests that are running without a GeoServer instance or an actual HTTP
     * request context. The value of the variable is set-up in the pom.xml, as a system property for surefire, in order
     * to avoid hard-coding the value in the code.
     */
    public static final String OPENID_TEST_GS_PROXY_BASE = "OPENID_TEST_GS_PROXY_BASE";

    /** This is the GeoServer URL that the OIDC IDP should redirect to (with the oidc "code") */
    public static final String OIDC_INCOMING_CODE_ENDPOINT = "web/login/oauth2/code/";

    // Common for all providers
    private String baseRedirectUri = baseRedirectUri();

    // Google
    private boolean googleEnabled;
    private String googleClientId;
    private String googleClientSecret;
    private String googleUserNameAttribute = "email";
    private String googleRedirectUri;

    // GitHub
    private boolean gitHubEnabled;
    private String gitHubClientId;
    private String gitHubClientSecret;
    private String gitHubUserNameAttribute = "id";
    private String gitHubRedirectUri;

    // Microsoft Azure
    private boolean msEnabled;
    private String msClientId;
    private String msClientSecret;
    private String msUserNameAttribute = "sub";
    private String msRedirectUri;
    private String msScopes = "openid profile email";

    // custom OpenID Connect
    private boolean oidcEnabled;
    private String oidcClientId;
    private String oidcClientSecret;
    private String oidcUserNameAttribute = "email";
    private String oidcRedirectUri;
    private String oidcScopes = "openid";

    private String oidcDiscoveryUri;
    private String oidcTokenUri;
    private String oidcAuthorizationUri;
    private String oidcUserInfoUri;
    private String oidcJwkSetUri;
    private String oidcLogoutUri;
    private String oidcResponseMode;
    /** currently no UI counterpart */
    private String oidcJwsAlgorithmName;

    private boolean oidcForceAuthorizationUriHttps = true;
    private boolean oidcForceTokenUriHttps = true;
    private boolean oidcEnforceTokenValidation = true;
    private boolean oidcUsePKCE = false;
    private boolean oidcAuthenticationMethodPostSecret = false;
    /**
     * Add extra logging. NOTE: this might spill confidential information to the log - do not turn on in normal
     * operation!
     */
    private boolean oidcAllowUnSecureLogging = false;

    // further common attributes affecting all providers
    private String tokenRolesClaim;
    private String postLogoutRedirectUri;
    private boolean enableRedirectAuthenticationEntryPoint;

    // MSGraph

    /** true -> get roles from MSGraph's memberOf endpoint (usually for groups) */
    private boolean msGraphMemberOf;

    /** true -> get roles from MSGraph's appRoleAssignments endpoint (for app roles associated with the user) */
    private boolean msGraphAppRoleAssignments;

    /** if msGraphAppRoleAssignments is true, then what is the Enterprise App's Object ID (NOT the Client Id). */
    private String msGraphAppRoleAssignmentsObjectId;

    // for role extraction
    private String roleConverterString;
    private boolean onlyExternalListedRoles;

    public GeoServerOAuth2LoginFilterConfig() {
        this.postLogoutRedirectUri = createPostLogoutRedirectUri();
        this.calculateRedirectUris();
    }

    public void calculateRedirectUris() {
        this.googleRedirectUri = redirectUri(REG_ID_GOOGLE);
        this.gitHubRedirectUri = redirectUri(REG_ID_GIT_HUB);
        this.msRedirectUri = redirectUri(REG_ID_MICROSOFT);
        this.oidcRedirectUri = redirectUri(REG_ID_OIDC);
    }

    private String redirectUri(String pRegId) {
        String lBase = baseRedirectUriNormalized();
        return lBase + OIDC_INCOMING_CODE_ENDPOINT + pRegId;
    }

    private String createPostLogoutRedirectUri() {
        String lBase = baseRedirectUri();
        if (!lBase.endsWith("/web/")) {
            lBase += "web/";
        }
        return lBase;
    }

    /** @return an URI ending with "/" */
    private String baseRedirectUriNormalized() {
        return ofNullable(baseRedirectUri)
                .map(s -> s.endsWith("/") ? s : s + "/")
                .orElse("/");
    }

    public String getAuthenticationEntryPointRedirectUri() {
        List<String> lRegIds = new ArrayList<>();
        if (isGoogleEnabled()) {
            lRegIds.add(REG_ID_GOOGLE);
        }
        if (isGitHubEnabled()) {
            lRegIds.add(REG_ID_GIT_HUB);
        }
        if (isMsEnabled()) {
            lRegIds.add(REG_ID_MICROSOFT);
        }
        if (isOidcEnabled()) {
            lRegIds.add(REG_ID_OIDC);
        }
        if (lRegIds.size() != 1) {
            return null;
        }
        String lBase = baseRedirectUriNormalized();

        return lBase + DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + lRegIds.get(0);
    }

    /**
     * we add "/" at the end since not having it will SOMETIME cause issues. This will either use the proxyBaseURL (if
     * set), or from ServletUriComponentsBuilder.fromCurrentContextPath().
     *
     * @return
     */
    String baseRedirectUri() {
        Optional<String> proxbaseUrl = Optional.ofNullable(GeoServerExtensions.bean(GeoServer.class))
                .map(gs -> gs.getSettings())
                .map(s -> s.getProxyBaseUrl());
        if (proxbaseUrl.isPresent() && StringUtils.hasText(proxbaseUrl.get())) {
            return proxbaseUrl + "/";
        }
        if (RequestContextHolder.getRequestAttributes() != null)
            return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + "/";
        // fallback to run tests without a full environment
        return GeoServerExtensions.getProperty(OPENID_TEST_GS_PROXY_BASE);
    }

    public String getOidcUserNameAttribute() {
        return oidcUserNameAttribute;
    }

    @Override
    public boolean providesAuthenticationEntryPoint() {
        return false;
    }

    public int getActiveProviderCount() {
        int lActiveCount = isGoogleEnabled() ? 1 : 0;
        lActiveCount += isGitHubEnabled() ? 1 : 0;
        lActiveCount += isMsEnabled() ? 1 : 0;
        lActiveCount += isOidcEnabled() ? 1 : 0;
        return lActiveCount;
    }

    /** @return the cliendId */
    public String getOidcClientId() {
        return oidcClientId;
    }

    /** @param cliendId the cliendId to set */
    public void setOidcClientId(String cliendId) {
        this.oidcClientId = cliendId;
    }

    /** @return the clientSecret */
    public String getOidcClientSecret() {
        return oidcClientSecret;
    }

    /** @param clientSecret the clientSecret to set */
    public void setOidcClientSecret(String clientSecret) {
        this.oidcClientSecret = clientSecret;
    }

    /** @return the accessTokenUri */
    public String getOidcTokenUri() {
        return oidcTokenUri;
    }

    /** @param accessTokenUri the accessTokenUri to set */
    public void setOidcTokenUri(String accessTokenUri) {
        this.oidcTokenUri = accessTokenUri;
    }

    /** @return the userAuthorizationUri */
    public String getOidcAuthorizationUri() {
        return oidcAuthorizationUri;
    }

    /** @param userAuthorizationUri the userAuthorizationUri to set */
    public void setOidcAuthorizationUri(String userAuthorizationUri) {
        this.oidcAuthorizationUri = userAuthorizationUri;
    }

    /** @return the redirectUri */
    public String getOidcRedirectUri() {
        return oidcRedirectUri;
    }

    /** @param redirectUri the redirectUri to set */
    public void setOidcRedirectUri(String redirectUri) {
        this.oidcRedirectUri = redirectUri;
    }

    /** @return the checkTokenEndpointUrl */
    public String getOidcUserInfoUri() {
        return oidcUserInfoUri;
    }

    /** @param checkTokenEndpointUrl the checkTokenEndpointUrl to set */
    public void setOidcUserInfoUri(String checkTokenEndpointUrl) {
        this.oidcUserInfoUri = checkTokenEndpointUrl;
    }

    /** @return the logoutUri */
    public String getOidcLogoutUri() {
        return oidcLogoutUri;
    }

    /** @param logoutUri the logoutUri to set */
    public void setOidcLogoutUri(String logoutUri) {
        this.oidcLogoutUri = logoutUri;
    }

    /** @return the scopes */
    public String getOidcScopes() {
        return oidcScopes;
    }

    /** @param scopes the scopes to set */
    public void setOidcScopes(String scopes) {
        this.oidcScopes = scopes;
    }

    /** @return the enableRedirectAuthenticationEntryPoint */
    public boolean getEnableRedirectAuthenticationEntryPoint() {
        return enableRedirectAuthenticationEntryPoint;
    }

    /** @param enableRedirectAuthenticationEntryPoint the enableRedirectAuthenticationEntryPoint to set */
    public void setEnableRedirectAuthenticationEntryPoint(boolean enableRedirectAuthenticationEntryPoint) {
        this.enableRedirectAuthenticationEntryPoint = enableRedirectAuthenticationEntryPoint;
    }

    public boolean getOidcForceTokenUriHttps() {
        return oidcForceTokenUriHttps;
    }

    public void setOidcForceTokenUriHttps(boolean forceAccessTokenUriHttps) {
        this.oidcForceTokenUriHttps = forceAccessTokenUriHttps;
    }

    public boolean getOidcForceAuthorizationUriHttps() {
        return oidcForceAuthorizationUriHttps;
    }

    public void setOidcForceAuthorizationUriHttps(boolean forceUserAuthorizationUriHttps) {
        this.oidcForceAuthorizationUriHttps = forceUserAuthorizationUriHttps;
    }

    public void setOidcUserNameAttribute(String principalKey) {
        this.oidcUserNameAttribute = principalKey;
    }

    public String getOidcJwkSetUri() {
        return oidcJwkSetUri;
    }

    public void setOidcJwkSetUri(String jwkURI) {
        this.oidcJwkSetUri = jwkURI;
    }

    public String getTokenRolesClaim() {
        return tokenRolesClaim;
    }

    public void setTokenRolesClaim(String tokenRolesClaim) {
        this.tokenRolesClaim = tokenRolesClaim;
    }

    public String getOidcResponseMode() {
        return oidcResponseMode;
    }

    public void setOidcResponseMode(String responseMode) {
        this.oidcResponseMode = responseMode;
    }

    public boolean isOidcAuthenticationMethodPostSecret() {
        return oidcAuthenticationMethodPostSecret;
    }

    public void setOidcAuthenticationMethodPostSecret(boolean sendClientSecret) {
        this.oidcAuthenticationMethodPostSecret = sendClientSecret;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public boolean isOidcUsePKCE() {
        return oidcUsePKCE;
    }

    public void setOidcUsePKCE(boolean usePKCE) {
        this.oidcUsePKCE = usePKCE;
    }

    public boolean isOidcEnforceTokenValidation() {
        return oidcEnforceTokenValidation;
    }

    public void setOidcEnforceTokenValidation(boolean enforceTokenValidation) {
        this.oidcEnforceTokenValidation = enforceTokenValidation;
    }

    /** @return the googleEnabled */
    public boolean isGoogleEnabled() {
        return googleEnabled;
    }

    /** @param pGoogleEnabled the googleEnabled to set */
    public void setGoogleEnabled(boolean pGoogleEnabled) {
        googleEnabled = pGoogleEnabled;
    }

    /** @return the googleCliendId */
    public String getGoogleClientId() {
        return googleClientId;
    }

    /** @param pGoogleCliendId the googleCliendId to set */
    public void setGoogleClientId(String pGoogleCliendId) {
        googleClientId = pGoogleCliendId;
    }

    /** @return the googleClientSecret */
    public String getGoogleClientSecret() {
        return googleClientSecret;
    }

    /** @param pGoogleClientSecret the googleClientSecret to set */
    public void setGoogleClientSecret(String pGoogleClientSecret) {
        googleClientSecret = pGoogleClientSecret;
    }

    /** @return the googleUserNameAttribute */
    public String getGoogleUserNameAttribute() {
        return googleUserNameAttribute;
    }

    /** @param pGoogleUserNameAttribute the googleUserNameAttribute to set */
    public void setGoogleUserNameAttribute(String pGoogleUserNameAttribute) {
        googleUserNameAttribute = pGoogleUserNameAttribute;
    }

    /** @return the gitHubEnabled */
    public boolean isGitHubEnabled() {
        return gitHubEnabled;
    }

    /** @param pGitHubEnabled the gitHubEnabled to set */
    public void setGitHubEnabled(boolean pGitHubEnabled) {
        gitHubEnabled = pGitHubEnabled;
    }

    /** @return the gitHubClientId */
    public String getGitHubClientId() {
        return gitHubClientId;
    }

    /** @param pGitHubClientId the gitHubClientId to set */
    public void setGitHubClientId(String pGitHubClientId) {
        gitHubClientId = pGitHubClientId;
    }

    /** @return the gitHubClientSecret */
    public String getGitHubClientSecret() {
        return gitHubClientSecret;
    }

    /** @param pGitHubClientSecret the gitHubClientSecret to set */
    public void setGitHubClientSecret(String pGitHubClientSecret) {
        gitHubClientSecret = pGitHubClientSecret;
    }

    /** @return the gitHubUserNameAttribute */
    public String getGitHubUserNameAttribute() {
        return gitHubUserNameAttribute;
    }

    /** @param pGitHubUserNameAttribute the gitHubUserNameAttribute to set */
    public void setGitHubUserNameAttribute(String pGitHubUserNameAttribute) {
        gitHubUserNameAttribute = pGitHubUserNameAttribute;
    }

    /** @return the enabled */
    public boolean isOidcEnabled() {
        return oidcEnabled;
    }

    /** @param pEnabled the enabled to set */
    public void setOidcEnabled(boolean pEnabled) {
        oidcEnabled = pEnabled;
    }

    /** @return the msEnabled */
    public boolean isMsEnabled() {
        return msEnabled;
    }

    /** @param pMsEnabled the msEnabled to set */
    public void setMsEnabled(boolean pMsEnabled) {
        msEnabled = pMsEnabled;
    }

    /** @return the msClientId */
    public String getMsClientId() {
        return msClientId;
    }

    /** @param pMsClientId the msClientId to set */
    public void setMsClientId(String pMsClientId) {
        msClientId = pMsClientId;
    }

    /** @return the msClientSecret */
    public String getMsClientSecret() {
        return msClientSecret;
    }

    /** @param pMsClientSecret the msClientSecret to set */
    public void setMsClientSecret(String pMsClientSecret) {
        msClientSecret = pMsClientSecret;
    }

    /** @return the msNameAttribute */
    public String getMsUserNameAttribute() {
        return msUserNameAttribute;
    }

    /** @param pMsNameAttribute the msNameAttribute to set */
    public void setMsUserNameAttribute(String pMsNameAttribute) {
        msUserNameAttribute = pMsNameAttribute;
    }

    /** @return the baseRedirectUri */
    public String getBaseRedirectUri() {
        return baseRedirectUri;
    }

    /** @param pBaseRedirectUri the baseRedirectUri to set */
    public void setBaseRedirectUri(String pBaseRedirectUri) {
        baseRedirectUri = pBaseRedirectUri;
    }

    /** @return the googleRedirectUri */
    public String getGoogleRedirectUri() {
        return googleRedirectUri;
    }

    /** @param pGoogleRedirectUri the googleRedirectUri to set */
    public void setGoogleRedirectUri(String pGoogleRedirectUri) {
        googleRedirectUri = pGoogleRedirectUri;
    }

    /** @return the gitHubRedirectUri */
    public String getGitHubRedirectUri() {
        return gitHubRedirectUri;
    }

    /** @param pGitHubRedirectUri the gitHubRedirectUri to set */
    public void setGitHubRedirectUri(String pGitHubRedirectUri) {
        gitHubRedirectUri = pGitHubRedirectUri;
    }

    /** @return the msRedirectUri */
    public String getMsRedirectUri() {
        return msRedirectUri;
    }

    /** @param pMsRedirectUri the msRedirectUri to set */
    public void setMsRedirectUri(String pMsRedirectUri) {
        msRedirectUri = pMsRedirectUri;
    }

    /** @return the msScopes */
    public String getMsScopes() {
        return msScopes;
    }

    /** @param pMsScopes the msScopes to set */
    public void setMsScopes(String pMsScopes) {
        msScopes = pMsScopes;
    }

    /** @return the oidcDiscoveryURL */
    public String getOidcDiscoveryUri() {
        return oidcDiscoveryUri;
    }

    /** @param pOidcDiscoveryURL the oidcDiscoveryURL to set */
    public void setOidcDiscoveryUri(String pOidcDiscoveryURL) {
        oidcDiscoveryUri = pOidcDiscoveryURL;
    }

    /** @return the allowUnSecureLogging */
    public boolean isOidcAllowUnSecureLogging() {
        return oidcAllowUnSecureLogging;
    }

    /** @param pAllowUnSecureLogging the allowUnSecureLogging to set */
    public void setOidcAllowUnSecureLogging(boolean pAllowUnSecureLogging) {
        oidcAllowUnSecureLogging = pAllowUnSecureLogging;
    }

    /** @return the jwsAlgorithmName */
    public String getOidcJwsAlgorithmName() {
        return oidcJwsAlgorithmName;
    }

    /** @param pJwsAlgorithmName the jwsAlgorithmName to set */
    public void setOidcJwsAlgorithmName(String pJwsAlgorithmName) {
        oidcJwsAlgorithmName = pJwsAlgorithmName;
    }

    public String getRoleConverterString() {
        return roleConverterString;
    }

    public void setRoleConverterString(String roleConverterString) {
        this.roleConverterString = roleConverterString;
    }

    public boolean isOnlyExternalListedRoles() {
        return onlyExternalListedRoles;
    }

    public void setOnlyExternalListedRoles(boolean onlyExternalListedRoles) {
        this.onlyExternalListedRoles = onlyExternalListedRoles;
    }

    public boolean isMsGraphMemberOf() {
        return msGraphMemberOf;
    }

    public void setMsGraphMemberOf(boolean msGraphMemberOf) {
        this.msGraphMemberOf = msGraphMemberOf;
    }

    public boolean isMsGraphAppRoleAssignments() {
        return msGraphAppRoleAssignments;
    }

    public void setMsGraphAppRoleAssignments(boolean msGraphAppRoleAssignments) {
        this.msGraphAppRoleAssignments = msGraphAppRoleAssignments;
    }

    public String getMsGraphAppRoleAssignmentsObjectId() {
        return msGraphAppRoleAssignmentsObjectId;
    }

    public void setMsGraphAppRoleAssignmentsObjectId(String msGraphAppRoleAssignmentsObjectId) {
        this.msGraphAppRoleAssignmentsObjectId = msGraphAppRoleAssignmentsObjectId;
    }
}

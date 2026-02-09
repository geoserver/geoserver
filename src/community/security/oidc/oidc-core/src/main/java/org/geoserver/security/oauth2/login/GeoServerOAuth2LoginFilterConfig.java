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
     * Constant used to set up the proxy base in tests that are running without a GeoServer instance or an actual HTTP
     * request context. The value of the variable is set up in the pom.xml, as a system property for surefire, in order
     * to avoid hard-coding the value in the code.
     */
    public static final String OPENID_TEST_GS_PROXY_BASE = "OPENID_TEST_GS_PROXY_BASE";

    /** This is the GeoServer URL that the OIDC IDP should redirect to (with the oidc "code") */
    public static final String OIDC_INCOMING_CODE_ENDPOINT = "web/login/oauth2/code/";

    // Common for all providers
    private String baseRedirectUri = resolveBaseRedirectUri();

    /**
     * Tracks whether {@link #setBaseRedirectUri(String)} was called in this session. When {@code false} (the default,
     * and after XStream deserialization since this is transient), {@link #getBaseRedirectUri()} resolves dynamically
     * from the current Proxy Base URL. When {@code true}, the explicitly set value is returned.
     *
     * <p>This allows the dynamic resolution to kick in after config reload (so changes to the global Proxy Base URL are
     * reflected), while still honoring explicit programmatic or UI-driven overrides within the same session.
     */
    private transient boolean baseRedirectUriExplicitlySet = false;

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

    private String oidcIntrospectionUrl;

    private boolean oidcForceAuthorizationUriHttps = true;
    private boolean oidcForceTokenUriHttps = true;
    private boolean oidcUsePKCE = false;
    private boolean oidcAuthenticationMethodPostSecret = false;
    private boolean disableSignatureValidation = false;
    /**
     * Add extra logging. NOTE: this might spill confidential information to the log - do not turn on in normal
     * operation!
     */
    private boolean oidcAllowUnSecureLogging = false;

    // further common attributes affecting all providers
    private String tokenRolesClaim;
    private String postLogoutRedirectUri;
    private boolean enableRedirectAuthenticationEntryPoint;

    /**
     * Hybrid mode: accept machine-to-machine requests via Authorization: Bearer <JWT> and validate them using the same
     * provider configuration already stored in this filter.
     *
     * <p>Enabled by default.
     */
    private boolean enableResourceServerMode = true;

    // Resource Server (Bearer JWT) optional validations
    private boolean validateTokenAudience = false;
    private String validateTokenAudienceClaimName = "aud";
    private String validateTokenAudienceClaimValue;

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
        String lBase = resolveBaseRedirectUri();
        if (lBase == null) {
            return null;
        }
        if (!lBase.endsWith("/web/")) {
            lBase += "web/";
        }
        return lBase;
    }

    /** @return a URI ending with "/" */
    private String baseRedirectUriNormalized() {
        return ofNullable(getBaseRedirectUri())
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
     * Resolves the base redirect URI dynamically from the current environment. Appends "/" to ensure consistent
     * trailing slash. Resolution order:
     *
     * <ol>
     *   <li>{@code PROXY_BASE_URL} environment variable / system property (container deployments)
     *   <li>{@code proxyBaseUrl} from GeoServer settings ({@code global.xml} or per-workspace)
     *   <li>Current HTTP request context path (when available)
     *   <li>{@link #OPENID_TEST_GS_PROXY_BASE} system property (test fallback)
     * </ol>
     *
     * @return the resolved base URI, or {@code null} if none of the above sources provides a value
     */
    private String resolveBaseRedirectUri() {
        // 1. PROXY_BASE_URL env var / system property — highest priority, used in container
        //    deployments where the env var overrides global.xml at runtime
        String envProxyBase = GeoServerExtensions.getProperty("PROXY_BASE_URL");
        if (StringUtils.hasText(envProxyBase)) {
            return ensureTrailingSlash(envProxyBase);
        }
        // 2. proxyBaseUrl from GeoServer settings (global.xml or per-workspace)
        //    In GeoServer 3.x, getProxyBaseUrl() returns Optional<String> instead of String.
        //    Calling Optional.toString() produces "Optional[url]", so we must unwrap it.
        GeoServer gs = GeoServerExtensions.bean(GeoServer.class);
        if (gs != null && gs.getSettings() != null) {
            String url = unwrapToString(gs.getSettings().getProxyBaseUrl());
            if (StringUtils.hasText(url)) {
                return ensureTrailingSlash(url);
            }
        }
        // 3. derive from current HTTP request context
        if (RequestContextHolder.getRequestAttributes() != null) {
            return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + "/";
        }
        // 4. fallback to run tests without a full environment
        return GeoServerExtensions.getProperty(OPENID_TEST_GS_PROXY_BASE);
    }

    /** Appends "/" if not already present. */
    private static String ensureTrailingSlash(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    /**
     * Converts a value to a plain String, unwrapping {@link java.util.Optional} if the runtime type is Optional.
     *
     * <p>In GeoServer 3.x, {@code SettingsInfo.getProxyBaseUrl()} returns {@code Optional<String>}.
     * {@code Optional.toString()} produces the debug form {@code "Optional[url]"}, not the inner value. This helper
     * detects that case and extracts the wrapped string.
     *
     * <p>Package-visible for unit testing.
     */
    static String unwrapToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.util.Optional<?> opt) {
            return opt.map(Object::toString).orElse(null);
        }
        return value.toString();
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

    /** @return the redirectUri, dynamically computed from the current base redirect URI */
    public String getOidcRedirectUri() {
        return redirectUri(REG_ID_OIDC);
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
        this.oidcUserInfoUri = StringUtils.hasText(checkTokenEndpointUrl) ? checkTokenEndpointUrl : null;
    }

    /** @return the logoutUri */
    public String getOidcLogoutUri() {
        return oidcLogoutUri;
    }

    /** @param logoutUri the logoutUri to set */
    public void setOidcLogoutUri(String logoutUri) {
        this.oidcLogoutUri = StringUtils.hasText(logoutUri) ? logoutUri : null;
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

    /** whether hybrid resource-server mode is enabled (Authorization: Bearer <JWT>) */
    public boolean isEnableResourceServerMode() {
        return enableResourceServerMode;
    }

    /** whether hybrid resource-server mode is enabled (Authorization: Bearer <JWT>) */
    public boolean getEnableResourceServerMode() {
        return enableResourceServerMode;
    }

    /** enableResourceServerMode enable/disable hybrid resource-server mode */
    public void setEnableResourceServerMode(boolean enableResourceServerMode) {
        this.enableResourceServerMode = enableResourceServerMode;
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
        this.tokenRolesClaim = StringUtils.hasText(tokenRolesClaim) ? tokenRolesClaim : null;
    }

    public String getOidcResponseMode() {
        return oidcResponseMode;
    }

    public void setOidcResponseMode(String responseMode) {
        this.oidcResponseMode = StringUtils.hasText(responseMode) ? responseMode : null;
    }

    public boolean isOidcAuthenticationMethodPostSecret() {
        return oidcAuthenticationMethodPostSecret;
    }

    public void setOidcAuthenticationMethodPostSecret(boolean sendClientSecret) {
        this.oidcAuthenticationMethodPostSecret = sendClientSecret;
    }

    public boolean isDisableSignatureValidation() {
        return disableSignatureValidation;
    }

    public void setDisableSignatureValidation(boolean disableSignatureValidation) {
        this.disableSignatureValidation = disableSignatureValidation;
    }

    /**
     * Returns the post-logout redirect URI, dynamically computed from the current base redirect URI. This ensures
     * changes to PROXY_BASE_URL are reflected immediately without re-saving the filter.
     */
    public String getPostLogoutRedirectUri() {
        String dynamicUri = createPostLogoutRedirectUri();
        return dynamicUri != null ? dynamicUri : postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = StringUtils.hasText(postLogoutRedirectUri) ? postLogoutRedirectUri : null;
    }

    public boolean isOidcUsePKCE() {
        return oidcUsePKCE;
    }

    public void setOidcUsePKCE(boolean usePKCE) {
        this.oidcUsePKCE = usePKCE;
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

    /**
     * Returns the currently selected (active) provider key, derived from the individual enabled flags. Defaults to
     * {@link OAuth2Provider#OIDC} if no provider is explicitly enabled.
     *
     * @return the property prefix of the active {@link OAuth2Provider}
     */
    public String getSelectedProvider() {
        if (googleEnabled) return OAuth2Provider.GOOGLE.getPropertyPrefix();
        if (gitHubEnabled) return OAuth2Provider.GITHUB.getPropertyPrefix();
        if (msEnabled) return OAuth2Provider.MICROSOFT.getPropertyPrefix();
        // OIDC is the default
        return OAuth2Provider.OIDC.getPropertyPrefix();
    }

    /**
     * Selects the given provider by enabling it and disabling all others. This is the model property backing the
     * provider selection dropdown in the UI.
     *
     * @param provider the {@link OAuth2Provider#getPropertyPrefix() property prefix} of the provider to select
     */
    public void setSelectedProvider(String provider) {
        googleEnabled = OAuth2Provider.GOOGLE.getPropertyPrefix().equals(provider);
        gitHubEnabled = OAuth2Provider.GITHUB.getPropertyPrefix().equals(provider);
        msEnabled = OAuth2Provider.MICROSOFT.getPropertyPrefix().equals(provider);
        oidcEnabled = OAuth2Provider.OIDC.getPropertyPrefix().equals(provider);
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

    /**
     * Returns the base redirect URI, dynamically resolved from the current Proxy Base URL. This ensures that changes to
     * the global Proxy Base URL (or the {@code PROXY_BASE_URL} environment variable) are immediately reflected in the
     * OIDC redirect URIs without requiring a manual re-save of the filter configuration.
     *
     * <p>If {@link #setBaseRedirectUri(String)} was called in this session (e.g. via the admin UI or programmatically),
     * the explicitly set value is returned instead, honoring the override.
     *
     * <p>Falls back to the stored field value when dynamic resolution is not possible (e.g. during deserialization
     * before GeoServer is fully initialized).
     *
     * @return the base redirect URI
     */
    public String getBaseRedirectUri() {
        if (baseRedirectUriExplicitlySet) {
            return baseRedirectUri;
        }
        String resolved = resolveBaseRedirectUri();
        return resolved != null ? resolved : baseRedirectUri;
    }

    /** @param pBaseRedirectUri the baseRedirectUri to set */
    public void setBaseRedirectUri(String pBaseRedirectUri) {
        baseRedirectUri = pBaseRedirectUri;
        baseRedirectUriExplicitlySet = true;
    }

    /** @return the googleRedirectUri, dynamically computed from the current base redirect URI */
    public String getGoogleRedirectUri() {
        return redirectUri(REG_ID_GOOGLE);
    }

    /** @param pGoogleRedirectUri the googleRedirectUri to set */
    public void setGoogleRedirectUri(String pGoogleRedirectUri) {
        googleRedirectUri = pGoogleRedirectUri;
    }

    /** @return the gitHubRedirectUri, dynamically computed from the current base redirect URI */
    public String getGitHubRedirectUri() {
        return redirectUri(REG_ID_GIT_HUB);
    }

    /** @param pGitHubRedirectUri the gitHubRedirectUri to set */
    public void setGitHubRedirectUri(String pGitHubRedirectUri) {
        gitHubRedirectUri = pGitHubRedirectUri;
    }

    /** @return the msRedirectUri, dynamically computed from the current base redirect URI */
    public String getMsRedirectUri() {
        return redirectUri(REG_ID_MICROSOFT);
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
        oidcDiscoveryUri = StringUtils.hasText(pOidcDiscoveryURL) ? pOidcDiscoveryURL : null;
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
        oidcJwsAlgorithmName = StringUtils.hasText(pJwsAlgorithmName) ? pJwsAlgorithmName : null;
    }

    public String getOidcIntrospectionUrl() {
        return oidcIntrospectionUrl;
    }

    public void setOidcIntrospectionUrl(String oidcIntrospectionUrl) {
        this.oidcIntrospectionUrl = StringUtils.hasText(oidcIntrospectionUrl) ? oidcIntrospectionUrl : null;
    }

    public String getRoleConverterString() {
        return roleConverterString;
    }

    public void setRoleConverterString(String roleConverterString) {
        this.roleConverterString = StringUtils.hasText(roleConverterString) ? roleConverterString : null;
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
        this.msGraphAppRoleAssignmentsObjectId =
                StringUtils.hasText(msGraphAppRoleAssignmentsObjectId) ? msGraphAppRoleAssignmentsObjectId : null;
    }
    /**
     * When enabled, the resource server (Bearer JWT) validator will require the configured audience claim to match the
     * expected value.
     */
    public boolean isValidateTokenAudience() {
        return validateTokenAudience;
    }

    public void setValidateTokenAudience(boolean validateTokenAudience) {
        this.validateTokenAudience = validateTokenAudience;
    }

    public String getValidateTokenAudienceClaimName() {
        return validateTokenAudienceClaimName;
    }

    public void setValidateTokenAudienceClaimName(String validateTokenAudienceClaimName) {
        this.validateTokenAudienceClaimName = validateTokenAudienceClaimName;
    }

    public String getValidateTokenAudienceClaimValue() {
        return validateTokenAudienceClaimValue;
    }

    public void setValidateTokenAudienceClaimValue(String validateTokenAudienceClaimValue) {
        this.validateTokenAudienceClaimValue = validateTokenAudienceClaimValue;
    }
}

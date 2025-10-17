/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.geoserver.platform.exception.GeoServerRuntimException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.oauth2.common.GeoServerOAuth2FilterConfigException;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;
import org.springframework.util.StringUtils;

/**
 * Validates {@link GeoServerOAuth2LoginFilterConfig} objects.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 * @author awaterme
 */
public class GeoServerOAuth2LoginFilterConfigValidator extends FilterConfigValidator {

    public GeoServerOAuth2LoginFilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    public void validateFilterConfig(SecurityNamedServiceConfig config) throws FilterConfigException {

        if (config instanceof GeoServerOAuth2LoginFilterConfig filterConfig) {
            validateOAuth2FilterConfig(filterConfig);
        } else {
            super.validateFilterConfig(config);
        }
    }

    public void validateOAuth2FilterConfig(GeoServerOAuth2LoginFilterConfig filterConfig) throws FilterConfigException {
        super.validateFilterConfig((SecurityNamedServiceConfig) filterConfig);

        validNoOtherInstance(filterConfig);

        String lProviderName = "OpenID Connect";
        if (filterConfig.isOidcEnabled()) {
            validateUserNameAttribute(filterConfig.getOidcUserNameAttribute(), lProviderName);
            validateOidcAuthorizationUri(filterConfig);
            validateOidcTokenUri(filterConfig);
            validateOidcLogoutUri(filterConfig);
            validateOidcUserInfoUri(filterConfig);
            validateOidcRedirectUri(filterConfig);
            validateClientId(filterConfig.getOidcClientId(), lProviderName);
            if (!filterConfig.isOidcUsePKCE()) {
                validateClientSecret(filterConfig.getOidcClientSecret(), lProviderName);
            }
            validateScopes(filterConfig.getOidcScopes(), lProviderName);
            validateOidcJwkSet(filterConfig);
            validateAuthenticationEntryPoint(filterConfig);
        }

        lProviderName = "Google";
        if (filterConfig.isGoogleEnabled()) {
            validateUserNameAttribute(filterConfig.getGoogleUserNameAttribute(), lProviderName);
            validateClientId(filterConfig.getGoogleClientId(), lProviderName);
            validateClientSecret(filterConfig.getGoogleClientSecret(), lProviderName);
        }
        lProviderName = "GitHub";
        if (filterConfig.isGitHubEnabled()) {
            validateUserNameAttribute(filterConfig.getGitHubUserNameAttribute(), lProviderName);
            validateClientId(filterConfig.getGitHubClientId(), lProviderName);
            validateClientSecret(filterConfig.getGitHubClientSecret(), lProviderName);
        }
        lProviderName = "Microsoft Azure";
        if (filterConfig.isMsEnabled()) {
            validateUserNameAttribute(filterConfig.getMsUserNameAttribute(), lProviderName);
            validateClientId(filterConfig.getMsClientId(), lProviderName);
            validateClientSecret(filterConfig.getMsClientSecret(), lProviderName);
            validateScopes(filterConfig.getMsScopes(), lProviderName);
        }

        validateRoleSourceMsGraph(filterConfig);
        validateRoleSourceIdToken(filterConfig);
        validateRoleSourceUserInfo(filterConfig);
    }

    private void validateRoleSourceUserInfo(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws GeoServerOAuth2FilterConfigException {
        if (OpenIdRoleSource.UserInfo.equals(filterConfig.getRoleSource())
                && !StringUtils.hasLength(filterConfig.getOidcUserInfoUri())) {
            throw createFilterException(GeoServerOAuth2FilterConfigException.ROLE_SOURCE_USER_INFO_URI_REQUIRED);
        }
    }

    private void validateRoleSourceIdToken(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws GeoServerOAuth2FilterConfigException {
        if (OpenIdRoleSource.IdToken.equals(filterConfig.getRoleSource()) && filterConfig.isGitHubEnabled()) {
            throw createFilterException(GeoServerOAuth2FilterConfigException.ROLE_SOURCE_ID_TOKEN_INVALID_FOR_GITHUB);
        }
    }

    /**
     * @param pFilterConfig
     * @throws GeoServerOAuth2FilterConfigException
     */
    private void validateRoleSourceMsGraph(GeoServerOAuth2LoginFilterConfig pFilterConfig)
            throws GeoServerOAuth2FilterConfigException {
        RoleSource lRoleSource = pFilterConfig.getRoleSource();
        if (!OpenIdRoleSource.MSGraphAPI.equals(lRoleSource)) {
            return;
        }
        int lCount = pFilterConfig.getActiveProviderCount();
        boolean lNoEnabled = lCount == 0;
        boolean lOnlyMs = pFilterConfig.isMsEnabled() && lCount == 1;

        if (!(lNoEnabled || lOnlyMs)) {
            throw createFilterException(GeoServerOAuth2FilterConfigException.MSGRAPH_COMBINATION_INVALID);
        }
    }

    /**
     * @param pFilterConfig
     * @throws GeoServerOAuth2FilterConfigException
     */
    private void validateAuthenticationEntryPoint(GeoServerOAuth2LoginFilterConfig pFilterConfig)
            throws GeoServerOAuth2FilterConfigException {
        if (pFilterConfig.getEnableRedirectAuthenticationEntryPoint()) {
            int lActiveCount = pFilterConfig.getActiveProviderCount();
            if (lActiveCount != 1) {
                throw createFilterException(GeoServerOAuth2FilterConfigException.AEP_DENIED_WRONG_PROVIDER_COUNT);
            }
        }
    }

    private void validateOidcJwkSet(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws GeoServerOAuth2FilterConfigException {
        // currently required
        // when UI option to choose algorithm is introduced, this becomes conditionally optional
        if (!StringUtils.hasLength(filterConfig.getOidcJwkSetUri())) {
            throw createFilterException(GeoServerOAuth2FilterConfigException.OAUTH2_JWK_SET_URI_REQUIRED);
        }

        try {
            new URL(filterConfig.getOidcJwkSetUri());
        } catch (MalformedURLException ex) {
            throw new GeoServerOAuth2FilterConfigException(
                    GeoServerOAuth2FilterConfigException.OAUTH2_WKTS_URL_MALFORMED);
        }
    }

    private void validateScopes(String pScopes, String pProviderName) throws GeoServerOAuth2FilterConfigException {
        if (!StringUtils.hasLength(pScopes)) {
            throw createFilterException(GeoServerOAuth2FilterConfigException.OAUTH2_SCOPE_REQUIRED, pProviderName);
        }
        String[] lScopes = ScopeUtils.valueOf(pScopes);
        boolean lMix = Arrays.stream(lScopes).anyMatch(s -> s.contains(" "));
        if (lMix) {
            throw createFilterException(
                    GeoServerOAuth2FilterConfigException.OAUTH2_SCOPE_DELIMITER_MIXED, pProviderName);
        }
    }

    private void validateClientId(String pClientId, String pProviderName) throws GeoServerOAuth2FilterConfigException {
        if (!StringUtils.hasLength(pClientId)) {
            throw createFilterException(GeoServerOAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED, pProviderName);
        }
    }

    private void validateUserNameAttribute(String pUserName, String pProviderName)
            throws GeoServerOAuth2FilterConfigException {
        if (!StringUtils.hasLength(pUserName)) {
            throw createFilterException(
                    GeoServerOAuth2FilterConfigException.OAUTH2_CLIENT_USER_NAME_REQUIRED, pProviderName);
        }
    }

    private void validateOidcRedirectUri(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws GeoServerOAuth2FilterConfigException {
        if (StringUtils.hasLength(filterConfig.getOidcRedirectUri())) {
            try {
                new URL(filterConfig.getOidcRedirectUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(GeoServerOAuth2FilterConfigException.OAUTH2_REDIRECT_URI_MALFORMED);
            }
        }
    }

    private void validateOidcLogoutUri(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws GeoServerOAuth2FilterConfigException {
        if (StringUtils.hasLength(filterConfig.getOidcLogoutUri())) {
            try {
                new URL(filterConfig.getOidcLogoutUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(GeoServerOAuth2FilterConfigException.OAUTH2_URL_IN_LOGOUT_URI_MALFORMED);
            }
        }
    }

    private void validateOidcAuthorizationUri(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws GeoServerOAuth2FilterConfigException {
        if (!StringUtils.hasLength(filterConfig.getOidcAuthorizationUri())) {
            throw createFilterException(GeoServerOAuth2FilterConfigException.OAUTH2_USERAUTHURI_MALFORMED);
        }
        if (StringUtils.hasLength(filterConfig.getOidcAuthorizationUri())) {
            URL userAuthorizationUri = null;
            try {
                userAuthorizationUri = new URL(filterConfig.getOidcAuthorizationUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(GeoServerOAuth2FilterConfigException.OAUTH2_USERAUTHURI_MALFORMED);
            }
            if (filterConfig.getOidcForceAuthorizationUriHttps()
                    && "https".equalsIgnoreCase(userAuthorizationUri.getProtocol()) == false)
                throw createFilterException(GeoServerOAuth2FilterConfigException.OAUTH2_USERAUTHURI_NOT_HTTPS);
        }
    }

    private void validateOidcTokenUri(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws GeoServerOAuth2FilterConfigException {
        if (!StringUtils.hasLength(filterConfig.getOidcTokenUri())) {
            throw createFilterException(GeoServerOAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_MALFORMED);
        }
        if (StringUtils.hasLength(filterConfig.getOidcTokenUri())) {
            URL accessTokenUri = null;
            try {
                accessTokenUri = new URL(filterConfig.getOidcTokenUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(GeoServerOAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_MALFORMED);
            }
            if (filterConfig.getOidcForceTokenUriHttps()
                    && "https".equalsIgnoreCase(accessTokenUri.getProtocol()) == false)
                throw createFilterException(GeoServerOAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_NOT_HTTPS);
        }
    }

    private void validNoOtherInstance(GeoServerOAuth2LoginFilterConfig filterConfig)
            throws GeoServerOAuth2FilterConfigException {
        Set<String> lOAuthFilterNames;
        try {
            lOAuthFilterNames = manager.listFilters(GeoServerOAuth2LoginAuthenticationFilter.class);
        } catch (IOException e) {
            throw new GeoServerRuntimException("Validation failed. Error while listing existing filters.", e);
        }
        if (lOAuthFilterNames == null) {
            lOAuthFilterNames = new HashSet<>();
        }
        lOAuthFilterNames.remove(filterConfig.getName());
        if (!lOAuthFilterNames.isEmpty()) {
            throw createFilterException(
                    "OAUTH2_MULTIPLE_INSTANCE_NOT_SUPPORTED",
                    lOAuthFilterNames.iterator().next());
        }
    }

    /** Only require checkTokenEndpointUrl if JSON Web Key set URI is empty. */
    private void validateOidcUserInfoUri(GeoServerOAuth2LoginFilterConfig filterConfig) throws FilterConfigException {
        // Note: Spring uses userInfoEndpoint OIDC case if a) specified and b) scopes require it, see
        // OidcUserService.shouldRetrieveUserInfo
        boolean lUriPresent = StringUtils.hasLength(filterConfig.getOidcUserInfoUri());
        List<String> lScopes = Arrays.asList(ScopeUtils.valueOf(filterConfig.getOidcScopes()));
        boolean lOidcScopePresent = lScopes.contains("openid");

        if (!lUriPresent) {
            if (!lOidcScopePresent) {
                throw new GeoServerOAuth2FilterConfigException(
                        GeoServerOAuth2FilterConfigException.OAUTH2_USER_INFO_URI_REQUIRED_NO_OIDC);
            }
        } else {
            try {
                new URL(filterConfig.getOidcUserInfoUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        GeoServerOAuth2FilterConfigException.OAUTH2_CHECKTOKENENDPOINT_URL_MALFORMED);
            }
        }
    }

    /**
     * Validate {@code client_secret} if required.
     *
     * <p>Default implementation requires {@code client_secret} to be provided. Subclasses can override if working with
     * a public client that cannot keep a secret.
     *
     * @param pClientSecret
     * @param pProviderName
     */
    private void validateClientSecret(String pClientSecret, String pProviderName) throws FilterConfigException {
        if (!StringUtils.hasLength(pClientSecret)) {
            throw createFilterException(
                    GeoServerOAuth2FilterConfigException.OAUTH2_CLIENT_SECRET_REQUIRED, pProviderName);
        }
    }

    @Override
    protected GeoServerOAuth2FilterConfigException createFilterException(String errorid, Object... args) {
        return new GeoServerOAuth2FilterConfigException(errorid, args);
    }
}

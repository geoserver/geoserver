/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.net.MalformedURLException;
import java.net.URL;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;
import org.springframework.util.StringUtils;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *     <p>Validates {@link OAuth2FilterConfig} objects.
 */
public class OAuth2FilterConfigValidator extends FilterConfigValidator {

    public OAuth2FilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    public void validateFilterConfig(SecurityNamedServiceConfig config)
            throws FilterConfigException {

        if (config instanceof OAuth2FilterConfig) {
            validateOAuth2FilterConfig((OAuth2FilterConfig) config);
        } else {
            super.validateFilterConfig(config);
        }
    }

    public void validateOAuth2FilterConfig(OAuth2FilterConfig filterConfig)
            throws FilterConfigException {
        if (StringUtils.hasLength(filterConfig.getLogoutUri())) {
            try {
                new URL(filterConfig.getLogoutUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_URL_IN_LOGOUT_URI_MALFORMED);
            }
        }
        super.validateFilterConfig((SecurityNamedServiceConfig) filterConfig);

        if (StringUtils.hasLength(filterConfig.getCheckTokenEndpointUrl()) == false)
            throw createFilterException(
                    OAuth2FilterConfigException.OAUTH2_CHECKTOKENENDPOINT_URL_REQUIRED);

        try {
            new URL(filterConfig.getCheckTokenEndpointUrl());
        } catch (MalformedURLException ex) {
            throw createFilterException(
                    OAuth2FilterConfigException.OAUTH2_CHECKTOKENENDPOINT_URL_MALFORMED);
        }

        if (StringUtils.hasLength(filterConfig.getAccessTokenUri())) {
            URL accessTokenUri = null;
            try {
                accessTokenUri = new URL(filterConfig.getAccessTokenUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_MALFORMED);
            }
            if (filterConfig.getForceAccessTokenUriHttps()
                    && "https".equalsIgnoreCase(accessTokenUri.getProtocol()) == false)
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_ACCESSTOKENURI_NOT_HTTPS);
        }

        if (StringUtils.hasLength(filterConfig.getUserAuthorizationUri())) {
            URL userAuthorizationUri = null;
            try {
                userAuthorizationUri = new URL(filterConfig.getUserAuthorizationUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_USERAUTHURI_MALFORMED);
            }
            if (filterConfig.getForceUserAuthorizationUriHttps()
                    && "https".equalsIgnoreCase(userAuthorizationUri.getProtocol()) == false)
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_USERAUTHURI_NOT_HTTPS);
        }

        if (StringUtils.hasLength(filterConfig.getRedirectUri())) {
            try {
                new URL(filterConfig.getRedirectUri());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_REDIRECT_URI_MALFORMED);
            }
        }

        if (!StringUtils.hasLength(filterConfig.getCliendId())) {
            throw createFilterException(OAuth2FilterConfigException.OAUTH2_CLIENT_ID_REQUIRED);
        }

        if (!StringUtils.hasLength(filterConfig.getClientSecret())) {
            throw createFilterException(OAuth2FilterConfigException.OAUTH2_CLIENT_SECRET_REQUIRED);
        }

        if (!StringUtils.hasLength(filterConfig.getScopes())) {
            throw createFilterException(OAuth2FilterConfigException.OAUTH2_SCOPE_REQUIRED);
        }
    }

    protected OAuth2FilterConfigException createFilterException(String errorid, Object... args) {
        return new OAuth2FilterConfigException(errorid, args);
    }
}

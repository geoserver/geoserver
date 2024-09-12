/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.net.MalformedURLException;
import java.net.URL;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.validation.FilterConfigException;
import org.springframework.util.StringUtils;

public class OpenIdConnectFilterConfigValidator extends OAuth2FilterConfigValidator {

    public OpenIdConnectFilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    public void validateOAuth2FilterConfig(OAuth2FilterConfig filterConfig)
            throws FilterConfigException {
        super.validateOAuth2FilterConfig(filterConfig);
        OpenIdConnectFilterConfig oidcFilterConfig = (OpenIdConnectFilterConfig) filterConfig;

        if (StringUtils.hasLength(oidcFilterConfig.getJwkURI()) != false) {
            try {
                new URL(oidcFilterConfig.getJwkURI());
            } catch (MalformedURLException ex) {
                throw new OpenIdConnectFilterConfigException(
                        OpenIdConnectFilterConfigException.OAUTH2_WKTS_URL_MALFORMED);
            }
        }
    }
    /** Only require checkTokenEndpointUrl if JSON Web Key set URI is empty. */
    protected void validateCheckTokenEndpointUrl(OAuth2FilterConfig filterConfig)
            throws FilterConfigException {
        var oidcFilterConfig = (OpenIdConnectFilterConfig) filterConfig;
        if (StringUtils.hasLength(filterConfig.getCheckTokenEndpointUrl()) == false
                && StringUtils.hasLength(oidcFilterConfig.getJwkURI()) == false) {
            // One of checkTokenEndpointUrl or jwkURI is required
            throw new OpenIdConnectFilterConfigException(
                    OpenIdConnectFilterConfigException
                            .OAUTH2_CHECKTOKEN_OR_WKTS_ENDPOINT_URL_REQUIRED);
        }
        if (StringUtils.hasLength(filterConfig.getCheckTokenEndpointUrl()) != false) {
            try {
                new URL(filterConfig.getCheckTokenEndpointUrl());
            } catch (MalformedURLException ex) {
                throw createFilterException(
                        OAuth2FilterConfigException.OAUTH2_CHECKTOKENENDPOINT_URL_MALFORMED);
            }
        }
    }

    /** Only require {@code client_secret} when not using PKCE. */
    protected void validateClientSecret(OAuth2FilterConfig filterConfig)
            throws FilterConfigException {
        var oidcFilterConfig = (OpenIdConnectFilterConfig) filterConfig;
        if (!oidcFilterConfig.isUsePKCE()) {
            super.validateClientSecret(filterConfig);
        }
    }
}

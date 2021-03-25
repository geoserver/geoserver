/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.security.oauth2.services;

import java.util.Map;
import org.geoserver.security.oauth2.GeoServerAccessTokenConverter;
import org.geoserver.security.oauth2.GeoServerOAuthRemoteTokenServices;
import org.geoserver.security.oauth2.GeoServerUserAuthenticationConverter;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** Remote Token Services for OpenId token details. */
public class OpenIdConnectTokenServices extends GeoServerOAuthRemoteTokenServices {

    public OpenIdConnectTokenServices() {
        super(new GeoServerAccessTokenConverter());
    }

    /**
     * According to the spec, the token can be verified issuing a GET request, and putting the token
     * in the Authorization header. See
     * https://openid.net/specs/openid-connect-core-1_0.html#UserInfo
     */
    @Override
    protected Map<String, Object> checkToken(String accessToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAuthorizationHeader(accessToken));
        return restTemplate
                .exchange(
                        checkTokenEndpointUrl,
                        HttpMethod.GET,
                        new HttpEntity<>(formData, headers),
                        Map.class)
                .getBody();
    }

    @Override
    protected void verifyTokenResponse(String accessToken, Map<String, Object> checkTokenResponse) {
        if (checkTokenResponse.containsKey("message")
                && checkTokenResponse.get("message").toString().startsWith("Problems")) {
            logger.debug("check_token returned error: " + checkTokenResponse.get("message"));
            throw new InvalidTokenException(accessToken);
        }
    }

    /** Overriden to remove the assertion about remote tokens */
    @Override
    public OAuth2Authentication loadAuthentication(String accessToken)
            throws AuthenticationException, InvalidTokenException {
        Map<String, Object> checkTokenResponse = checkToken(accessToken);

        verifyTokenResponse(accessToken, checkTokenResponse);

        transformNonStandardValuesToStandardValues(checkTokenResponse);

        return tokenConverter.extractAuthentication(checkTokenResponse);
    }

    public void setConfiguration(OpenIdConnectFilterConfig config) {
        setAccessTokenConverter(
                new GeoServerAccessTokenConverter(
                        new GeoServerUserAuthenticationConverter(config.getPrincipalKey())));
    }
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import org.geoserver.security.oauth2.GeoServerOAuthRemoteTokenServices;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Remote Token Services for Google token details.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GoogleTokenServices extends GeoServerOAuthRemoteTokenServices {

    public GoogleTokenServices() {
        tokenConverter = new GoogleAccessTokenConverter();
        restTemplate = new RestTemplate();
        ((RestTemplate) restTemplate)
                .setErrorHandler(
                        new DefaultResponseErrorHandler() {
                            @Override
                            // Ignore 400
                            public void handleError(ClientHttpResponse response)
                                    throws IOException {
                                if (response.getRawStatusCode() != 400) {
                                    super.handleError(response);
                                }
                            }
                        });
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken)
            throws AuthenticationException, InvalidTokenException {
        Map<String, Object> checkTokenResponse = checkToken(accessToken);

        if (checkTokenResponse.containsKey("error")) {
            logger.debug("check_token returned error: " + checkTokenResponse.get("error"));
            throw new InvalidTokenException(accessToken);
        }

        transformNonStandardValuesToStandardValues(checkTokenResponse);

        Assert.state(
                checkTokenResponse.containsKey("client_id"),
                "Client id must be present in response from auth server");
        return tokenConverter.extractAuthentication(checkTokenResponse);
    }

    private Map<String, Object> checkToken(String accessToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("token", accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAuthorizationHeader(clientId, clientSecret));
        String accessTokenUrl =
                new StringBuilder(checkTokenEndpointUrl)
                        .append("?access_token=")
                        .append(accessToken)
                        .toString();
        return postForMap(accessTokenUrl, formData, headers);
    }

    private void transformNonStandardValuesToStandardValues(Map<String, Object> map) {
        LOGGER.debug("Original map = " + map);
        map.put("client_id", map.get("issued_to")); // Google sends 'client_id' as 'issued_to'
        map.put("user_name", map.get("user_id")); // Google sends 'user_name' as 'user_id'
        LOGGER.debug("Transformed = " + map);
    }

    private String getAuthorizationHeader(String clientId, String clientSecret) {
        String creds = String.format("%s:%s", clientId, clientSecret);
        try {
            return "Basic " + new String(Base64.encode(creds.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Could not convert String");
        }
    }

    private Map<String, Object> postForMap(
            String path, MultiValueMap<String, String> formData, HttpHeaders headers) {
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }
        ParameterizedTypeReference<Map<String, Object>> map =
                new ParameterizedTypeReference<Map<String, Object>>() {};
        return restTemplate
                .exchange(path, HttpMethod.POST, new HttpEntity<>(formData, headers), map)
                .getBody();
    }
}

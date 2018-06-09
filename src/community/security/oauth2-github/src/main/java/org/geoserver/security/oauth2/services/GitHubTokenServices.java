/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.services;

import java.io.IOException;
import java.util.Map;
import org.geoserver.security.oauth2.GeoServerOAuthRemoteTokenServices;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Remote Token Services for GitHub token details.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GitHubTokenServices extends GeoServerOAuthRemoteTokenServices {

    public GitHubTokenServices() {
        tokenConverter = new GitHubAccessTokenConverter();
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

        if (checkTokenResponse.containsKey("message")
                && checkTokenResponse.get("message").toString().startsWith("Problems")) {
            logger.debug("check_token returned error: " + checkTokenResponse.get("message"));
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
        // headers.set("Authorization", getAuthorizationHeader(clientId, clientSecret));
        headers.set("Authorization", getAuthorizationHeader(accessToken));
        String accessTokenUrl =
                new StringBuilder(checkTokenEndpointUrl)
                        .append("?access_token=")
                        .append(accessToken)
                        .toString();
        return postForMap(accessTokenUrl, formData, headers);
    }

    private void transformNonStandardValuesToStandardValues(Map<String, Object> map) {
        LOGGER.debug("Original map = " + map);
        map.put("client_id", clientId); // GitHub does not send 'client_id'
        map.put("user_name", map.get("login")); // GitHub sends 'user_name' as 'login'
        LOGGER.debug("Transformed = " + map);
    }

    private String getAuthorizationHeader(String accessToken) {
        return "Bearer " + accessToken;
    }

    private Map<String, Object> postForMap(
            String path, MultiValueMap<String, String> formData, HttpHeaders headers) {
        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        }
        ParameterizedTypeReference<Map<String, Object>> map =
                new ParameterizedTypeReference<Map<String, Object>>() {};
        return restTemplate
                .exchange(path, HttpMethod.GET, new HttpEntity<>(formData, headers), map)
                .getBody();
    }
}

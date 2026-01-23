/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import java.util.Map;
import org.geotools.util.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

public class TokenIntrospector {

    private String introspectionEndpointUrl;

    private RestTemplate restTemplate;

    private String clientId;

    private String clientSecret;

    public TokenIntrospector(String introspectionEndpointUrl, String clientId, String clientSecret) {
        this.introspectionEndpointUrl = introspectionEndpointUrl;
        this.restTemplate = new RestTemplate();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Runs the introspection endpoint against an opaque token for validation and check. See
     * https://datatracker.ietf.org/doc/html/rfc7662
     */
    public Map<String, Object> introspectToken(String accessToken) {
        if (introspectionEndpointUrl == null)
            throw new RuntimeException("Cannot introspect JWE token, the introspection endpoint URL is not set");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("token", accessToken);
        formData.add("token_type_hint", "access_token");
        MultiValueMap<String, String> headersMap = new LinkedMultiValueMap<>();
        headersMap.add("Authorization", "Basic " + Base64.encodeBytes((clientId + ":" + clientSecret).getBytes()));
        return restTemplate
                .exchange(introspectionEndpointUrl, HttpMethod.POST, new HttpEntity<>(formData, headersMap), Map.class)
                .getBody();
    }
}

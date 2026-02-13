/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import java.util.Map;
import java.util.Objects;
import org.geotools.util.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/** Runs the OAuth2 Token Introspection endpoint (RFC 7662) against an access token. */
public class TokenIntrospector {

    private final String introspectionEndpointUrl;
    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final boolean clientSecretPost;

    /** Uses HTTP Basic authentication (client_secret_basic). */
    public TokenIntrospector(String introspectionEndpointUrl, String clientId, String clientSecret) {
        this(introspectionEndpointUrl, clientId, clientSecret, false);
    }

    /**
     * @param clientSecretPost when true, uses client_secret_post (client_id/client_secret in the form body). When
     *     false, uses HTTP Basic authentication.
     */
    public TokenIntrospector(
            String introspectionEndpointUrl, String clientId, String clientSecret, boolean clientSecretPost) {
        this.introspectionEndpointUrl = introspectionEndpointUrl;
        this.restTemplate = new RestTemplate();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clientSecretPost = clientSecretPost;
    }

    /**
     * Runs the introspection endpoint against a token. See RFC 7662.
     *
     * @return the introspection response as a Map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> introspectToken(String accessToken) {
        if (introspectionEndpointUrl == null) {
            throw new RuntimeException("Cannot introspect token: the introspection endpoint URL is not set");
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("token", Objects.toString(accessToken, ""));
        formData.add("token_type_hint", "access_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        if (clientSecretPost) {
            // client_secret_post
            formData.add("client_id", Objects.toString(clientId, ""));
            formData.add("client_secret", Objects.toString(clientSecret, ""));
        } else {
            // client_secret_basic
            String basic = Base64.encodeBytes((clientId + ":" + clientSecret).getBytes());
            headers.add(HttpHeaders.AUTHORIZATION, "Basic " + basic);
        }

        return restTemplate
                .exchange(introspectionEndpointUrl, HttpMethod.POST, new HttpEntity<>(formData, headers), Map.class)
                .getBody();
    }
}

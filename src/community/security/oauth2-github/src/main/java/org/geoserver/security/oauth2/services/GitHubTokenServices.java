/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.services;

import java.util.Map;
import org.geoserver.security.oauth2.GeoServerAccessTokenConverter;
import org.geoserver.security.oauth2.GeoServerOAuthRemoteTokenServices;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.util.MultiValueMap;

/**
 * Remote Token Services for GitHub token details.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GitHubTokenServices extends GeoServerOAuthRemoteTokenServices {

    public GitHubTokenServices() {
        super(new GeoServerAccessTokenConverter());
    }

    protected void transformNonStandardValuesToStandardValues(Map<String, Object> map) {
        LOGGER.debug("Original map = " + map);
        map.put("client_id", clientId); // GitHub does not send 'client_id'
        map.put("user_name", map.get("login")); // GitHub sends 'user_name' as 'login'
        LOGGER.debug("Transformed = " + map);
    }

    protected Map<String, Object> postForMap(
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

    @Override
    protected void verifyTokenResponse(String accessToken, Map<String, Object> checkTokenResponse) {
        if (checkTokenResponse.containsKey("message")
                && checkTokenResponse.get("message").toString().startsWith("Problems")) {
            logger.debug("check_token returned error: " + checkTokenResponse.get("message"));
            throw new InvalidTokenException(accessToken);
        }
    }
}

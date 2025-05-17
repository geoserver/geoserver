/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.login;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.geoserver.ows.util.ResponseUtils;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geotools.util.SuppressFBWarnings;
import org.springframework.web.client.RestTemplate;

/** Client for auto-configuration of */
public class DiscoveryClient {

    private static final String PROVIDER_END_PATH = "/.well-known/openid-configuration";
    private static final String AUTHORIZATION_ENDPOINT_ATTR_NAME = "authorization_endpoint";
    private static final String TOKEN_ENDPOINT_ATTR_NAME = "token_endpoint";
    private static final String USERINFO_ENDPOINT_ATTR_NAME = "userinfo_endpoint";
    private static final String END_SESSION_ENDPONT = "end_session_endpoint";
    private static final String JWK_SET_URI_ATTR_NAME = "jwks_uri";
    private static final String SCOPES_SUPPORTED = "scopes_supported";

    private final RestTemplate restTemplate;
    private String location;

    public DiscoveryClient(String location) {
        setLocation(location);
        this.restTemplate = new RestTemplate();
    }

    public DiscoveryClient(String location, RestTemplate restTemplate) {
        setLocation(location);
        this.restTemplate = restTemplate;
    }

    private void setLocation(String location) {
        if (!location.endsWith(PROVIDER_END_PATH)) {
            location = ResponseUtils.appendPath(location, PROVIDER_END_PATH);
        }
        this.location = location;
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public void autofill(GeoServerOAuth2LoginFilterConfig conf) {
        Map response = restTemplate.getForObject(this.location, Map.class);
        Optional.ofNullable(response.get(AUTHORIZATION_ENDPOINT_ATTR_NAME))
                .ifPresent(uri -> conf.setOidcAuthorizationUri((String) uri));
        Optional.ofNullable(response.get(TOKEN_ENDPOINT_ATTR_NAME))
                .ifPresent(uri -> conf.setOidcTokenUri((String) uri));
        Optional.ofNullable(response.get(USERINFO_ENDPOINT_ATTR_NAME))
                .ifPresent(uri -> conf.setOidcUserInfoUri((String) uri));
        Optional.ofNullable(response.get(JWK_SET_URI_ATTR_NAME)).ifPresent(uri -> conf.setOidcJwkSetUri((String) uri));
        Optional.ofNullable(response.get(END_SESSION_ENDPONT)).ifPresent(uri -> conf.setOidcLogoutUri((String) uri));
        Optional.ofNullable(response.get(SCOPES_SUPPORTED)).ifPresent(s -> {
            @SuppressWarnings("unchecked")
            List<String> scopes = (List<String>) s;
            conf.setOidcScopes(collectScopes(scopes));
        });
    }

    private String collectScopes(List<String> scopes) {
        return scopes.stream().collect(Collectors.joining(" "));
    }
}

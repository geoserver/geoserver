/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.util.Arrays;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.RequestEnhancer;
import org.springframework.util.MultiValueMap;

/** This class is used to add to Token Requests the client_secret in the query string. */
public class ClientSecretRequestEnhancer implements RequestEnhancer {

    /** {@code client_secret} - used in Token Request. */
    public static final String CLIENT_SECRET = "client_secret";

    @Override
    public void enhance(
            AccessTokenRequest request,
            OAuth2ProtectedResourceDetails resource,
            MultiValueMap<String, String> form,
            HttpHeaders headers) {
        form.put(CLIENT_SECRET, Arrays.asList(resource.getClientSecret()));
    }
}

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.services;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Map;
import org.geoserver.security.oauth2.GeoServerAccessTokenConverter;
import org.geoserver.security.oauth2.GeoServerOAuthRemoteTokenServices;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Remote Token Services for GeoNode token details.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GeoNodeTokenServices extends GeoServerOAuthRemoteTokenServices {

    public GeoNodeTokenServices() {
        super(new GeoServerAccessTokenConverter());
    }

    protected Map<String, Object> checkToken(String accessToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("token", accessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAuthorizationHeader(accessToken));
        return postForMap(checkTokenEndpointUrl, formData, headers);
    }

    protected void transformNonStandardValuesToStandardValues(Map<String, Object> map) {
        LOGGER.debug("Original map = " + map);
        map.put("user_name", map.get("issued_to")); // GeoNode sends 'client_id' as 'issued_to'
        LOGGER.debug("Transformed = " + map);
    }

    protected String getAuthorizationHeader(String accessToken) {
        String creds = String.format("%s:%s", clientId, clientSecret);
        try {
            return "Basic " + new String(Base64.getEncoder().encode(creds.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Could not convert String");
        }
    }
}

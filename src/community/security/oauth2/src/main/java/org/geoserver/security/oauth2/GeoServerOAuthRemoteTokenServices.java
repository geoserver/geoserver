/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.token.AccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.web.client.RestOperations;

/**
 * Base Class for GeoServer specific {@link RemoteTokenServices}. Each specific GeoServer OAuth2
 * Extension must implement its own.
 *
 * @author Alessio Fabiani, GeoSoltuions S.A.S.
 */
public abstract class GeoServerOAuthRemoteTokenServices extends RemoteTokenServices {

    protected static Logger LOGGER =
            LoggerFactory.getLogger(GeoServerOAuthRemoteTokenServices.class);

    protected RestOperations restTemplate;

    protected String checkTokenEndpointUrl;

    protected String clientId;

    protected String clientSecret;

    protected AccessTokenConverter tokenConverter;

    public void setRestTemplate(RestOperations restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setCheckTokenEndpointUrl(String checkTokenEndpointUrl) {
        this.checkTokenEndpointUrl = checkTokenEndpointUrl;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setAccessTokenConverter(AccessTokenConverter accessTokenConverter) {
        this.tokenConverter = accessTokenConverter;
    }
}

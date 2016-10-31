/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;

/**
 * Base OAuth2 Configuration Class. Each OAuth2 specific Extension must implement its own {@link OAuth2RestTemplate}
 * 
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public abstract class GeoServerOAuth2SecurityConfiguration implements OAuth2SecurityConfiguration {

    @Autowired
    protected Environment env;

    @Resource
    @Qualifier("accessTokenRequest")
    private AccessTokenRequest accessTokenRequest;

    /**
     * Returns the resource bean containing the Access Token Request info.
     * 
     * @return the accessTokenRequest
     */
    public AccessTokenRequest getAccessTokenRequest() {
        return accessTokenRequest;
    }

    /**
     * Set the accessTokenRequest property.
     * 
     * @param accessTokenRequest the accessTokenRequest to set
     */
    public void setAccessTokenRequest(AccessTokenRequest accessTokenRequest) {
        this.accessTokenRequest = accessTokenRequest;
    }
    
    /**
     * Details for an OAuth2-protected resource.
     */
    public abstract OAuth2ProtectedResourceDetails geoServerOAuth2Resource();
    
    /**
     * Rest template that is able to make OAuth2-authenticated REST requests with the credentials of the provided resource.
     */
    public abstract OAuth2RestTemplate geoServerOauth2RestTemplate();
}

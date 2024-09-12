/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;

/**
 * Interface for {@link GeoServerOAuth2SecurityConfiguration}
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public interface OAuth2SecurityConfiguration {

    /**
     * Details for an OAuth2-protected resource.
     *
     * @return {@link OAuth2ProtectedResourceDetails} geoServerOAuth2Resource
     */
    public OAuth2ProtectedResourceDetails geoServerOAuth2Resource();

    /**
     * Rest template that is able to make OAuth2-authenticated REST requests with the credentials of
     * the provided resource.
     *
     * @return {@link OAuth2RestTemplate} geoServerOauth2RestTemplate
     */
    public OAuth2RestTemplate geoServerOauth2RestTemplate();
}

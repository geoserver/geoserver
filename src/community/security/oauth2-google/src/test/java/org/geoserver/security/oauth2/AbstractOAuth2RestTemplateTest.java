/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.logging.Logger;

import org.geoserver.test.GeoServerMockTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.DefaultOAuth2RequestAuthenticator;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *
 */
public abstract class AbstractOAuth2RestTemplateTest extends GeoServerMockTestSupport {

    static protected Logger LOGGER = Logging.getLogger("org.geoserver.security");

    DefaultOAuth2RequestAuthenticator authenticator = new DefaultOAuth2RequestAuthenticator();

    GoogleOAuth2SecurityConfiguration configuration;

    AccessTokenRequest accessTokenRequest = mock(AccessTokenRequest.class);

    AuthorizationCodeResourceDetails resource;

    OAuth2RestTemplate restTemplate;

    ClientHttpRequest request;

    HttpHeaders headers;

    @Before
    public void open() throws Exception {
        configuration = new GoogleOAuth2SecurityConfiguration();
        configuration.setAccessTokenRequest(accessTokenRequest);
        resource = (AuthorizationCodeResourceDetails) configuration.geoServerOAuth2Resource();

        assertNotNull(resource);

        resource.setTokenName("bearer_token");
        restTemplate = configuration.geoServerOauth2RestTemplate();

        assertNotNull(restTemplate);

        request = mock(ClientHttpRequest.class);
        headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        HttpStatus statusCode = HttpStatus.OK;
        when(response.getStatusCode()).thenReturn(statusCode);
        when(request.execute()).thenReturn(response);
    }

}

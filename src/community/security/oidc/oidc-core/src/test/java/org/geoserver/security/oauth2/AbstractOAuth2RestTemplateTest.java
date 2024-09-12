/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static org.mockito.Mockito.mock;

import java.util.logging.Logger;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Before;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.security.oauth2.client.DefaultOAuth2RequestAuthenticator;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public abstract class AbstractOAuth2RestTemplateTest extends GeoServerMockTestSupport {

    protected static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    protected DefaultOAuth2RequestAuthenticator authenticator =
            new DefaultOAuth2RequestAuthenticator();

    protected GeoServerOAuth2SecurityConfiguration configuration;

    protected AccessTokenRequest accessTokenRequest = mock(AccessTokenRequest.class);

    protected AuthorizationCodeResourceDetails resource;

    protected OAuth2RestTemplate restTemplate;

    protected ClientHttpRequest request;

    protected HttpHeaders headers;

    @Before
    public abstract void open() throws Exception;
}

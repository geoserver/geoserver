/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RequestAuthenticator;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class OAuth2RestTemplateTest extends AbstractOAuth2RestTemplateTest {

    @Override
    public void open() throws Exception {
        configuration = new GitHubOAuth2SecurityConfiguration();
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

    @Test(expected = AccessTokenRequiredException.class)
    public void testAccessDeneiedException() throws Exception {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken("12345");
        token.setTokenType("access_token");
        authenticator.authenticate(resource, restTemplate.getOAuth2ClientContext(), request);
    }

    @Test
    public void testNonBearerToken() throws Exception {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken("12345");
        token.setTokenType("access_token");
        restTemplate.getOAuth2ClientContext().setAccessToken(token);
        authenticator.authenticate(resource, restTemplate.getOAuth2ClientContext(), request);
        String auth = request.getHeaders().getFirst("Authorization");

        assertTrue(auth.startsWith("access_token "));
    }

    @Test
    public void testCustomAuthenticator() throws Exception {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken("12345");
        token.setTokenType("access_token");
        restTemplate.getOAuth2ClientContext().setAccessToken(token);
        OAuth2RequestAuthenticator customAuthenticator =
                new OAuth2RequestAuthenticator() {

                    @Override
                    public void authenticate(
                            OAuth2ProtectedResourceDetails resource,
                            OAuth2ClientContext clientContext,
                            ClientHttpRequest req) {
                        req.getHeaders()
                                .set(
                                        "X-Authorization",
                                        clientContext.getAccessToken().getTokenType()
                                                + " "
                                                + "Nah-nah-na-nah-nah");
                    }
                };

        customAuthenticator.authenticate(resource, restTemplate.getOAuth2ClientContext(), request);
        String auth = request.getHeaders().getFirst("X-Authorization");

        assertEquals("access_token Nah-nah-na-nah-nah", auth);
    }
}

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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.security.oauth2.services.GeoNodeTokenServices;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RequestAuthenticator;
import org.springframework.security.oauth2.client.http.AccessTokenRequiredException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.MultiValueMap;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class OAuth2RestTemplateTest extends AbstractOAuth2RestTemplateTest {

    @Override
    public void open() throws Exception {
        configuration = new GeoNodeOAuth2SecurityConfiguration();
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

    @Test
    public void testBearerAccessTokenURLMangler() {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken("12345");
        token.setTokenType("access_token");
        token.setTokenType(OAuth2AccessToken.BEARER_TYPE);
        restTemplate.getOAuth2ClientContext().setAccessToken(token);
        authenticator.authenticate(resource, restTemplate.getOAuth2ClientContext(), request);
        String auth = request.getHeaders().getFirst("Authorization");

        assertTrue(auth.startsWith(OAuth2AccessToken.BEARER_TYPE));

        OAuth2AccessTokenURLMangler urlMangler =
                new OAuth2AccessTokenURLMangler(getSecurityManager(), configuration, restTemplate);
        urlMangler.geoServerOauth2RestTemplate = restTemplate;

        assertNotNull(urlMangler);

        Authentication user =
                new UsernamePasswordAuthenticationToken(
                        "admin",
                        "geoserver",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")
                                }));
        SecurityContextHolder.getContext().setAuthentication(user);

        StringBuilder baseURL = new StringBuilder("http://test.geoserver-org/wms");
        StringBuilder path = new StringBuilder();
        Map<String, String> kvp = new HashMap<String, String>();
        kvp.put("request", "GetCapabilities");

        urlMangler.mangleURL(baseURL, path, kvp, URLType.SERVICE);

        assertTrue(kvp.containsKey("access_token"));
        assertTrue("12345".equals(kvp.get("access_token")));
    }

    @Test
    public void testAccessTokenConverter() throws Exception {
        final String path = "http://foo.url";
        final String accessToken = "access_token Nah-nah-na-nah-nah";

        GeoNodeTokenServices accessTokenServices = new MockGeoNodeTokenServices(accessToken);

        accessTokenServices.checkTokenEndpointUrl = path;
        accessTokenServices.setClientId("1234");
        accessTokenServices.setClientSecret("56789-10");

        OAuth2Authentication auth = accessTokenServices.loadAuthentication(accessToken);
        assertEquals("1234", auth.getOAuth2Request().getClientId());
    }

    class MockGeoNodeTokenServices extends GeoNodeTokenServices {

        final String accessToken;

        public MockGeoNodeTokenServices(String accessToken) {
            this.accessToken = accessToken;
        }

        protected Map<String, Object> postForMap(
                String path, MultiValueMap<String, String> formData, HttpHeaders headers) {

            assertTrue(headers.containsKey("Authorization"));
            assertEquals(getAuthorizationHeader(accessToken), headers.get("Authorization").get(0));

            Map<String, Object> body = new HashMap<String, Object>();
            body.put("client_id", clientId);
            return body;
        }
    }
}

/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_GOOGLE;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.geoserver.security.oauth2.common.ConfidentialLogger;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;

/** Tests for {@link GeoServerOAuth2AccessTokenResponseClient} */
public class GeoServerOAuth2AccessTokenResponseClientTest {

    @SuppressWarnings("unchecked")
    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> mockDelegate =
            mock(OAuth2AccessTokenResponseClient.class);

    private ClientRegistration mockClientRegistration = mock(ClientRegistration.class);

    private GeoServerOidcIdTokenDecoderFactory jwtDecoderFactory = new GeoServerOidcIdTokenDecoderFactory();
    private GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();

    private GeoServerOAuth2AccessTokenResponseClient sut =
            new GeoServerOAuth2AccessTokenResponseClient(mockDelegate, jwtDecoderFactory);

    private Level originalLevel;
    private boolean originalEnabled;

    @Before
    public void setUp() {
        originalLevel = ConfidentialLogger.getLevel();
        originalEnabled = ConfidentialLogger.isEnabled();
        ConfidentialLogger.setLevel(Level.FINE);
        ConfidentialLogger.setEnabled(true);

        when(mockClientRegistration.getRegistrationId()).thenReturn(REG_ID_OIDC);

        jwtDecoderFactory.setGeoServerOAuth2LoginFilterConfig(config);
    }

    @After
    public void tearDown() {
        ConfidentialLogger.setLevel(originalLevel);
        ConfidentialLogger.setEnabled(originalEnabled);
    }

    @Test
    public void testSmoke() {
        // given
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("myAuthorizationUri")
                .redirectUri("myRedirectUri")
                .clientId("myClientId")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code")
                .redirectUri("myRedirectUri")
                .build();
        OAuth2AuthorizationExchange lExchange =
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);
        OAuth2AuthorizationCodeGrantRequest lRequest =
                new OAuth2AuthorizationCodeGrantRequest(mockClientRegistration, lExchange);
        OAuth2AccessTokenResponse lDelegatesResponse = OAuth2AccessTokenResponse.withToken("myToken")
                .tokenType(TokenType.BEARER)
                .build();

        // when
        when(mockDelegate.getTokenResponse(any())).thenReturn(lDelegatesResponse);
        OAuth2AccessTokenResponse lTokenResponse = sut.getTokenResponse(lRequest);

        // then
        assertNotNull(lTokenResponse);
        assertSame(lDelegatesResponse, lTokenResponse);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullDelegate() {
        new GeoServerOAuth2AccessTokenResponseClient(null, jwtDecoderFactory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullJwtDecoderFactory() {
        new GeoServerOAuth2AccessTokenResponseClient(mockDelegate, null);
    }

    @Test
    public void testGetTokenResponseDelegatesToDelegate() {
        // given
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("myAuthorizationUri")
                .redirectUri("myRedirectUri")
                .clientId("myClientId")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code")
                .redirectUri("myRedirectUri")
                .build();
        OAuth2AuthorizationExchange exchange =
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);
        OAuth2AuthorizationCodeGrantRequest request =
                new OAuth2AuthorizationCodeGrantRequest(mockClientRegistration, exchange);
        OAuth2AccessTokenResponse expectedResponse = OAuth2AccessTokenResponse.withToken("token")
                .tokenType(TokenType.BEARER)
                .build();

        when(mockDelegate.getTokenResponse(any())).thenReturn(expectedResponse);

        // when
        OAuth2AccessTokenResponse actualResponse = sut.getTokenResponse(request);

        // then
        verify(mockDelegate, times(1)).getTokenResponse(request);
        assertSame(expectedResponse, actualResponse);
    }

    @Test
    public void testGetTokenResponsePropagatesException() {
        // given
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("myAuthorizationUri")
                .redirectUri("myRedirectUri")
                .clientId("myClientId")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code")
                .redirectUri("myRedirectUri")
                .build();
        OAuth2AuthorizationExchange exchange =
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);
        OAuth2AuthorizationCodeGrantRequest request =
                new OAuth2AuthorizationCodeGrantRequest(mockClientRegistration, exchange);

        OAuth2Error error = new OAuth2Error("invalid_grant", "The authorization code has expired", null);
        when(mockDelegate.getTokenResponse(any())).thenThrow(new OAuth2AuthorizationException(error));

        // when/then
        try {
            sut.getTokenResponse(request);
            fail("Expected OAuth2AuthorizationException");
        } catch (OAuth2AuthorizationException e) {
            assertNotNull(e.getError());
        }
    }

    @Test
    public void testGetTokenResponseWithJwtAccessToken() {
        // given - a JWT-formatted access token (contains dots)
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("myAuthorizationUri")
                .redirectUri("myRedirectUri")
                .clientId("myClientId")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code")
                .redirectUri("myRedirectUri")
                .build();
        OAuth2AuthorizationExchange exchange =
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);
        OAuth2AuthorizationCodeGrantRequest request =
                new OAuth2AuthorizationCodeGrantRequest(mockClientRegistration, exchange);

        // JWT-like token (header.payload.signature)
        String jwtToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.signature";
        OAuth2AccessTokenResponse response = OAuth2AccessTokenResponse.withToken(jwtToken)
                .tokenType(TokenType.BEARER)
                .build();

        when(mockDelegate.getTokenResponse(any())).thenReturn(response);

        // when
        OAuth2AccessTokenResponse actualResponse = sut.getTokenResponse(request);

        // then
        assertNotNull(actualResponse);
        assertSame(response, actualResponse);
    }

    @Test
    public void testGetTokenResponseWithIdToken() {
        // given
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("myAuthorizationUri")
                .redirectUri("myRedirectUri")
                .clientId("myClientId")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code")
                .redirectUri("myRedirectUri")
                .build();
        OAuth2AuthorizationExchange exchange =
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);
        OAuth2AuthorizationCodeGrantRequest request =
                new OAuth2AuthorizationCodeGrantRequest(mockClientRegistration, exchange);

        // Response with id_token in additional parameters
        Map<String, Object> additionalParams = new HashMap<>();
        additionalParams.put("id_token", "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.signature");

        OAuth2AccessTokenResponse response = OAuth2AccessTokenResponse.withToken("access_token")
                .tokenType(TokenType.BEARER)
                .additionalParameters(additionalParams)
                .build();

        when(mockDelegate.getTokenResponse(any())).thenReturn(response);

        // when
        OAuth2AccessTokenResponse actualResponse = sut.getTokenResponse(request);

        // then
        assertNotNull(actualResponse);
    }

    @Test
    public void testGetTokenResponseWithCommonProvider() {
        // given - test with a common provider (Google) which has different logging
        when(mockClientRegistration.getRegistrationId()).thenReturn(REG_ID_GOOGLE);

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("myAuthorizationUri")
                .redirectUri("myRedirectUri")
                .clientId("myClientId")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code")
                .redirectUri("myRedirectUri")
                .build();
        OAuth2AuthorizationExchange exchange =
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);
        OAuth2AuthorizationCodeGrantRequest request =
                new OAuth2AuthorizationCodeGrantRequest(mockClientRegistration, exchange);

        OAuth2AccessTokenResponse response = OAuth2AccessTokenResponse.withToken("token")
                .tokenType(TokenType.BEARER)
                .build();

        when(mockDelegate.getTokenResponse(any())).thenReturn(response);

        // when
        OAuth2AccessTokenResponse actualResponse = sut.getTokenResponse(request);

        // then
        assertNotNull(actualResponse);
    }

    @Test
    public void testGetTokenResponseWithLoggingDisabled() {
        // given - disable logging
        ConfidentialLogger.setEnabled(false);
        ConfidentialLogger.setLevel(Level.OFF);

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("myAuthorizationUri")
                .redirectUri("myRedirectUri")
                .clientId("myClientId")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code")
                .redirectUri("myRedirectUri")
                .build();
        OAuth2AuthorizationExchange exchange =
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);
        OAuth2AuthorizationCodeGrantRequest request =
                new OAuth2AuthorizationCodeGrantRequest(mockClientRegistration, exchange);

        OAuth2AccessTokenResponse response = OAuth2AccessTokenResponse.withToken("token")
                .tokenType(TokenType.BEARER)
                .build();

        when(mockDelegate.getTokenResponse(any())).thenReturn(response);

        // when
        OAuth2AccessTokenResponse actualResponse = sut.getTokenResponse(request);

        // then
        assertNotNull(actualResponse);
    }

    @Test
    public void testGetTokenResponseWithOpaqueToken() {
        // given - opaque token (no dots)
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("myAuthorizationUri")
                .redirectUri("myRedirectUri")
                .clientId("myClientId")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code")
                .redirectUri("myRedirectUri")
                .build();
        OAuth2AuthorizationExchange exchange =
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);
        OAuth2AuthorizationCodeGrantRequest request =
                new OAuth2AuthorizationCodeGrantRequest(mockClientRegistration, exchange);

        OAuth2AccessTokenResponse response = OAuth2AccessTokenResponse.withToken("opaque-token-without-dots")
                .tokenType(TokenType.BEARER)
                .build();

        when(mockDelegate.getTokenResponse(any())).thenReturn(response);

        // when
        OAuth2AccessTokenResponse actualResponse = sut.getTokenResponse(request);

        // then
        assertNotNull(actualResponse);
    }

    @Test
    public void testGetTokenResponseWithScopes() {
        // given
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("myAuthorizationUri")
                .redirectUri("myRedirectUri")
                .clientId("myClientId")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("code")
                .redirectUri("myRedirectUri")
                .build();
        OAuth2AuthorizationExchange exchange =
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse);
        OAuth2AuthorizationCodeGrantRequest request =
                new OAuth2AuthorizationCodeGrantRequest(mockClientRegistration, exchange);

        OAuth2AccessTokenResponse response = OAuth2AccessTokenResponse.withToken("token")
                .tokenType(TokenType.BEARER)
                .scopes(java.util.Set.of("openid", "profile", "email"))
                .build();

        when(mockDelegate.getTokenResponse(any())).thenReturn(response);

        // when
        OAuth2AccessTokenResponse actualResponse = sut.getTokenResponse(request);

        // then
        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getAccessToken().getScopes());
    }
}

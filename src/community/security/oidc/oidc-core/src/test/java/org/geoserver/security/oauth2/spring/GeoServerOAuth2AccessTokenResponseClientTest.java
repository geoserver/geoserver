/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Before
    public void setUp() {
        originalLevel = ConfidentialLogger.getLevel();
        ConfidentialLogger.setLevel(Level.FINE);
        ConfidentialLogger.setEnabled(true);

        when(mockClientRegistration.getRegistrationId()).thenReturn(REG_ID_OIDC);

        jwtDecoderFactory.setGeoServerOAuth2LoginFilterConfig(config);
    }

    @After
    public void tearDown() {
        ConfidentialLogger.setLevel(originalLevel);
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
}

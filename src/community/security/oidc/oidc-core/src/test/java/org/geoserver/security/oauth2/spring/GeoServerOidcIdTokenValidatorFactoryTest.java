/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

/** Tests for {@link GeoServerOidcIdTokenValidatorFactory}. */
public class GeoServerOidcIdTokenValidatorFactoryTest {

    private GeoServerOAuth2LoginFilterConfig config;

    @Before
    public void setUp() {
        config = new GeoServerOAuth2LoginFilterConfig();
        config.setOidcEnabled(true);
    }

    @Test
    public void testConstructorWithValidConfig() {
        GeoServerOidcIdTokenValidatorFactory factory = new GeoServerOidcIdTokenValidatorFactory(config);
        assertNotNull(factory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullConfig() {
        new GeoServerOidcIdTokenValidatorFactory(null);
    }

    @Test
    public void testApplyReturnsValidator() {
        GeoServerOidcIdTokenValidatorFactory factory = new GeoServerOidcIdTokenValidatorFactory(config);

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("test-client")
                .clientId("my-client-id")
                .clientSecret("my-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://localhost/callback")
                .authorizationUri("https://issuer.example.com/authorize")
                .tokenUri("https://issuer.example.com/token")
                .userInfoUri("https://issuer.example.com/userinfo")
                .jwkSetUri("https://issuer.example.com/.well-known/jwks.json")
                .issuerUri("https://issuer.example.com")
                .build();

        OAuth2TokenValidator<Jwt> validator = factory.apply(clientRegistration);

        assertNotNull(validator);
        assertTrue(validator instanceof DelegatingOAuth2TokenValidator);
    }

    @Test
    public void testApplyWithDifferentClientRegistrations() {
        GeoServerOidcIdTokenValidatorFactory factory = new GeoServerOidcIdTokenValidatorFactory(config);

        // Test with different client registrations
        ClientRegistration client1 = ClientRegistration.withRegistrationId("client1")
                .clientId("client1-id")
                .clientSecret("client1-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://localhost/callback1")
                .authorizationUri("https://issuer1.example.com/authorize")
                .tokenUri("https://issuer1.example.com/token")
                .issuerUri("https://issuer1.example.com")
                .build();

        ClientRegistration client2 = ClientRegistration.withRegistrationId("client2")
                .clientId("client2-id")
                .clientSecret("client2-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://localhost/callback2")
                .authorizationUri("https://issuer2.example.com/authorize")
                .tokenUri("https://issuer2.example.com/token")
                .issuerUri("https://issuer2.example.com")
                .build();

        OAuth2TokenValidator<Jwt> validator1 = factory.apply(client1);
        OAuth2TokenValidator<Jwt> validator2 = factory.apply(client2);

        assertNotNull(validator1);
        assertNotNull(validator2);
        // Each call should create a new validator instance (they may be equal but not same)
        assertTrue(validator1 instanceof DelegatingOAuth2TokenValidator);
        assertTrue(validator2 instanceof DelegatingOAuth2TokenValidator);
    }

    @Test
    public void testApplyWithMinimalClientRegistration() {
        GeoServerOidcIdTokenValidatorFactory factory = new GeoServerOidcIdTokenValidatorFactory(config);

        // Minimal required fields for client registration
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("minimal")
                .clientId("minimal-client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://localhost/callback")
                .authorizationUri("https://issuer.example.com/authorize")
                .tokenUri("https://issuer.example.com/token")
                .build();

        OAuth2TokenValidator<Jwt> validator = factory.apply(clientRegistration);

        assertNotNull(validator);
    }
}

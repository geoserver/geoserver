/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.geoserver.security.jwtheaders.JwtConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

/** A token accepted under one configuration must not be taken as accepted under another one. */
public class TokenValidatorCacheScopeTest {

    private static final String JWKS_A = "https://idp-a.example.org/keys";
    private static final String JWKS_B = "https://idp-b.example.org/keys";

    private static RSAKey keyA;
    private static RSAKey keyB;

    @BeforeClass
    public static void createKeys() throws Exception {
        // same key id on purpose: only the key material differs
        keyA = new RSAKeyGenerator(2048).keyID("k1").generate();
        keyB = new RSAKeyGenerator(2048).keyID("k1").generate();
    }

    @Before
    public void publishKeySets() {
        TokenSignatureValidator.jwks.put(JWKS_A, new JWKSet(keyA.toPublicJWK()));
        TokenSignatureValidator.jwks.put(JWKS_B, new JWKSet(keyB.toPublicJWK()));
    }

    @After
    public void clearKeySets() {
        TokenSignatureValidator.jwks.invalidate(JWKS_A);
        TokenSignatureValidator.jwks.invalidate(JWKS_B);
    }

    private static JwtConfiguration signatureConfig(String jwksUrl) {
        JwtConfiguration config = new JwtConfiguration();
        config.setValidateToken(true);
        config.setValidateTokenSignature(true);
        config.setValidateTokenSignatureURL(jwksUrl);
        config.setValidateTokenExpiry(true);
        return config;
    }

    @Test
    public void testSignatureAcceptanceIsNotShared() throws Exception {
        String token = TokenIssuerValidatorTest.token(keyA, "https://idp-a.example.org");

        // accepted, and remembered, by the validator trusting key set A
        new TokenValidator(signatureConfig(JWKS_A)).validate(token);

        // a validator trusting key set B must still check the signature, and reject it
        TokenValidator validatorB = new TokenValidator(signatureConfig(JWKS_B));
        Exception e = assertThrows(Exception.class, () -> validatorB.validate(token));
        assertEquals("Could not verify signature of the JWT with the given RSA Public Key", e.getMessage());
    }

    @Test
    public void testEndpointAcceptanceIsNotShared() throws Exception {
        String token = TokenIssuerValidatorTest.token(keyA, "https://idp-a.example.org");

        TokenValidator validatorA = new TokenValidator(endpointConfig("https://idp-a.example.org/userinfo"));
        validatorA.tokenEndpointValidator = spy(validatorA.tokenEndpointValidator);
        doReturn("{\"sub\":\"someone\"}")
                .when(validatorA.tokenEndpointValidator)
                .download(ArgumentMatchers.any(), ArgumentMatchers.any());

        TokenValidator validatorB = new TokenValidator(endpointConfig("https://idp-b.example.org/userinfo"));
        validatorB.tokenEndpointValidator = spy(validatorB.tokenEndpointValidator);
        doReturn(null).when(validatorB.tokenEndpointValidator).download(ArgumentMatchers.any(), ArgumentMatchers.any());

        validatorA.validate(token);
        // the second call is answered from the cache
        validatorA.validate(token);
        verify(validatorA.tokenEndpointValidator, times(1)).download(ArgumentMatchers.any(), ArgumentMatchers.any());

        // validator B asks its own endpoint, which refuses the token
        Exception e = assertThrows(Exception.class, () -> validatorB.validate(token));
        assertEquals("ValidateTokenAgainstURLEndpoint - failed", e.getMessage());
    }

    private static JwtConfiguration endpointConfig(String endpoint) {
        JwtConfiguration config = new JwtConfiguration();
        config.setValidateToken(true);
        config.setValidateTokenAgainstURL(true);
        config.setValidateTokenAgainstURLEndpoint(endpoint);
        config.setValidateSubjectWithEndpoint(true);
        return config;
    }
}

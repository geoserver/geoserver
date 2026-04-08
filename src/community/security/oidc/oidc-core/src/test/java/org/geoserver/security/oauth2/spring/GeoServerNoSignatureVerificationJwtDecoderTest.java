/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

/** Tests for {@link GeoServerNoSignatureVerificationJwtDecoder}. */
public class GeoServerNoSignatureVerificationJwtDecoderTest {

    // 256-bit secret for HS256 signing (32 bytes minimum)
    private static final byte[] SECRET = "01234567890123456789012345678901".getBytes();

    @SuppressWarnings("unchecked")
    private OAuth2TokenValidator<Jwt> mockValidator = mock(OAuth2TokenValidator.class);

    private GeoServerNoSignatureVerificationJwtDecoder decoder;

    @Before
    public void setUp() {
        decoder = new GeoServerNoSignatureVerificationJwtDecoder(mockValidator);
        // Default: validation passes
        when(mockValidator.validate(any(Jwt.class))).thenReturn(OAuth2TokenValidatorResult.success());
    }

    private String createSignedJwt(JWTClaimsSet claimsSet) throws Exception {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        JWSSigner signer = new MACSigner(SECRET);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    @Test
    public void testDecodeWithValidJwt() throws Exception {
        // Create a valid JWT
        Date now = new Date();
        Date exp = new Date(now.getTime() + 3600000); // 1 hour from now

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("testuser")
                .issuer("https://issuer.example.com")
                .issueTime(now)
                .expirationTime(exp)
                .build();

        String token = createSignedJwt(claimsSet);

        Jwt jwt = decoder.decode(token);

        assertNotNull(jwt);
        assertEquals("testuser", jwt.getSubject());
        assertEquals("https://issuer.example.com", jwt.getIssuer().toString());
        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());
    }

    @Test
    public void testDecodeWithCustomClaims() throws Exception {
        Date now = new Date();
        Date exp = new Date(now.getTime() + 3600000);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("testuser")
                .issuer("https://issuer.example.com")
                .issueTime(now)
                .expirationTime(exp)
                .claim("email", "user@example.com")
                .claim("preferred_username", "testuser123")
                .build();

        String token = createSignedJwt(claimsSet);

        Jwt jwt = decoder.decode(token);

        assertNotNull(jwt);
        assertEquals("user@example.com", jwt.getClaim("email"));
        assertEquals("testuser123", jwt.getClaim("preferred_username"));
    }

    @Test
    public void testDecodeWithoutIssuedAt() throws Exception {
        Date exp = new Date(System.currentTimeMillis() + 3600000);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("testuser")
                .issuer("https://issuer.example.com")
                .expirationTime(exp)
                // No issueTime
                .build();

        String token = createSignedJwt(claimsSet);

        Jwt jwt = decoder.decode(token);

        assertNotNull(jwt);
        assertNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());
    }

    @Test
    public void testDecodeWithoutExpiresAt() throws Exception {
        Date now = new Date();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("testuser")
                .issuer("https://issuer.example.com")
                .issueTime(now)
                // No expirationTime
                .build();

        String token = createSignedJwt(claimsSet);

        Jwt jwt = decoder.decode(token);

        assertNotNull(jwt);
        assertNotNull(jwt.getIssuedAt());
        assertNull(jwt.getExpiresAt());
    }

    @Test(expected = JwtException.class)
    public void testDecodeWithInvalidToken() {
        decoder.decode("not.a.valid.jwt");
    }

    @Test(expected = JwtException.class)
    public void testDecodeWithMalformedToken() {
        decoder.decode("malformed-token-without-dots");
    }

    @Test(expected = JwtException.class)
    public void testDecodeWithEmptyToken() {
        decoder.decode("");
    }

    @Test
    public void testDecodeWithValidationErrors() throws Exception {
        // Setup validator to return errors
        OAuth2Error error = new OAuth2Error("invalid_token", "Token is expired", null);
        when(mockValidator.validate(any(Jwt.class))).thenReturn(OAuth2TokenValidatorResult.failure(error));

        Date now = new Date();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("testuser")
                .issuer("https://issuer.example.com")
                .issueTime(now)
                .build();

        String token = createSignedJwt(claimsSet);

        try {
            decoder.decode(token);
        } catch (JwtValidationException e) {
            assertNotNull(e.getErrors());
            assertEquals(1, e.getErrors().size());
            assertTrue(e.getMessage().contains("ID Token validation failed"));
            return;
        }

        throw new AssertionError("Expected JwtValidationException");
    }

    @Test
    public void testDecodeWithMultipleValidationErrors() throws Exception {
        // Setup validator to return multiple errors
        OAuth2Error error1 = new OAuth2Error("invalid_token", "Token is expired", null);
        OAuth2Error error2 = new OAuth2Error("invalid_issuer", "Invalid issuer", null);
        when(mockValidator.validate(any(Jwt.class))).thenReturn(OAuth2TokenValidatorResult.failure(error1, error2));

        Date now = new Date();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("testuser")
                .issuer("https://issuer.example.com")
                .issueTime(now)
                .build();

        String token = createSignedJwt(claimsSet);

        try {
            decoder.decode(token);
        } catch (JwtValidationException e) {
            assertNotNull(e.getErrors());
            assertEquals(2, e.getErrors().size());
            return;
        }

        throw new AssertionError("Expected JwtValidationException");
    }

    @Test
    public void testDecodePreservesHeaders() throws Exception {
        Date now = new Date();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("testuser")
                .issuer("https://issuer.example.com")
                .issueTime(now)
                .build();

        String token = createSignedJwt(claimsSet);

        Jwt jwt = decoder.decode(token);

        assertNotNull(jwt);
        assertNotNull(jwt.getHeaders());
        assertEquals("HS256", jwt.getHeaders().get("alg"));
    }

    @Test
    public void testDecodeWithAudienceClaim() throws Exception {
        Date now = new Date();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("testuser")
                .issuer("https://issuer.example.com")
                .issueTime(now)
                .audience("my-client-id")
                .build();

        String token = createSignedJwt(claimsSet);

        Jwt jwt = decoder.decode(token);

        assertNotNull(jwt);
        assertNotNull(jwt.getAudience());
        assertTrue(jwt.getAudience().contains("my-client-id"));
    }

    @Test
    public void testDecodeWithNonce() throws Exception {
        Date now = new Date();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("testuser")
                .issuer("https://issuer.example.com")
                .issueTime(now)
                .claim("nonce", "abc123")
                .build();

        String token = createSignedJwt(claimsSet);

        Jwt jwt = decoder.decode(token);

        assertNotNull(jwt);
        assertEquals("abc123", jwt.getClaim("nonce"));
    }

    @Test
    public void testDecodeTokenValuePreserved() throws Exception {
        Date now = new Date();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("testuser")
                .issuer("https://issuer.example.com")
                .issueTime(now)
                .build();

        String token = createSignedJwt(claimsSet);

        Jwt jwt = decoder.decode(token);

        assertNotNull(jwt);
        assertEquals(token, jwt.getTokenValue());
    }
}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Date;
import java.util.List;
import org.geoserver.security.jwtheaders.JwtConfiguration;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TokenIssuerValidatorTest {

    private static final String JWKS_URL = "https://idp.example.org/shared/keys";

    private static RSAKey key;

    @BeforeClass
    public static void createKey() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("shared").generate();
    }

    @After
    public void clearKeySet() {
        TokenSignatureValidator.jwks.invalidate(JWKS_URL);
    }

    static String token(RSAKey signingKey, String issuer) throws Exception {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .subject("someone")
                .expirationTime(new Date(System.currentTimeMillis() + 600_000));
        if (issuer != null) claims.issuer(issuer);
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .keyID(signingKey.getKeyID())
                        .build(),
                claims.build());
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    private static TokenIssuerValidator validator(String issuers) {
        JwtConfiguration config = new JwtConfiguration();
        config.setValidateTokenIssuer(issuers);
        return new TokenIssuerValidator(config);
    }

    @Test
    public void testNoIssuerConfiguredAcceptsAnyIssuer() throws Exception {
        validator(null).validate(JWSObject.parse(token(key, "https://anyone.example.org")));
        validator("  ").validate(JWSObject.parse(token(key, null)));
    }

    @Test
    public void testAcceptedIssuer() throws Exception {
        validator("https://a.example.org, https://b.example.org")
                .validate(JWSObject.parse(token(key, "https://b.example.org")));
    }

    @Test
    public void testForeignIssuerRejected() throws Exception {
        TokenIssuerValidator validator = validator("https://a.example.org");
        Exception e = assertThrows(
                Exception.class, () -> validator.validate(JWSObject.parse(token(key, "https://other.example.org"))));
        assertEquals("token issuer is not accepted", e.getMessage());
    }

    @Test
    public void testMissingIssuerRejectedWhenConfigured() throws Exception {
        TokenIssuerValidator validator = validator("https://a.example.org");
        assertThrows(Exception.class, () -> validator.validate(JWSObject.parse(token(key, null))));
    }

    @Test
    public void testAcceptedIssuersParsing() {
        assertEquals(List.of("a", "b", "c"), TokenIssuerValidator.acceptedIssuers(" a , b,c,"));
        assertTrue(TokenIssuerValidator.acceptedIssuers(null).isEmpty());
        assertTrue(TokenIssuerValidator.acceptedIssuers("").isEmpty());
    }

    /**
     * A key set shared by several issuers: the signature of a token from another issuer is valid, so only the issuer
     * check tells the tokens apart.
     */
    @Test
    public void testSharedKeySetTokenFromOtherIssuerRejected() throws Exception {
        TokenSignatureValidator.jwks.put(JWKS_URL, new JWKSet(key.toPublicJWK()));

        JwtConfiguration config = new JwtConfiguration();
        config.setValidateToken(true);
        config.setValidateTokenSignature(true);
        config.setValidateTokenSignatureURL(JWKS_URL);
        config.setValidateTokenIssuer("https://idp.example.org/tenant-a");
        TokenValidator validator = new TokenValidator(config);

        validator.validate(token(key, "https://idp.example.org/tenant-a"));
        String otherTenant = token(key, "https://idp.example.org/tenant-b");
        Exception e = assertThrows(Exception.class, () -> validator.validate(otherTenant));
        assertEquals("token issuer is not accepted", e.getMessage());
    }
}

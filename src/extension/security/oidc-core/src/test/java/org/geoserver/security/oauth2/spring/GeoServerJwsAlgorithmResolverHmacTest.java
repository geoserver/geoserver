/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId;
import org.junit.Test;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Regression test for HS256 (symmetric / client-secret) id_token verification.
 *
 * <p>Guards GEOS-12132: the OIDC {@link ClientRegistration} id is scoped by filter name (e.g.
 * {@code "openidconnect__oidc"}), so {@link GeoServerJwsAlgorithmResolver} must match it via
 * {@link OAuth2ClientRegistrationId#isRegIdOfType(String, String)} rather than an exact equals on {@code "oidc"}. If it
 * does not, the configured HS256 algorithm is dropped and the resolver falls back to RS256; Spring's
 * {@link OidcIdTokenDecoderFactory} then builds an asymmetric JWKS decoder and verification of the HMAC-signed token
 * fails in nimbus with "Another algorithm expected, or no matching key(s) found".
 */
public class GeoServerJwsAlgorithmResolverHmacTest {

    private static final String CLIENT_ID = "kbyuFDidLLm280LIwVFiazOqjO3ty8KH";
    /** 63 ASCII chars = 504 bits, valid for HS256 (>= 256 bits). */
    private static final String CLIENT_SECRET = "60Op4HFM0I8ajz0WdiStAbziZ-VFQttXuxixHHs2R7r7-CW8GR79l-mmLqMhc-Sa";

    /** The resolver must honour the configured HS256 algorithm for the scoped runtime registration id. */
    @Test
    public void testResolverReturnsConfiguredHs256ForScopedRegistrationId() {
        GeoServerJwsAlgorithmResolver resolver = resolverFor(JwsAlgorithms.HS256);

        // Unscoped base id and the scoped runtime id ("<filterName>__oidc") must both resolve to HS256.
        assertEquals(MacAlgorithm.HS256, resolver.apply(registration(OAuth2ClientRegistrationId.REG_ID_OIDC)));
        assertEquals(MacAlgorithm.HS256, resolver.apply(registration("openidconnect__oidc")));
    }

    /** Without a configured algorithm the resolver still defaults to RS256 (Spring's default). */
    @Test
    public void testResolverDefaultsToRs256WhenUnset() {
        GeoServerJwsAlgorithmResolver resolver = resolverFor(null);
        assertEquals(SignatureAlgorithm.RS256, resolver.apply(registration("openidconnect__oidc")));
    }

    /**
     * End-to-end decoder construction: a token signed HS256 with the raw client secret (as a real OIDC provider would)
     * must round-trip through the decoder that Spring's {@link OidcIdTokenDecoderFactory} builds when configured with
     * {@link GeoServerJwsAlgorithmResolver} and the scoped registration id. This is the exact wiring of
     * {@link GeoServerOidcIdTokenDecoderFactory} for the signature-validation case; before the fix it threw
     * {@link JwtException} ("no matching key(s) found").
     */
    @Test
    public void testHs256SignDecodeRoundTripWithScopedRegistrationId() throws Exception {
        ClientRegistration reg = registration("openidconnect__oidc");
        JwtDecoder decoder = decoderFor(reg, CLIENT_SECRET);

        Jwt jwt = decoder.decode(signHs256(CLIENT_SECRET));

        assertNotNull(jwt);
        assertEquals("100301874944276879963462152", jwt.getSubject());
        assertEquals(MacAlgorithm.HS256.getName(), jwt.getHeaders().get("alg"));
    }

    /** Signature verification must still reject a token signed with a different secret (no bypass). */
    @Test
    public void testHs256RejectsTokenSignedWithWrongSecret() throws Exception {
        ClientRegistration reg = registration("openidconnect__oidc");
        JwtDecoder decoder = decoderFor(reg, CLIENT_SECRET);

        String tamperedJwt = signHs256("a-different-but-equally-long-secret-value-0123456789abc");
        assertThrows(JwtException.class, () -> decoder.decode(tamperedJwt));
    }

    private static GeoServerJwsAlgorithmResolver resolverFor(String jwsAlgorithmName) {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcJwsAlgorithmName(jwsAlgorithmName);
        return new GeoServerJwsAlgorithmResolver(cfg);
    }

    /**
     * Mirrors {@link GeoServerOidcIdTokenDecoderFactory}'s signature-validation branch: Spring's stock
     * {@link OidcIdTokenDecoderFactory} with the GeoServer algorithm resolver. The claim/nonce validator is stubbed to
     * success so this test isolates the signature-verification construction (iss/aud/nonce are already covered
     * end-to-end by GeoServerOAuth2LoginIntegrationTest).
     */
    private static JwtDecoder decoderFor(ClientRegistration reg, String configuredSecret) {
        GeoServerOAuth2LoginFilterConfig cfg = new GeoServerOAuth2LoginFilterConfig();
        cfg.setOidcClientSecret(configuredSecret);
        cfg.setOidcJwsAlgorithmName(JwsAlgorithms.HS256);

        OidcIdTokenDecoderFactory factory = new OidcIdTokenDecoderFactory();
        factory.setJwsAlgorithmResolver(new GeoServerJwsAlgorithmResolver(cfg));
        factory.setJwtValidatorFactory(r -> token -> OAuth2TokenValidatorResult.success());
        return factory.createDecoder(reg);
    }

    private static String signHs256(String secret) throws Exception {
        long now = Instant.now().getEpochSecond();
        String idToken = "{"
                + "\"iss\":\"https://samples.auth0.com/\","
                + "\"sub\":\"100301874944276879963462152\","
                + "\"aud\":\"" + CLIENT_ID + "\","
                + "\"iat\":" + now + ","
                + "\"exp\":" + (now + 3600) + ","
                + "\"nonce\":\"abc\""
                + "}";
        JWSObject jwsObject = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(idToken));
        jwsObject.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwsObject.serialize();
    }

    private static ClientRegistration registration(String registrationId) {
        return ClientRegistration.withRegistrationId(registrationId)
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/login/oauth2/code/oidc")
                .scope("openid")
                .authorizationUri("http://localhost/authorize")
                .tokenUri("http://localhost/token")
                .jwkSetUri("http://localhost/.well-known/jwks.json")
                .build();
    }
}

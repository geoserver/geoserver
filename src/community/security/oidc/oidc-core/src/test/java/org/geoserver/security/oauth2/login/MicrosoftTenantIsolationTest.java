/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_MICROSOFT;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Covers what actually confines a single-tenant Microsoft filter on the Bearer path.
 *
 * <p>The point of the fixture is the shared signing key: every tenant path of the stub returns the <em>same</em> key
 * set, which is what Microsoft does. A token from any tenant therefore carries a valid signature, so a tenant-scoped
 * JWKS URI proves nothing about origin and only the issuer and audience checks separate one tenant from another.
 *
 * <p>The decoder under test is the one the builder assembles for resource-server mode. Only the key fetch is redirected
 * at the local stub; issuer, client ID and everything else stay as the builder produced them. The surrounding filter
 * chain is not exercised here — this covers the decoder assembly.
 */
public class MicrosoftTenantIsolationTest {

    private static final String ENTRA = "https://login.microsoftonline.com/";
    private static final String STS = "https://sts.windows.net/";
    private static final String HOME_TENANT = "87f91494-c0dc-493e-83c3-9226c111850a";
    private static final String FOREIGN_TENANT = "11111111-2222-3333-4444-555555555555";
    private static final String KEY_ID = "shared-entra-signing-key";
    private static final String CLIENT_ID = "594c52eb-e3a4-4c74-bbdf-ccc803383c99";

    /** One key for every tenant — this is what makes the JWKS URI useless as a tenant boundary. */
    private RSAKey signingKey;

    private WireMockServer entra;

    @Before
    public void setUp() throws Exception {
        signingKey = new RSAKeyGenerator(2048)
                .keyID(KEY_ID)
                .algorithm(JWSAlgorithm.RS256)
                .generate();

        entra = new WireMockServer(wireMockConfig().dynamicPort());
        entra.start();
        // Every tenant path returns the same key set, as Microsoft does.
        entra.stubFor(get(urlPathMatching("/.*/discovery/v2\\.0/keys"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(new JWKSet(signingKey.toPublicJWK()).toString())));
    }

    @After
    public void tearDown() {
        if (entra != null) {
            entra.shutdown();
        }
    }

    @Test
    public void singleTenantFilterAcceptsItsOwnTenant() {
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);
        assertNotNull("resource-server mode should have built a decoder", decoder);

        assertNotNull(decoder.decode(tokenIssuedBy(HOME_TENANT)));
    }

    @Test
    public void singleTenantFilterRejectsAnotherTenant() {
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        JwtException thrown = assertThrows(JwtException.class, () -> decoder.decode(tokenIssuedBy(FOREIGN_TENANT)));

        assertTrue(
                "a token from a different directory must be refused, signature notwithstanding: " + thrown.getMessage(),
                thrown.getMessage().contains("Microsoft Entra tenant"));
    }

    /**
     * Without a tenant the filter addresses the shared endpoint, where tokens legitimately carry many different
     * issuers. Refusing them would break every existing multi-tenant deployment, so this stays permissive on purpose.
     */
    @Test
    public void multiTenantFilterStillAcceptsAnyTenant() {
        JwtDecoder decoder = decoderForTenant(null);
        assertNotNull(decoder);

        assertNotNull(decoder.decode(tokenIssuedBy(FOREIGN_TENANT)));
    }

    /** Entra emits the v1.0 issuer when accessTokenAcceptedVersion is unset; it is still tenant-scoped. */
    @Test
    public void singleTenantFilterAcceptsTheV1EndpointIssuer() {
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        assertNotNull(decoder.decode(token(STS + HOME_TENANT + "/", CLIENT_ID)));
    }

    @Test
    public void singleTenantFilterRejectsTheV1IssuerOfAnotherTenant() {
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        assertThrows(JwtException.class, () -> decoder.decode(token(STS + FOREIGN_TENANT + "/", CLIENT_ID)));
    }

    /**
     * A directory hosts many application registrations, all of them issuing tokens with the same issuer. Confining the
     * tenant alone would let any of them in.
     */
    @Test
    public void singleTenantFilterRejectsATokenMintedForAnotherApplication() {
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        JwtException thrown = assertThrows(
                JwtException.class,
                () -> decoder.decode(token(ENTRA + HOME_TENANT + "/v2.0", "some-other-application")));

        assertTrue(thrown.getMessage().contains("this GeoServer application"));
    }

    @Test
    public void singleTenantFilterRejectsATokenWithNoAudienceAtAll() {
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        assertThrows(JwtException.class, () -> decoder.decode(token(ENTRA + HOME_TENANT + "/v2.0", null)));
    }

    /** The default application ID URI form Entra mints for an API registration. */
    @Test
    public void singleTenantFilterAcceptsTheApiApplicationIdUri() {
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        assertNotNull(decoder.decode(token(ENTRA + HOME_TENANT + "/v2.0", "api://" + CLIENT_ID)));
    }

    /**
     * With audience validation configured explicitly the administrator's claim and value govern instead of the built-in
     * client-ID rule, which is the escape hatch for a customised application ID URI.
     */
    @Test
    public void explicitAudienceValidationReplacesTheBuiltInClientRule() {
        GeoServerOAuth2LoginFilterConfig config = configFor(HOME_TENANT);
        config.setValidateTokenAudience(true);
        config.setValidateTokenAudienceClaimName("aud");
        config.setValidateTokenAudienceClaimValue("https://contoso.example/geoserver");

        JwtDecoder decoder = decoderFor(config);

        assertNotNull(decoder.decode(token(ENTRA + HOME_TENANT + "/v2.0", "https://contoso.example/geoserver")));
        // the client id is no longer what is accepted
        assertThrows(JwtException.class, () -> decoder.decode(token(ENTRA + HOME_TENANT + "/v2.0", CLIENT_ID)));
    }

    /** Resource-server mode is opt-in here, so nothing is assembled until it is switched on. */
    @Test
    public void noDecoderIsBuiltWhileResourceServerModeIsOff() {
        GeoServerOAuth2LoginFilterConfig config = configFor(HOME_TENANT);
        config.setEnableResourceServerMode(false);

        assertNull(decoderFor(config));
    }

    private JwtDecoder decoderForTenant(String pTenantId) {
        return decoderFor(configFor(pTenantId));
    }

    private JwtDecoder decoderFor(GeoServerOAuth2LoginFilterConfig pConfig) {
        GeoServerOAuth2LoginAuthenticationFilterBuilder builder = new GeoServerOAuth2LoginAuthenticationFilterBuilder();
        builder.setConfiguration(pConfig);
        // building the registrations publishes login-button enablement events
        builder.setEventPublisher(mock(ApplicationEventPublisher.class));

        // Let the builder produce the real registration, then redirect ONLY the key fetch at the stub.
        ClientRegistration real = builder.getClientRegistrationRepository().findByRegistrationId(REG_ID_MICROSOFT);
        assertNotNull("a Microsoft registration should have been built", real);

        String tenant = MicrosoftEntraTenant.normalize(pConfig.getMsTenantId());
        String keyPath = entra.baseUrl() + "/" + (tenant == null ? "common" : tenant) + "/discovery/v2.0/keys";
        ClientRegistration local = ClientRegistration.withClientRegistration(real)
                .jwkSetUri(keyPath)
                .build();
        builder.setClientRegistrationRepository(new InMemoryClientRegistrationRepository(List.of(local)));

        return builder.createResourceServerJwtDecoderIfApplicable();
    }

    private GeoServerOAuth2LoginFilterConfig configFor(String pTenantId) {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
        config.setName("azure");
        config.setMsEnabled(true);
        config.setMsClientId(CLIENT_ID);
        config.setMsClientSecret("secret");
        config.setMsTenantId(pTenantId);
        config.setEnableResourceServerMode(true);
        return config;
    }

    /** A token signed with the shared key, differing from its siblings only in issuer and audience. */
    private String tokenIssuedBy(String pTenantId) {
        return token(ENTRA + pTenantId + "/v2.0", CLIENT_ID);
    }

    private String token(String pIssuer, String pAudience) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                    .subject("someone@contoso.com")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(10, ChronoUnit.MINUTES)));
            if (pIssuer != null) {
                builder.issuer(pIssuer);
            }
            if (pAudience != null) {
                builder.audience(pAudience);
            }
            JWTClaimsSet claims = builder.build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims);
            jwt.sign(new RSASSASigner(signingKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("could not mint test token", e);
        }
    }
}

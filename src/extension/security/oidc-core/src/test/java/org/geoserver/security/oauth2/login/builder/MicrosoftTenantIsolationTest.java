/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login.builder;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_GOOGLE;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_MICROSOFT;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.scopedRegId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.MicrosoftEntraTenant;
import org.geoserver.security.oauth2.login.builder.HttpSecurityConfigurer.BuilderContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;

/**
 * Proves that configuring a Microsoft Entra tenant actually confines Bearer-token access to that tenant.
 *
 * <p>The scenario Entra presents, and the reason a tenant-scoped JWKS URI is not on its own sufficient: Microsoft
 * serves the <em>same</em> v2.0 signing keys from every tenant path. A token minted for tenant B therefore carries a
 * valid signature against tenant A's key set — only the {@code iss} claim distinguishes them. This test reproduces that
 * exactly, with one RSA key served from one key set and two tokens differing only in issuer.
 *
 * <p>{@link #multiTenantFilterStillAcceptsAnyTenant()} is the control: it pins the pre-existing behaviour for filters
 * that leave the tenant blank, and it is what fails if the tenant field is ever wired to reject cross-tenant tokens
 * unconditionally.
 */
public class MicrosoftTenantIsolationTest {

    private static final String ENTRA = "https://login.microsoftonline.com/";
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
        assertNotNull("resource-server mode should build a decoder", decoder);

        assertEquals(
                "token from the configured tenant must still authenticate",
                "someone@contoso.com",
                decoder.decode(tokenIssuedBy(HOME_TENANT)).getSubject());
    }

    @Test
    public void singleTenantFilterRejectsAnotherTenant() {
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        JwtValidationException ex =
                assertThrows(JwtValidationException.class, () -> decoder.decode(tokenIssuedBy(FOREIGN_TENANT)));

        // The signature verified — the key set is shared. Only the issuer check refuses it.
        assertTrue(
                "expected the issuer claim to be what refuses the token, but got: " + ex.getMessage(),
                ex.getMessage().contains("iss"));
    }

    @Test
    public void multiTenantFilterStillAcceptsAnyTenant() {
        // Tenant left blank: the filter targets the shared "common" endpoint, where tokens legitimately
        // carry their own tenant's issuer. Pinning here would break every multi-tenant deployment.
        JwtDecoder decoder = decoderForTenant(null);
        assertNotNull(decoder);

        assertEquals(
                "someone@contoso.com",
                decoder.decode(tokenIssuedBy(FOREIGN_TENANT)).getSubject());
    }

    @Test
    public void singleTenantFilterAcceptsTheV1EndpointIssuer() {
        // Entra emits https://sts.windows.net/{tid}/ whenever the resource application's
        // accessTokenAcceptedVersion is unset. The login flow never sees it — GeoServer pins v2.0 endpoints —
        // but a Bearer token minted elsewhere legitimately carries it, and it is still tenant-scoped.
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        assertEquals(
                "someone@contoso.com",
                decoder.decode(token("https://sts.windows.net/" + HOME_TENANT + "/", CLIENT_ID))
                        .getSubject());
    }

    @Test
    public void singleTenantFilterRejectsTheV1IssuerOfAnotherTenant() {
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        assertThrows(
                JwtValidationException.class,
                () -> decoder.decode(token("https://sts.windows.net/" + FOREIGN_TENANT + "/", CLIENT_ID)));
    }

    @Test
    public void singleTenantFilterRejectsATokenMintedForAnotherApplication() {
        // The tenant hosts many app registrations. Confining the issuer alone would let a token minted for any
        // of them authenticate against GeoServer.
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        JwtValidationException ex = assertThrows(
                JwtValidationException.class,
                () -> decoder.decode(token(ENTRA + HOME_TENANT + "/v2.0", "11112222-3333-4444-5555-666677778888")));

        assertTrue(
                "expected the audience to be what refuses the token, but got: " + ex.getMessage(),
                ex.getMessage().contains("this GeoServer application"));
    }

    @Test
    public void singleTenantFilterRejectsATokenWithNoAudienceAtAll() {
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        assertThrows(JwtValidationException.class, () -> decoder.decode(token(ENTRA + HOME_TENANT + "/v2.0", null)));
    }

    @Test
    public void singleTenantFilterAcceptsTheApplicationIdUriAsAudience() {
        // v1.0 tokens name the resource by its application ID URI rather than the bare client ID.
        JwtDecoder decoder = decoderForTenant(HOME_TENANT);

        assertEquals(
                "someone@contoso.com",
                decoder.decode(token(ENTRA + HOME_TENANT + "/v2.0", "api://" + CLIENT_ID))
                        .getSubject());
    }

    @Test
    public void explicitAudienceValidationReplacesTheImplicitOne() {
        // An administrator who has customised the application ID URI configures audience validation explicitly;
        // their claim and value then govern, and the implicit client-ID check must not veto them.
        GeoServerOAuth2LoginFilterConfig config = configFor(HOME_TENANT);
        config.setValidateTokenAudience(true);
        config.setValidateTokenAudienceClaimName("aud");
        config.setValidateTokenAudienceClaimValue("https://contoso.example/geoserver");

        JwtDecoder decoder = decoderFor(config);

        assertEquals(
                "someone@contoso.com",
                decoder.decode(token(ENTRA + HOME_TENANT + "/v2.0", "https://contoso.example/geoserver"))
                        .getSubject());
        // Still confined to the tenant.
        assertThrows(
                JwtValidationException.class,
                () -> decoder.decode(token(ENTRA + FOREIGN_TENANT + "/v2.0", "https://contoso.example/geoserver")));
    }

    @Test
    public void aNonMicrosoftProviderIsLeftAlone() {
        // The tenant rules belong to Microsoft. A Google-only filter must keep the unpinned behaviour even with a
        // stale tenant ID left in the configuration -- Google registrations already carry an issuer URI of their
        // own, so a provider-agnostic implementation would silently start enforcing one here.
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
        config.setName("azure");
        config.setGoogleEnabled(true);
        config.setGoogleClientId("google-client");
        config.setGoogleClientSecret("secret");
        config.setMsTenantId(HOME_TENANT);

        JwtDecoder decoder = decoderFor(config);
        assertNotNull(decoder);

        assertEquals(
                "a Google filter must not acquire Microsoft's issuer or audience rules",
                "someone@contoso.com",
                decoder.decode(token("https://accounts.google.com", "some-unrelated-audience"))
                        .getSubject());
    }

    @Test
    public void issuerIsRecordedOnlyForASingleTenant() {
        assertEquals(
                ENTRA + HOME_TENANT + "/v2.0",
                microsoftRegistration(HOME_TENANT).getProviderDetails().getIssuerUri());
        assertNull(
                "the shared common endpoint has no single issuer to record",
                microsoftRegistration(null).getProviderDetails().getIssuerUri());
    }

    private JwtDecoder decoderForTenant(String pTenantId) {
        return decoderFor(configFor(pTenantId));
    }

    /**
     * Assembles the decoder the way production does: real {@link ClientRegistrationFactory}, real
     * {@link HttpSecurityConfigurer#buildJwtDecoderIfApplicable()}. Only the key fetch is redirected at the local stub
     * -- issuer, client ID and everything else stay as the factory produced them. The surrounding filter wiring is not
     * exercised; this covers the decoder assembly, not the chain.
     */
    private JwtDecoder decoderFor(GeoServerOAuth2LoginFilterConfig pConfig) {
        String baseRegId = pConfig.isMsEnabled() ? REG_ID_MICROSOFT : REG_ID_GOOGLE;
        ClientRegistration reg = registrationOf(pConfig, baseRegId);

        String tenant = MicrosoftEntraTenant.normalize(pConfig.getMsTenantId());
        String keyPath = entra.baseUrl() + "/" + (tenant == null ? "common" : tenant) + "/discovery/v2.0/keys";
        ClientRegistration local = ClientRegistration.withClientRegistration(reg)
                .jwkSetUri(keyPath)
                .build();

        BuilderContext ctx = mock(BuilderContext.class);
        ClientRegistrationRepository repo = new InMemoryClientRegistrationRepository(List.of(local));
        when(ctx.clientRegistrationRepository()).thenReturn(repo);

        return new HttpSecurityConfigurer(null, pConfig, null, null, List.of(), ctx).buildJwtDecoderIfApplicable();
    }

    private ClientRegistration microsoftRegistration(String pTenantId) {
        return registrationOf(configFor(pTenantId), REG_ID_MICROSOFT);
    }

    private ClientRegistration registrationOf(GeoServerOAuth2LoginFilterConfig pConfig, String pBaseRegId) {
        ClientRegistrationRepository repo = new ClientRegistrationFactory(pConfig, null).build(event -> {}, null, this);
        ClientRegistration reg = repo.findByRegistrationId(scopedRegId(pConfig.getName(), pBaseRegId));
        assertNotNull("registration should have been built for " + pBaseRegId, reg);
        return reg;
    }

    private GeoServerOAuth2LoginFilterConfig configFor(String pTenantId) {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
        config.setName("azure");
        config.setMsEnabled(true);
        config.setMsClientId(CLIENT_ID);
        config.setMsClientSecret("secret");
        config.setMsTenantId(pTenantId);
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

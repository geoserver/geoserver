/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.intgration.keycloak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.geoserver.security.oauth2.login.MicrosoftEntraTenant;
import org.junit.Test;
import org.kordamp.json.JSONObject;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Exercises {@link MicrosoftEntraTenant}'s Bearer-token rules against tokens a real identity provider actually minted,
 * rather than against claim sets a test wrote for itself.
 *
 * <p>What this adds over {@code MicrosoftTenantIsolationTest}, which stubs the key set with WireMock: the tokens here
 * are signed by keys Keycloak generated, fetched over HTTP from a real JWKS endpoint, and carry whatever claim encoding
 * the provider chose. The {@code aud} claim in particular is emitted by different providers as a bare string or as an
 * array, and it is our own claim extraction that has to cope with both — so exercising it against a real token is the
 * point.
 *
 * <p>Entra itself is unreachable from a test, so the issuer rules are tested by their refusals: a genuine token from a
 * provider that is not the configured Entra tenant must be turned away. The matching acceptances, which need
 * Entra-shaped issuers, live in {@code MicrosoftTenantIsolationTest}. Neither suite covers the wiring that installs
 * these rules on the decoder — that is {@code MicrosoftTenantIsolationTest} as well.
 *
 * <p>Skipped in full when Docker is unavailable, like every suite in this package.
 */
public class EntraTokenRulesKeycloakIntegrationTest extends KeyCloakIntegrationTestSupport {

    private static final String SOME_TENANT = "87f91494-c0dc-493e-83c3-9226c111850a";

    private String jwkSetUri() {
        return authServerUrl + "/realms/gs-realm/protocol/openid-connect/certs";
    }

    /** A real token for a real user, signed by keys Keycloak generated. */
    private Jwt realToken() throws Exception {
        JSONObject response = getTokenFromKeycloak(normalUserName, normalUserPassword);
        assertTrue("Keycloak should have returned an access token", response.has("access_token"));

        Jwt jwt = NimbusJwtDecoder.withJwkSetUri(jwkSetUri()).build().decode(response.getString("access_token"));
        assertNotNull(jwt);
        return jwt;
    }

    @Test
    public void audienceRuleAcceptsARealTokenMintedForThatClient() throws Exception {
        Jwt token = realToken();
        String audience = token.getAudience().get(0);

        OAuth2TokenValidatorResult result =
                MicrosoftEntraTenant.issuedForClient(audience).validate(token);

        assertFalse(
                "our audience rule must accept a genuine token naming that client, but said: " + describe(result),
                result.hasErrors());
    }

    @Test
    public void audienceRuleRejectsARealTokenMintedForSomeoneElse() throws Exception {
        Jwt token = realToken();

        OAuth2TokenValidatorResult result = MicrosoftEntraTenant.issuedForClient("11112222-3333-4444-5555-666677778888")
                .validate(token);

        assertTrue("a token minted for another client must be refused", result.hasErrors());
        assertEquals("Token was not issued for this GeoServer application", describe(result));
    }

    @Test
    public void audienceRuleAlsoAcceptsTheApplicationIdUriForm() throws Exception {
        Jwt token = realToken();
        String audience = token.getAudience().get(0);

        // Entra v1.0 tokens name the resource by "api://<client id>". Whichever form the provider used, the
        // other one must not be what makes the rule pass.
        assertFalse(
                MicrosoftEntraTenant.issuedForClient(audience).validate(token).hasErrors());
        assertTrue(MicrosoftEntraTenant.issuedForClient("api://" + audience + "-not-this-one")
                .validate(token)
                .hasErrors());
    }

    @Test
    public void tenantRuleRefusesAGenuineTokenFromAnotherProvider() throws Exception {
        Jwt token = realToken();

        // Keycloak's issuer is a perfectly valid one — it is simply not the configured Entra tenant. This is the
        // refusal that stops a correctly signed, unexpired, entirely genuine token from being accepted.
        OAuth2TokenValidatorResult result =
                MicrosoftEntraTenant.issuedByTenant(SOME_TENANT).validate(token);

        assertTrue(result.hasErrors());
        assertEquals("Token was not issued by the configured Microsoft Entra tenant", describe(result));
    }

    @Test
    public void bothRulesTogetherStillDecodeARealToken() throws Exception {
        // Composed the way HttpSecurityConfigurer composes them, to show the rules do not break a genuine
        // decode when they are satisfied: the tenant rule is given the provider's own issuer so it passes.
        JSONObject response = getTokenFromKeycloak(normalUserName, normalUserPassword);
        String raw = response.getString("access_token");

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri()).build();
        Jwt probe = NimbusJwtDecoder.withJwkSetUri(jwkSetUri()).build().decode(raw);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),
                MicrosoftEntraTenant.issuedForClient(probe.getAudience().get(0))));

        assertEquals(normalUserName, decoder.decode(raw).getClaimAsString("preferred_username"));

        // And the same decoder refuses the same token once the audience no longer matches.
        NimbusJwtDecoder strict = NimbusJwtDecoder.withJwkSetUri(jwkSetUri()).build();
        strict.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), MicrosoftEntraTenant.issuedForClient("a-different-client")));

        JwtValidationException ex = assertThrows(JwtValidationException.class, () -> strict.decode(raw));
        assertTrue(ex.getMessage().contains("this GeoServer application"));
    }

    private static String describe(OAuth2TokenValidatorResult pResult) {
        return pResult.getErrors().stream()
                .map(e -> e.getDescription())
                .findFirst()
                .orElse("");
    }
}

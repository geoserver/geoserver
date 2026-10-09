/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login.builder;

import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_MICROSOFT;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.scopedRegId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.spring.GeoServerOidcIdTokenValidatorFactory;
import org.junit.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Covers the interactive login side of a configured Microsoft Entra tenant.
 *
 * <p>Recording an issuer on the {@link ClientRegistration} is not inert: Spring's {@code OidcIdTokenValidator} starts
 * enforcing the {@code iss} claim the moment that field is non-null. These tests exist because that is a behaviour
 * change on the login path made in service of the Bearer path, and nothing else in the suite would notice if it started
 * refusing legitimate sign-ins — {@code GeoServerOidcIdTokenValidatorFactoryTest} only asserts that a validator is
 * returned, never that it accepts or rejects a token.
 *
 * <p>The Bearer counterpart is {@code MicrosoftTenantIsolationTest}.
 */
public class MicrosoftIdTokenIssuerTest {

    private static final String ENTRA = "https://login.microsoftonline.com/";
    private static final String HOME_TENANT = "87f91494-c0dc-493e-83c3-9226c111850a";
    private static final String FOREIGN_TENANT = "11111111-2222-3333-4444-555555555555";
    private static final String CLIENT_ID = "594c52eb-e3a4-4c74-bbdf-ccc803383c99";

    @Test
    public void singleTenantLoginAcceptsAnIdTokenFromThatTenant() {
        OAuth2TokenValidator<Jwt> validator = idTokenValidatorFor(HOME_TENANT);

        assertFalse(
                "a legitimate sign-in must not be refused by the issuer we record",
                validator.validate(idToken(ENTRA + HOME_TENANT + "/v2.0")).hasErrors());
    }

    @Test
    public void singleTenantLoginRejectsAnIdTokenFromAnotherTenant() {
        OAuth2TokenValidator<Jwt> validator = idTokenValidatorFor(HOME_TENANT);

        OAuth2TokenValidatorResult result = validator.validate(idToken(ENTRA + FOREIGN_TENANT + "/v2.0"));

        assertTrue("an id_token from another tenant must be refused", result.hasErrors());
        assertTrue(
                "expected the issuer to be what refuses it, but got: " + describe(result),
                describe(result).contains("iss"));
    }

    @Test
    public void multiTenantLoginIsUnchanged() {
        // Tenant left blank: no issuer is recorded, so id_tokens keep being accepted from any tenant, which is
        // the whole point of a multi-tenant application.
        OAuth2TokenValidator<Jwt> validator = idTokenValidatorFor(null);

        assertFalse(
                validator.validate(idToken(ENTRA + FOREIGN_TENANT + "/v2.0")).hasErrors());
        assertFalse(validator.validate(idToken(ENTRA + HOME_TENANT + "/v2.0")).hasErrors());
    }

    @Test
    public void theRecordedIssuerIsTheV2FormTheLoginEndpointsProduce() {
        // The authorization and token endpoints this filter uses are always /oauth2/v2.0/..., so the id_token
        // issuer is always the v2.0 form and an exact match is correct here. The Bearer path is the one that
        // additionally tolerates the v1.0 issuer, because the token comes from somewhere GeoServer does not
        // control.
        assertEquals(
                ENTRA + HOME_TENANT + "/v2.0",
                microsoftRegistration(HOME_TENANT).getProviderDetails().getIssuerUri());
        assertNull(microsoftRegistration(null).getProviderDetails().getIssuerUri());
    }

    private OAuth2TokenValidator<Jwt> idTokenValidatorFor(String pTenantId) {
        GeoServerOAuth2LoginFilterConfig config = configFor(pTenantId);
        OAuth2TokenValidator<Jwt> validator =
                new GeoServerOidcIdTokenValidatorFactory(config).apply(microsoftRegistration(pTenantId));
        assertNotNull(validator);
        return validator;
    }

    private ClientRegistration microsoftRegistration(String pTenantId) {
        GeoServerOAuth2LoginFilterConfig config = configFor(pTenantId);
        ClientRegistrationRepository repo = new ClientRegistrationFactory(config, null).build(event -> {}, null, this);
        ClientRegistration reg = repo.findByRegistrationId(scopedRegId(config.getName(), REG_ID_MICROSOFT));
        assertNotNull("Microsoft registration should have been built", reg);
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

    /** The claim set Entra puts in a v2.0 id_token, reduced to what the validator looks at. */
    private Jwt idToken(String pIssuer) {
        Instant now = Instant.now();
        Instant exp = now.plus(10, ChronoUnit.MINUTES);
        return new Jwt(
                "id-token",
                now,
                exp,
                Map.of("alg", "RS256", "typ", "JWT"),
                Map.of(
                        "iss", pIssuer,
                        "sub", "oV3o_mu_PccTipAPJSpLJxzdzV2LKZv8mDQauGnY",
                        "aud", List.of(CLIENT_ID),
                        "exp", exp,
                        "iat", now));
    }

    private static String describe(OAuth2TokenValidatorResult pResult) {
        return pResult.getErrors().stream()
                .map(e -> e.getDescription())
                .findFirst()
                .orElse("");
    }
}

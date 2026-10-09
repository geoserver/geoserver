/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_MICROSOFT;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.scopedRegId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.junit.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Covers the isolation between OAuth2 login filters that share the one {@link GeoServerOidcIdTokenDecoderFactory} bean.
 *
 * <p>The bean is a singleton by necessity -- Spring Security's {@code OAuth2LoginConfigurer} resolves the id-token
 * {@code JwtDecoderFactory} from the application context by type -- so every configured filter hands its own config to
 * the same instance. Holding one delegate meant the filter configured last decided signature validation, the JWS
 * algorithm and the id-token validator chain for all of them, which let a staging filter with signature validation
 * switched off disable id-token verification for a production filter configured beside it.
 *
 * <p>{@link GeoServerOidcIdTokenDecoderFactoryScopeTest} covers the other axis of the same setting: which
 * <em>providers</em> within one configuration may skip signature verification.
 */
public class OidcIdTokenDecoderFactoryFilterIsolationTest {

    private static final String PROD = "prod-idp";
    private static final String STAGING = "staging-idp";

    private GeoServerOAuth2LoginFilterConfig config(String pFilterName, boolean pDisableSignatureValidation) {
        GeoServerOAuth2LoginFilterConfig lConfig = new GeoServerOAuth2LoginFilterConfig();
        lConfig.setName(pFilterName);
        lConfig.setDisableSignatureValidation(pDisableSignatureValidation);
        return lConfig;
    }

    private ClientRegistration registration(String pRegistrationId) {
        return ClientRegistration.withRegistrationId(pRegistrationId)
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/cb")
                .authorizationUri("https://idp.example.org/authorize")
                .tokenUri("https://idp.example.org/token")
                .jwkSetUri("https://idp.example.org/keys")
                .userNameAttributeName("sub")
                .build();
    }

    private boolean skipsSignatureCheck(GeoServerOidcIdTokenDecoderFactory pFactory, String pRegistrationId) {
        JwtDecoder lDecoder = pFactory.createDecoder(registration(pRegistrationId));
        assertNotNull(lDecoder);
        return lDecoder instanceof GeoServerNoSignatureVerificationJwtDecoder;
    }

    /** The whole point: one filter's development setting must not reach another filter's registrations. */
    @Test
    public void oneFiltersDisabledSignatureCheckDoesNotReachAnother() {
        GeoServerOidcIdTokenDecoderFactory lFactory = new GeoServerOidcIdTokenDecoderFactory();
        lFactory.setGeoServerOAuth2LoginFilterConfig(config(PROD, false));
        lFactory.setGeoServerOAuth2LoginFilterConfig(config(STAGING, true));

        assertTrue(
                "the staging filter asked for signature validation to be skipped and must still get it",
                skipsSignatureCheck(lFactory, scopedRegId(STAGING, REG_ID_OIDC)));
        assertFalse(
                "the production filter never disabled signature validation, so its id tokens must still be verified"
                        + " even though another filter did",
                skipsSignatureCheck(lFactory, scopedRegId(PROD, REG_ID_OIDC)));
    }

    /** Build order is not something an administrator controls, so it must not change the outcome. */
    @Test
    public void theOutcomeDoesNotDependOnWhichFilterWasConfiguredLast() {
        GeoServerOidcIdTokenDecoderFactory lFactory = new GeoServerOidcIdTokenDecoderFactory();
        lFactory.setGeoServerOAuth2LoginFilterConfig(config(STAGING, true));
        lFactory.setGeoServerOAuth2LoginFilterConfig(config(PROD, false));

        assertTrue(skipsSignatureCheck(lFactory, scopedRegId(STAGING, REG_ID_OIDC)));
        assertFalse(skipsSignatureCheck(lFactory, scopedRegId(PROD, REG_ID_OIDC)));
    }

    /** Per-provider scoping has to keep working per filter, not just globally. */
    @Test
    public void microsoftKeepsVerifyingEvenForTheFilterThatDisabledTheCheck() {
        GeoServerOidcIdTokenDecoderFactory lFactory = new GeoServerOidcIdTokenDecoderFactory();
        lFactory.setGeoServerOAuth2LoginFilterConfig(config(PROD, false));
        lFactory.setGeoServerOAuth2LoginFilterConfig(config(STAGING, true));

        assertFalse(
                "the setting belongs to the custom OpenID Connect provider, so Microsoft verifies regardless",
                skipsSignatureCheck(lFactory, scopedRegId(STAGING, REG_ID_MICROSOFT)));
    }

    /** Reconfiguring a filter must replace its rules rather than add a second set alongside them. */
    @Test
    public void reconfiguringAFilterReplacesItsPreviousSettings() {
        GeoServerOidcIdTokenDecoderFactory lFactory = new GeoServerOidcIdTokenDecoderFactory();
        lFactory.setGeoServerOAuth2LoginFilterConfig(config(PROD, true));
        assertTrue(skipsSignatureCheck(lFactory, scopedRegId(PROD, REG_ID_OIDC)));

        lFactory.setGeoServerOAuth2LoginFilterConfig(config(PROD, false));

        assertFalse(
                "turning the setting back off in the UI has to take effect, not leave the old decoder in place",
                skipsSignatureCheck(lFactory, scopedRegId(PROD, REG_ID_OIDC)));
    }

    /**
     * A filter may itself be named with the scope separator, so the filter name is taken from the last one. Named
     * {@code "a__b"}, its OIDC registration is {@code "a__b__oidc"} and must resolve to that filter, not to
     * {@code "a"}.
     */
    @Test
    public void aFilterNamedWithTheSeparatorStillResolvesToItself() {
        GeoServerOidcIdTokenDecoderFactory lFactory = new GeoServerOidcIdTokenDecoderFactory();
        lFactory.setGeoServerOAuth2LoginFilterConfig(config("a", true));
        lFactory.setGeoServerOAuth2LoginFilterConfig(config("a__b", false));

        assertFalse(
                "\"a__b__oidc\" belongs to the filter named \"a__b\", which did not disable signature validation",
                skipsSignatureCheck(lFactory, scopedRegId("a__b", REG_ID_OIDC)));
    }

    /**
     * A bare registration ID names no filter. With a single configuration there is nothing to confuse it with, so it
     * still resolves -- this is what every single-filter deployment relies on.
     */
    @Test
    public void aBareRegistrationIdResolvesWhileOnlyOneFilterIsConfigured() {
        GeoServerOidcIdTokenDecoderFactory lFactory = new GeoServerOidcIdTokenDecoderFactory();
        lFactory.setGeoServerOAuth2LoginFilterConfig(config(PROD, true));

        assertTrue(skipsSignatureCheck(lFactory, REG_ID_OIDC));
    }

    /** Once a second filter exists a bare ID is ambiguous, and guessing is what caused the leak. Fail closed. */
    @Test
    public void aBareRegistrationIdIsRefusedOnceItIsAmbiguous() {
        GeoServerOidcIdTokenDecoderFactory lFactory = new GeoServerOidcIdTokenDecoderFactory();
        lFactory.setGeoServerOAuth2LoginFilterConfig(config(PROD, false));
        lFactory.setGeoServerOAuth2LoginFilterConfig(config(STAGING, true));

        IllegalStateException lThrown =
                assertThrows(IllegalStateException.class, () -> lFactory.createDecoder(registration(REG_ID_OIDC)));
        assertTrue(lThrown.getMessage().contains("configuration is missing"));
    }

    /** An unconfigured factory has never been able to decode, and still must not. */
    @Test
    public void anUnconfiguredFactoryRefusesToDecode() {
        GeoServerOidcIdTokenDecoderFactory lFactory = new GeoServerOidcIdTokenDecoderFactory();

        assertThrows(
                IllegalStateException.class,
                () -> lFactory.createDecoder(registration(scopedRegId(PROD, REG_ID_OIDC))));
    }
}

/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_GOOGLE;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_MICROSOFT;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.scopedRegId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.junit.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * "Disable token signature validation" must only affect the provider whose panel offers it.
 *
 * <p>The checkbox is added to the custom OpenID Connect container alone, so an administrator configuring a Microsoft or
 * Google filter never sees it. The flag nevertheless lives on the shared filter configuration, which means a value set
 * in a data directory, over the REST API, or left behind by switching a filter's provider used to disable signature
 * verification for <em>every</em> registration the filter builds. Nothing in the user interface would have shown it,
 * and for Microsoft it silently voided tenant confinement: pinning the {@code iss} claim only means something on a
 * signature that was actually verified.
 */
public class GeoServerOidcIdTokenDecoderFactoryScopeTest {

    @Test
    public void signatureValidationStaysOnForMicrosoftWhenTheFlagIsSet() {
        JwtDecoder decoder = decoderFor(REG_ID_MICROSOFT, true);

        assertFalse(
                "a Microsoft registration must keep verifying signatures, whatever the custom OIDC provider's"
                        + " setting says",
                decoder instanceof GeoServerNoSignatureVerificationJwtDecoder);
    }

    @Test
    public void signatureValidationStaysOnForGoogleWhenTheFlagIsSet() {
        JwtDecoder decoder = decoderFor(REG_ID_GOOGLE, true);

        assertFalse(decoder instanceof GeoServerNoSignatureVerificationJwtDecoder);
    }

    @Test
    public void theCustomOidcProviderStillHonoursTheFlag() {
        // The setting has to keep working where it is offered: a hand-configured IDP in development.
        JwtDecoder decoder = decoderFor(REG_ID_OIDC, true);

        assertTrue(
                "the custom OpenID Connect provider is the one that offers this setting",
                decoder instanceof GeoServerNoSignatureVerificationJwtDecoder);
    }

    @Test
    public void everyProviderVerifiesWhenTheFlagIsClear() {
        for (String regId : new String[] {REG_ID_OIDC, REG_ID_MICROSOFT, REG_ID_GOOGLE}) {
            assertFalse(regId, decoderFor(regId, false) instanceof GeoServerNoSignatureVerificationJwtDecoder);
        }
    }

    @Test
    public void theScopedRegistrationIdIsRecognised() {
        // Registration ids are scoped by filter name, so the check has to match "<filter>__oidc" too.
        assertEquals("azure__microsoft", scopedRegId("azure", REG_ID_MICROSOFT));

        assertFalse(
                decoderFor(scopedRegId("azure", REG_ID_MICROSOFT), true)
                        instanceof GeoServerNoSignatureVerificationJwtDecoder);
        assertTrue(
                decoderFor(scopedRegId("my-idp", REG_ID_OIDC), true)
                        instanceof GeoServerNoSignatureVerificationJwtDecoder);
    }

    private JwtDecoder decoderFor(String pRegistrationId, boolean pDisableSignatureValidation) {
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
        config.setName("azure");
        config.setDisableSignatureValidation(pDisableSignatureValidation);

        GeoServerOidcIdTokenDecoderFactory factory = new GeoServerOidcIdTokenDecoderFactory();
        factory.setGeoServerOAuth2LoginFilterConfig(config);

        JwtDecoder decoder = factory.createDecoder(registration(pRegistrationId));
        assertNotNull(decoder);
        return decoder;
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
}

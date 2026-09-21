/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.isRegIdOfType;

import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;

/**
 * GeoServer factory for OIDC token decoding allows to replace the default Spring {@link OidcIdTokenDecoderFactory}.
 * Required to support reconfiguration through the GS admin UI. The {@link #delegate} has to be replaced to use empty
 * stale caches.
 *
 * @author awaterme
 */
public class GeoServerOidcIdTokenDecoderFactory implements JwtDecoderFactory<ClientRegistration> {

    private volatile JwtDecoderFactory<ClientRegistration> delegate;

    @Override
    public JwtDecoder createDecoder(ClientRegistration pContext) {
        if (delegate == null) {
            throw new IllegalStateException("Decoder creation failed. Required configuration is missing.");
        }
        return delegate.createDecoder(pContext);
    }

    public void setGeoServerOAuth2LoginFilterConfig(GeoServerOAuth2LoginFilterConfig pConfig) {
        if (pConfig == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }
        resolveDelegate(pConfig);
    }

    private void resolveDelegate(GeoServerOAuth2LoginFilterConfig pConfig) {
        GeoServerOidcIdTokenValidatorFactory jwtValidatorFactory = new GeoServerOidcIdTokenValidatorFactory(pConfig);

        OidcIdTokenDecoderFactory lFactory = new OidcIdTokenDecoderFactory();
        lFactory.setJwsAlgorithmResolver(new GeoServerJwsAlgorithmResolver(pConfig));
        lFactory.setJwtValidatorFactory(jwtValidatorFactory);

        if (!pConfig.isDisableSignatureValidation()) {
            this.delegate = lFactory;
            return;
        }

        final GeoServerOidcIdTokenValidatorFactory validatorFactory = jwtValidatorFactory;
        final OidcIdTokenDecoderFactory verifyingFactory = lFactory;
        this.delegate = (ClientRegistration clientRegistration) -> {
            if (!isRegIdOfType(clientRegistration.getRegistrationId(), REG_ID_OIDC)) {
                // "Disable token signature validation" is a setting of the custom OpenID Connect provider --
                // it is the only provider whose panel offers the checkbox, because it is the only one whose
                // endpoints an administrator supplies by hand and may need to point at a development IDP.
                // Google, GitHub and Microsoft have fixed, publicly documented key sets, so there is no reason
                // to skip verification for them and every reason not to: for Microsoft it would also void the
                // tenant confinement, whose issuer check is only meaningful on a signature that was verified.
                return verifyingFactory.createDecoder(clientRegistration);
            }
            // build validator chain for this client
            final OAuth2TokenValidator<Jwt> validator = validatorFactory.apply(clientRegistration);

            return new GeoServerNoSignatureVerificationJwtDecoder(validator);
        };
    }

    /** @return the delegate */
    public JwtDecoderFactory<ClientRegistration> getDelegate() {
        return delegate;
    }
}

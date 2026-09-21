/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.filterNameOf;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.isRegIdOfType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;

/**
 * GeoServer factory for OIDC token decoding allows to replace the default Spring {@link OidcIdTokenDecoderFactory}.
 * Required to support reconfiguration through the GS admin UI. The delegates have to be replaced to use empty stale
 * caches.
 *
 * <p>This bean is a singleton, and has to be: Spring Security's {@code OAuth2LoginConfigurer} resolves the
 * {@link JwtDecoderFactory} for the OIDC provider from the application context by type rather than from the filter
 * builder, so a prototype-scoped bean would hand Spring a fresh instance that no filter had configured. Every
 * {@link org.geoserver.security.oauth2.login.builder.HttpSecurityConfigurer#configure()} call therefore reaches this
 * same object, once per configured filter.
 *
 * <p>That is why the delegate is held per filter rather than in a single field. With one shared delegate, the filter
 * configured last dictated {@code disableSignatureValidation}, the JWS algorithm resolved by
 * {@link GeoServerJwsAlgorithmResolver} and the whole id-token validator chain built by
 * {@link GeoServerOidcIdTokenValidatorFactory} for every OIDC login filter in the JVM -- so a staging filter with
 * signature validation switched off took id-token verification down for a production filter beside it. Registration IDs
 * are scoped as {@code "<filter>__<provider>"}, which is what lets a decode request be routed back to the configuration
 * that owns it.
 *
 * @author awaterme
 */
public class GeoServerOidcIdTokenDecoderFactory implements JwtDecoderFactory<ClientRegistration> {

    /** Keyed by filter name; a filter with no name contributes the {@link #UNNAMED_FILTER} key. */
    private final Map<String, JwtDecoderFactory<ClientRegistration>> delegatesByFilter = new ConcurrentHashMap<>();

    private static final String UNNAMED_FILTER = "";

    @Override
    public JwtDecoder createDecoder(ClientRegistration pContext) {
        JwtDecoderFactory<ClientRegistration> lDelegate = resolveDelegateFor(pContext);
        if (lDelegate == null) {
            throw new IllegalStateException("Decoder creation failed. Required configuration is missing.");
        }
        return lDelegate.createDecoder(pContext);
    }

    public void setGeoServerOAuth2LoginFilterConfig(GeoServerOAuth2LoginFilterConfig pConfig) {
        if (pConfig == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }
        String lFilterName = pConfig.getName();
        delegatesByFilter.put(lFilterName == null ? UNNAMED_FILTER : lFilterName, buildDelegate(pConfig));
    }

    /**
     * Routes a decode request to the configuration of the filter that owns the registration.
     *
     * @return the delegate to decode with, or {@code null} when the registration cannot be attributed to a
     *     configuration, in which case the caller fails closed
     */
    private JwtDecoderFactory<ClientRegistration> resolveDelegateFor(ClientRegistration pContext) {
        String lFilterName = pContext == null ? null : filterNameOf(pContext.getRegistrationId());
        if (lFilterName != null) {
            JwtDecoderFactory<ClientRegistration> lDelegate = delegatesByFilter.get(lFilterName);
            if (lDelegate != null) {
                return lDelegate;
            }
        }
        // A bare registration ID carries no filter name, so it can only be attributed while there is nothing to
        // confuse it with. Guessing once a second filter exists is precisely the cross-filter leak this routing
        // exists to prevent, so leave it unresolved and let the caller refuse.
        if (delegatesByFilter.size() == 1) {
            return delegatesByFilter.values().iterator().next();
        }
        return null;
    }

    private JwtDecoderFactory<ClientRegistration> buildDelegate(GeoServerOAuth2LoginFilterConfig pConfig) {
        GeoServerOidcIdTokenValidatorFactory jwtValidatorFactory = new GeoServerOidcIdTokenValidatorFactory(pConfig);

        OidcIdTokenDecoderFactory lFactory = new OidcIdTokenDecoderFactory();
        lFactory.setJwsAlgorithmResolver(new GeoServerJwsAlgorithmResolver(pConfig));
        lFactory.setJwtValidatorFactory(jwtValidatorFactory);

        if (!pConfig.isDisableSignatureValidation()) {
            return lFactory;
        }

        final GeoServerOidcIdTokenValidatorFactory validatorFactory = jwtValidatorFactory;
        final OidcIdTokenDecoderFactory verifyingFactory = lFactory;
        return (ClientRegistration clientRegistration) -> {
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
}

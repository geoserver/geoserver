/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.REG_ID_OIDC;
import static org.geoserver.security.oauth2.login.OAuth2ClientRegistrationId.isRegIdOfType;

import java.util.function.Function;
import org.geoserver.security.oauth2.config.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.token.JwsAlgorithmNameParser;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

/**
 * Determines the JWT token algorithm based on the {@link GeoServerOAuth2LoginFilterConfig}.
 *
 * @author awaterme
 */
public class GeoServerJwsAlgorithmResolver implements Function<ClientRegistration, JwsAlgorithm> {

    private GeoServerOAuth2LoginFilterConfig configuration;

    /** @param pConfiguration */
    public GeoServerJwsAlgorithmResolver(GeoServerOAuth2LoginFilterConfig pConfiguration) {
        super();
        configuration = pConfiguration;
    }

    @Override
    public JwsAlgorithm apply(ClientRegistration pClientReg) {
        JwsAlgorithm lAlg = null;
        // Registration IDs are scoped by filter name (e.g. "myFilter__oidc"), so match by base ID
        // via isRegIdOfType(..) rather than an exact equals on REG_ID_OIDC. Otherwise the configured
        // algorithm is ignored and we fall back to RS256 below; for an HS256 (symmetric) provider this
        // pushes Spring's OidcIdTokenDecoderFactory into the asymmetric JWKS branch, and verification of
        // the HMAC-signed id_token fails in nimbus with "Another algorithm expected, or no matching
        // key(s) found".
        if (isRegIdOfType(pClientReg.getRegistrationId(), REG_ID_OIDC)) {
            String lName = configuration.getOidcJwsAlgorithmName();
            lAlg = new JwsAlgorithmNameParser().parse(lName);
        }
        if (lAlg == null) {
            // also spring default
            lAlg = SignatureAlgorithm.RS256;
        }
        return lAlg;
    }
}

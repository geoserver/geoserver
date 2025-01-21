/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_OIDC;

import java.util.function.Function;
import org.geoserver.security.oauth2.common.JwsAlgorithmNameParser;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
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
        if (REG_ID_OIDC.equals(pClientReg.getRegistrationId())) {
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

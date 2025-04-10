/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_OIDC;

import java.util.function.Function;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.util.Assert;

/**
 * Factory creates a {@link GeoServerOidcConfigurableTokenValidator} for the OIDC provider.
 *
 * @author awaterme
 */
public class GeoServerOidcIdTokenValidatorFactory implements Function<ClientRegistration, OAuth2TokenValidator<Jwt>> {

    private GeoServerOAuth2LoginFilterConfig config;

    /** @param pConfig */
    public GeoServerOidcIdTokenValidatorFactory(GeoServerOAuth2LoginFilterConfig pConfig) {
        super();
        config = pConfig;
        Assert.notNull(config, "configuration must not be null");
    }

    @Override
    public OAuth2TokenValidator<Jwt> apply(ClientRegistration pClientReg) {
        // src:
        // org.springframework.security.oauth2.client.oidc.authentication.DefaultOidcIdTokenValidatorFactory
        OAuth2TokenValidator<Jwt> lDefaultValidator =
                new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), new OidcIdTokenValidator(pClientReg));

        String pRegId = pClientReg.getRegistrationId();
        if (!REG_ID_OIDC.equals(pRegId)) {
            return lDefaultValidator;
        }

        return new GeoServerOidcConfigurableTokenValidator(config, lDefaultValidator);
    }
}

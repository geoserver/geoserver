/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import static java.util.stream.Collectors.joining;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geotools.util.logging.Logging;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.Assert;

/**
 * {@link OAuth2TokenValidator} implementation uses a {@link #delegate} for token validation. If OIDC token validation
 * is configured to be non-enforcing, the validation result will always be "success." In such cases, if the delegate
 * validation fails, only a warning message will be logged.
 *
 * @author awaterme
 */
public class GeoServerOidcConfigurableTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static Logger LOGGER = Logging.getLogger(GeoServerOidcConfigurableTokenValidator.class);

    private GeoServerOAuth2LoginFilterConfig config;
    private OAuth2TokenValidator<Jwt> delegate;

    /**
     * @param pConfig
     * @param pDelegate
     */
    public GeoServerOidcConfigurableTokenValidator(
            GeoServerOAuth2LoginFilterConfig pConfig, OAuth2TokenValidator<Jwt> pDelegate) {
        super();
        config = pConfig;
        delegate = pDelegate;
        Assert.notNull(config, "configuration must not be null");
        Assert.notNull(delegate, "delegate must not be null");
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt pToken) {
        OAuth2TokenValidatorResult lResult = delegate.validate(pToken);
        if (config.isOidcEnforceTokenValidation()) {
            return lResult;
        }

        if (lResult.hasErrors()) {
            int lCount = lResult.getErrors().size();
            String lTxt = "OIDC token validation failed with %d errors.".formatted(lCount);
            StringBuilder lBuilder = new StringBuilder(lTxt);
            if (LOGGER.isLoggable(Level.FINE)) {
                lBuilder.append(" ")
                        .append(lResult.getErrors().stream()
                                .map(e -> e.toString())
                                .collect(joining()));
            }
            LOGGER.log(Level.WARNING, lBuilder.toString());
        }

        return OAuth2TokenValidatorResult.success();
    }

    /** @return the config */
    public GeoServerOAuth2LoginFilterConfig getConfiguration() {
        return config;
    }
}

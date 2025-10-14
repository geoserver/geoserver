/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import com.nimbusds.jose.JWSObject;
import org.geoserver.security.jwtheaders.JwtConfiguration;

/**
 * validates a token - according to the GeoServerJwtHeadersFilterConfig. This will use the various Token...Validator
 * classes to do the actual validation.
 */
public class TokenValidator {

    public JwtConfiguration jwtHeadersConfig;
    public TokenEndpointValidator tokenEndpointValidator;
    public TokenAudienceValidator tokenAudienceValidator;
    public TokenExpiryValidator tokenExpiryValidator;
    public TokenSignatureValidator tokenSignatureValidator;

    public TokenValidator(JwtConfiguration config) {
        jwtHeadersConfig = config;

        tokenAudienceValidator = new TokenAudienceValidator(jwtHeadersConfig);
        tokenEndpointValidator = new TokenEndpointValidator(jwtHeadersConfig);
        tokenExpiryValidator = new TokenExpiryValidator(jwtHeadersConfig);
        tokenSignatureValidator = new TokenSignatureValidator(jwtHeadersConfig);
    }

    public void validate(String accessToken) throws Exception {

        accessToken = accessToken.replaceFirst("^Bearer", "");
        accessToken = accessToken.replaceFirst("^bearer", "");
        accessToken = accessToken.trim();

        if (!jwtHeadersConfig.isValidateToken()) {
            return;
        }

        validateSignature(accessToken);

        JWSObject jwsToken = JWSObject.parse(accessToken);

        validateExpiry(jwsToken);
        validateEndpoint(accessToken);
        validateAudience(jwsToken);
    }

    private void validateAudience(JWSObject accessToken) throws Exception {

        tokenAudienceValidator.validate(accessToken);
    }

    private void validateEndpoint(String token) throws Exception {
        tokenEndpointValidator.validate(token);
    }

    private void validateExpiry(JWSObject jwsToken) throws Exception {
        tokenExpiryValidator.validate(jwsToken);
    }

    private void validateSignature(String accessToken) throws Exception {
        tokenSignatureValidator.validate(accessToken);
    }
}

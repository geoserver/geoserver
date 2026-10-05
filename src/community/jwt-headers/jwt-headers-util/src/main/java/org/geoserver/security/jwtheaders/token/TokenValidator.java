/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import com.nimbusds.jose.JWSObject;
import java.util.Map;
import org.geoserver.security.jwtheaders.JwtConfiguration;

/**
 * validates a token - according to the GeoServerJwtHeadersFilterConfig. This will use the various Token...Validator
 * classes to do the actual validation.
 *
 * <p>The signature and endpoint results are cached per key set URL and per endpoint, so a token accepted under one
 * configuration is not taken as accepted by a configuration that trusts another key set or endpoint.
 */
public class TokenValidator {

    public JwtConfiguration jwtHeadersConfig;
    public TokenEndpointValidator tokenEndpointValidator;
    public TokenAudienceValidator tokenAudienceValidator;
    public TokenExpiryValidator tokenExpiryValidator;
    public TokenSignatureValidator tokenSignatureValidator;
    public TokenIssuerValidator tokenIssuerValidator;

    public TokenValidator(JwtConfiguration config) {
        jwtHeadersConfig = config;

        tokenAudienceValidator = new TokenAudienceValidator(jwtHeadersConfig);
        tokenEndpointValidator = new TokenEndpointValidator(jwtHeadersConfig);
        tokenExpiryValidator = new TokenExpiryValidator(jwtHeadersConfig);
        tokenSignatureValidator = new TokenSignatureValidator(jwtHeadersConfig);
        tokenIssuerValidator = new TokenIssuerValidator(jwtHeadersConfig);
    }

    public void validate(String accessToken) throws Exception {
        validateAndParse(accessToken);
    }

    /**
     * Runs every enabled check on the token and returns its claims.
     *
     * @param accessToken the header value, optionally prefixed with "Bearer"
     * @return the claims of the validated token, or null when token validation is disabled (no claim can be trusted
     *     then)
     * @throws Exception if any enabled check fails
     */
    public Map<String, Object> validateAndParse(String accessToken) throws Exception {

        accessToken = accessToken.replaceFirst("^Bearer", "");
        accessToken = accessToken.replaceFirst("^bearer", "");
        accessToken = accessToken.trim();

        if (!jwtHeadersConfig.isValidateToken()) {
            return null;
        }

        validateSignature(accessToken);

        JWSObject jwsToken = JWSObject.parse(accessToken);

        // local checks first, so a token we can reject ourselves never reaches the endpoint
        validateExpiry(jwsToken);
        tokenIssuerValidator.validate(jwsToken);
        validateAudience(jwsToken);
        validateEndpoint(accessToken);

        return jwsToken.getPayload().toJSONObject();
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

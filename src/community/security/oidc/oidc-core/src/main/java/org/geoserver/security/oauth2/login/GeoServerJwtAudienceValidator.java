/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static org.geoserver.security.jwtheaders.roles.JwtHeadersRolesExtractor.asStringList;

import java.util.List;
import java.util.Map;
import org.geoserver.security.jwtheaders.username.JwtHeaderUserNameExtractor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Validates that a JWT contains the expected audience (or an equivalent custom claim).
 *
 * <p>This is intended for resource-server (Bearer JWT) mode.
 */
public final class GeoServerJwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final String ERROR_CODE = "invalid_token";

    private final String claimName;
    private final String expectedValue;

    public GeoServerJwtAudienceValidator(String claimName, String expectedValue) {
        this.claimName = claimName;
        this.expectedValue = expectedValue;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token == null) {
            return failure("JWT token is missing");
        }
        if (!StringUtils.hasText(claimName) || !StringUtils.hasText(expectedValue)) {
            // Misconfiguration should be caught by config validation, but fail closed here.
            return failure("Audience validation misconfigured");
        }

        Map<String, Object> claims = token.getClaims();
        Object raw = JwtHeaderUserNameExtractor.getClaim(claims, claimName);
        List<String> audiences = asStringList(raw);

        boolean match = audiences != null && audiences.stream().anyMatch(expectedValue::equals);
        if (match) {
            return OAuth2TokenValidatorResult.success();
        }

        // Some providers emit aud as a single string; asStringList already handles this,
        // but keep a defensive branch for unexpected claim types.
        if (raw instanceof String s && expectedValue.equals(s)) {
            return OAuth2TokenValidatorResult.success();
        }

        return failure("Required audience claim did not match");
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(ERROR_CODE, description, null));
    }
}

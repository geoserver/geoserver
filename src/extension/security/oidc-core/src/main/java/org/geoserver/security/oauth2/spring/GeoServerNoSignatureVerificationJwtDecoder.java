/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.spring;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.text.ParseException;
import java.time.Instant;
import java.util.Map;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.converter.ClaimTypeConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

/**
 * Custom decoder that does not verify the JWT signature. Attempts to mimic the OidcIdTokenDecoderFactory, but without
 * signature validation.
 */
public class GeoServerNoSignatureVerificationJwtDecoder implements JwtDecoder {

    private OAuth2TokenValidator<Jwt> validator;

    private static final ClaimTypeConverter DEFAULT_CLAIM_TYPE_CONVERTER =
            new ClaimTypeConverter(OidcIdTokenDecoderFactory.createDefaultClaimTypeConverters());

    public GeoServerNoSignatureVerificationJwtDecoder(OAuth2TokenValidator<Jwt> validator) {
        this.validator = validator;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            JWT parsed = JWTParser.parse(token);

            Map<String, Object> headers = parsed.getHeader().toJSONObject();
            Map<String, Object> claims = parsed.getJWTClaimsSet().toJSONObject();

            Instant issuedAt = parsed.getJWTClaimsSet().getIssueTime() != null
                    ? parsed.getJWTClaimsSet().getIssueTime().toInstant()
                    : null;
            Instant expiresAt = parsed.getJWTClaimsSet().getExpirationTime() != null
                    ? parsed.getJWTClaimsSet().getExpirationTime().toInstant()
                    : null;

            Map<String, Object> convertedClaims = DEFAULT_CLAIM_TYPE_CONVERTER.convert(claims);

            Jwt jwt = Jwt.withTokenValue(token)
                    .headers(h -> h.putAll(headers))
                    .claims(c -> c.putAll(convertedClaims))
                    .issuedAt(issuedAt)
                    .expiresAt(expiresAt)
                    .build();

            OAuth2TokenValidatorResult validate = validator.validate(jwt);
            if (validate.hasErrors()) {
                throw new JwtValidationException("ID Token validation failed", validate.getErrors());
            }
            return jwt;
        } catch (ParseException e) {
            throw new JwtException("Failed to parse JWT without signature verification", e);
        }
    }
}

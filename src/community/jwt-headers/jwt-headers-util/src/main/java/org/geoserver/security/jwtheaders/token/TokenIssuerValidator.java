/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import com.nimbusds.jose.JWSObject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.security.jwtheaders.JwtConfiguration;

/**
 * Validates the issuer ("iss") of an access token against the configured list of accepted issuers.
 *
 * <p>A signing key set can be shared by several issuers (multi-tenant identity providers publish the same keys for
 * every tenant), so a valid signature alone does not tell which issuer minted the token. When no issuer is configured
 * this check is skipped, to keep existing configurations working.
 */
public class TokenIssuerValidator {

    JwtConfiguration jwtHeadersConfig;

    public TokenIssuerValidator(JwtConfiguration config) {
        jwtHeadersConfig = config;
    }

    public void validate(JWSObject jwsToken) throws Exception {
        List<String> accepted = acceptedIssuers(jwtHeadersConfig.getValidateTokenIssuer());
        if (accepted.isEmpty()) return; // nothing configured

        Object issuer = jwsToken.getPayload().toJSONObject().get("iss");
        if (!(issuer instanceof String) || !accepted.contains(issuer)) {
            throw new Exception("token issuer is not accepted");
        }
    }

    /**
     * Parses the configured issuer list.
     *
     * @param issuers accepted issuers, separated by commas (may be null)
     * @return the accepted issuers, empty when none is configured
     */
    public static List<String> acceptedIssuers(String issuers) {
        if (issuers == null || issuers.isBlank()) return List.of();
        return Arrays.stream(issuers.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableList());
    }
}

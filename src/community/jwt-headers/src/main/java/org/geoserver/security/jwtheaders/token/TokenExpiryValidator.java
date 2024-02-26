/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import com.nimbusds.jose.JWSObject;
import java.time.Instant;
import java.util.Date;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;

/**
 * this tests the access token to ensure its still valid (hasn't expired).
 *
 * <p>We look at the "exp" claim and make sure its in the future.
 */
public class TokenExpiryValidator {

    GeoServerJwtHeadersFilterConfig jwtHeadersConfig;

    public TokenExpiryValidator(GeoServerJwtHeadersFilterConfig config) {
        jwtHeadersConfig = config;
    }

    public void validate(JWSObject jwsToken) throws Exception {
        if (!jwtHeadersConfig.isValidateTokenExpiry()) return; // nothing to do

        Long exp = (Long) jwsToken.getPayload().toJSONObject().get("exp");
        long exp_ms = exp.longValue() * 1000;
        Instant exp_instant = (new Date(exp_ms)).toInstant();
        if (exp_instant.isBefore(Instant.now())) throw new Exception("token has expired!");
    }
}

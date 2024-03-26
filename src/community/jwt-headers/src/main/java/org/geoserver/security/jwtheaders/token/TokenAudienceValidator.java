/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.token;

import static org.geoserver.security.jwtheaders.roles.JwtHeadersRolesExtractor.asStringList;

import com.nimbusds.jose.JWSObject;
import java.util.List;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;

/**
 * validates the Audience of an accesstoken. The OAUTH2/OIDC spec recommends validating this.
 * Security concern - the AccessToken was retrieved for a different client (not ours).
 */
public class TokenAudienceValidator {

    GeoServerJwtHeadersFilterConfig jwtHeadersConfig;

    public TokenAudienceValidator(GeoServerJwtHeadersFilterConfig config) {
        jwtHeadersConfig = config;
    }

    public void validate(JWSObject jwsToken) throws Exception {
        if (!jwtHeadersConfig.isValidateTokenAudience()) return; // nothing to do

        String aud_claimName = jwtHeadersConfig.getValidateTokenAudienceClaimName();
        List<String> aud = asStringList(jwsToken.getPayload().toJSONObject().get(aud_claimName));

        if (!aud.contains(jwtHeadersConfig.getValidateTokenAudienceClaimValue()))
            throw new Exception("token audience didn't match");
    }
}

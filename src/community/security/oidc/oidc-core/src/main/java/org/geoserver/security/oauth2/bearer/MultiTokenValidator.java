/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.bearer;

import java.util.List;
import java.util.Map;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;

/**
 * This is a token validator that runs a list of TokenValidators. This doesn't do any validation on
 * its own...
 */
public class MultiTokenValidator implements TokenValidator {

    List<TokenValidator> validators;

    public MultiTokenValidator(List<TokenValidator> validators) {
        this.validators = validators;
    }

    @Override
    public void verifyToken(
            OpenIdConnectFilterConfig config, Map accessTokenClaims, Map userInfoClaims)
            throws Exception {
        if (validators == null) {
            return; // nothing to do
        }
        for (TokenValidator validator : validators) {
            validator.verifyToken(config, accessTokenClaims, userInfoClaims);
        }
    }
}

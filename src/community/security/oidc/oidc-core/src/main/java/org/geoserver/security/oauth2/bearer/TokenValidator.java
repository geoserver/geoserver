/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.bearer;

import java.util.Map;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;

/**
 * Bearer tokens should be checked to make sure they are applicable to this application (to prevent
 * token reuse from another application)
 */
public interface TokenValidator {

    /**
     * @param accessTokenClaims - map of claims in the Access Token
     * @param userInfoClaims - map of claims from the oidc "userInfo" endpoint
     * @throws Exception - if there is a problem, throw an exception.
     */
    void verifyToken(OpenIdConnectFilterConfig config, Map accessTokenClaims, Map userInfoClaims)
            throws Exception;
}

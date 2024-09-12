/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.bearer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;

/**
 * This checks that the token is connected to this application. This will prevent a token for
 * another application being used by us.
 *
 * <p>NOTE: Azure AD's JWT AccessToken has a fixed "aud", no "azp", but an "appid" (should be our
 * client id). example; "aud": "00000003-0000-0000-c000-000000000000", "appid":
 * "b9e8d05a-08b6-48a5-81c8-9590a0f550f3",
 *
 * <p>NOTE: Keycloak has the audience as "account", no "appid", but "azp" should be our client id.
 * example; "aud": "account", "azp": "live-key",
 */
public class AudienceAccessTokenValidator implements TokenValidator {

    private final String AUDIENCE_CLAIM_NAME = "aud";
    private final String APPID_CLAIM_NAME = "appid";
    private final String KEYCLOAK_AUDIENCE_CLAIM_NAME = "azp";

    /**
     * "aud" must be our client id OR "azp" must be our client id (or, if its a list, contain our
     * client id) OR "appid" must be our client id.
     *
     * <p>Otherwise, its a token not for us...
     *
     * <p>This checks that the audience of the JWT access token is us. The main attack this tries to
     * prevent is someone getting an access token (i.e. from keycloak or azure) that was meant for
     * another application (say a silly calendar app), and then using that token here. The IDP
     * provider (keycloak/azure) will validate the token as "good", but it wasn't generated for us.
     * This does a check of the token that OUR client ID is mentioned (not another app).
     */
    @Override
    public void verifyToken(OpenIdConnectFilterConfig config, Map claimsJWT, Map userInfoClaims)
            throws Exception {
        String clientId = config.getCliendId();
        if ((claimsJWT.get(AUDIENCE_CLAIM_NAME) != null)) {
            if (claimsJWT.get(AUDIENCE_CLAIM_NAME).equals(clientId)) {
                return;
            } else if (claimsJWT.get(AUDIENCE_CLAIM_NAME) instanceof Collection
                    && ((Collection) claimsJWT.get(AUDIENCE_CLAIM_NAME)).contains(clientId)) {
                return;
            }
        }

        if ((claimsJWT.get(APPID_CLAIM_NAME) != null)
                && claimsJWT.get(APPID_CLAIM_NAME).equals(clientId)) {
            return; // azure specific
        }

        // azp - keycloak
        Object azp = claimsJWT.get(KEYCLOAK_AUDIENCE_CLAIM_NAME);
        if (azp != null) {
            if (azp instanceof String) {
                if (((String) azp).equals(config.getCliendId())) {
                    return;
                }
            } else if (azp instanceof List) {
                List azps = (List) azp;
                for (Object o : azps) {
                    if ((o instanceof String) && (o.equals(clientId))) {
                        return;
                    }
                }
            }
        }
        throw new Exception("JWT Bearer token - probably not meant for this application");
    }
}

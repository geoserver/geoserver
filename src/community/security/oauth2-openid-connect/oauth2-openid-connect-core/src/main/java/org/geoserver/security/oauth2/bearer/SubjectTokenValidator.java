/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.bearer;

import java.util.Map;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;

/**
 * This verifies that the token is about our user (i.e. the access token and userinfo endpoint agree
 * on who).
 *
 * <p>for keycloak, the "sub" of the JWT and userInfo are the same. for Azure AD, the "sub" of the
 * userInfo is in the JWT "xms_st" claim. "xms_st": { "sub":
 * "982kuI1hxIANLB__lrKejDgDnyjPnhbKLdPUF0JmOD1" },
 *
 * <p>The spec suggests verifying the user vs token subjects match, so this does that check.
 */
public class SubjectTokenValidator implements TokenValidator {

    private final String SUBJECT_CLAIM_NAME = "sub";
    private final String AZURE_SUBJECT_CONTAINER_NAME = "xms_st";

    @Override
    public void verifyToken(OpenIdConnectFilterConfig config, Map claims, Map userInfoClaims)
            throws Exception {
        // normal case - subjects are the same
        if ((claims.get(SUBJECT_CLAIM_NAME) != null)
                && (userInfoClaims.get(SUBJECT_CLAIM_NAME) != null)) {
            if (claims.get(SUBJECT_CLAIM_NAME).equals(userInfoClaims.get(SUBJECT_CLAIM_NAME)))
                return;
        }

        // Azure AD case - use accesstoken.xms_st.sub vs userinfo.sub
        if ((claims.get(AZURE_SUBJECT_CONTAINER_NAME) != null)
                && (claims.get(AZURE_SUBJECT_CONTAINER_NAME) instanceof Map)) {
            Map xmls_st = (Map) claims.get(AZURE_SUBJECT_CONTAINER_NAME);
            if (xmls_st.get(SUBJECT_CLAIM_NAME) != null) {
                if (xmls_st.get(SUBJECT_CLAIM_NAME).equals(userInfoClaims.get(SUBJECT_CLAIM_NAME)))
                    return;
            }
        }
        throw new Exception("JWT Bearer token VS UserInfo - subjects dont match");
    }
}

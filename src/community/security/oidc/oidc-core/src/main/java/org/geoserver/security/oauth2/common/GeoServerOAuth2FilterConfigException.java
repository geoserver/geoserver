/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import java.io.Serial;
import org.geoserver.security.validation.FilterConfigException;

public class GeoServerOAuth2FilterConfigException extends FilterConfigException {

    /** serialVersionUID */
    @Serial
    private static final long serialVersionUID = -3686715589371356406L;

    public GeoServerOAuth2FilterConfigException(String errorId, Object... args) {
        super(errorId, args);
    }

    public GeoServerOAuth2FilterConfigException(String errorId, String message, Object... args) {
        super(errorId, message, args);
    }

    public static final String OAUTH2_WKTS_URL_MALFORMED = "OAUTH2_WKTS_URL_MALFORMED";
    public static final String OAUTH2_CHECKTOKEN_OR_WKTS_ENDPOINT_URL_REQUIRED =
            "OAUTH2_CHECKTOKEN_OR_WKTS_ENDPOINT_URL_REQUIRED";
    public static final String OAUTH2_SCOPE_DELIMITER_MIXED = "OAUTH2_SCOPE_DELIMITER_MIXED";

    public static final String OAUTH2_CHECKTOKENENDPOINT_URL_REQUIRED = "OAUTH2_CHECKTOKENENDPOINT_URL_REQUIRED";

    public static final String OAUTH2_CHECKTOKENENDPOINT_URL_MALFORMED = "OAUTH2_CHECKTOKENENDPOINT_URL_MALFORMED";

    public static final String OAUTH2_URL_IN_LOGOUT_URI_MALFORMED = "OAUTH2_URL_IN_LOGOUT_URI_MALFORMED";

    public static final String OAUTH2_ACCESSTOKENURI_MALFORMED = "OAUTH2_ACCESSTOKENURI_MALFORMED";

    public static final String OAUTH2_ACCESSTOKENURI_NOT_HTTPS = "OAUTH2_ACCESSTOKENURI_NOT_HTTPS";

    public static final String OAUTH2_USERAUTHURI_MALFORMED = "OAUTH2_USERAUTHURI_MALFORMED";

    public static final String OAUTH2_USERAUTHURI_NOT_HTTPS = "OAUTH2_USERAUTHURI_NOT_HTTPS";

    public static final String OAUTH2_REDIRECT_URI_MALFORMED = "OAUTH2_REDIRECT_URI_MALFORMED";

    public static final String OAUTH2_CLIENT_ID_REQUIRED = "OAUTH2_CLIENT_ID_REQUIRED";

    public static final String OAUTH2_CLIENT_USER_NAME_REQUIRED = "OAUTH2_CLIENT_USER_NAME_REQUIRED";

    public static final String OAUTH2_CLIENT_SECRET_REQUIRED = "OAUTH2_CLIENT_SECRET_REQUIRED";

    public static final String OAUTH2_SCOPE_REQUIRED = "OAUTH2_SCOPE_REQUIRED";

    public static final String OAUTH2_URI_REQUIRED = "OAUTH2_URI_REQUIRED";

    public static final String OAUTH2_URI_INVALID = "OAUTH2_URI_INVALID";

    public static final String AEP_DENIED_WRONG_PROVIDER_COUNT = "AEP_DENIED_WRONG_PROVIDER_COUNT";

    public static final String MSGRAPH_COMBINATION_INVALID = "MSGRAPH_COMBINATION_INVALID";

    public static final String ROLE_SOURCE_ID_TOKEN_INVALID_FOR_GITHUB = "ROLE_SOURCE_ID_TOKEN_INVALID_FOR_GITHUB";

    public static final String OAUTH2_USER_INFO_URI_REQUIRED_NO_OIDC = "OAUTH2_USER_INFO_URI_REQUIRED_NO_OIDC";

    public static final String ROLE_SOURCE_USER_INFO_URI_REQUIRED = "ROLE_SOURCE_USER_INFO_URI_REQUIRED";

    public static final String OAUTH2_JWK_SET_URI_REQUIRED = "OAUTH2_JWK_SET_URI_REQUIRED";
}

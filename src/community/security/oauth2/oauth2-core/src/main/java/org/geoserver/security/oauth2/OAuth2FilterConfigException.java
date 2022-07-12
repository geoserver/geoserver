/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import org.geoserver.security.validation.FilterConfigException;

/**
 * Exception for OAuth2 filter configurations
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class OAuth2FilterConfigException extends FilterConfigException {

    private static final long serialVersionUID = 1L;

    public OAuth2FilterConfigException(String errorId, Object... args) {
        super(errorId, args);
    }

    public OAuth2FilterConfigException(String errorId, String message, Object... args) {
        super(errorId, message, args);
    }

    public static final String OAUTH2_CHECKTOKENENDPOINT_URL_REQUIRED =
            "OAUTH2_CHECKTOKENENDPOINT_URL_REQUIRED";

    public static final String OAUTH2_CHECKTOKENENDPOINT_URL_MALFORMED =
            "OAUTH2_CHECKTOKENENDPOINT_URL_MALFORMED";

    public static final String OAUTH2_URL_IN_LOGOUT_URI_MALFORMED =
            "OAUTH2_URL_IN_LOGOUT_URI_MALFORMED";

    public static final String OAUTH2_ACCESSTOKENURI_MALFORMED = "OAUTH2_ACCESSTOKENURI_MALFORMED";

    public static final String OAUTH2_ACCESSTOKENURI_NOT_HTTPS = "OAUTH2_ACCESSTOKENURI_NOT_HTTPS";

    public static final String OAUTH2_USERAUTHURI_MALFORMED = "OAUTH2_USERAUTHURI_MALFORMED";

    public static final String OAUTH2_USERAUTHURI_NOT_HTTPS = "OAUTH2_USERAUTHURI_NOT_HTTPS";

    public static final String OAUTH2_REDIRECT_URI_MALFORMED = "OAUTH2_REDIRECT_URI_MALFORMED";

    public static final String OAUTH2_CLIENT_ID_REQUIRED = "OAUTH2_CLIENT_ID_REQUIRED";

    public static final String OAUTH2_CLIENT_SECRET_REQUIRED = "OAUTH2_CLIENT_SECRET_REQUIRED";

    public static final String OAUTH2_SCOPE_REQUIRED = "OAUTH2_SCOPE_REQUIRED";
}

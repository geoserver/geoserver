/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import org.geoserver.security.validation.FilterConfigException;

public class OpenIdConnectFilterConfigException extends FilterConfigException {

    public OpenIdConnectFilterConfigException(String errorId, Object... args) {
        super(errorId, args);
    }

    public OpenIdConnectFilterConfigException(String errorId, String message, Object... args) {
        super(errorId, message, args);
    }

    public static final String OAUTH2_WKTS_URL_MALFORMED = "OAUTH2_WKTS_URL_MALFORMED";
    public static final String OAUTH2_CHECKTOKEN_OR_WKTS_ENDPOINT_URL_REQUIRED =
            "OAUTH2_CHECKTOKEN_OR_WKTS_ENDPOINT_URL_REQUIRED";
}

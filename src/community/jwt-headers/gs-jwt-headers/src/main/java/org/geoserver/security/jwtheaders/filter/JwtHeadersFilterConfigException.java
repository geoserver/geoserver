/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import org.geoserver.security.validation.FilterConfigException;

/** Validation errors of a JWT Headers filter configuration. Messages are in GeoServerException.properties. */
public class JwtHeadersFilterConfigException extends FilterConfigException {

    private static final long serialVersionUID = 1L;

    public static final String JWT_CONFIGURATION_NEEDED = "JWT_CONFIGURATION_NEEDED";
    public static final String USER_NAME_HEADER_NEEDED = "USER_NAME_HEADER_NEEDED";
    public static final String USER_NAME_PATH_NEEDED = "USER_NAME_PATH_NEEDED";
    public static final String ROLES_PATH_NEEDED = "ROLES_PATH_NEEDED";
    public static final String VALIDATION_METHOD_NEEDED = "VALIDATION_METHOD_NEEDED";
    public static final String VALIDATION_NEEDS_JWT_FORMAT = "VALIDATION_NEEDS_JWT_FORMAT";
    public static final String SIGNATURE_URL_INVALID = "SIGNATURE_URL_INVALID";
    public static final String ENDPOINT_URL_INVALID = "ENDPOINT_URL_INVALID";
    public static final String ISSUER_NEEDED = "ISSUER_NEEDED";
    public static final String AUDIENCE_NEEDED = "AUDIENCE_NEEDED";
    public static final String ROLES_NOT_FROM_VALIDATED_TOKEN = "ROLES_NOT_FROM_VALIDATED_TOKEN";

    public JwtHeadersFilterConfigException(String errorId, Object... args) {
        super(errorId, args);
    }
}

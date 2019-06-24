/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import org.geoserver.security.validation.FilterConfigException;

/**
 * Exception for {@link AuthenticationKeyFilterConfig} objects
 *
 * @author mcr
 */
public class AuthenticationKeyFilterConfigException extends FilterConfigException {

    private static final long serialVersionUID = 1L;

    public AuthenticationKeyFilterConfigException(String errorId, Object... args) {
        super(errorId, args);
    }

    public AuthenticationKeyFilterConfigException(String errorId, String message, Object... args) {
        super(errorId, message, args);
    }

    public static final String AUTH_KEY_PARAM_NAME_REQUIRED = "AUTH_KEY_PARAM_NAME_REQUIRED";
    public static final String AUTH_KEY_MAPPER_NAME_REQUIRED = "AUTH_KEY_MAPPER_NAME_REQUIRED";
    public static final String AUTH_KEY_MAPPER_NOT_FOUND_$1 = "AUTH_KEY_MAPPER_NOT_FOUND";
    public static final String INVALID_AUTH_KEY_MAPPER_$2 = "INVALID_AUTH_KEY_MAPPER";
    public static final String INVALID_AUTH_KEY_MAPPER_PARAMETER_$3 =
            "INVALID_AUTH_KEY_MAPPER_PARAMETER";
}

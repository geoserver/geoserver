/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.validation;

/**
 * @author Lorenzo Natali, GeoSolutions
 * @author Mauro Bartolomeoli, GeoSolutions
 */
public class CredentialsFromRequestHeaderFilterConfigException extends FilterConfigException {
    private static final long serialVersionUID = 1L;

    public CredentialsFromRequestHeaderFilterConfigException(String errorId, Object... args) {
        super(errorId, args);
    }

    public CredentialsFromRequestHeaderFilterConfigException(
            String errorId, String message, Object... args) {
        super(errorId, message, args);
    }

    public static final String USERNAME_HEADER_REQUIRED = "USERNAME_HEADER_REQUIRED";

    public static final String USERNAME_REGEX_REQUIRED = "USERNAME_REGEX_REQUIRED";

    public static final String PASSWORD_REGEX_REQUIRED = "PASSWORD_REGEX_REQUIRED";

    public static final String PASSWORD_HEADER_REQUIRED = "PASSWORD_HEADER_REQUIRED";
}

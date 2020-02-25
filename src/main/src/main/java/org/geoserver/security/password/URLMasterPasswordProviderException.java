/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

/**
 * Exception class for url master password provider config.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class URLMasterPasswordProviderException extends MasterPasswordProviderException {

    public static final String URL_REQUIRED = "URL_REQUIRED";
    public static final String URL_INVALID = "URL_INVALID";
    public static final String URL_LOCATION_NOT_READABLE = "URL_LOCATION_NOT_READABLE";

    public URLMasterPasswordProviderException(String errorId, Object... args) {
        super(errorId, args);
    }

    public URLMasterPasswordProviderException(String errorId, String message, Object... args) {
        super(errorId, message, args);
    }
}

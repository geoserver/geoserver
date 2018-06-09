/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import org.geoserver.security.validation.FilterConfigException;

/**
 * Exception for cas filter configurations
 *
 * @author mcr
 */
public class CasFilterConfigException extends FilterConfigException {

    private static final long serialVersionUID = 1L;

    public CasFilterConfigException(String errorId, Object... args) {
        super(errorId, args);
    }

    public CasFilterConfigException(String errorId, String message, Object... args) {
        super(errorId, message, args);
    }

    public static final String CAS_SERVER_URL_REQUIRED = "CAS_SERVER_URL_REQUIRED";
    public static final String CAS_SERVER_URL_MALFORMED = "CAS_SERVER_URL_MALFORMED";
    public static final String CAS_URL_IN_LOGOUT_PAGE_MALFORMED =
            "CAS_URL_IN_LOGOUT_PAGE_MALFORMED";
    public static final String CAS_PROXYCALLBACK_MALFORMED = "CAS_PROXYCALLBACK_MALFORMED";
    public static final String CAS_PROXYCALLBACK_NOT_HTTPS = "CAS_PROXYCALLBACK_NOT_HTTPS";
}

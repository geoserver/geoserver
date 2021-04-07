/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.auth.web;

import org.geoserver.security.validation.SecurityConfigException;

public class WebAuthConfigException extends SecurityConfigException {

    public static final String INVALID_WEB_SERVICE_URL = "INVALID_WEB_SERVICE_URL";

    public static final String PLACE_HOLDERS_NOT_FOUND = "PLACE_HOLDERS_NOT_FOUND";

    public static final String INVALID_TIMEOUT = "INVALID_TIMEOUT";

    public static final String INVALID_REGEX_EXPRESSION = "INVALID_REGEX_EXPRESSION";

    public static final String NO_ROLE_SERVICE_SELECTED = "NO_ROLE_SERVICE_SELECTED";

    public static final String HTTP_CONNECTION_NOT_ALLOWED = "HTTP_CONNECTION_NOT_ALLOWED";

    public WebAuthConfigException(String errorId, Object[] args) {
        super(errorId, args);
    }
}

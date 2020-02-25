/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import org.springframework.http.HttpStatus;

public class InvalidParameterValueException extends APIException {

    public static final String CODE = "InvalidParameterValue";

    public InvalidParameterValueException(String message) {
        super(CODE, message, HttpStatus.MULTI_STATUS.BAD_REQUEST);
    }

    public InvalidParameterValueException(String message, Throwable cause) {
        super(CODE, message, HttpStatus.MULTI_STATUS.BAD_REQUEST, cause);
    }
}

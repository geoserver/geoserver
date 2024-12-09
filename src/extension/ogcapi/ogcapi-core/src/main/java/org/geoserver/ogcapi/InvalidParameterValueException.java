/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import org.springframework.http.HttpStatus;

/** A OGC API specific exception class for invalid parameter values. */
public class InvalidParameterValueException extends APIException {

    public static final String CODE = "InvalidParameterValue";

    public InvalidParameterValueException(String message) {
        super(CODE, message, HttpStatus.MULTI_STATUS.BAD_REQUEST);
    }

    public InvalidParameterValueException(String message, Throwable cause) {
        super(CODE, message, HttpStatus.MULTI_STATUS.BAD_REQUEST, cause);
    }
}

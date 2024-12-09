/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import org.springframework.http.HttpStatus;

/**
 * A OGC API specific exception class, supports creation of JSON exceptions. The standard used to
 * mandate JSON payloads for exceptions, now it is not any longer, this could be a good reference
 * https://tools.ietf.org/html/rfc7807. See also
 * https://github.com/opengeospatial/oapi_common/issues/73
 */
public class APIException extends RuntimeException {

    public static final String NOT_FOUND = "NotFound";
    public static final String INVALID_PARAMETER = "InvalidParameter";
    public static final String INVALID_PARAMETER_VALUE = "InvalidParameterValue";
    public static final String NO_APPLICABLE_CODE = "NoApplicableCode";
    public static final String NOT_IMPLEMENTED = "NotImplemented";
    public static final String CONFLICT = "Conflict";

    private final HttpStatus status;
    private final String code;

    public APIException(String code, String body, HttpStatus status) {
        super(body);
        this.status = status;
        this.code = code;
    }

    public APIException(String code, String body, HttpStatus status, Throwable t) {
        super(body, t);
        this.status = status;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName());
        if (status != null) {
            builder.append(" ");
            builder.append(status.value());
            builder.append(" ");
            builder.append(status.name());
        }
        String message = getLocalizedMessage();
        if (message != null) {
            builder.append(": ");
            builder.append(message);
        }
        return builder.toString();
    }
}

package org.geoserver.restng;

import org.springframework.http.HttpStatus;

/**
 * General purpose rest exceptions
 */
public class RestException extends RuntimeException {
    private final HttpStatus status;

    public RestException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public RestException(String s, HttpStatus status, Exception e) {
        super(s, e);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

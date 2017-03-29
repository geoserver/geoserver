package org.geoserver.rest;

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

    public RestException(String s, HttpStatus status, Throwable t) {
        super(s, t);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

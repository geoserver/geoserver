/* (c) 2017 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.springframework.http.HttpStatus;

/** Rest Exception including {@link HttpStatus} code. */
public class RestException extends RuntimeException {
    /** serialVersionUID */
    private static final long serialVersionUID = 5762645820684796082L;

    private final HttpStatus status;

    public RestException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public RestException(String message, HttpStatus status, Throwable t) {
        super(message, t);
        this.status = status;
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

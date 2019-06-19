/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public class APIException extends RuntimeException {

    private final HttpStatus status;
    private MediaType mediaType = MediaType.TEXT_PLAIN;

    public APIException(String body, HttpStatus status) {
        super(body);
        this.status = status;
    }

    public APIException(String body, HttpStatus status, Throwable t) {
        super(body, t);
        this.status = status;
    }

    public APIException(String body, HttpStatus status, MediaType mediaType, Throwable t) {
        super(body, t);
        this.status = status;
        this.mediaType = mediaType;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public MediaType getMediaType() {
        return mediaType;
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

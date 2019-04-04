/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import org.springframework.http.HttpStatus;

/** Explicit exception for {@link HttpStatus#NOT_FOUND} (404) exceptions. */
public class ResourceNotFoundException extends RestException {

    /** serialVersionUID */
    private static final long serialVersionUID = 4656222203528783838L;

    public ResourceNotFoundException() {
        super("Not Found", HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}

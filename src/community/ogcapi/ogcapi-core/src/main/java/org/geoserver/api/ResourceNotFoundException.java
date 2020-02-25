/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import org.springframework.http.HttpStatus;

/**
 * APIException for resources that were not found in the system (statically available, but
 * dynamically discovered not to be there/not to be supportable
 */
public class ResourceNotFoundException extends APIException {

    public static String CODE = "ResourceNotFound";

    public ResourceNotFoundException(String body) {
        super(CODE, body, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String body, Throwable t) {
        super(CODE, body, HttpStatus.NOT_FOUND, t);
    }
}

/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.api;

import org.geoserver.gsr.model.exception.ServiceError;

/** Exception containing a {@link ServiceError}, for handling error responses */
public class ServiceException extends RuntimeException {
    ServiceError error;

    public ServiceException(ServiceError error) {
        super();
        this.error = error;
    }

    public ServiceException(String message, Throwable cause, ServiceError error) {
        super(message, cause);
        this.error = error;
    }

    public ServiceException(Throwable cause, ServiceError error) {
        super(cause);
        this.error = error;
    }

    public ServiceException(String message, ServiceError error) {
        super(message);
        this.error = error;
    }

    public ServiceError getError() {
        return error;
    }
}

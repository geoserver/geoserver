package com.boundlessgeo.gsr.api;

import com.boundlessgeo.gsr.model.exception.ServiceError;

/**
 * Exception containing a {@link ServiceError}, for handling error responses
 */
public class ServiceException extends Exception {
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
}

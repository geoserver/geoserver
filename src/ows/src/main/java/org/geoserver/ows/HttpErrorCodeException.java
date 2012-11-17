/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

/**
 * An exception thrown by a service to report back an http error code.
 * <p>
 * Instances of this exception are recognized by the dispatcher. The {@link #getErrorCode()}
 * is used to set  
 * </p>
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class HttpErrorCodeException extends RuntimeException {

    /**
     * the error code
     */
    final int errorCode;
    
    public HttpErrorCodeException(int errorCode) {
        super();
        this.errorCode = errorCode;
    }

    public HttpErrorCodeException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public HttpErrorCodeException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public HttpErrorCodeException(int errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}

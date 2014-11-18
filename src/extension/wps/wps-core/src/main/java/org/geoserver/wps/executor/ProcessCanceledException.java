/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

/**
 * Exception used to "poison" inputs and listener methods to force processes to exit when a cancel
 * request was submitted
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class ProcessCanceledException extends RuntimeException {

    private static final long serialVersionUID = -4266240008696107774L;

    public ProcessCanceledException() {
    }

    public ProcessCanceledException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ProcessCanceledException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessCanceledException(String message) {
        super(message);
    }

    public ProcessCanceledException(Throwable cause) {
        super(cause);
    }

}

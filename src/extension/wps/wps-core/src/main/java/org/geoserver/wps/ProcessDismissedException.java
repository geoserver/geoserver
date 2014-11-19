/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

/**
 * Exception used to "poison" inputs and listener methods to force processes to exit when a dismiss
 * request was submitted
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class ProcessDismissedException extends RuntimeException {

    private static final long serialVersionUID = -4266240008696107774L;

    public ProcessDismissedException() {
        this("The process execution has been dismissed");
    }

    public ProcessDismissedException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ProcessDismissedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessDismissedException(String message) {
        super(message);
    }

    public ProcessDismissedException(Throwable cause) {
        super(cause);
    }

}

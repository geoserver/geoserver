package org.geoserver.importer;

/**
 * An exception used to propagate information back to user regarding invalid
 * input. Used to differentiate from other exceptions.
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class ValidationException extends RuntimeException {
    
    public ValidationException(String message, Throwable cause) {
        super(message,cause);
    }

    public ValidationException(String message) {
        super(message);
    }

}

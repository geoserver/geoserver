/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.exception;

/**
 * Base class for runtime exceptions whose messages can be localized.
 *
 * @see GeoServerException
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerRuntimException extends RuntimeException implements IGeoServerException {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** id for the exception, used to locate localized message for the exception */
    String id;

    /** arguments to pass into the localized exception message */
    Object[] args;

    /** localized message */
    String message;

    public GeoServerRuntimException() {
        super();
    }

    public GeoServerRuntimException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeoServerRuntimException(String message) {
        super(message);
    }

    public GeoServerRuntimException(Throwable cause) {
        super(cause);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GeoServerRuntimException id(String id) {
        setId(id);
        return this;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object... args) {
        this.args = args;
    }

    public GeoServerRuntimException args(Object... args) {
        setArgs(args);
        return this;
    }

    @Override
    public String getMessage() {
        if (id == null) {
            return super.getMessage();
        }

        String localized = GeoServerExceptions.localize(this);
        return localized != null ? localized : super.getMessage();
    }

    void setMessage(String message) {
        this.message = message;
    }
}

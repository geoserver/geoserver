/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.exception;

/**
 * Base class for exceptions whose messages can be localized.
 *
 * <p>This class overrides the {{@link #getMessage()} function and uses {@link #getId()} to locate a
 * localized version of the message via a {@link GeoServerExceptions#localize(IGeoServerException)}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerException extends Exception implements IGeoServerException {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** id for the exception, used to locate localized message for the exception */
    String id;

    /** arguments to pass into the localized exception message */
    Object[] args;

    /** localized message */
    String message;

    public GeoServerException() {
        super();
    }

    public GeoServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeoServerException(String message) {
        super(message);
    }

    public GeoServerException(Throwable cause) {
        super(cause);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GeoServerException id(String id) {
        setId(id);
        return this;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object... args) {
        this.args = args;
    }

    public GeoServerException args(Object... args) {
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

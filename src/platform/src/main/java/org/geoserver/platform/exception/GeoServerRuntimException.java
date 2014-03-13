/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.exception;

/**
 * Base class for runtime exceptions whose messages can be localized.
 *
 * @see GeoServerException
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerRuntimException extends RuntimeException implements IGeoServerException {

    /** serialVersionUID */
    private static final long serialVersionUID = -7087177221649521201L;

    /** id for the exception, used to locate localized message for the exception */
    private String id;
    
    /** arguments to pass into the localized exception message */
    private Object[] args;
    
    /** localized message */
    private String message;

    public GeoServerRuntimException() {
        super();
    }

    public GeoServerRuntimException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public GeoServerRuntimException(final String message) {
        super(message);
    }

    public GeoServerRuntimException(final Throwable cause) {
        super(cause);
    }

    @Override
    public final String getId() {
        return id;
    }

    public final void setId(final String id) {
        this.id = id;
    }

    public final GeoServerRuntimException id(final String id) {
        setId(id);
        return this;
    }

    @Override
    public Object[] getArgs() {
        return args;
    }

    public void setArgs(final Object... args) {
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
    
        final String localized = GeoServerExceptions.localize(this);
        return localized != null ? localized : super.getMessage();
    }
    
    private void setMessage(final String message) {
        this.message = message;
    }
}

/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform.exception;

/**
 * Base class for exceptions whose messages can be localized.
 * <p>
 * This class overrides the {{@link #getMessage()} function and uses {@link #getId()} to 
 * locate a localized version of the message via a {@link GeoServerExceptions#localize(GeoServerException)}.
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class GeoServerException extends Exception implements IGeoServerException {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** id for the exception, used to locate localized message for the exception */
    private String id;

    /** arguments to pass into the localized exception message */
    private Object[] args;

    /** localized message */
    private String message;

    public GeoServerException() {
        super();
    }

    public GeoServerException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public GeoServerException(final String message) {
        super(message);
    }

    public GeoServerException(final Throwable cause) {
        super(cause);
    }

    @Override
    public final String getId() {
        return id;
    }

    public final void setId(final String id) {
        this.id = id;
    }

    public final GeoServerException id(final String id) {
        setId(id);
        return this;
    }

    @Override
    public final Object[] getArgs() {
        return args;
    }

    public final void setArgs(final Object... args) {
        this.args = args;
    }

    public final GeoServerException args(final Object... args) {
        setArgs(args);
        return this;
    }

    @Override
    public final String getMessage() {
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

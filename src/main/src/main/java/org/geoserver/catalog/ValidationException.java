/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.text.MessageFormat;

/**
 * An exception suitable for translation in the Wicket UI, by providing a key and a set of
 * parameters, as well as a default message to be used for logging and the REST API.
 */
public class ValidationException extends RuntimeException {

    private final String key;
    private final Object[] parameters;

    public ValidationException(String key, String messageFormat, Object... parameters) {
        super(new MessageFormat(messageFormat).format(parameters));
        this.key = key;
        this.parameters = parameters;
    }

    public ValidationException(
            String key, String messageFormat, Throwable cause, Object... parameters) {
        super(new MessageFormat(messageFormat).format(parameters), cause);
        this.key = key;
        this.parameters = parameters;
    }

    public String getKey() {
        return key;
    }

    public Object[] getParameters() {
        return parameters;
    }
}

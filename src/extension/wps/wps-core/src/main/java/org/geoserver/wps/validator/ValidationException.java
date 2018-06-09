/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import org.geoserver.wps.WPSException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

/**
 * A WPS exception occurring when validation on a certain input fails
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ValidationException extends WPSException {
    private static final long serialVersionUID = 1888292623044848453L;
    private Errors errors;

    public ValidationException(Errors errors, String locator) {
        super(buildMessage(errors));
        this.locator = locator;
        this.errors = errors;
    }

    private static String buildMessage(Errors errors) {
        StringBuilder sb =
                new StringBuilder("Validation failed for input '")
                        .append(errors.getObjectName())
                        .append("': ");
        for (ObjectError error : errors.getGlobalErrors()) {
            sb.append(error.getDefaultMessage());
            sb.append("\n");
        }
        for (FieldError error : errors.getFieldErrors()) {
            sb.append(error.getField()).append("[").append(error.getDefaultMessage()).append("]");
            sb.append("\n");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public Errors getErrors() {
        return errors;
    }
}

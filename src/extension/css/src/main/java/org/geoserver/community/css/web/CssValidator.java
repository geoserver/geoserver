package org.geoserver.community.css.web;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.web.GeoServerApplication;
import org.geotools.util.logging.Logging;

public class CssValidator implements IValidator<String> {

    static final Logger LOGGER = Logging.getLogger(CssValidator.class);

    @Override
    public void validate(IValidatable<String> text) {
        if (text.getValue() != null) {
            List<Exception> exceptions;
            try {
                CssHandler handler = GeoServerApplication.get().getBeanOfType(CssHandler.class);
                exceptions = handler.validate(text.getValue(), null, null);
                for (Exception exception : exceptions) {
                    ValidationError error = new ValidationError();
                    error.setMessage(exception.getMessage());
                    text.error(error);
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unexpected error validating CSS", e);
                ValidationError error = new ValidationError();
                error.setMessage("Validation failed: " + e.getMessage());
                text.error(error);
            }
        } else {
            ValidationError error = new ValidationError();
            error.setMessage("CSS text must not be empty");
            text.error(error);
        }
    }
}
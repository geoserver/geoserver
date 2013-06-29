package org.geoserver.community.css.web;

import java.io.Reader;
import java.io.StringReader;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.geoscript.geocss.compat.CSS2SLD;

public class CssValidator implements IValidator<String> {
    @Override
    public void validate(IValidatable<String> text) {
        if (text.getValue() != null) {
            try {
                Reader in = new StringReader(text.getValue());
                CSS2SLD.convert(in);
                in.close();
            } catch (Exception e) {
                ValidationError error = new ValidationError();
                error.setMessage(e.getMessage());
                text.error(error);
            }
        } else {
            ValidationError error = new ValidationError();
            error.setMessage("CSS text must not be empty");
            text.error(error);
        }
    }
}

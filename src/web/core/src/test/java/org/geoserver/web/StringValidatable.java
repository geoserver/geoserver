/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;

/** Helper class to test validators that need to validate a String object */
public class StringValidatable implements IValidatable<String> {
    List<IValidationError> errors = new ArrayList<>();
    String value;

    public StringValidatable(String value) {
        this.value = value;
    }

    @Override
    public void error(IValidationError error) {
        errors.add(error);
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<IValidationError> getErrors() {
        return errors;
    }

    @Override
    public IModel<String> getModel() {
        return null;
    }
}

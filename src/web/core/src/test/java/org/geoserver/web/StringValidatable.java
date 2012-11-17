/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;

/**
 * Helper class to test validators that need to validate a String object
 */
public class StringValidatable implements IValidatable {
    List<IValidationError> errors = new ArrayList<IValidationError>();
    String value;
    
    public StringValidatable(String value) {
        this.value = value;
    }

    public void error(IValidationError error) {
        errors.add(error);            
    }

    public Object getValue() {
        return value;
    }

    public boolean isValid() {
        return errors.size() == 0;
    }
    
    public List<IValidationError> getErrors() {
        return errors;
    }
    
}
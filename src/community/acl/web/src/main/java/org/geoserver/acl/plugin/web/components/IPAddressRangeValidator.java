/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.components;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.PatternValidator;

@SuppressWarnings("serial")
public class IPAddressRangeValidator extends PatternValidator {

    public IPAddressRangeValidator() {
        super("^([0-9]{1,3}\\.){3}[0-9]{1,3}(/([0-9]|[1-2][0-9]|3[0-2]))?$");
    }

    @Override
    protected IValidationError decorate(IValidationError error, IValidatable<String> validatable) {
        validatable.error(new ValidationError().addKey("IPAdressRange.invalid"));
        return error;
    }
}

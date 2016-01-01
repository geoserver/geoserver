/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.net.URI;
import java.util.Collections;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

/**
 * Validates a URI syntax by building a {@link URI} object around it
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class URIValidator implements IValidator {

    @Override
    public void validate(IValidatable validatable) {
        String uri = (String) validatable.getValue();
        try {
            new URI(uri);
        } catch(Exception e) {
            IValidationError err = new ValidationError("invalidURI:" + Collections.singletonMap("uri", uri));
            validatable.error(err);
        }
    }

}

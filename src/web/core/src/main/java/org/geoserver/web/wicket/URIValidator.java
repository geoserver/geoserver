/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.net.URI;
import java.util.Collections;

import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;

/**
 * Validates a URI syntax by building a {@link URI} object around it
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class URIValidator extends AbstractValidator {

    @Override
    protected void onValidate(IValidatable validatable) {
        String uri = (String) validatable.getValue();
        try {
            new URI(uri);
        } catch(Exception e) {
            error(validatable, "invalidURI", Collections.singletonMap("uri", uri));
        }

    }

}

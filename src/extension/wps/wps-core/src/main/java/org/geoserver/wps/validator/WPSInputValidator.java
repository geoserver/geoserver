/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import java.io.Serializable;
import org.springframework.validation.Validator;

/**
 * A validator for WPS process inputs
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface WPSInputValidator extends Validator, Serializable {

    /** Creates a copy of this validator */
    public WPSInputValidator copy();

    /**
     * Returns true if the validator is unset, that is, does not have valid configuration to operate
     * onto. In this case any attempt to run a validation will be ignored silently
     */
    public boolean isUnset();
}

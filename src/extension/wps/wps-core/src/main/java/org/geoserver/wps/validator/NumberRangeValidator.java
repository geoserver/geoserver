/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import org.geotools.util.NumberRange;
import org.springframework.validation.Errors;

/**
 * Validates numbers against a range of validity
 *
 * @author Andrea Aime - GeoSolutions
 */
public class NumberRangeValidator implements WPSInputValidator {

    private static final long serialVersionUID = 1146580888238281822L;

    static final String CODE = "OutOfRangeValue";

    NumberRange<?> range;

    public NumberRangeValidator(NumberRange<?> range) {
        this.range = range;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Number.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (range != null && !range.isEmpty() && !range.contains((Number) target)) {
            errors.reject(CODE, "Value " + target + " is out of the valid range " + range);
        }
    }

    public NumberRange<?> getRange() {
        return range;
    }

    public void setRange(NumberRange<?> range) {
        this.range = range;
    }

    @Override
    public String toString() {
        return "NumberRangeValidator [range=" + range + "]";
    }

    @Override
    public WPSInputValidator copy() {
        return new NumberRangeValidator(range);
    }

    @Override
    public boolean isUnset() {
        return range == null || range.isEmpty();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((range == null) ? 0 : range.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        NumberRangeValidator other = (NumberRangeValidator) obj;
        if (range == null) {
            if (other.range != null) return false;
        } else if (!range.equals(other.range)) return false;
        return true;
    }
}

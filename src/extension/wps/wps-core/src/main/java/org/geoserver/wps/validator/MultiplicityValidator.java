/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import org.hsqldb.lib.Collection;
import org.springframework.validation.Errors;

/**
 * Check there are not too many instances of the input
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MultiplicityValidator implements WPSInputValidator {

    private static final long serialVersionUID = 6088321363424263041L;

    static final String CODE = "TooManyValues";

    int maxInstances;

    public MultiplicityValidator(int maxInstances) {
        this.maxInstances = maxInstances;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return Collection.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (target instanceof Collection && maxInstances > 0) {
            int size = ((Collection) target).size();
            if (size > maxInstances) {
                errors.reject(
                        CODE,
                        "Input has been provided in too many values, found "
                                + size
                                + " but the maximum accepted amount is "
                                + maxInstances);
            }
        }
    }

    public int getMaxInstances() {
        return maxInstances;
    }

    public void setMaxInstances(int maxInstancens) {
        this.maxInstances = maxInstancens;
    }

    @Override
    public String toString() {
        return "MultiplicityValidator [maxInstancens=" + maxInstances + "]";
    }

    @Override
    public WPSInputValidator copy() {
        return new MultiplicityValidator(maxInstances);
    }

    @Override
    public boolean isUnset() {
        return maxInstances > 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + maxInstances;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MultiplicityValidator other = (MultiplicityValidator) obj;
        if (maxInstances != other.maxInstances) return false;
        return true;
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import com.google.common.reflect.Parameter;
import org.springframework.validation.Errors;

/**
 * A validator checking the maximum size of an object, in MB. This validator uses
 * ObjectSizeEstimator classes, and receives special treatment to advertise the limits to the
 * outside world, and apply them on raw binary inputs without going through the estimation
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MaxSizeValidator implements WPSInputValidator {

    private static final long serialVersionUID = 6486547223545859567L;

    /** Key in the {@link Parameter} metadata map representing the size limits */
    public static String PARAMETER_KEY = "MaxSizeMB";

    /** The size of a megabyte, in bytes */
    static final long MB = 1024 * 1024;

    static final String CODE = "ExcessSize";

    int maxSizeMB;

    public MaxSizeValidator(int maxSizeMB) {
        super();
        this.maxSizeMB = maxSizeMB;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    public void validate(Object target, Errors errors) {
        long size = ObjectSizeEstimators.getSizeOf(target);
        if (size > 0 && size > maxSizeMB * MB) {
            errors.reject(CODE, getErrorMessage(size));
        }
    }

    public String getErrorMessage(long size) {
        return "The size of the input has been estimated to "
                + size
                + ", which exceeds the maximum allowed "
                + maxSizeMB * MB;
    }

    public int getMaxSizeMB() {
        return maxSizeMB;
    }

    public void setMaxSizeMB(int maxSizeMB) {
        this.maxSizeMB = maxSizeMB;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + maxSizeMB;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MaxSizeValidator other = (MaxSizeValidator) obj;
        if (maxSizeMB != other.maxSizeMB) return false;
        return true;
    }

    @Override
    public String toString() {
        return "MaxSizeValidator [maxSizeMB=" + maxSizeMB + "]";
    }

    @Override
    public WPSInputValidator copy() {
        return new MaxSizeValidator(maxSizeMB);
    }

    @Override
    public boolean isUnset() {
        return maxSizeMB <= 0;
    }
}

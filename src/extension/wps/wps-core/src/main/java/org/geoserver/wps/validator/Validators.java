/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.validation.Validator;

/**
 * Validation related utilites
 *
 * @author Andrea Aime - GeoSolutions
 */
public class Validators {

    /**
     * Returns a clone of the validators collection where none of the validators implementing one of
     * the specified filter classes is available
     */
    public static List<Validator> filterOutClasses(
            Collection<Validator> validators, Class... filteredClasses) {
        if (validators == null) {
            return null;
        }

        List<Validator> result = new ArrayList<>();
        for (Validator v : validators) {
            Class<? extends Validator> validatorClass = v.getClass();
            boolean skip = false;
            for (Class filteredClass : filteredClasses) {
                if (filteredClass.isAssignableFrom(validatorClass)) {
                    skip = true;
                    break;
                }
            }

            if (!skip) {
                result.add(v);
            }
        }

        return result;
    }

    /**
     * Returning the most restrictive size limit in the {@link MaxSizeValidator} contained in the
     * validators collection, or -1 if there is no limit
     */
    public static int getMaxSizeMB(Collection<Validator> validators) {
        int maxSize = Integer.MAX_VALUE;
        if (validators != null) {
            for (Validator v : validators) {
                if (v instanceof MaxSizeValidator) {
                    MaxSizeValidator ms = (MaxSizeValidator) v;
                    int msSize = ms.getMaxSizeMB();
                    maxSize = Math.min(maxSize, msSize);
                }
            }
        }

        if (maxSize <= 0 || maxSize == Integer.MAX_VALUE) {
            return -1;
        } else {
            return maxSize;
        }
    }
}

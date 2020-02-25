/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import java.util.Collections;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Central class providing static methods to estimate the size of an object. It will scan the
 * application context looking for {@link ObjectSizeEstimator} objects
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ObjectSizeEstimators implements ApplicationContextAware {

    private static List<ObjectSizeEstimator> estimators = Collections.emptyList();

    /**
     * Looks up all the {@link ObjectSizeEstimator} available in the application context, and will
     * return the result of the first one returning a positive size, or {@link
     * ObjectSizeEstimator#UNKNOWN_SIZE} if none returns a positive value
     */
    public static long getSizeOf(Object object) {

        for (ObjectSizeEstimator estimator : estimators) {
            long size = estimator.getSizeOf(object);
            if (size > 0) {
                return size;
            }
        }

        return ObjectSizeEstimator.UNKNOWN_SIZE;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        estimators = GeoServerExtensions.extensions(ObjectSizeEstimator.class, applicationContext);
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.simple.SimpleFeatureTypeImpl;

/** Utility methods for feature type operations. */
public class FeatureTypeUtils {

    /**
     * Checks if the feature collection contains complex features.
     *
     * @param results the feature collection response
     * @return true if any feature type in the collection is not a simple feature type
     */
    public static boolean isComplexFeature(FeatureCollectionResponse results) {
        boolean hasComplex = false;
        for (int fcIndex = 0; fcIndex < results.getFeature().size(); fcIndex++) {
            if (!(results.getFeature().get(fcIndex).getSchema() instanceof SimpleFeatureTypeImpl)) {
                hasComplex = true;
                break;
            }
        }
        return hasComplex;
    }
}

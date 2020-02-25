/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature.sort;

import java.util.Comparator;
import org.opengis.feature.Feature;

/**
 * Compares two feature based on their feature id
 *
 * @author Andrea Aime - GeoSolutions
 */
class FidComparator implements Comparator<Feature> {

    boolean ascending;

    /**
     * Builds a new comparator
     *
     * @param ascending If true the comparator will force an ascending order (descending otherwise)
     */
    public FidComparator(boolean ascending) {
        this.ascending = ascending;
    }

    public int compare(Feature f1, Feature f2) {
        int result = compareAscending(f1, f2);
        if (ascending) {
            return result;
        } else {
            return result * -1;
        }
    }

    private int compareAscending(Feature f1, Feature f2) {
        String id1 = f1.getIdentifier().getID();
        String id2 = f2.getIdentifier().getID();

        if (id1 == null) {
            if (id2 == null) {
                return 0;
            } else {
                return -1;
            }
        } else if (id2 == null) {
            return 1;
        } else {
            return id1.compareTo(id2);
        }
    }
}

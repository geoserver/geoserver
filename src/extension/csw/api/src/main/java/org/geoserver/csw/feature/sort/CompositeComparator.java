/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature.sort;

import java.util.Comparator;
import java.util.List;

/**
 * A composite comparator that applies the provided comparators as a hierarchical list, the first
 * comparator that returns a non zero value "wins"
 *
 * @author Andrea Aime - GeoSolutions
 */
class CompositeComparator<T> implements Comparator<T> {

    List<Comparator<T>> comparators;

    public CompositeComparator(List<Comparator<T>> comparators) {
        this.comparators = comparators;
    }

    public int compare(T f1, T f2) {
        for (Comparator<T> comp : comparators) {
            int result = comp.compare(f1, f2);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }
}

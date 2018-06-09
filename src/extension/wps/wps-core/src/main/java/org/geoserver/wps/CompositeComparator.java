/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.Comparator;
import java.util.List;

/**
 * Applies N comparators cascading from one to the next until one returning non zero is found
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 */
public class CompositeComparator<T> implements Comparator<T> {
    List<Comparator<T>> comparators;

    public CompositeComparator(List<Comparator<T>> comparators) {
        this.comparators = comparators;
    }

    @Override
    public int compare(T o1, T o2) {
        for (Comparator<T> comparator : comparators) {
            int result = comparator.compare(o1, o2);
            if (result != 0) {
                return result;
            }
        }

        return 0;
    }
}

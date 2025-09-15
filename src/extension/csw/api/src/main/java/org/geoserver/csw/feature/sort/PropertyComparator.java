/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature.sort;

import java.util.Comparator;
import org.geotools.api.feature.Attribute;
import org.geotools.api.filter.expression.PropertyName;

/**
 * Compares two feature based on an attribute value
 *
 * @author Andrea Aime - GeoSolutions
 */
class PropertyComparator<T> implements Comparator<T> {

    PropertyName propertyName;

    boolean ascending;

    /**
     * Builds a new comparator
     *
     * @param propertyName The property name to be used
     * @param ascending If true the comparator will force an ascending order (descending otherwise)
     */
    public PropertyComparator(PropertyName propertyName, boolean ascending) {
        this.propertyName = propertyName;
        this.ascending = ascending;
    }

    @Override
    public int compare(T f1, T f2) {
        int result = compareAscending(f1, f2);
        if (ascending) {
            return result;
        } else {
            return result * -1;
        }
    }

    @SuppressWarnings("unchecked")
    private int compareAscending(T f1, T f2) {
        Object a1 = propertyName.evaluate(f1);
        Comparable o1, o2;
        if (a1 instanceof Attribute attribute) {
            o1 = (Comparable) attribute.getValue();
        } else {
            o1 = a1 != null ? (Comparable) a1 : null;
        }

        Object a2 = propertyName.evaluate(f2);
        if (a2 instanceof Attribute attribute) {
            o2 = (Comparable) attribute.getValue();
        } else {
            o2 = a2 != null ? (Comparable) a2 : null;
        }

        if (o1 == null) {
            if (o2 == null) {
                return 0;
            } else {
                return -1;
            }
        } else if (o2 == null) {
            return 1;
        } else {
            return o1.compareTo(o2);
        }
    }
}

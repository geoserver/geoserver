/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature.sort;

import java.util.Comparator;

import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.filter.expression.PropertyName;

/**
 * Compares two feature based on an attribute value
 * 
 * @author Andrea Aime - GeoSolutions
 */
class PropertyComparator implements Comparator<Feature> {

    PropertyName propertyName;

    boolean ascending;

    /**
     * Builds a new comparator
     * 
     * @param propertyName The property name to be used
     * @param inverse If true the comparator will force an ascending order (descending otherwise)
     */
    public PropertyComparator(PropertyName propertyName, boolean ascending) {
        this.propertyName = propertyName;
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
        Attribute a1 = (Attribute) propertyName.evaluate(f1);
        Comparable o1 = a1 != null ? (Comparable) a1.getValue() : null;
        Attribute a2 = (Attribute) propertyName.evaluate(f2);
        Comparable o2 = a2 != null ? (Comparable) a2.getValue() : null;

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

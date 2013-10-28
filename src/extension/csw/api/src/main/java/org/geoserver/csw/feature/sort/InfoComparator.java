/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature.sort;

import java.util.Comparator;

import org.geoserver.catalog.Info;

/**
 * Compares two feature based on their catalog info id
 * 
 * @author Niels Charlier
 */
class InfoComparator implements Comparator<Info> {

    boolean ascending;

    /**
     * Builds a new comparator
     * 
     * @param inverse If true the comparator will force an ascending order (descending otherwise)
     */
    public InfoComparator(boolean ascending) {
        this.ascending = ascending;
    }

    public int compare(Info i1, Info i2) {
        int result = compareAscending(i1, i2);
        if (ascending) {
            return result;
        } else {
            return result * -1;
        }
    }

    private int compareAscending(Info i1, Info i2) {
        String id1 = i1.getId();
        String id2 = i2.getId();

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

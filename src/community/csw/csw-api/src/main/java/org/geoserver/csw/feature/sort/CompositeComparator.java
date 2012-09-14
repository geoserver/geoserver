/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature.sort;

import java.util.Comparator;
import java.util.List;

import org.opengis.feature.Feature;

/**
 * A composite comparator that applies the provided comparators as a hierarchical list, the first
 * comparator that returns a non zero value "wins"
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
class CompositeComparator implements Comparator<Feature> {

    List<Comparator<Feature>> comparators;

    public CompositeComparator(List<Comparator<Feature>> comparators) {
        this.comparators = comparators;
    }

    public int compare(Feature f1, Feature f2) {
        for (Comparator<Feature> comp : comparators) {
            int result = comp.compare(f1, f2);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

}

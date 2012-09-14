/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.geoserver.csw.feature.sort.ComplexComparatorFactory;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * A fully in memory feature collection
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class MemoryFeatureCollection extends AbstractFeatureCollection<FeatureType, Feature> {

    protected ArrayList<Feature> features;

    public MemoryFeatureCollection(FeatureType memberType) {
        this(memberType, null);
    }

    public MemoryFeatureCollection(FeatureType memberType, List<Feature> features) {
        super(memberType);
        this.features = new ArrayList<Feature>();
        if (features != null) {
            for (Feature f : features) {
                if (!f.getType().equals(memberType)) {
                    // TODO: handle inheritance
                    throw new IllegalArgumentException(
                            "Found a feature whose feature type is not equal to the declared one: "
                                    + f);
                }
                this.features.add(f);
            }
        }
    }

    @Override
    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        List<Feature> results = new ArrayList<Feature>();
        for (Feature f : features) {
            if (filter.evaluate(f)) {
                results.add(f);
            }
        }

        return new MemoryFeatureCollection(getSchema(), results);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        List<Feature> results = new ArrayList<Feature>(features);
        Comparator<Feature> comparator = ComplexComparatorFactory.buildComparator(order);
        Collections.sort(results, comparator);

        return new MemoryFeatureCollection(getSchema(), results);
    }

    @Override
    protected Iterator<Feature> openIterator() {
        return features.iterator();
    }

    @Override
    protected void closeIterator(Iterator<Feature> close) {
        // nothing to do
    }

    @Override
    public int size() {
        return features.size();
    }

}

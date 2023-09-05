/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.feature.FeatureCollection;

public class CompositeFeatureCollection extends AbstractFeatureCollection<FeatureType, Feature> {

    private List<FeatureCollection<FeatureType, Feature>> collections;

    protected CompositeFeatureCollection(
            List<FeatureCollection<FeatureType, Feature>> collections) {
        super(collections.get(0).getSchema());
        this.collections = collections;

        // check consistency
        for (FeatureCollection<FeatureType, Feature> fc : collections) {
            if (!getSchema().equals(fc.getSchema())) {
                throw new IllegalArgumentException(
                        "All feature collections must have the same type, found "
                                + getSchema()
                                + " and "
                                + fc.getSchema()
                                + " instead");
            }
        }
    }

    @Override
    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        List<FeatureCollection<FeatureType, Feature>> filtered = new ArrayList<>();
        for (FeatureCollection<FeatureType, Feature> fc : filtered) {
            filtered.add(fc.subCollection(filter));
        }

        return new CompositeFeatureCollection(filtered);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        // being very lazy here, since I don't believe we need this method in CSW
        // TODO: create a SortMergeFeatureCollection instead
        MemoryFeatureCollection memory = new MemoryFeatureCollection(getSchema());
        for (FeatureCollection<FeatureType, Feature> fc : collections) {
            memory.addAll(fc);
        }
        return memory.sort(order);
    }

    @Override
    protected Iterator<Feature> openIterator() {
        return new CompositeIterator(collections);
    }

    @Override
    protected void closeIterator(Iterator<Feature> close) {
        if (close instanceof CompositeIterator) {
            ((CompositeIterator) close).close();
        }
    }
}

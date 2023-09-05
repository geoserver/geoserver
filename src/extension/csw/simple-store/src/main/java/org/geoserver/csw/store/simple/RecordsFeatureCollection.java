/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.geoserver.csw.feature.AbstractFeatureCollection;
import org.geoserver.csw.feature.MemoryFeatureCollection;
import org.geoserver.csw.records.CSWRecordDescriptor;
import org.geoserver.platform.resource.Resource;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.feature.FeatureCollection;

/**
 * A feature collection reading record files from the specified directory
 *
 * @author Andrea Aime - GeoSolutions
 */
class RecordsFeatureCollection extends AbstractFeatureCollection<FeatureType, Feature> {

    Resource root;

    int offset;

    public RecordsFeatureCollection(Resource root, int offset) {
        super(CSWRecordDescriptor.RECORD_TYPE);
        this.root = root;
        this.offset = offset;
    }

    @Override
    protected Iterator<Feature> openIterator() {
        return new SimpleRecordIterator(root, offset);
    }

    @Override
    protected void closeIterator(Iterator<Feature> close) {
        // nothing to do, the SimpleRecordIterator does not keep any reference to streams and the
        // like
    }

    @Override
    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        return new FilteringFeatureCollection<>(this, filter);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        List<Feature> features = new ArrayList<>();
        MemoryFeatureCollection memory = new MemoryFeatureCollection(getSchema(), features);
        return memory.sort(order);
    }
}

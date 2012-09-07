package org.geoserver.csw.store.simple;

import java.io.File;
import java.util.Iterator;

import org.geoserver.csw.records.CSWRecordTypes;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

public class RecordsFeatureCollection extends AbstractFeatureCollection<FeatureType, Feature> {

    File root;
    int offset;

    public RecordsFeatureCollection(File root, int offset) {
        super(CSWRecordTypes.RECORD);
        this.root = root;
        this.offset = offset;
    }

    @Override
    protected Iterator<Feature> openIterator() {
        return new SimpleRecordIterator(root, offset);
    }

    @Override
    protected void closeIterator(Iterator<Feature> close) {
        // nothing to do, the SimpleRecordIterator does not keep any reference to streams and the like
    }

    @Override
    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        return new FilteringFeatureCollection<FeatureType, Feature>(this, filter);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        throw new UnsupportedOperationException("Sorting is not supported at the moment, sorry");
    }

    @Override
    public int size() {
        FeatureIterator<Feature> fi = null;
        int count = 0;
        try {
            fi = features();
            while (fi.hasNext()) {
                fi.next();
                count++;
            }
        } finally {
            if (fi != null) {
                fi.close();
            }
        }

        return count;
    }

    @Override
    public ReferencedEnvelope getBounds() {
        FeatureIterator<Feature> fi = null;
        ReferencedEnvelope bounds = null;
        try {
            fi = features();
            while (fi.hasNext()) {
                Feature f = fi.next();
                ReferencedEnvelope re = ReferencedEnvelope.reference(f.getBounds());
                if (bounds == null) {
                    bounds = re;
                } else {
                    bounds.expandToInclude(re);
                }
            }
        } finally {
            if (fi != null) {
                fi.close();
            }
        }

        return bounds;
    }

}

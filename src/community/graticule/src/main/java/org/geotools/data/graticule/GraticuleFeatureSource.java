/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import java.io.IOException;
import java.util.List;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.geometry.jts.ReferencedEnvelope;

public class GraticuleFeatureSource extends ContentFeatureSource implements SimpleFeatureSource {
    private final List<Double> steps;

    private final SimpleFeatureType schema;
    private final ReferencedEnvelope bounds;
    private final GraticuleDataStore parent;

    public GraticuleFeatureSource(
            ContentEntry entry,
            Query query,
            List<Double> steps,
            ReferencedEnvelope bounds,
            SimpleFeatureType schema) {
        super(entry, query);

        this.parent = (GraticuleDataStore) entry.getDataStore();
        this.steps = steps;
        this.bounds = bounds;
        this.schema = schema;
    }

    @Override
    protected ReferencedEnvelope getBoundsInternal(Query query) throws IOException {
        return bounds;
    }

    @Override
    protected int getCountInternal(Query query) throws IOException {
        return 0;
    }

    @Override
    protected FeatureReader<SimpleFeatureType, SimpleFeature> getReaderInternal(Query query)
            throws IOException {
        return new GraticuleFeatureReader((GraticuleDataStore) this.getDataStore(), query);
    }

    @Override
    protected SimpleFeatureType buildFeatureType() throws IOException {
        return schema;
    }

    @Override
    public Name getName() {
        return schema.getName();
    }
}

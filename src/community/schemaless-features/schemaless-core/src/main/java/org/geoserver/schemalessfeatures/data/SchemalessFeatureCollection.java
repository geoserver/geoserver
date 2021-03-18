/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.data;

import java.io.IOException;
import java.util.Collection;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

public class SchemalessFeatureCollection implements FeatureCollection<FeatureType, Feature> {

    protected SchemalessFeatureSource featureSource;

    protected Query query;

    public SchemalessFeatureCollection(Query q, SchemalessFeatureSource source) {
        this.query = q;
        this.featureSource = source;
    }

    @Override
    public FeatureIterator<Feature> features() {
        return new SchemalessFeatureIterator(featureSource.getReader(this.query));
    }

    @Override
    public FeatureType getSchema() {
        return featureSource.getSchema();
    }

    @Override
    public String getID() {
        return null;
    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {}

    @Override
    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        return null;
    }

    @Override
    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        return null;
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return ReferencedEnvelope.create(getSchema().getCoordinateReferenceSystem());
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> o) {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        try {
            return featureSource.getCount(query);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to get the collection size. Exception is " + e.getMessage(), e);
        }
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <O> O[] toArray(O[] a) {
        return null;
    }
}

/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.schemalessfeatures.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

public class ComplexFeatureCollection implements FeatureCollection<FeatureType, Feature> {

    protected ComplexFeatureSource featureSource;

    protected Query query;

    public ComplexFeatureCollection(Query q, ComplexFeatureSource source) {
        this.query = q;
        this.featureSource = source;
    }

    @Override
    public FeatureIterator<Feature> features() {
        return new ComplexFeatureIterator(featureSource.getReader(this.query));
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
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        DataUtilities.visit(this, visitor, progress);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        Query query = new Query();
        query.setFilter(filter);
        query = DataUtilities.mixQueries(this.query, query, null);
        return new ComplexFeatureCollection(query, featureSource);
    }

    @Override
    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        Query query = new Query();
        query.setSortBy(new org.opengis.filter.sort.SortBy[] {order});

        query = DataUtilities.mixQueries(this.query, query, null);
        return new ComplexFeatureCollection(query, featureSource);
    }

    @Override
    public ReferencedEnvelope getBounds() {
        FeatureReader<FeatureType, Feature> reader = null;
        try {
            ReferencedEnvelope result = featureSource.getBounds(query);
            if (result != null) {
                return result;
            }

            // ops, we have to compute the results by hand. Let's load just the
            // geometry attributes though
            Query q = new Query(query);
            FeatureType schema = getSchema();
            GeometryDescriptor geometry = schema.getGeometryDescriptor();
            if (geometry != null) q.setPropertyNames(geometry.getLocalName());
            // grab the features and scan through them
            reader = featureSource.getReader(q);
            while (reader.hasNext()) {
                Feature f = reader.next();
                ReferencedEnvelope featureBounds = ReferencedEnvelope.reference(f.getBounds());
                if (result == null) {
                    result = featureBounds;
                } else if (featureBounds != null) {
                    result.expandToInclude(featureBounds);
                }
            }
            // return the results if we got any, or return an empty one otherwise
            if (result != null) {
                return result;
            } else {
                return ReferencedEnvelope.create(getSchema().getCoordinateReferenceSystem());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    // we tried...
                }
            }
        }
    }

    @Override
    public boolean contains(Object o) {
        FeatureIterator<Feature> e = null;
        try {
            e = this.features();
            if (o == null) {
                while (e.hasNext()) {
                    if (e.next() == null) {
                        return true;
                    }
                }
            } else {
                while (e.hasNext()) {
                    if (o.equals(e.next())) {
                        return true;
                    }
                }
            }
            return false;
        } finally {
            if (e != null) {
                e.close();
            }
        }
    }

    @Override
    public boolean containsAll(Collection<?> o) {
        Iterator<?> e = o.iterator();
        try {
            while (e.hasNext()) {
                if (!contains(e.next())) {
                    return false;
                }
            }
            return true;
        } finally {
            if (e instanceof FeatureIterator) {
                ((FeatureIterator<?>) e).close();
            }
        }
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
        ArrayList<Feature> array = new ArrayList<>();
        FeatureIterator<Feature> e = null;
        try {
            e = features();
            while (e.hasNext()) {
                array.add(e.next());
            }
            return array.toArray(new Feature[array.size()]);
        } finally {
            if (e != null) {
                e.close();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O> O[] toArray(O[] array) {
        int size = size();
        if (array.length < size) {
            array =
                    (O[])
                            java.lang.reflect.Array.newInstance(
                                    array.getClass().getComponentType(), size);
        }
        FeatureIterator<Feature> it = features();
        try {
            Object[] result = array;
            for (int i = 0; it.hasNext() && i < size; i++) {
                result[i] = it.next();
            }
            if (array.length > size) {
                array[size] = null;
            }
            return array;
        } finally {
            if (it != null) {
                it.close();
            }
        }
    }
}

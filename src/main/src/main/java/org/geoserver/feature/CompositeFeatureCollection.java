/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.FilteringFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * Wraps multiple feature collections into a single.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class CompositeFeatureCollection<T extends FeatureType, F extends Feature>
        implements FeatureCollection<T, F> {
    /** wrapped collecitons */
    List<? extends FeatureCollection<T, F>> collections;

    T schema;

    public CompositeFeatureCollection(List<? extends FeatureCollection<T, F>> collections) {
        this.collections = collections;
    }

    public CompositeFeatureCollection(
            List<? extends FeatureCollection<T, F>> collections, T schema) {
        this.collections = collections;
        this.schema = schema;
    }

    @Override
    public FeatureIterator<F> features() {
        return new CompositeIterator();
    }

    @Override
    public T getSchema() {
        return schema;
    }

    @Override
    public String getID() {
        throw new RuntimeException(
                "Can't get the id for a composite featurecollection; you need to identify the consituent collections directly.");
    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        DataUtilities.visit(this, visitor, progress);
    }

    @Override
    public FeatureCollection<T, F> subCollection(Filter filter) {
        return new FilteringFeatureCollection<>(this, filter);
    }

    @Override
    @SuppressWarnings("unchecked")
    public FeatureCollection<T, F> sort(SortBy order) {
        if (schema instanceof SimpleFeatureCollection) {
            return (FeatureCollection<T, F>)
                    new SortedSimpleFeatureCollection(
                            DataUtilities.simple(
                                    (FeatureCollection<SimpleFeatureType, SimpleFeature>) this),
                            new SortBy[] {order});
        }
        throw new UnsupportedOperationException("Cannot perform sorting on complex features");
    }

    @Override
    public ReferencedEnvelope getBounds() {
        // crazy, this same mapper inlined in the stream does not compile...
        Function<FeatureCollection<T, F>, ReferencedEnvelope> mapper =
                c -> {
                    final ReferencedEnvelope envelope = c.getBounds();
                    if (envelope == null) {
                        return DataUtilities.bounds(c);
                    } else {
                        return envelope;
                    }
                };
        return collections.stream()
                .map(mapper)
                .reduce(
                        (e1, e2) -> {
                            CoordinateReferenceSystem crs1 = e1.getCoordinateReferenceSystem();
                            CoordinateReferenceSystem crs2 = e2.getCoordinateReferenceSystem();
                            if (crs1 != crs2 && !CRS.equalsIgnoreMetadata(crs1, crs2)) {
                                throw new RuntimeException(
                                        "Two collections are returning different CRSs, cannot perform this "
                                                + "accumulation (yet): \n"
                                                + crs1
                                                + "\n"
                                                + crs2);
                            }
                            e1.expandToInclude(e2);
                            return e1;
                        })
                .orElse(null);
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
        return collections.stream().allMatch(c -> c.isEmpty());
    }

    @Override
    public int size() {
        return collections.stream()
                .mapToInt(
                        c -> {
                            int size = c.size();
                            if (size < 0) {
                                size = DataUtilities.count(c);
                            }
                            return size;
                        })
                .sum();
    }

    class CompositeIterator implements FeatureIterator<F> {
        int index;
        FeatureIterator<F> iterator;

        public CompositeIterator() {
            index = 0;
        }

        public void remove() {}

        @Override
        public boolean hasNext() {
            // is there a current iterator that has another element
            if ((iterator != null) && iterator.hasNext()) {
                return true;
            }

            // get the next iterator
            while (index < collections.size()) {
                // close current before we move to next
                if (iterator != null) {
                    iterator.close();
                }

                // grap next
                iterator = collections.get(index++).features();

                if (iterator.hasNext()) {
                    return true;
                }
            }

            // no more
            if (iterator != null) {
                // close the last iterator
                iterator.close();
            }

            return false;
        }

        @Override
        public F next() {
            return iterator.next();
        }

        @Override
        public void close() {
            if (iterator instanceof Closeable) {
                iterator.close();
            }
        }
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @Override
    public <O> O[] toArray(O[] array) {
        List<SimpleFeature> list = new ArrayList<>();

        Iterator<? extends FeatureCollection<T, F>> it = collections.iterator();
        while (it.hasNext()) {
            SimpleFeatureCollection col = (SimpleFeatureCollection) it.next();
            try (SimpleFeatureIterator it2 = col.features()) {
                while (it2.hasNext()) {
                    list.add(it2.next());
                }
            }
        }

        return list.toArray(array);
    }

    /** @return the collections */
    public List<? extends FeatureCollection<T, F>> getCollections() {
        return collections;
    }

    /**
     * Returns true if the composite only contains simple features
     *
     * @return
     */
    public boolean isSimple() {
        for (FeatureCollection collection : collections) {
            if (!(collection instanceof SimpleFeatureCollection)
                    && !(collection.getSchema() instanceof SimpleFeatureType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Forcefully casts this to a SimpleFeatureCollection, since it cannot be done directly with
     * DataUtilities.simple if the target feature type is null (due to mixing heterogeneous
     * collections)
     *
     * @return
     */
    public SimpleFeatureCollection simple() {
        if (isSimple()) {
            @SuppressWarnings("unchecked")
            FeatureCollection<SimpleFeatureType, SimpleFeature> cast =
                    (FeatureCollection<SimpleFeatureType, SimpleFeature>) this;
            return new SimpleFeatureCollectionBridge(cast);
        }
        throw new ClassCastException(
                "This collection cannot be coerced to SimpleFeatureCollection");
    }
}

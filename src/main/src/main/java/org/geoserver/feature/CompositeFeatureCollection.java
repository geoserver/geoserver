/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import org.geotools.data.CloseableIterator;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.DataFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Wraps multiple feature collections into a single.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class CompositeFeatureCollection extends DataFeatureCollection {
    /** wrapped collecitons */
    List<SimpleFeatureCollection> collections;

    SimpleFeatureType schema;

    public CompositeFeatureCollection(List<SimpleFeatureCollection> collections) {
        this.collections = collections;
    }

    public CompositeFeatureCollection(
            List<SimpleFeatureCollection> collections, SimpleFeatureType schema) {
        this.collections = collections;
        this.schema = schema;
    }

    protected Iterator<SimpleFeature> openIterator() throws IOException {
        return new CompositeIterator();
    }

    public SimpleFeatureType getSchema() {
        return schema;
    }

    public ReferencedEnvelope getBounds() {
        // crazy, this same mapper inlined in the stream does not compile...
        Function<SimpleFeatureCollection, ReferencedEnvelope> mapper =
                c -> {
                    final ReferencedEnvelope envelope = c.getBounds();
                    if (envelope == null) {
                        return DataUtilities.bounds(c);
                    } else {
                        return envelope;
                    }
                };
        return collections
                .stream()
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

    public int getCount() throws IOException {
        return collections
                .stream()
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

    class CompositeIterator implements CloseableIterator<SimpleFeature> {
        int index;
        SimpleFeatureIterator iterator;

        public CompositeIterator() {
            index = 0;
        }

        public void remove() {}

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

        public SimpleFeature next() {
            return iterator.next();
        }

        @Override
        public void close() throws IOException {
            if (iterator instanceof Closeable) {
                iterator.close();
            }
        }
    }

    public <T> T[] toArray(T[] array) {
        List<SimpleFeature> list = new ArrayList<>();

        Iterator<SimpleFeatureCollection> it = collections.iterator();
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

    public FeatureId getIdentifier() {
        throw new RuntimeException(
                "Can't get the id for a composite featurecollection; you need to identify the consituent collections directly.");
    }

    /** @return the collections */
    public List<SimpleFeatureCollection> getCollections() {
        return collections;
    }
}

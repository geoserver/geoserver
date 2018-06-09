/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.geotools.data.DataUtilities;
import org.geotools.data.store.DataFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.identity.FeatureId;

/**
 * Wraps multiple feature collections into a single.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class CompositeFeatureCollection extends DataFeatureCollection {
    /** wrapped collecitons */
    List<FeatureCollection> collections;

    public CompositeFeatureCollection(List collections) {
        this.collections = collections;
    }

    protected Iterator openIterator() throws IOException {
        return new CompositeIterator();
    }

    public SimpleFeatureType getSchema() {
        return null;
    }

    public ReferencedEnvelope getBounds() {
        return DataUtilities.bounds(this);
    }

    public int getCount() throws IOException {
        int count = 0;
        Iterator i = iterator();

        try {
            while (i.hasNext()) {
                i.next();
                count++;
            }
        } finally {
            close(i);
        }

        return count;
    }

    class CompositeIterator implements Iterator {
        int index;
        FeatureIterator iterator;

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

        public Object next() {
            return iterator.next();
        }
    }

    public Object[] toArray(Object[] arg0) {
        List list = new ArrayList();

        Iterator it = collections.iterator();
        while (it.hasNext()) {
            FeatureCollection col = (FeatureCollection) it.next();
            FeatureIterator it2 = col.features();
            while (it2.hasNext()) {
                list.add(it.next());
            }
            it2.close();
        }

        return list.toArray(arg0);
    }

    public FeatureId getIdentifier() {
        throw new RuntimeException(
                "Can't get the id for a composite featurecollection; you need to identify the consituent collections directly.");
    }

    /** @return the collections */
    public List<FeatureCollection> getCollections() {
        return collections;
    }
}

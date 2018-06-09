/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature;

import java.io.Closeable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * A feature iterator wrapping N feature collections with the same feature type
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CompositeIterator implements Iterator<Feature>, Closeable {

    FeatureCollection<FeatureType, Feature> currentCollection;

    FeatureIterator<Feature> current;

    private List<FeatureCollection<FeatureType, Feature>> collections;

    public CompositeIterator(List<FeatureCollection<FeatureType, Feature>> collections) {
        this.collections = collections;
        this.currentCollection = collections.remove(0);
        this.current = currentCollection.features();
    }

    @Override
    public boolean hasNext() {
        while (!current.hasNext() && !collections.isEmpty()) {
            current.close();
            this.currentCollection = collections.remove(0);
            this.current = currentCollection.features();
        }

        return false;
    }

    @Override
    public Feature next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        return current.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public void close() {
        current.close();
        collections.clear();
    }
}

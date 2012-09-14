/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.feature;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

/**
 * A feature iterator wrapping N feature collections with the same feature type
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CompositeIterator implements Iterator<Feature> {

    FeatureCollection currentCollection;

    Iterator<Feature> current;

    private List<FeatureCollection> collections;

    public CompositeIterator(List<FeatureCollection> collections) {
        this.collections = collections;
        this.currentCollection = collections.remove(0);
        this.current = currentCollection.iterator();
    }

    @Override
    public boolean hasNext() {
        while (!current.hasNext() && !collections.isEmpty()) {
            currentCollection.close(current);
            this.currentCollection = collections.remove(0);
            this.current = currentCollection.iterator();
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
        if(currentCollection != null) {
            currentCollection.close(current);
        }
        collections.clear();
    }

}

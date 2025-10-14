/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureIterator;

/**
 * Simple Feature iterators are read only by design, but just to make extra sure there are no write enabled subclasses
 * floating around we make the wrapping anyways,
 *
 * @author Josh Vote, CSIRO Earth Science and Resource Engineering
 */
public class SecuredSimpleFeatureIterator implements SimpleFeatureIterator, Iterator<SimpleFeature> {

    SimpleFeatureIterator wrapped;

    public SecuredSimpleFeatureIterator(SimpleFeatureIterator wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void close() {
        wrapped.close();
    }

    @Override
    public boolean hasNext() {
        return wrapped.hasNext();
    }

    @Override
    public SimpleFeature next() throws NoSuchElementException {
        return wrapped.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

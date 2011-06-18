package org.geoserver.security.decorators;

import java.util.NoSuchElementException;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Simple Feature iterators are read only by design, but just to make extra sure there
 * are no write enabled subclasses floating around we make the wrapping anyways,
 * 
 * 
 * @author Josh Vote, CSIRO Earth Science and Resource Engineering
 */
public class SecuredSimpleFeatureIterator implements SimpleFeatureIterator {

    SimpleFeatureIterator wrapped;

    public SecuredSimpleFeatureIterator(SimpleFeatureIterator wrapped) {
        this.wrapped = wrapped;
    }

    public void close() {
        wrapped.close();
    }

    public boolean hasNext() {
        return wrapped.hasNext();
    }

    public SimpleFeature next() throws NoSuchElementException {
        return wrapped.next();
    }

}

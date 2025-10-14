/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import java.util.NoSuchElementException;
import org.geotools.api.feature.Feature;
import org.geotools.feature.FeatureIterator;

/**
 * A feature iterator allowing to push back one feature (will be used to map the results of a join)
 *
 * @author Andrea Aime - GeoSolutions
 */
class PushbackFeatureIterator<F extends Feature> implements FeatureIterator<F> {

    FeatureIterator<F> delegate;

    F last;

    F current;

    public PushbackFeatureIterator(FeatureIterator<F> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        return current != null || delegate.hasNext();
    }

    @Override
    public F next() throws NoSuchElementException {
        if (current != null) {
            last = current;
            current = null;
        } else {
            last = delegate.next();
        }

        return last;
    }

    /**
     * Pushes back the last feature returned by next(). Will throw an {@link IllegalStateException} if there is no
     * feature to push back. Only a single pushBack call can be performed between two calls to next()
     */
    public void pushBack() {
        if (last != null) {
            current = last;
            last = null;
        } else {
            throw new IllegalStateException("There is no feature to push back");
        }
    }

    @Override
    public void close() {
        delegate.close();
    }
}

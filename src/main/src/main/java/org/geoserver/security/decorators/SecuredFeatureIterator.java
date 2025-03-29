/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.NoSuchElementException;
import org.geotools.api.feature.Feature;
import org.geotools.feature.FeatureIterator;

/**
 * Feature iterators are read only by design, but just to make extra sure there are no write enabled subclasses floating
 * around we make the wrapping anyways, this will make instanceof
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredFeatureIterator<F extends Feature> implements FeatureIterator<F> {

    FeatureIterator<F> wrapped;

    public SecuredFeatureIterator(FeatureIterator<F> wrapped) {
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
    public F next() throws NoSuchElementException {
        return wrapped.next();
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.NoSuchElementException;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;

/**
 * Feature iterators are read only by design, but just to make extra sure there are no write enabled
 * subclasses floating around we make the wrapping anyways, this will make instanceof
 *
 * @author Andrea Aime - GeoSolutions
 */
public class SecuredFeatureIterator implements FeatureIterator {

    FeatureIterator wrapped;

    public SecuredFeatureIterator(FeatureIterator wrapped) {
        this.wrapped = wrapped;
    }

    public void close() {
        wrapped.close();
    }

    public boolean hasNext() {
        return wrapped.hasNext();
    }

    public Feature next() throws NoSuchElementException {
        return wrapped.next();
    }
}

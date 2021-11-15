/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.timeout;

import java.util.NoSuchElementException;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

class TimeoutFeatureIterator<F extends Feature> implements FeatureIterator<F> {

    static class SimpleTimeoutFeatureIterator extends TimeoutFeatureIterator<SimpleFeature>
            implements SimpleFeatureIterator {

        public SimpleTimeoutFeatureIterator(
                TimeoutVerifier timeoutVerifier, FeatureIterator<SimpleFeature> delegate) {
            super(timeoutVerifier, delegate);
        }
    }

    TimeoutVerifier timeoutVerifier;

    FeatureIterator<F> delegate;

    public TimeoutFeatureIterator(TimeoutVerifier timeoutVerifier, FeatureIterator<F> delegate) {
        this.timeoutVerifier = timeoutVerifier;
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        timeoutVerifier.checkTimeout();
        return delegate.hasNext();
    }

    @Override
    public F next() throws NoSuchElementException {
        timeoutVerifier.checkTimeout();
        return delegate.next();
    }

    @Override
    public void close() {
        // do not check timeout on close, we are done already...
        delegate.close();
    }
}

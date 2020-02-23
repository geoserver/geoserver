/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg.timeout;

import java.io.IOException;
import java.util.Collection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

/**
 * A {@link FeatureCollection} decorator that checks if the timeout expired, and throws an exception
 * in case it is
 *
 * @param <T>
 * @param <F>
 */
class TimeoutFeatureCollection<T extends FeatureType, F extends Feature>
        implements FeatureCollection<T, F> {

    /**
     * Wraps a feature collection into a timing out decorator, keeping its {@link SimpleFeature}
     * nature if possible
     *
     * @param <R>
     */
    public static FeatureCollection wrap(TimeoutVerifier timeoutVerifier, FeatureCollection fc) {
        if (fc instanceof SimpleFeatureCollection) {
            return new SimpleTimeoutCollection(timeoutVerifier, fc);
        } else {
            return new TimeoutFeatureCollection<>(timeoutVerifier, fc);
        }
    }

    /** Simple feature version of {@link TimeoutFeatureCollection} */
    static class SimpleTimeoutCollection
            extends TimeoutFeatureCollection<SimpleFeatureType, SimpleFeature>
            implements SimpleFeatureCollection {

        public SimpleTimeoutCollection(
                TimeoutVerifier timeoutVerifier,
                FeatureCollection<SimpleFeatureType, SimpleFeature> delegate) {
            super(timeoutVerifier, delegate);
        }

        @Override
        public SimpleFeatureIterator features() {
            return new TimeoutFeatureIterator.SimpleTimeoutFeatureIterator(
                    timeoutVerifier, super.features());
        }

        @Override
        public SimpleFeatureCollection subCollection(Filter filter) {
            timeoutVerifier.checkTimeout();
            return new SimpleTimeoutCollection(timeoutVerifier, delegate.subCollection(filter));
        }

        @Override
        public SimpleFeatureCollection sort(SortBy order) {
            return new SimpleTimeoutCollection(timeoutVerifier, delegate.sort(order));
        }
    }

    TimeoutVerifier timeoutVerifier;
    FeatureCollection<T, F> delegate;

    public TimeoutFeatureCollection(
            TimeoutVerifier timeoutVerifier, FeatureCollection<T, F> delegate) {
        this.timeoutVerifier = timeoutVerifier;
        this.delegate = delegate;
    }

    // Timeout delegate creating methods

    @Override
    public FeatureIterator<F> features() {
        timeoutVerifier.checkTimeout();
        return new TimeoutFeatureIterator<>(timeoutVerifier, delegate.features());
    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        timeoutVerifier.checkTimeout();
        TimeoutFeatureVisitor timeoutVisitor = new TimeoutFeatureVisitor(timeoutVerifier, visitor);
        delegate.accepts(timeoutVisitor, progress);
    }

    @Override
    public FeatureCollection<T, F> subCollection(Filter filter) {
        timeoutVerifier.checkTimeout();
        return new TimeoutFeatureCollection<>(timeoutVerifier, delegate.subCollection(filter));
    }

    @Override
    public FeatureCollection<T, F> sort(SortBy order) {
        timeoutVerifier.checkTimeout();
        return new TimeoutFeatureCollection<>(timeoutVerifier, delegate.sort(order));
    }

    // Simple check and delegate methods

    @Override
    public T getSchema() {
        timeoutVerifier.checkTimeout();
        return delegate.getSchema();
    }

    @Override
    public String getID() {
        timeoutVerifier.checkTimeout();
        return delegate.getID();
    }

    @Override
    public ReferencedEnvelope getBounds() {
        timeoutVerifier.checkTimeout();
        return delegate.getBounds();
    }

    @Override
    public boolean contains(Object o) {
        timeoutVerifier.checkTimeout();
        return delegate.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> o) {
        timeoutVerifier.checkTimeout();
        return delegate.containsAll(o);
    }

    @Override
    public boolean isEmpty() {
        timeoutVerifier.checkTimeout();
        return delegate.isEmpty();
    }

    @Override
    public int size() {
        timeoutVerifier.checkTimeout();
        return delegate.size();
    }

    @Override
    public Object[] toArray() {
        timeoutVerifier.checkTimeout();
        return delegate.toArray();
    }

    @Override
    public <O> O[] toArray(O[] a) {
        timeoutVerifier.checkTimeout();
        return delegate.toArray(a);
    }
}

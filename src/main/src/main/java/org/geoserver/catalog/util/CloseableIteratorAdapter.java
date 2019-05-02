/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.util;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.io.Closeables;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;

public class CloseableIteratorAdapter<T> implements CloseableIterator<T> {

    private static final Logger LOGGER = Logging.getLogger(CloseableIteratorAdapter.class);

    protected final Iterator<T> wrapped;

    protected Closeable whatToClose;

    public CloseableIteratorAdapter(Iterator<T> wrapped) {
        this.wrapped = wrapped;
        if (wrapped instanceof Closeable) {
            this.whatToClose = (Closeable) wrapped;
        } else {
            this.whatToClose = null;
        }
    }

    public CloseableIteratorAdapter(Iterator<T> filteredNotCloseable, Closeable closeMe) {
        this.wrapped = filteredNotCloseable;
        this.whatToClose = closeMe;
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = wrapped.hasNext();
        if (!hasNext) {
            // auto close
            close();
        }
        return hasNext;
    }

    @Override
    public T next() {
        return wrapped.next();
    }

    @Override
    public void remove() {
        wrapped.remove();
    }

    /**
     * Closes the wrapped iterator if its an instance of {@code CloseableIterator}, does nothing
     * otherwise; override if needed.
     *
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() {
        try {
            Closeables.close(whatToClose, false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            whatToClose = null;
        }
    }

    @Override
    @SuppressWarnings("deprecation") // finalize is deprecated in Java 9
    protected void finalize() {
        if (whatToClose != null) {
            try {
                close();
            } finally {
                LOGGER.warning(
                        "There is code not closing CloseableIterator!!! Auto closing at finalize().");
            }
        }
    }

    public static <T> CloseableIterator<T> filter(final Iterator<T> iterator, final Filter filter) {

        Predicate<T> predicate = filterAdapter(filter);
        return filter(iterator, predicate);
    }

    public static <T> CloseableIterator<T> filter(
            final Iterator<T> iterator, final Predicate<T> predicate) {

        UnmodifiableIterator<T> filteredNotCloseable = Iterators.filter(iterator, predicate);
        Closeable closeable = iterator instanceof Closeable ? (Closeable) iterator : null;

        return new CloseableIteratorAdapter<T>(filteredNotCloseable, closeable);
    }

    public static <F, T> CloseableIterator<T> transform(
            Iterator<F> iterator, Function<? super F, ? extends T> function) {

        Iterator<T> transformedNotCloseable = Iterators.transform(iterator, function);
        Closeable closeable = (Closeable) (iterator instanceof CloseableIterator ? iterator : null);

        return new CloseableIteratorAdapter<T>(transformedNotCloseable, closeable);
    }

    public static <T> CloseableIterator<T> limit(final Iterator<T> iterator, int maxElements) {

        Iterator<T> limitedNotCloseable = Iterators.limit(iterator, maxElements);
        Closeable closeable = iterator instanceof Closeable ? (Closeable) iterator : null;

        return new CloseableIteratorAdapter<T>(limitedNotCloseable, closeable);
    }

    public static void close(Iterator<?> iterator) {
        if (iterator instanceof Closeable) {
            try {
                Closeables.close((Closeable) iterator, false);
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Ignoring exception on CloseableIteratorAdapter.close()", e);
            }
        }
    }

    public static <T> CloseableIterator<T> empty() {
        Iterator<T> empty = Collections.emptyIterator();
        return new CloseableIteratorAdapter<T>(empty);
    }

    private static <T> com.google.common.base.Predicate<T> filterAdapter(
            final Filter catalogPredicate) {

        return new com.google.common.base.Predicate<T>() {

            @Override
            public boolean apply(T input) {
                return catalogPredicate.evaluate(input);
            }
        };
    }
}

/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector.iterator;

/** A {@link VTIterator} wrapper that can push back into iteration the last feature returned */
public class PushBackVTIterator implements VTIterator {
    VTFeature last;
    VTFeature current;
    VTIterator delegate;

    public PushBackVTIterator(VTIterator delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        return current != null || delegate.hasNext();
    }

    @Override
    public VTFeature next() {
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

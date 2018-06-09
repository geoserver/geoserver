/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.Serializable;
import org.geoserver.catalog.Wrapper;

/**
 * Generic delegating base class. Provides the follwing features:
 *
 * <ul>
 *   <li>null check for the delegate object
 *   <li>direct forwarding of {@link #equals(Object)}, {@link #hashCode()} and {@link #toString()}
 *       to the delegate
 *   <li>implements the Wrapper interface for programmatic extraction
 * </ul>
 */
public abstract class AbstractDecorator<D> implements Wrapper, Serializable {
    protected D delegate;

    public AbstractDecorator(D delegate) {
        if (delegate == null) throw new NullPointerException("Cannot delegate to a null object");
        this.delegate = delegate;
    }

    public boolean isWrapperFor(Class<?> iface) {
        // first drill down to the latest wrapper, then check if the last delegate actually
        // implements the required interface
        if (delegate instanceof Wrapper) return ((Wrapper) delegate).isWrapperFor(iface);
        else if (iface.isInstance(delegate)) return true;
        else return false;
    }

    public <T> T unwrap(Class<T> iface) throws IllegalArgumentException {
        // first drill down to the latest wrapper, then check if the last delegate actually
        // implements the required interface and return it
        if (delegate instanceof Wrapper) return ((Wrapper) delegate).unwrap(iface);
        else if (iface.isInstance(delegate)) return (T) delegate;
        else
            throw new IllegalArgumentException("Cannot unwrap to the requested interface " + iface);
    }

    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName())
                .append('[')
                .append(delegate)
                .append(']')
                .toString();
    }
}

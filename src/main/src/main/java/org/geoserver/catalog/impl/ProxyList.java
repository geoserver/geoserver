/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.AbstractList;
import java.util.List;

/**
 * An unmodifiable list proxy in which each element in the list is wrapped in a proxy of its own.
 *
 * <p>Subclasses should implement {@link #createProxy(Object, Class)}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class ProxyList<T> extends AbstractList<T> {

    protected List<T> proxyList;
    protected Class<T> proxyInterface;

    public ProxyList(List<T> proxyList, Class<T> proxyInterface) {
        this.proxyList = proxyList;
        this.proxyInterface = proxyInterface;
    }

    @Override
    public T get(int index) {
        T proxyObject = proxyList.get(index);
        return createProxy(proxyObject, proxyInterface);
    }

    @Override
    public T set(int index, T element) {
        throw new IllegalArgumentException("Object is not a proxy, or not a proxy of the correct type");
    }

    @Override
    public int size() {
        return proxyList.size();
    }

    /** Wraps an object from the underlying list in the proxy. */
    protected abstract <T> T createProxy(T proxyObject, Class<T> proxyInterface);

    /**
     * Unwraps a proxy object or insertion into the underlying list.
     *
     * <p>Note: This method should handle the case of the object not being a proxy instance, but an regular instance of
     * proxyInterface.
     */
    protected abstract <T> T unwrapProxy(T proxy, Class<T> proxyInterface);
}

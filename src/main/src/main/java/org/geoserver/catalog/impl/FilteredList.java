/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.AbstractList;
import java.util.List;

/**
 * Allows exposing a filtered list of items, but keeping in synch the underlying full list on
 * modification
 *
 * @author Niels Charlier (grabbed from SecuredLayerGroupInfo.getLayers() inner class)
 */
public class FilteredList<T> extends AbstractList<T> {

    List<T> filtered;

    List<T> delegate;

    public FilteredList(List<T> filtered, List<T> delegate) {
        this.filtered = filtered;
        this.delegate = delegate;
    }

    @Override
    public T get(int index) {
        return filtered.get(index);
    }

    @Override
    public int size() {
        return filtered.size();
    }

    @Override
    public void add(int index, T element) {
        delegate.add(index, unwrap(element));
        filtered.add(index, element);
    }

    public T set(int index, T element) {
        delegate.set(index, unwrap(element));
        return filtered.set(index, element);
    }

    public T remove(int index) {
        delegate.remove(index);
        return filtered.remove(index);
    }

    @Override
    public boolean remove(Object o) {
        delegate.remove(o);
        return filtered.remove(o);
    }

    /** Allows to unwrap an element before setting it into the delegate list */
    protected T unwrap(T element) {
        return element;
    }
}

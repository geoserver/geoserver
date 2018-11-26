/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml.iterator;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import org.springframework.util.CompositeIterator;

/**
 * A simple way to compose to lists into one. Will only work as a iterator source, it does not
 * really implement the {@link List} basic functionalities
 *
 * @author Andrea Aime - GeoSolutions
 * @param <T>
 */
public class CompositeList<T> extends AbstractList<T> {

    List<T>[] lists;

    public CompositeList(List<T>... lists) {
        this.lists = lists;
    }

    @Override
    public Iterator iterator() {
        CompositeIterator<T> cit = new CompositeIterator<T>();
        for (List<T> list : lists) {
            cit.add(list.iterator());
        }

        return cit;
    }

    @Override
    public T get(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return -1;
    }
}

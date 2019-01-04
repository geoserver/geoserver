/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.SortedSet;
import java.util.TreeMap;

public class LookupServiceImpl<T extends Named> implements LookupService<T> {

    private NavigableMap<String, T> map = new TreeMap<String, T>();

    @Override
    public T get(String name) {
        return map.get(name);
    }

    protected void setNamed(List<T> list) {
        for (T o : list) {
            map.put(o.getName(), o);
        }
    }

    @Override
    public SortedSet<String> names() {
        return map.navigableKeySet();
    }

    @Override
    public Collection<T> all() {
        return map.values();
    }

    @Override
    public <S extends T> S get(String name, Class<S> clazz) {
        T object = map.get(name);
        if (clazz.isInstance(object)) {
            return clazz.cast(object);
        } else {
            return null;
        }
    }
}

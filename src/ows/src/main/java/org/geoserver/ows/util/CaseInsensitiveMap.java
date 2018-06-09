/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Map decorator which makes String keys case-insensitive.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class CaseInsensitiveMap implements Map {
    Map delegate = new TreeMap();

    public CaseInsensitiveMap(Map delegate) {
        putAll(delegate);
    }

    public void clear() {
        delegate.clear();
    }

    public boolean containsKey(Object key) {
        return delegate.containsKey(upper(key));
    }

    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    public Set entrySet() {
        return delegate.entrySet();
    }

    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    public Object get(Object key) {
        return delegate.get(upper(key));
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public Set keySet() {
        return delegate.keySet();
    }

    public Object put(Object key, Object value) {
        return delegate.put(upper(key), value);
    }

    public void putAll(Map t) {
        // make sure to upcase all keys
        for (Object entry : t.entrySet()) {
            Object key = ((Entry) entry).getKey();
            Object value = ((Entry) entry).getValue();
            put(key, value);
        }
    }

    public Object remove(Object key) {
        return delegate.remove(upper(key));
    }

    public int size() {
        return delegate.size();
    }

    public Collection values() {
        return delegate.values();
    }

    Object upper(Object key) {
        if ((key != null) && key instanceof String) {
            return ((String) key).toUpperCase();
        }

        return key;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    /**
     * Wraps a map in case insensitive one.
     *
     * <p>If the instance is already a case insensitive map it is returned as is.
     */
    public static Map wrap(Map other) {
        if (other instanceof CaseInsensitiveMap) {
            return other;
        }
        return new CaseInsensitiveMap(other);
    }
}

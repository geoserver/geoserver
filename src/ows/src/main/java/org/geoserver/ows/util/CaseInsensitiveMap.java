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
// Implementation note, the "K extends String" bit is weird, but fixing String would have meant
// to have a single parameter map, I've found it to be even more strange to go and declare
// CaseInsensitiveMap<Object> in the user code
public class CaseInsensitiveMap<K extends String, V> implements Map<K, V> {
    Map<K, V> delegate = new TreeMap<>();

    public CaseInsensitiveMap(Map<K, V> delegate) {
        putAll(delegate);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(upper(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public V get(Object key) {
        return delegate.get(upper(key));
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public V put(K key, V value) {
        return delegate.put(upper(key), value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> t) {
        // make sure to upcase all keys
        for (Entry<? extends K, ? extends V> entry : t.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            put(key, value);
        }
    }

    @Override
    public V remove(Object key) {
        return delegate.remove(upper(key));
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @SuppressWarnings("unchecked")
    <T> T upper(T key) {
        if ((key != null) && key instanceof String) {
            return (T) ((String) key).toUpperCase();
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
    public static <V> Map<String, V> wrap(Map<String, V> other) {
        if (other instanceof CaseInsensitiveMap) {
            return other;
        }
        return new CaseInsensitiveMap<>(other);
    }
}

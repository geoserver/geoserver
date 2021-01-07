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
import org.checkerframework.checker.units.qual.K;

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

    public void clear() {
        delegate.clear();
    }

    public boolean containsKey(Object key) {
        return delegate.containsKey(upper(key));
    }

    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }

    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    public V get(Object key) {
        return delegate.get(upper(key));
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public Set<K> keySet() {
        return delegate.keySet();
    }

    public V put(K key, V value) {
        return delegate.put(upper(key), value);
    }

    public void putAll(Map<? extends K, ? extends V> t) {
        // make sure to upcase all keys
        for (Entry<? extends K, ? extends V> entry : t.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            put(key, value);
        }
    }

    public V remove(Object key) {
        return delegate.remove(upper(key));
    }

    public int size() {
        return delegate.size();
    }

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

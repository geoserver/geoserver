/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Map which makes keys case insensitive.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
// Implementation note, the "K extends String" bit is weird, but fixing String would have meant
// to have a single parameter map, I've found it to be even more strange to go and declare
// KvpMap<Object> in the user code
public class KvpMap<K extends String, V> extends HashMap<K, V> {

    private static final long serialVersionUID = 1L;

    public KvpMap() {
        super();
    }

    public KvpMap(Map<K, V> other) {
        this();
        for (Entry<K, V> entry : other.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(upper(key));
    }

    @Override
    public V get(Object key) {
        return super.get(upper(key));
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return super.getOrDefault(upper(key), defaultValue);
    }

    @Override
    public V put(K key, V value) {
        return super.put(upper(key), value);
    }

    @SuppressWarnings("unchecked")
    K upper(Object key) {
        if ((key != null) && key instanceof String) {
            return (K) ((String) key).toUpperCase();
        }

        return null;
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Map which makes keys case insensitive.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class KvpMap<V> extends HashMap<String, V> {

    private static final long serialVersionUID = 1L;

    public KvpMap() {
        super();
    }

    public KvpMap(Map<String, V> other) {
        this();
        for (Iterator<Entry<String, V>> e = other.entrySet().iterator(); e.hasNext(); ) {
            Map.Entry<String, V> entry = e.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public boolean containsKey(Object key) {
        return super.containsKey(upper(key));
    }

    public V get(Object key) {
        return super.get(upper(key));
    }

    public V getOrDefault(Object key, V defaultValue) {
        return super.getOrDefault(upper(key), defaultValue);
    }

    public V put(String key, V value) {
        return super.put(upper(key), value);
    }

    String upper(Object key) {
        if ((key != null) && key instanceof String) {
            return ((String) key).toUpperCase();
        }

        return null;
    }
}

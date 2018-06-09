/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class implementing a LRU (last recently used) cache based on {@link LinkedHashMap}
 *
 * @author christian
 * @param <K>
 * @param <V>
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private static final long serialVersionUID = 1L;

    private final int capacity;
    private long accessCount = 0;
    private long hitCount = 0;

    public LRUCache(int capacity) {
        super(capacity + 1, 1.1f, true);
        this.capacity = capacity;
    }

    @Override
    public V get(Object key) {
        accessCount++;
        if (containsKey(key)) {
            hitCount++;
        }
        return super.get(key);
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

    public long getAccessCount() {
        return accessCount;
    }

    public long getHitCount() {
        return hitCount;
    }
}

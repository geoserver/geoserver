/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Base class encapsulating common behavior for properties implementations that control in-memory ordering and
 * write-time sorting.
 */
public abstract class AbstractSortedProperties extends Properties {

    private static final long serialVersionUID = 1L;

    protected final Map<Object, Object> linkMap;

    protected AbstractSortedProperties(Map<Object, Object> backing) {
        super();
        this.linkMap = backing;
    }

    protected AbstractSortedProperties(Map<Object, Object> backing, Properties defaults) {
        super(defaults);
        this.linkMap = backing;
        if (defaults != null) {
            this.linkMap.putAll(defaults);
        }
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        return linkMap.put(key, value);
    }

    @Override
    public synchronized void putAll(Map<? extends Object, ? extends Object> t) {
        linkMap.putAll(t);
    }

    @Override
    public synchronized Object setProperty(String key, String value) {
        return linkMap.put(key, value);
    }

    @Override
    public synchronized Object get(Object key) {
        return linkMap.get(key);
    }

    @Override
    public synchronized String getProperty(String key) {
        return (String) linkMap.get(key);
    }

    @Override
    public synchronized String getProperty(String key, String defaultValue) {
        return (String) (linkMap.containsKey(key) ? linkMap.get(key) : defaultValue);
    }

    @Override
    public synchronized boolean contains(Object value) {
        return linkMap.containsValue(value);
    }

    @Override
    public boolean containsValue(Object value) {
        return linkMap.containsValue(value);
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(linkMap.keySet());
    }

    @Override
    public synchronized Enumeration<Object> elements() {
        return Collections.enumeration(linkMap.values());
    }

    @Override
    public Set<Object> keySet() {
        return linkMap.keySet();
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return linkMap.entrySet();
    }

    @Override
    public synchronized void clear() {
        linkMap.clear();
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return linkMap.containsKey(key);
    }

    @Override
    public synchronized int size() {
        return linkMap.size();
    }

    @Override
    public synchronized String toString() {
        return linkMap.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof java.util.Properties props)) return false;
        // compare as property key/value pairs regardless of concrete implementation
        if (props.size() != this.size()) return false;
        for (Map.Entry<Object, Object> e : linkMap.entrySet()) {
            Object key = e.getKey();
            Object val = e.getValue();
            Object otherVal = props.get(key);
            if (!java.util.Objects.equals(val, otherVal)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return linkMap.hashCode();
    }
}

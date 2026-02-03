/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * A Properties subclass that maintains insertion order using a {@link java.util.LinkedHashMap}. LinkedProperties is
 * insertion-order in-memory and will write properties in insertion order when stored. Use {@link SortedProperties} when
 * deterministic alphabetical ordering is required (written alphabetically).
 *
 * <p>Examples:
 *
 * <pre>
 * // Insertion-order: LinkedProperties
 * LinkedProperties props = new LinkedProperties();
 * props.setProperty("zebra", "last");
 * props.setProperty("alpha", "first");
 * props.store(out, null);  // Writes: zebra=last, alpha=first (in insertion order)
 *
 * // Alphabetical: SortedProperties
 * SortedProperties sorted = new SortedProperties();
 * sorted.setProperty("zebra", "last");
 * sorted.setProperty("alpha", "first");
 * sorted.store(out, null);  // Writes: alpha=first, zebra=last (alphabetical)
 * </pre>
 */
public class LinkedProperties extends AbstractSortedProperties {

    private static final long serialVersionUID = 1L;

    public LinkedProperties() {
        super(new LinkedHashMap<>());
    }

    public LinkedProperties(Properties defaults) {
        super(new LinkedHashMap<>(), defaults);
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
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}

/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * A Properties subclass that maintains insertion order using a LinkedHashMap. By default, properties are sorted
 * alphabetically when stored to make diffs easier. Call {@link #preserveOrder()} before {@link #store(OutputStream,
 * String)} or {@link #store(Writer, String)} to maintain insertion order instead.
 *
 * <p>Examples:
 *
 * <pre>
 * // Default: sorted alphabetically
 * LinkedProperties props = new LinkedProperties();
 * props.setProperty("zebra", "last");
 * props.setProperty("alpha", "first");
 * props.store(out, null);  // Writes: alpha=first, zebra=last
 *
 * // Explicit: preserve insertion order
 * LinkedProperties props = new LinkedProperties();
 * props.setProperty("/**", "ADMIN");
 * props.setProperty("/public/**", "ANONYMOUS");
 * props.preserveOrder().store(out, null);  // Writes in insertion order
 * </pre>
 */
public class LinkedProperties extends Properties {

    private static final long serialVersionUID = 1L;

    private Map<Object, Object> linkMap = new LinkedHashMap<>();
    private volatile boolean sortOnStore = true; // Default: sort alphabetically

    public LinkedProperties() {}

    public LinkedProperties(Properties defaults) {
        super(defaults);
        if (defaults != null) {
            this.linkMap.putAll(defaults);
        }
    }

    /**
     * Mark this LinkedProperties to preserve insertion order when stored. Call this before store() to maintain the
     * order properties were added. Use this for properties files where order has semantic meaning (e.g., security
     * rules).
     *
     * @return this instance for method chaining
     */
    public LinkedProperties preserveOrder() {
        this.sortOnStore = false;
        return this;
    }

    @Override
    public void store(OutputStream out, String comments) throws IOException {
        if (sortOnStore) {
            // Sort alphabetically before storing
            LinkedProperties sorted = new LinkedProperties();
            sorted.sortOnStore = false; // Don't re-sort
            TreeSet<String> keys = new TreeSet<>(this.stringPropertyNames());
            for (String key : keys) {
                sorted.setProperty(key, this.getProperty(key));
            }
            sorted.store(out, comments);
        } else {
            // Store in insertion order (LinkedHashMap iteration order)
            super.store(out, comments);
        }
    }

    @Override
    public void store(Writer writer, String comments) throws IOException {
        if (sortOnStore) {
            LinkedProperties sorted = new LinkedProperties();
            sorted.sortOnStore = false;
            TreeSet<String> keys = new TreeSet<>(this.stringPropertyNames());
            for (String key : keys) {
                sorted.setProperty(key, this.getProperty(key));
            }
            sorted.store(writer, comments);
        } else {
            super.store(writer, comments);
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
        if (o == null || getClass() != o.getClass()) return false;
        LinkedProperties that = (LinkedProperties) o;
        return Objects.equals(linkMap, that.linkMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), linkMap);
    }
}

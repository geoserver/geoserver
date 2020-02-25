/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;

/**
 * A simple class to support reloadable property files. Watches last modified date on the specified
 * file, and allows to read a Properties out of it.
 *
 * @author Andrea Aime
 */
public class PropertyFileWatcher extends FileWatcher<Properties> {
    public PropertyFileWatcher(Resource resource) {
        super(resource);
    }

    /**
     * Read properties from file.
     *
     * @return properties from file, or null if file does not exist yet
     */
    public Properties getProperties() throws IOException {
        return read();
    }

    @Override
    protected Properties parseFileContents(InputStream in) throws IOException {
        Properties p = new LinkedProperties();
        p.load(in);
        return p;
    }

    public boolean isStale() {
        return isModified();
    }

    /**
     * Subclass of Properties that maintains order by actually storing keys in an underlying
     * LinkedHashMap.
     */
    public static class LinkedProperties extends Properties {

        private static final long serialVersionUID = 1L;

        private Map<Object, Object> linkMap = new LinkedHashMap<Object, Object>();

        public LinkedProperties() {}

        public LinkedProperties(Properties defaults) {
            super(defaults);
            this.linkMap.putAll(defaults);
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
}

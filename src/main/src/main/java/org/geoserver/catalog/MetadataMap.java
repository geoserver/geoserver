/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.geotools.util.Converters;

/**
 * A map used to store metadata on catalog and configuration objects.
 *
 * <p>This map is used to convert values from the map to a specified type, via the {@link
 * #get(String, Class)} method. Usage:
 *
 * <pre>
 * MetadataMap map = new MetadataMap();
 * map.put( "one", "1");
 *
 * map.get( "one" ) ==> "1"
 * map.get( "one", Integer.class) ==> 1
 * map.get( "one", Double.class ) ==> 1.0
 * </pre>
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MetadataMap implements Map<String, Serializable>, Serializable {

    private static final long serialVersionUID = -3267986531863264568L;
    protected String id;

    protected Map<String, Serializable> map;

    public MetadataMap() {
        this(new HashMap());
    }

    public MetadataMap(Map<String, Serializable> map) {
        if (!(map instanceof Serializable)) {
            throw new IllegalArgumentException("map is not serializable");
        }
        this.map = map;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Serializable> getMap() {
        return map;
    }

    public void setMap(Map<String, Serializable> map) {
        this.map = map;
    }

    public <T> T get(String key, Class<T> clazz) {
        Object obj = get(key);
        if (obj == null) {
            return null;
        }

        return Converters.convert(obj, clazz);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    public Serializable get(Object key) {
        return map.get(key);
    }

    public Serializable put(String key, Serializable value) {
        return map.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends Serializable> t) {
        map.putAll(t);
    }

    public Serializable remove(Object key) {
        return map.remove(key);
    }

    public void clear() {
        map.clear();
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public Collection<Serializable> values() {
        return map.values();
    }

    public Set<java.util.Map.Entry<String, Serializable>> entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object o) {
        return map.equals(o);
    }

    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return map.toString();
    }
}

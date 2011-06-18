/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


/**
 * Map decorator which makes String keys case-insensitive.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class CaseInsensitiveMap implements Map {
    Map delegate;

    public CaseInsensitiveMap(Map delegate) {
        this.delegate = delegate;
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

    public Set entrySet() {
        return delegate.entrySet();
    }

    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    public Object get(Object key) {
        return delegate.get(upper(key));
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public Set keySet() {
        return delegate.keySet();
    }

    public Object put(Object key, Object value) {
        return delegate.put(upper(key), value);
    }

    public void putAll(Map t) {
        delegate.putAll(t);
    }

    public Object remove(Object key) {
        return delegate.remove(upper(key));
    }

    public int size() {
        return delegate.size();
    }

    public Collection values() {
        return delegate.values();
    }

    Object upper(Object key) {
        if ((key != null) && key instanceof String) {
            return ((String) key).toUpperCase();
        }

        return key;
    }
    
    @Override
    public String toString() {
        return delegate.toString();
    }
}

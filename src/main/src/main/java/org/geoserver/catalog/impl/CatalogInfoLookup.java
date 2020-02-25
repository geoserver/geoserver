/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;

/**
 * A support index for {@link DefaultCatalogFacade}, can perform fast lookups of {@link CatalogInfo}
 * objects by id or by "name", where the name is defined by a a user provided mapping function.
 *
 * <p>The lookups by predicate have been tested and optimized for performance, in particular the
 * current for loops turned out to be significantly faster than building and returning streams
 *
 * @param <T>
 */
class CatalogInfoLookup<T extends CatalogInfo> {
    static final Logger LOGGER = Logging.getLogger(CatalogInfoLookup.class);

    ConcurrentHashMap<Class<T>, Map<String, T>> idMultiMap = new ConcurrentHashMap<>();
    ConcurrentHashMap<Class<T>, Map<Name, T>> nameMultiMap = new ConcurrentHashMap<>();
    Function<T, Name> nameMapper;
    static final Predicate TRUE = x -> true;

    public CatalogInfoLookup(Function<T, Name> nameMapper) {
        super();
        this.nameMapper = nameMapper;
    }

    <K> Map<K, T> getMapForValue(ConcurrentHashMap<Class<T>, Map<K, T>> maps, T value) {
        Class<T> vc;
        if (Proxy.isProxyClass(value.getClass())) {
            ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(value);
            Object po = (T) h.getProxyObject();
            vc = (Class<T>) po.getClass();
        } else {
            vc = (Class<T>) value.getClass();
        }

        return getMapForValue(maps, vc);
    }

    protected <K> Map<K, T> getMapForValue(ConcurrentHashMap<Class<T>, Map<K, T>> maps, Class vc) {
        Map<K, T> vcMap = maps.get(vc);
        if (vcMap == null) {
            vcMap = maps.computeIfAbsent(vc, k -> new ConcurrentSkipListMap<K, T>());
        }
        return vcMap;
    }

    public T add(T value) {
        if (Proxy.isProxyClass(value.getClass())) {
            ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(value);
            value = (T) h.getProxyObject();
        }
        Map<Name, T> nameMap = getMapForValue(nameMultiMap, value);
        Name name = nameMapper.apply(value);
        nameMap.put(name, value);
        Map<String, T> idMap = getMapForValue(idMultiMap, value);
        return idMap.put(value.getId(), value);
    }

    public Collection<T> values() {
        List<T> result = new ArrayList<>();
        for (Map<String, T> v : idMultiMap.values()) {
            result.addAll(v.values());
        }

        return result;
    }

    public T remove(T value) {
        Name name = nameMapper.apply(value);
        Map<Name, T> nameMap = getMapForValue(nameMultiMap, value);
        nameMap.remove(name);
        Map<String, T> idMap = getMapForValue(idMultiMap, value);
        return idMap.remove(value.getId());
    }

    /** Updates the value in the name map. The new value must be a ModificationProxy */
    public void update(T proxiedValue) {
        ModificationProxy h = (ModificationProxy) Proxy.getInvocationHandler(proxiedValue);
        T actualValue = (T) h.getProxyObject();

        Name oldName = nameMapper.apply(actualValue);
        Name newName = nameMapper.apply(proxiedValue);
        if (!oldName.equals(newName)) {
            Map<Name, T> nameMap = getMapForValue(nameMultiMap, actualValue);
            nameMap.remove(oldName);
            nameMap.put(newName, actualValue);
        }
    }

    public void clear() {
        idMultiMap.clear();
        nameMultiMap.clear();
    }

    /**
     * Looks up objects by class and matching predicate.
     *
     * <p>This method is significantly faster than creating a stream and the applying the predicate
     * on it. Just using this approach instead of the stream makes the overall startup of GeoServer
     * with 20k layers go down from 50s to 44s (which is a lot, considering there is a lot of other
     * things going on)
     */
    <U extends CatalogInfo> List<U> list(Class<U> clazz, Predicate<U> predicate) {
        ArrayList<U> result = new ArrayList<U>();
        for (Class<T> key : nameMultiMap.keySet()) {
            if (clazz.isAssignableFrom(key)) {
                Map<Name, T> valueMap = nameMultiMap.get(key);
                if (valueMap != null) {
                    for (T v : valueMap.values()) {
                        final U u = (U) v;
                        if (predicate == TRUE || predicate.test(u)) {
                            result.add(u);
                        }
                    }
                }
            }
        }

        return result;
    }

    /** Looks up a CatalogInfo by class and identifier */
    public <U extends CatalogInfo> U findById(String id, Class<U> clazz) {
        for (Class<T> key : idMultiMap.keySet()) {
            if (clazz.isAssignableFrom(key)) {
                Map<String, T> valueMap = idMultiMap.get(key);
                if (valueMap != null) {
                    T t = valueMap.get(id);
                    if (t != null) {
                        return (U) t;
                    }
                }
            }
        }

        return null;
    }

    /** Looks up a CatalogInfo by class and name */
    public <U extends CatalogInfo> U findByName(Name name, Class<U> clazz) {
        for (Class<T> key : nameMultiMap.keySet()) {
            if (clazz.isAssignableFrom(key)) {
                Map<Name, T> valueMap = nameMultiMap.get(key);
                if (valueMap != null) {
                    T t = valueMap.get(name);
                    if (t != null) {
                        return (U) t;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Looks up objects by class and matching predicate.
     *
     * <p>This method is significantly faster than creating a stream and the applying the predicate
     * on it. Just using this approach instead of the stream makes the overall startup of GeoServer
     * with 20k layers go down from 50s to 44s (which is a lot, considering there is a lot of other
     * things going on)
     */
    <U extends CatalogInfo> U findFirst(Class<U> clazz, Predicate<U> predicate) {
        for (Class<T> key : nameMultiMap.keySet()) {
            if (clazz.isAssignableFrom(key)) {
                Map<Name, T> valueMap = nameMultiMap.get(key);
                if (valueMap != null) {
                    for (T v : valueMap.values()) {
                        final U u = (U) v;
                        if (predicate == TRUE || predicate.test(u)) {
                            return u;
                        }
                    }
                }
            }
        }

        return null;
    }

    /** Sets the specified catalog into all CatalogInfo objects contained in this lookup */
    public CatalogInfoLookup setCatalog(Catalog catalog) {
        for (Map<Name, T> valueMap : nameMultiMap.values()) {
            if (valueMap != null) {
                for (T v : valueMap.values()) {
                    if (v instanceof CatalogInfo) {
                        Method setter = OwsUtils.setter(v.getClass(), "catalog", Catalog.class);
                        if (setter != null) {
                            try {
                                setter.invoke(v, catalog);
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.FINE,
                                        "Failed to switch CatalogInfo to new catalog impl",
                                        e);
                            }
                        }
                    }
                }
            }
        }

        return this;
    }
}

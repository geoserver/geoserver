/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geotools.util.logging.Logging;

/**
 * Utility class used to wrap/clone objects and collections by various strategies:
 *
 * <ul>
 *   <li>Avoid cloning ModificationProxy proxies, as well as any CatalogInfo object
 *   <li>Avoid cloning at all well known objects that are known to be immutable (several classes in java.lang)
 *   <li>Wrap in ModificatinoProxy any object that is a CatalogInfo
 *   <li>Using {@link Cloneable} if available
 *   <li>Using copy constructors if available
 *   <li>Falling back on XStream serialization if the above fails
 *
 * @author Andrea Aime - GeoSolutions
 */
class ModificationProxyCloner {

    private static final XStreamPersisterFactory XSTREAM_PERSISTER_FACTORY = new XStreamPersisterFactory();

    static final Logger LOGGER = Logging.getLogger(ModificationProxyCloner.class);

    static final Map<Class<? extends CatalogInfo>, Class<? extends CatalogInfo>> CATALOGINFO_INTERFACE_CACHE =
            new ConcurrentHashMap<>();

    /**
     * Best effort object cloning utility, tries different lightweight strategies, then falls back on copy by XStream
     * serialization (we use that one as we have a number of hooks to avoid deep copying the catalog, and re-attaching
     * to it, in there)
     */
    @SuppressWarnings("unchecked") // too many casts to (T)
    static <T> T clone(T source)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        // null?
        if (source == null) {
            return null;
        }

        // already a modification proxy?
        if (ModificationProxy.handler(source) != null) {
            return source;
        }

        // is it a catalog info?
        if (source instanceof CatalogInfo cis) {
            return (T) ModificationProxy.create(cis, getDeepestCatalogInfoInterface(cis));
        }

        if (source instanceof LayerGroupStyle) {
            return (T) ModificationProxy.create(source, LayerGroupStyle.class);
        }

        // if a known immutable?
        if (source instanceof String
                || source instanceof Byte
                || source instanceof Short
                || source instanceof Integer
                || source instanceof Float
                || source instanceof Double
                || source instanceof BigInteger
                || source instanceof BigDecimal) {
            return source;
        }

        // to avoid reflective access warnings
        if (source instanceof TimeZone zone) {
            return (T) zone.clone();
        }

        if (source instanceof Map<?, ?> map) {
            return (T) cloneMap(map, true);
        }

        if (source instanceof Collection<?> collection) {
            return (T) cloneCollection(collection, true);
        }

        // is it cloneable?
        try {
            if (source instanceof Cloneable) {
                // methodutils does not seem to work against "clone()"...
                // return (T) MethodUtils.invokeExactMethod(source, "clone", null, null);
                Method method = source.getClass().getDeclaredMethod("clone");
                if (Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 0) {
                    return (T) method.invoke(source);
                }
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.FINE,
                    "Source object is cloneable, yet it does not have a public no argument method 'clone'",
                    e);
        }

        // does it have a copy constructor?
        Constructor<?> copyConstructor =
                ConstructorUtils.getAccessibleConstructor(source.getClass(), source.getClass());
        if (copyConstructor != null) {
            try {
                return (T) copyConstructor.newInstance(source);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Source has a copy constructor, but it failed, skipping to XStream", e);
            }
        }

        if (source instanceof Serializable serializable) {
            return (T) cloneSerializable(serializable);
        } else {
            XStreamPersister persister = XSTREAM_PERSISTER_FACTORY.createXMLPersister();
            XStream xs = persister.getXStream();
            String xml = xs.toXML(source);
            T copy = (T) xs.fromXML(xml);
            return copy;
        }
    }

    static <T extends Serializable> T cloneSerializable(T source) {
        byte[] bytes = SerializationUtils.serialize(source);
        try (ObjectInputStream input = new ModProxyObjectInputStream(new ByteArrayInputStream(bytes))) {
            @SuppressWarnings("unchecked")
            T copy = (T) input.readObject();
            return copy;
        } catch (Exception e) {
            throw new RuntimeException("Error cloning serializable object", e);
        }
    }

    static Class<? extends CatalogInfo> getDeepestCatalogInfoInterface(CatalogInfo object) {
        Class<? extends CatalogInfo> sourceClass = object.getClass();
        Class<? extends CatalogInfo> result = CATALOGINFO_INTERFACE_CACHE.get(sourceClass);
        if (result == null) {
            List<Class<?>> interfaces = ClassUtils.getAllInterfaces(sourceClass);
            // collect only CatalogInfo related interfaces
            List<Class<? extends CatalogInfo>> cis = new ArrayList<>();
            for (Class<?> clazz : interfaces) {
                if (CatalogInfo.class.isAssignableFrom(clazz)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends CatalogInfo> cast = (Class<? extends CatalogInfo>) clazz;
                    cis.add(cast);
                }
            }
            if (cis.isEmpty()) {
                result = null;
            } else if (cis.size() == 1) {
                result = cis.get(0);
            } else {
                Collections.sort(cis, (Comparator<Class<?>>) (c1, c2) -> {
                    if (c1.isAssignableFrom(c2)) {
                        return 1;
                    } else if (c2.isAssignableFrom(c1)) {
                        return -1;
                    } else {
                        return 0;
                    }
                });

                result = cis.get(0);
            }

            CATALOGINFO_INTERFACE_CACHE.put(sourceClass, result);
        }

        return result;
    }

    /**
     * Shallow or deep copies the provided collection
     *
     * @param deepCopy If true, a deep copy will be done, otherwise the cloned collection will contain the exact same
     *     objects as the source
     */
    public static <T> Collection<T> cloneCollection(Collection<T> source, boolean deepCopy)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (source == null) {
            // nothing to copy
            return null;
        }
        Collection<T> copy;
        try {
            @SuppressWarnings("unchecked")
            Collection<T> coll = source.getClass().getDeclaredConstructor().newInstance();
            copy = coll;
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            // we'll just pick something
            if (source instanceof Set) {
                copy = new HashSet<>();
            } else {
                copy = new ArrayList<>();
            }
        }
        if (deepCopy) {
            for (T object : source) {
                T objectCopy = clone(object);
                copy.add(objectCopy);
            }
        } else {
            copy.addAll(source);
        }

        return copy;
    }

    /**
     * Shallow or deep copies the provided collection
     *
     * @param <K>
     * @param <V>
     * @param deepCopy If true, a deep copy will be done, otherwise the cloned collection will contain the exact same
     *     objects as the source
     */
    public static <K, V> Map<K, V> cloneMap(Map<K, V> source, boolean deepCopy)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (source == null) {
            // nothing to copy
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<K, V> copy = source.getClass().getDeclaredConstructor().newInstance();
        if (deepCopy) {
            for (Map.Entry<K, V> entry : source.entrySet()) {
                K keyCopy = clone(entry.getKey());
                V valueCopy = clone(entry.getValue());
                copy.put(keyCopy, valueCopy);
            }
        } else {
            copy.putAll(source);
        }

        return copy;
    }

    /** Custom object output stream used to ensure a stable class loader used. */
    static class ModProxyObjectInputStream extends ObjectInputStream {

        ClassLoader classLoader;

        public ModProxyObjectInputStream(InputStream input) throws IOException {
            super(input);
            this.classLoader = ModificationProxy.class.getClassLoader();
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            String name = desc.getName();
            try {
                return Class.forName(name, false, classLoader);
            } catch (ClassNotFoundException ex) {
                return super.resolveClass(desc);
            }
        }
    }
}

/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.UnaryOperator;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.ows.util.ClassProperties;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.FilterFactory;

/**
 * Proxies an object storing any modifications to it.
 *
 * <p>Each time a setter is called through this invocation handler, the property is stored and not
 * set on the underlying object being proxied until {@link #commit()} is called. When a getter is
 * called through this invocation handler, the local properties are checked for one that has been
 * previously set, if found it is returned, if not found the getter is forwarded to the underlying
 * proxy object being called.
 *
 * <p>Any collections handled through this interface are cloned and client code obtains a copy. The
 * two collections will be synced on a call to {@link #commit()}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *     <p>TODO: this class should use BeanUtils for all reflection stuff
 */
public class ModificationProxy implements WrappingProxy, Serializable {

    /** the proxy object */
    Object proxyObject;

    /** reflection helper */
    transient ClassProperties cp;

    /** "dirty" properties */
    volatile HashMap<String, Object> properties;

    /**
     * The old values of the live collections (we have to clone them because once the proxy commits
     * the original map will contain the same values as the new one, breaking getOldValues()
     */
    volatile HashMap<String, Object> oldCollectionValues;

    public ModificationProxy(Object proxyObject) {
        this.proxyObject = proxyObject;
    }

    private ClassProperties cp() {
        if (cp == null) {
            this.cp = OwsUtils.getClassProperties(proxyObject.getClass());
        }
        return cp;
    }

    /** Intercepts getter and setter methods. */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String property = null;
        if ((method.getName().startsWith("get") || method.getName().startsWith("is"))
                && method.getParameterCount() == 0) {
            // intercept getter to check the dirty property set
            property = method.getName().substring(method.getName().startsWith("get") ? 3 : 2);
            if (properties != null && properties().containsKey(property)) {
                // return the previously set object
                return properties().get(property);
            } else {
                // if collection, create a wrapper
                if (Collection.class.isAssignableFrom(method.getReturnType())) {
                    Collection real = (Collection) method.invoke(proxyObject, null);
                    if (real == null) {
                        // in this case there is nothing we can do
                        return null;
                    }
                    Collection wrap = ModificationProxyCloner.cloneCollection(real, true);
                    properties().put(property, wrap);
                    // we also need to store a clone of the initial state as the collection
                    // might be a live one
                    Collection clone = ModificationProxyCloner.cloneCollection(real, false);
                    oldCollectionValues().put(property, clone);
                    return wrap;
                } else if (Map.class.isAssignableFrom(method.getReturnType())) {
                    Map real = (Map) method.invoke(proxyObject, null);
                    if (real == null) {
                        // in this case there is nothing we can do
                        return null;
                    }
                    Map wrap = ModificationProxyCloner.cloneMap(real, true);
                    properties().put(property, wrap);
                    // we also need to store a clone of the initial state as the collection
                    // might be a live one
                    Map clone = ModificationProxyCloner.cloneMap(real, false);
                    oldCollectionValues().put(property, clone);
                    return wrap;
                }
            }
        }
        if (method.getName().startsWith("set") && args.length == 1) {
            // intercept setter and put new value in list
            property = method.getName().substring(3);
            properties().put(property, args[0]);

            return null;
        }

        try {
            Object result = method.invoke(proxyObject, args);

            // in case this is a live indirection, resolve it. Typically this means
            // the reference is dangling, and we are going to avoid a wrapper around null
            if (result instanceof Proxy
                    && Proxy.getInvocationHandler(result) instanceof ResolvingProxy) {
                ResolvingProxy rp = ProxyUtils.handler(result, ResolvingProxy.class);
                // try to resolve, and return null if the reference is dangling
                final Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
                result = rp.resolve(catalog, result);
            }

            // intercept result and wrap it in a proxy if it is another Info object
            if (result != null && shouldProxyProperty(result.getClass())) {
                // avoid double proxy
                Object o = ModificationProxy.unwrap(result);
                if (o == result) {
                    result = ModificationProxy.create(result, (Class) method.getReturnType());

                    // cache the proxy, in case it is modified itself
                    properties().put(property, result);
                }
            }
            return result;
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            throw targetException;
        }
    }

    public Object getProxyObject() {
        return proxyObject;
    }

    public HashMap<String, Object> getProperties() {
        return properties();
    }

    @SuppressWarnings("rawtypes")
    public void commit() {
        synchronized (proxyObject) {
            // commit changes to the proxy object
            for (Map.Entry<String, Object> e : properties().entrySet()) {
                String p = e.getKey();
                Object v = e.getValue();

                // use the getter to figure out the type for the setter
                try {
                    Method g = getter(p);

                    // handle collection case
                    if (Collection.class.isAssignableFrom(g.getReturnType())) {
                        Collection c = (Collection) g.invoke(proxyObject, null);
                        c.clear();
                        for (Object o : (Collection) v) {
                            c.add(unwrap(o));
                        }
                    } else if (Map.class.isAssignableFrom(g.getReturnType())) {
                        Map proxied = (Map) v;
                        Map m = (Map) g.invoke(proxyObject, null);
                        m.clear();
                        for (Object key : proxied.keySet()) {
                            Object uk = unwrap(key);
                            final Object value = proxied.get(key);
                            Object uv = unwrap(value);
                            m.put(uk, uv);
                        }
                    } else {
                        Method s = setter(p, g.getReturnType());

                        if (Info.class.isAssignableFrom(g.getReturnType())) {
                            // another info is the changed property, it could be one of two cases
                            // 1) the info object was changed in place: x.getY().setFoo(...)
                            // 2) a new info object was set x.setY(...)
                            Info original = (Info) g.invoke(proxyObject, null);
                            Info modified = (Info) unwrap(v);
                            if (original == modified) {
                                // case 1, in this case get the proxy and commit it
                                if (v instanceof Proxy) {
                                    ModificationProxy h = handler(v);
                                    if (h != null && h.isDirty()) {
                                        h.commit();
                                    }
                                }
                            } else if (s != null) {
                                // case 2, just call the setter with the new object
                                s.invoke(proxyObject, v);
                            } else {
                                throw new IllegalStateException(
                                        "New info object set, but no setter for it.");
                            }
                        } else {
                            // call the setter
                            s.invoke(proxyObject, v);
                        }
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            // reset
            properties = null;
        }
    }

    /** Helper method for determining if a property of a proxied object should also be proxied. */
    boolean shouldProxyProperty(Class propertyType) {
        if (Catalog.class.isAssignableFrom(propertyType)) {
            // never proxy the catalog
            return false;
        }
        return Info.class.isAssignableFrom(propertyType);
    }

    HashMap<String, Object> properties() {
        if (properties != null) {
            return properties;
        }

        synchronized (this) {
            if (properties != null) {
                return properties;
            }

            properties = new HashMap<String, Object>();
        }

        return properties;
    }

    HashMap<String, Object> oldCollectionValues() {
        if (oldCollectionValues != null) {
            return oldCollectionValues;
        }

        synchronized (this) {
            if (oldCollectionValues != null) {
                return oldCollectionValues;
            }

            oldCollectionValues = new HashMap<String, Object>();
        }

        return oldCollectionValues;
    }

    /** Flag which indicates whether any properties of the object being proxied are changed. */
    public boolean isDirty() {
        boolean dirty = false;
        for (Iterator i = properties().entrySet().iterator(); i.hasNext() && !dirty; ) {
            Map.Entry e = (Map.Entry) i.next();
            if (e.getValue() instanceof Proxy) {
                ModificationProxy h = handler(e.getValue());
                if (h != null && !h.isDirty()) {
                    continue;
                }
            } else {
                try {
                    Object orig = unwrap(getter((String) e.getKey()).invoke(proxyObject, null));
                    if (orig == null) {
                        if (e.getValue() == null) {
                            continue;
                        }
                    } else if (e.getValue() != null && orig.equals(e.getValue())) {
                        continue;
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            dirty = true;
        }
        return dirty;
    }

    List<String> getDirtyProperties() {
        List<String> propertyNames = new ArrayList<String>();

        for (String propertyName : properties().keySet()) {
            // in the case this property is another proxy, check that it is actually dirty
            Object value = properties.get(propertyName);
            if (value instanceof Proxy) {
                ModificationProxy h = handler(value);
                if (h != null && !h.isDirty()) {
                    // proxy reports it is not dirty, only return this property if the underling
                    // value is not the same as the current value of the property on the object
                    Object curr = unwrap(value);
                    try {
                        Object orig = unwrap(getter(propertyName).invoke(proxyObject, null));
                        if (curr == orig) {
                            continue;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            propertyNames.add(propertyName);
        }

        return propertyNames;
    }

    /** Returns the names of any changed properties. */
    public List<String> getPropertyNames() {
        List<String> propertyNames = getDirtyProperties();

        for (int i = 0; i < propertyNames.size(); i++) {
            String name = propertyNames.get(i);
            propertyNames.set(i, Character.toLowerCase(name.charAt(0)) + name.substring(1));
        }

        return propertyNames;
    }

    /** Returns the old values of any changed properties. */
    public List<Object> getOldValues() {
        List<Object> oldValues = new ArrayList<Object>();
        for (String propertyName : getDirtyProperties()) {
            if (oldCollectionValues().containsKey(propertyName)) {
                oldValues.add(oldCollectionValues.get(propertyName));
            } else {
                try {
                    Method g = getter(propertyName);
                    if (g == null) {
                        throw new IllegalArgumentException("No such property: " + propertyName);
                    }

                    oldValues.add(g.invoke(proxyObject, null));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return oldValues;
    }

    /** Returns the new values of any changed properties. */
    public List<Object> getNewValues() {
        ArrayList newValues = new ArrayList();
        for (String propertyName : getDirtyProperties()) {
            newValues.add(properties().get(propertyName));
        }
        return newValues;
    }

    /*
     * Helper method for looking up a getter method.
     */
    Method getter(String propertyName) {
        Method g = null;
        try {
            g = proxyObject.getClass().getMethod("get" + propertyName, null);
        } catch (NoSuchMethodException e1) {
            // could be boolean
            try {
                g = proxyObject.getClass().getMethod("is" + propertyName, null);
            } catch (NoSuchMethodException e2) {
            }
        }

        if (g == null) {
            g = cp().getter(propertyName, null);
        }

        return g;
    }

    /*
     * Helper method for looking up a getter method.
     */
    Method setter(String propertyName, Class type) {
        Method s = null;
        try {
            s = proxyObject.getClass().getMethod("set" + propertyName, type);
        } catch (NoSuchMethodException e) {
            s = cp().setter(propertyName, type);
        }
        return s;
    }

    private Object readResolve() throws ObjectStreamException {
        // replace the main proxy object
        if (proxyObject instanceof CatalogInfo) {
            CatalogInfo replacement = replaceCatalogInfo((CatalogInfo) proxyObject);
            if (replacement != null) {
                proxyObject = unwrap(replacement);
            }
        }

        // any dirty property value
        if (properties != null) {
            for (Entry<String, Object> property : properties.entrySet()) {
                Object value = property.getValue();
                if (value instanceof CatalogInfo) {
                    CatalogInfo replacement = replaceCatalogInfo((CatalogInfo) value);
                    if (replacement != null) {
                        property.setValue(unwrap(replacement));
                    }
                } else if (value instanceof Collection) {
                    Collection clone = cloneCollection((Collection) value);
                    property.setValue(clone);
                } else if (value instanceof MetadataMap) {
                    MetadataMap clone = cloneMetadataMap((MetadataMap) value);
                    property.setValue(clone);
                }
            }
        }

        // and eventually also contents of old collections, they might also be
        if (oldCollectionValues != null) {
            for (Entry<String, Object> oce : oldCollectionValues.entrySet()) {
                Object value = oce.getValue();
                if (value instanceof Collection) {
                    Collection oldCollection = (Collection) value;
                    Collection clone = cloneCollection(oldCollection);
                    oce.setValue(clone);
                } else if (value instanceof MetadataMap) {
                    MetadataMap clone = cloneMetadataMap((MetadataMap) value);
                    oce.setValue(clone);
                }
            }
        }

        return this;
    }

    private MetadataMap cloneMetadataMap(MetadataMap original) {
        MetadataMap clone = new MetadataMap();
        for (Entry<String, Serializable> entry : original.entrySet()) {
            String key = entry.getKey();
            Serializable value = entry.getValue();
            if (value instanceof CatalogInfo) {
                CatalogInfo replacement = replaceCatalogInfo((CatalogInfo) value);
                if (replacement != null) {
                    value = replacement;
                }
            }

            clone.put(key, value);
        }

        return clone;
    }

    private Collection cloneCollection(Collection oldCollection) {
        Class<? extends Collection> oldCollectionClass = oldCollection.getClass();
        try {
            Collection clone = oldCollectionClass.getDeclaredConstructor().newInstance();
            for (Object o : oldCollection) {
                if (o instanceof CatalogInfo) {
                    CatalogInfo replacement = replaceCatalogInfo((CatalogInfo) o);
                    if (replacement != null) {
                        clone.add(unwrap(replacement));
                    } else {
                        clone.add(o);
                    }
                } else {
                    clone.add(o);
                }
            }

            return clone;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unexpected failure while cloning collection of class " + oldCollectionClass,
                    e);
        }
    }

    private CatalogInfo replaceCatalogInfo(CatalogInfo ci) {
        String id = ci.getId();
        Catalog catalog = (Catalog) GeoServerExtensions.bean("catalog");
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Class iface = getCatalogInfoInterface(ci.getClass());
        CatalogInfo replacement =
                catalog.get(iface, ff.equal(ff.property("id"), ff.literal(id), true));
        return replacement;
    }

    /** Gathers the most specific CatalogInfo sub-interface from the specified class object */
    private Class getCatalogInfoInterface(Class<? extends CatalogInfo> clazz) {
        Class result = CatalogInfo.class;
        for (Class c : clazz.getInterfaces()) {
            if (result.isAssignableFrom(c)) {
                result = c;
            }
        }

        return result;
    }

    /**
     * Wraps an object in a proxy.
     *
     * @throws RuntimeException If creating the proxy fails.
     */
    public static <T> T create(T proxyObject, Class<T> clazz) {
        return ProxyUtils.createProxy(proxyObject, clazz, new ModificationProxy(proxyObject));
    }

    /** Wraps a list in a decorator which proxies each item in the list. */
    public static <T> List<T> createList(List<T> proxyList, Class<T> clazz) {
        return new list(proxyList, clazz);
    }

    /**
     * Wraps a proxy instance.
     *
     * <p>This method is safe in that if the object passed in is not a proxy it is simply returned.
     * If the proxy is not an instance of {@link ModificationProxy} it is also returned untouched.
     */
    public static <T> T unwrap(T object) {
        return ProxyUtils.unwrap(object, ModificationProxy.class);
    }

    /**
     * Returns the ModificationProxy invocation handler for an proxy object.
     *
     * <p>This method will return null in the case where the object is not a proxy, or it is being
     * proxies by another invocation handler.
     */
    public static ModificationProxy handler(Object object) {
        return ProxyUtils.handler(object, ModificationProxy.class);
    }

    /**
     * If the given object is a modification proxy, unwraps it, passes it to innerWrap, then wraps
     * the result with a proxy that has the same modifications as the original. If the object is not
     * a modification proxy, then it simply returns the result of applying innerWrap.
     *
     * <p>This will not recursively re-wrap properties that hold other ModificationProxies. If that
     * is needed, it is up to innerWrap do this itself.
     */
    @SuppressWarnings("unchecked")
    public static <T> T rewrap(T object, UnaryOperator<T> innerWrap, Class<T> clazz) {
        ModificationProxy oldHandler = handler(object);
        if (Objects.isNull(oldHandler)) {
            return innerWrap.apply(object);
        } else {
            T newProxyObject = innerWrap.apply((T) oldHandler.getProxyObject());
            T newProxy = create(newProxyObject, clazz);

            // Copy the old state onto the new proxy
            ModificationProxy newHandler = handler(newProxy);
            if (Objects.nonNull(oldHandler.oldCollectionValues)) {
                newHandler.oldCollectionValues =
                        new HashMap<String, Object>(oldHandler.oldCollectionValues);
            }
            if (Objects.nonNull(oldHandler.properties)) {
                newHandler.properties = new HashMap<String, Object>(oldHandler.properties);
            }

            return newProxy;
        }
    }

    static class list<T> extends ProxyList {

        list(List<T> list, Class<T> clazz) {
            super(list, clazz);
        }

        protected <T> T createProxy(T proxyObject, Class<T> proxyInterface) {
            if (proxyObject instanceof Proxy) {
                InvocationHandler h = handler(proxyObject);
                if (h != null && h instanceof ModificationProxy) {
                    return proxyObject;
                }
            }
            return ModificationProxy.create(proxyObject, proxyInterface);
        }

        protected <U> U unwrapProxy(U proxy, java.lang.Class<U> proxyInterface) {
            return ModificationProxy.unwrap(proxy);
        };
    }
}

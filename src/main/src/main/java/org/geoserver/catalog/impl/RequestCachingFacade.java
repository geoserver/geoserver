/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.geoserver.catalog.CatalogFacade.ANY_NAMESPACE;
import static org.geoserver.catalog.CatalogFacade.ANY_WORKSPACE;
import static org.geoserver.catalog.CatalogFacade.NO_WORKSPACE;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.sort.SortBy;

/**
 * Answers the single object lookups of a catalog or configuration facade from a cache that lives for one request. The
 * cache also remembers lookups that found nothing.
 *
 * <p>The cache exists on a thread only between {@link #open()} and {@link #close()}, see {@code RequestCachingFilter}.
 * Other threads, and calls outside a request, go straight to the wrapped facade. A lookup answers what it found on its
 * first call in the request, making it repeatable: changes saved by other threads or other nodes do not show in the
 * current request, but will from the next one.
 *
 * <p>The class uses a proxy and classifies methods as read ones, that get cached, while any other method clears the
 * cache, so a request sees its own writes. The known reads are {@code get*}, {@code list}, {@code count} and
 * {@code canSort}.
 *
 * <p>This cache is not active by default, backends with costly lookups (e.g., database queries) can opt in by wrapping
 * their {@link CatalogFacade} and {@link GeoServerFacade} with {@code create}, and by declaring a
 * {@code RequestCachingFilter} bean.
 */
public class RequestCachingFacade implements InvocationHandler, WrappingProxy {

    /** System property or environment variable that turns the cache off when set to {@code false}. */
    public static final String ENABLED_PROPERTY = "GEOSERVER_REQUEST_CACHE";

    private static final ThreadLocal<Map<List<Object>, Entry>> CACHE = new ThreadLocal<>();

    /** Stored for lookups that found nothing, because the map cannot tell a missing key from a null value. */
    private static final Entry NOT_FOUND = new Entry(null, false);

    private static final Method CATALOG_LIST = catalogList();

    private final Object delegate;

    private RequestCachingFacade(Object delegate) {
        this.delegate = delegate;
    }

    /** Wraps the catalog facade, or returns it unchanged when {@link #ENABLED_PROPERTY} is {@code false}. */
    public static CatalogFacade create(CatalogFacade facade) {
        return create(facade, CatalogFacade.class);
    }

    /** Wraps the configuration facade, or returns it unchanged when {@link #ENABLED_PROPERTY} is {@code false}. */
    public static GeoServerFacade create(GeoServerFacade facade) {
        return create(facade, GeoServerFacade.class);
    }

    private static <T> T create(T facade, Class<T> type) {
        if ("false".equalsIgnoreCase(GeoServerExtensions.getProperty(ENABLED_PROPERTY))) {
            return facade;
        }
        return ProxyUtils.createProxy(facade, type, new RequestCachingFacade(facade));
    }

    /** Opens the cache on the current thread, returns {@code false} if one is already open. */
    public static boolean open() {
        if (CACHE.get() != null) {
            return false;
        }
        CACHE.set(new HashMap<>());
        return true;
    }

    /** Drops the cache of the current thread. */
    public static void close() {
        CACHE.remove();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Map<List<Object>, Entry> cache = CACHE.get();
        if (cache == null || method.getDeclaringClass() == Object.class) {
            return call(method, args);
        }

        String name = method.getName();
        if (isLookup(method)) {
            List<Object> key = key(method, args);
            return key == null ? call(method, args) : lookup(cache, key, () -> call(method, args));
        }
        if (method.equals(CATALOG_LIST)) {
            return list(cache, method, args);
        }
        if (!name.startsWith("get") && !name.equals("count") && !name.equals("canSort")) {
            cache.clear();
        }
        return call(method, args);
    }

    private static boolean isLookup(Method method) {
        Class<?> type = method.getReturnType();
        // Catalog is a CatalogInfo too, but getCatalog is not a lookup
        return method.getName().startsWith("get") && Info.class.isAssignableFrom(type) && type != Catalog.class;
    }

    /**
     * Answers {@link CatalogFacade#list} when it is a lookup by id in disguise. {@link CatalogImpl#get(Class, Filter)}
     * runs every lookup by filter as a list, so {@code catalog.get(LayerInfo.class, equal("id", id))} reaches the
     * facade as {@code list(LayerInfo.class, id = <id>, null, 2, null)}. Any other list goes to the facade.
     */
    private Object list(Map<List<Object>, Entry> cache, Method method, Object[] args) throws Throwable {
        Class<?> type = (Class<?>) args[0];
        String id = idEquality((Filter) args[1]);
        Integer offset = (Integer) args[2];
        Integer count = (Integer) args[3];
        boolean sorted = args[4] instanceof SortBy[] sortBy && sortBy.length > 0;
        if (id == null || offset != null || Integer.valueOf(0).equals(count) || sorted) {
            return call(method, args);
        }

        Object found = lookup(cache, List.of(this, method, type, id), () -> first(method, args));
        List<Object> items = found == null ? List.of() : List.of(found);
        return new CloseableIteratorAdapter<>(items.iterator());
    }

    private static String idEquality(Filter filter) {
        if (filter instanceof PropertyIsEqualTo equal
                && equal.getExpression1() instanceof PropertyName property
                && "id".equals(property.getPropertyName())
                && equal.getExpression2() instanceof Literal literal
                && literal.getValue() instanceof String id) {
            return id;
        }
        return null;
    }

    /** Returns the first object of the list, the id is unique so there is at most one. */
    private Object first(Method method, Object[] args) throws Throwable {
        try (CloseableIterator<?> it = (CloseableIterator<?>) call(method, args)) {
            return it.hasNext() ? it.next() : null;
        }
    }

    /**
     * Builds the key from this facade, the method and its arguments, with each {@link Info} replaced by its id. Returns
     * {@code null} for an argument that has no stable value, so the call is not cached.
     */
    private List<Object> key(Method method, Object[] args) {
        List<Object> key = new ArrayList<>();
        key.add(this);
        key.add(method);
        if (args == null) {
            return key;
        }
        for (Object arg : args) {
            // the facade sentinels are proxies that answer null to every call, hashCode included
            if (arg == ANY_WORKSPACE) {
                key.add("ANY_WORKSPACE");
            } else if (arg == NO_WORKSPACE) {
                key.add("NO_WORKSPACE");
            } else if (arg == ANY_NAMESPACE) {
                key.add("ANY_NAMESPACE");
            } else if (arg instanceof Info info && info.getId() != null) {
                key.add(info.getId());
            } else if (arg == null || arg instanceof String || arg instanceof Class) {
                key.add(arg);
            } else {
                return null;
            }
        }
        return key;
    }

    /** Returns the cached answer, or runs the lookup and stores what it found, or that it found nothing. */
    private static Object lookup(Map<List<Object>, Entry> cache, List<Object> key, Lookup lookup) throws Throwable {
        Entry entry = cache.get(key);
        if (entry != null) {
            return entry.get();
        }
        Object result = lookup.run();
        cache.put(key, result == null ? NOT_FOUND : Entry.of(result));
        return result;
    }

    private Object call(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static Method catalogList() {
        try {
            return CatalogFacade.class.getMethod(
                    "list", Class.class, Filter.class, Integer.class, Integer.class, SortBy[].class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Object getProxyObject() {
        return delegate;
    }

    private interface Lookup {
        Object run() throws Throwable;
    }

    /**
     * Holds the unwrapped object. When the facade answered with a {@link ModificationProxy}, each hit gets a new one,
     * so callers never see changes another caller has not saved.
     */
    private record Entry(Object value, boolean proxied) {

        static Entry of(Object result) {
            return new Entry(ModificationProxy.unwrap(result), ModificationProxy.handler(result) != null);
        }

        Object get() {
            return proxied ? ModificationProxy.create((Info) value, Info.class) : value;
        }
    }
}

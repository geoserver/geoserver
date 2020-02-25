/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.catalog.*;

/**
 * A proxy which holds onto an identifier which will later be resolved into a real object.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class ResolvingProxy extends ProxyBase {

    /** Avoids the cost of looking up over and over the same proxy class */
    static final Map<Class, Constructor> PROXY_CLASS_CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();

    /**
     * Wraps an object in the proxy.
     *
     * @throws RuntimeException If creating the proxy fails.
     */
    public static <T> T create(String ref, Class<T> clazz) {
        return create(ref, null, clazz);
    }

    /**
     * Wraps an object in the proxy, specifying a prefix for the reference.
     *
     * @throws RuntimeException If creating the proxy fails.
     */
    public static <T> T create(String ref, String prefix, Class<T> clazz) {
        InvocationHandler h = new ResolvingProxy(ref, prefix);

        try {
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz}, h);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T resolve(Catalog catalog, T object) {
        if (object instanceof Proxy) {
            InvocationHandler h = Proxy.getInvocationHandler(object);
            if (h instanceof ResolvingProxy) {
                String ref = ((ResolvingProxy) h).getRef();
                String pre = ((ResolvingProxy) h).getPrefix();

                if (object instanceof WorkspaceInfo) {
                    Object ws = catalog.getWorkspace(ref);
                    if (ws == null) {
                        ws = catalog.getWorkspaceByName(ref);
                    }
                    return (T) ws;
                }
                if (object instanceof NamespaceInfo) {

                    Object ns = catalog.getNamespace(ref);
                    if (ns == null) {
                        ns = catalog.getNamespaceByPrefix(ref);
                    }
                    return (T) ns;
                }
                if (object instanceof StoreInfo) {
                    if (object instanceof DataStoreInfo) {
                        return (T) catalog.getDataStore(ref);
                    }
                    if (object instanceof CoverageStoreInfo) {
                        return (T) catalog.getCoverageStore(ref);
                    }

                    T resolved = (T) catalog.getStore(ref, StoreInfo.class);
                    if (resolved == null) {
                        if (ref.indexOf(":") > 0) {
                            String[] qualifiedName = ref.split(":");
                            resolved =
                                    (T)
                                            catalog.getStoreByName(
                                                    qualifiedName[0],
                                                    qualifiedName[1],
                                                    StoreInfo.class);
                        } else {
                            resolved = (T) catalog.getStoreByName(ref, StoreInfo.class);
                        }
                    }
                    return resolved;
                }
                if (object instanceof ResourceInfo) {
                    if (object instanceof FeatureTypeInfo) {
                        Object r = catalog.getFeatureType(ref);
                        if (r == null) {
                            r = catalog.getFeatureTypeByName(ref);
                        }
                        return (T) r;
                    }
                    if (object instanceof CoverageInfo) {
                        Object r = catalog.getCoverage(ref);
                        if (r == null) {
                            r = catalog.getCoverageByName(ref);
                        }
                        return (T) r;
                    }

                    Object r = catalog.getResource(ref, ResourceInfo.class);
                    if (r == null) {
                        r = catalog.getResourceByName(ref, ResourceInfo.class);
                    }
                    return (T) r;
                }
                if (object instanceof LayerInfo) {
                    Object l = catalog.getLayer(ref);
                    if (l == null) {
                        l = catalog.getLayerByName(ref);
                    }
                    return (T) l;
                }
                if (object instanceof LayerGroupInfo) {
                    Object g = catalog.getLayerGroup(ref);
                    if (g == null) {
                        g = catalog.getLayerGroupByName(ref);
                    }
                    return (T) g;
                }
                if (object instanceof PublishedInfo) {
                    // This can happen if you have a layer group with a null layer (style group)
                    if (null == ref || "".equals(ref)) {
                        return null;
                    }
                }
                if (object instanceof StyleInfo) {
                    Object s = catalog.getStyle(ref);
                    if (s == null) {
                        if (pre != null) {
                            // look up in workspace
                            s = catalog.getStyleByName(pre, ref);
                        }
                    }
                    if (s == null) {
                        // still no luck
                        s = catalog.getStyleByName(ref);
                    }
                    return (T) s;
                }
            }
        }

        return object;
    }

    /** the reference */
    String ref;

    /** optional prefix, used to reference by name inside of a workspace */
    String prefix;

    public ResolvingProxy(String ref) {
        this(ref, null);
    }

    public ResolvingProxy(String ref, String prefix) {
        this.ref = ref;
        this.prefix = prefix;
    }

    public String getRef() {
        return ref;
    }

    public String getPrefix() {
        return prefix;
    }

    @Override
    protected Object handleGetUnSet(Object proxy, Method method, String property) throws Throwable {
        if ("id".equalsIgnoreCase(property)) {
            return ref;
        }

        return null;
    }

    @Override
    protected Object handleOther(Object proxy, Method method, Object[] args) throws Throwable {
        // if we get here the reference is dangling, have it use the proxy hashcode and equals
        // to allow comparing the references with no cryptic exceptions that would not
        // help debugging the broken reference
        final String methodName = method.getName();
        if (methodName.equals("hashCode")) {
            return hashCode();
        } else if (methodName.equals("equals")) {
            // allows an object with dangling reference to be compared with itself by equality
            return args[0] == null || equals(args[0]);
        }
        return null;
    }
}

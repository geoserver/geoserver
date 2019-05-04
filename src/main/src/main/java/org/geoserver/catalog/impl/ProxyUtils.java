/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for working with proxies.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ProxyUtils {

    /** Avoids the cost of looking up over and over the same proxy class */
    static final Map<ProxyClassConstructorKey, Constructor> PROXY_CLASS_CACHE =
            new ConcurrentHashMap<>();

    static final class ProxyClassConstructorKey {
        Class c1;
        Class c2;

        public ProxyClassConstructorKey(Class c1, Class c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((c1 == null) ? 0 : c1.hashCode());
            result = prime * result + ((c2 == null) ? 0 : c2.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ProxyClassConstructorKey other = (ProxyClassConstructorKey) obj;
            if (c1 == null) {
                if (other.c1 != null) {
                    return false;
                }
            } else if (!c1.equals(other.c1)) {
                return false;
            }
            if (c2 == null) {
                if (other.c2 != null) {
                    return false;
                }
            } else if (!c2.equals(other.c2)) {
                return false;
            }
            return true;
        }
    }

    /**
     * Creates a proxy for the specified object.
     *
     * @param proxyObject The object to proxy.
     * @param clazz The explicit interface to proxy.
     * @param h The invocation handler to intercept method calls.
     */
    public static <T> T createProxy(T proxyObject, Class<T> clazz, InvocationHandler h) {
        try {
            // proxy all interfaces implemented by the source object
            List<Class> proxyInterfaces = Arrays.asList(proxyObject.getClass().getInterfaces());

            // ensure that the specified class is included
            boolean add = true;
            for (Class interfce : proxyObject.getClass().getInterfaces()) {
                if (clazz.isAssignableFrom(interfce)) {
                    add = false;
                    break;
                }
            }
            if (add) {
                // make the list mutable (Arrays.asList is not) and then add the extra
                // interfaces
                proxyInterfaces = new ArrayList<>(proxyInterfaces);
                proxyInterfaces.add(clazz);
            }

            return (T)
                    Proxy.newProxyInstance(
                            clazz.getClassLoader(),
                            proxyInterfaces.toArray(new Class[proxyInterfaces.size()]),
                            h);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Unwraps a proxy returning the underlying object, if one exists.
     *
     * <p>This method handles two cases, the first is the case of a {@link WrappingProxy} in which
     * the underlying proxy object is returned.. The second is the {@link ProxyList} case in which
     * the underlying list is returned.
     *
     * @param object The proxy object.
     * @param handlerClass The invocation handler class.
     * @return The underlying proxied object, or the object passed in if no underlying object is
     *     recognized.
     */
    public static <T> T unwrap(T object, Class<? extends InvocationHandler> handlerClass) {
        if (object instanceof Proxy) {
            InvocationHandler h = handler(object, handlerClass);
            if (h != null && h instanceof WrappingProxy) {
                return (T) ((WrappingProxy) h).getProxyObject();
            }
        }
        if (object instanceof ProxyList) {
            return (T) ((ProxyList) object).proxyList;
        }

        return object;
    }

    /**
     * Returns the invocation handler from a proxy object.
     *
     * @param object The proxy object.
     * @param handlerClass The class of invocation handler to return.
     * @return THe invocation handler, or null if non matchining the specified class can be found.
     */
    public static <H extends InvocationHandler> H handler(Object object, Class<H> handlerClass) {
        if (object instanceof Proxy) {
            InvocationHandler h = Proxy.getInvocationHandler(object);
            if (handlerClass.isInstance(h)) {
                return (H) h;
            }
        }

        return null;
    }
}

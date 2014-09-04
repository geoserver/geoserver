/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for working with proxies.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ProxyUtils {

    /**
     * Creates a proxy for the specified object.
     * 
     * @param proxyObject The object to proxy.
     * @param clazz The explicit interface to proxy.
     * @param h The invocation handler to intercept method calls.
     */
    public static <T> T createProxy(T proxyObject, Class<T> clazz, InvocationHandler h) {
        // proxy all interfaces implemented by the source object
        List<Class> proxyInterfaces = (List) Arrays.asList( proxyObject.getClass().getInterfaces() );
        
        // ensure that the specified class is included
        boolean add = true;
        for ( Class interfce : proxyObject.getClass().getInterfaces() ) {
            if ( clazz.isAssignableFrom( interfce) ) {
                add = false;
                break;
            }
        }
        if( add ) {
            // make the list mutable (Arrays.asList is not) and then add the extra interfaces
            proxyInterfaces = new ArrayList<Class>(proxyInterfaces);
            proxyInterfaces.add( clazz );
        }
        
        Class proxyClass = Proxy.getProxyClass( clazz.getClassLoader(), 
            (Class[]) proxyInterfaces.toArray(new Class[proxyInterfaces.size()]) );
        
        T proxy;
        try {
            proxy = (T) proxyClass.getConstructor(
                new Class[] { InvocationHandler.class }).newInstance(new Object[] { h } );
        }
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
        
        return proxy;
    }

    /**
     * Unwraps a proxy returning the underlying object, if one exists.
     * <p>
     * This method handles two cases, the first is the case of a {@link WrappingProxy} in which the 
     * underlying proxy object is returned.. The second is the {@link ProxyList} case in which the 
     * underlying list is returned.
     * </p>
     * @param object The proxy object.
     * @param handlerClass The invocation handler class.
     * 
     * @return The underlying proxied object, or the object passed in if no underlying object is 
     * recognized.
     */
    public static <T> T unwrap( T object, Class<? extends InvocationHandler> handlerClass) {
        if ( object instanceof Proxy ) {
            InvocationHandler h = handler( object, handlerClass );
            if ( h != null && h instanceof WrappingProxy) {
                return (T) ((WrappingProxy) h).getProxyObject();
            }
        }
        if ( object instanceof ProxyList ) {
            return (T) ((ProxyList)object).proxyList;
        }
        
        return object;
    }

    /**
     * Returns the invocation handler from a proxy object.
     * 
     * @param object The proxy object.
     * @param handlerClass The class of invocation handler to return.
     * 
     * @return THe invocation handler, or null if non matchining the specified class can be found.
     */
    public static <H extends InvocationHandler> H handler( Object object, Class<H> handlerClass ) {
        if ( object instanceof Proxy ) {
            InvocationHandler h = Proxy.getInvocationHandler( object );
            if (handlerClass.isInstance(h)) {
                return (H) h;
            }
        }
        
        return null;
    }
}

/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Base class for catalog proxies.
 *
 * <p>This class maintains a map of "dirty" properties which have been set via a well formed java
 * bean setter method. Subsequence getter methods accessing
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class ProxyBase implements InvocationHandler {

    /** "dirty" properties */
    private volatile HashMap<String, Object> properties;

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // getter?
        if ((method.getName().startsWith("get") || method.getName().startsWith("is"))
                && method.getParameterTypes().length == 0) {

            String property =
                    method.getName().substring(method.getName().startsWith("get") ? 3 : 2);

            return handleGet(proxy, method, property);
        }
        // setter?
        if (method.getName().startsWith("set") && args.length == 1) {

            String property = method.getName().substring(3);

            handleSet(proxy, method, args[0], property);
            return null;
        }

        // some other
        return handleOther(proxy, method, args);
    }

    protected Object handleGet(Object proxy, Method method, String property) throws Throwable {
        // intercept getter to check the dirty property set
        if (properties != null && properties().containsKey(property)) {
            // return the previously set object
            return properties().get(property);
        } else {
            return handleGetUnSet(proxy, method, property);
        }
    }

    protected Object handleGetUnSet(Object proxy, Method method, String property) throws Throwable {
        return method.invoke(proxy, null);
    }

    protected void handleSet(Object proxy, Method method, Object value, String property)
            throws Throwable {
        properties().put(property, value);
    }

    protected Object handleOther(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(proxy, args);
    }

    protected HashMap<String, Object> properties() {
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
}

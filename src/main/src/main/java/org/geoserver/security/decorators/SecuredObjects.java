/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.WrapperPolicy;

/**
 * Utility class that provides easy and fast access to the registered {@link SecuredObjectFactory}
 * implementations
 *
 * @author Andrea Aime - TOPP
 */
public class SecuredObjects {
    /** Caches factories that can handle a certain class for quick access */
    static final Map<Class<?>, SecuredObjectFactory> FACTORY_CACHE =
            new ConcurrentHashMap<Class<?>, SecuredObjectFactory>();

    /**
     * Given an object to secure and a wrapping policy, scans the extension points for a factory
     * that can do the proper wrapping and invokes it, or simply gives up and throws an {@link
     * IllegalArgumentException} if no factory can deal with securing the specified object.
     *
     * @param object the raw object to be secured
     * @param policy the wrapping policy (how the secured wrapper should behave)
     * @return the secured object, or null if the input is null.
     * @throws IllegalArgumentException if the factory is not able to wrap the object
     */
    public static Object secure(Object object, WrapperPolicy policy) {
        // null safety
        if (object == null) return null;

        // In case the object to be secured is wrapped in a ModificationProxy, get the proxied
        // Object class
        // if the object is not wrapped, ModificationProxy.unwrap() will just return the object
        // reference.
        final Object unwrappedObject = ModificationProxy.unwrap(object);

        // if we already know what can handle the wrapping, just do it, don't
        // scan the extension points once more
        Class<?> clazz = unwrappedObject.getClass();
        SecuredObjectFactory candidate = FACTORY_CACHE.get(clazz);

        // otherwise scan and store (or complain)
        if (candidate == null) {
            // scan the application context
            List<SecuredObjectFactory> factories =
                    GeoServerExtensions.extensions(SecuredObjectFactory.class);
            for (SecuredObjectFactory factory : factories) {
                if (factory.canSecure(clazz)) {
                    candidate = factory;
                    break;
                }
            }
            if (candidate == null)
                throw new IllegalArgumentException(
                        "Could not find a security wrapper for class "
                                + clazz
                                + ", cannot secure the object");
            FACTORY_CACHE.put(clazz, candidate);
        }

        return candidate.secure(object, policy);
    }
}

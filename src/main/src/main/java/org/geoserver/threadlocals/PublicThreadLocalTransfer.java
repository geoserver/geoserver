/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.threadlocals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * A helper class that transfers ThreadLocals that can be referenced as public static fields
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("unchecked")
public class PublicThreadLocalTransfer implements ThreadLocalTransfer {

    Field field;

    String key;

    public PublicThreadLocalTransfer(Class theClass, String threadLocalField)
            throws SecurityException, NoSuchFieldException {
        this.field = theClass.getDeclaredField(threadLocalField);
        if (field == null) {
            throw new IllegalArgumentException(
                    "Failed to locate field " + field + " in class " + theClass.getName());
        } else if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException(
                    "Field "
                            + field
                            + " in class "
                            + theClass.getName()
                            + " was found, but it's not a static variable");
        }
        this.key = theClass.getName() + "#" + field;
    }

    @Override
    public void collect(Map<String, Object> storage) {
        ThreadLocal threadLocal = getThreadLocal();
        if (threadLocal != null) {
            Object value = threadLocal.get();
            storage.put(key, value);
        }
    }

    @Override
    public void apply(Map<String, Object> storage) {
        Object value = storage.get(key);
        ThreadLocal threadLocal = getThreadLocal();
        if (threadLocal != null) {
            threadLocal.set(value);
        }
    }

    ThreadLocal getThreadLocal() {
        try {
            return (ThreadLocal) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to grab thread local " + key + " for transfer into other threads", e);
        }
    }

    @Override
    public void cleanup() {
        ThreadLocal threadLocal = getThreadLocal();
        if (threadLocal != null) {
            threadLocal.remove();
        }
    }
}

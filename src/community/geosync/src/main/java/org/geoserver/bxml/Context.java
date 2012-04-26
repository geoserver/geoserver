package org.geoserver.bxml;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class Context {

    private static ThreadLocal<Map<Class<?>, Object>> LOCAL_CONTEXT = new ThreadLocal<Map<Class<?>, Object>>();

    public static <T> void put(Class<T> key, T value) {
        Map<Class<?>, Object> ctx = LOCAL_CONTEXT.get();
        if (ctx == null) {
            ctx = new HashMap<Class<?>, Object>();
            LOCAL_CONTEXT.set(ctx);
        }
        ctx.put(key, value);
    }

    public static <T> T get(Class<T> key) {
        Map<Class<?>, Object> ctx = LOCAL_CONTEXT.get();
        T value;
        if (ctx == null || (value = (T) ctx.get(key)) == null) {
            throw new NoSuchElementException("No context provided for " + key);
        }
        return value;
    }

    public static void clear() {
        LOCAL_CONTEXT.remove();
    }
}

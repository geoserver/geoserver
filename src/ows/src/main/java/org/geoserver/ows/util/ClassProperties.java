/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Provides lookup information about java bean properties in a class.
 *
 * @author Justin Deoliveira, OpenGEO
 * @author Andrea Aime, OpenGEO
 */
public class ClassProperties {
    private static final Multimap<String, Method> EMPTY = ImmutableMultimap.of();

    private static final Set<String> COMMON_DERIVED_PROPERTIES =
            new HashSet<>(Arrays.asList("prefixedName"));
    Multimap<String, Method> methods;
    Multimap<String, Method> getters;
    Multimap<String, Method> setters;

    public ClassProperties(Class clazz) {
        methods =
                Multimaps.newListMultimap(
                        new TreeMap<>(String.CASE_INSENSITIVE_ORDER), () -> new ArrayList<>());
        getters =
                Multimaps.newListMultimap(
                        new TreeMap<>(String.CASE_INSENSITIVE_ORDER), () -> new ArrayList<>());
        setters =
                Multimaps.newListMultimap(
                        new TreeMap<>(String.CASE_INSENSITIVE_ORDER), () -> new ArrayList<>());
        for (Method method : clazz.getMethods()) {
            final String name = method.getName();
            methods.put(name, method);
            final Class<?>[] params = method.getParameterTypes();
            if ((name.startsWith("get")
                            || name.startsWith("is")
                            || COMMON_DERIVED_PROPERTIES.contains(name))
                    && params.length == 0) {
                getters.put(gp(method), method);
            } else if (name.startsWith("set") && params.length == 1) {
                setters.put(name.substring(3), method);
            }
        }

        // avoid keeping lots of useless empty arrays in memory for
        // the long term, use just one
        if (methods.size() == 0) methods = EMPTY;
        if (getters.size() == 0) getters = EMPTY;
        if (setters.size() == 0) setters = EMPTY;
    }

    /**
     * Returns a list of all the properties of the class.
     *
     * @return A list of string.
     */
    public List<String> properties() {
        ArrayList<String> properties = new ArrayList<String>();
        for (String key : getters.keySet()) {
            if (key.equals("Resource")) {
                properties.add(0, key);
            } else {
                properties.add(key);
            }
        }
        return properties;
    }

    /**
     * Looks up a setter method by property name.
     *
     * <p>setter("foo",Integer) --&gt; void setFoo(Integer);
     *
     * @param property The property.
     * @param type The type of the property.
     * @return The setter for the property, or null if it does not exist.
     */
    public Method setter(String property, Class type) {
        Collection<Method> methods = setters.get(property);
        for (Method setter : methods) {
            if (type == null) {
                return setter;
            } else {
                Class target = setter.getParameterTypes()[0];
                if (target.isAssignableFrom(type)
                        || (target.isPrimitive() && type == wrapper(target))
                        || (type.isPrimitive() && target == wrapper(type))) {
                    return setter;
                }
            }
        }

        // could not be found, try again with a more lax match
        String lax = lax(property);
        if (!lax.equals(property)) {
            return setter(lax, type);
        }

        return null;
    }

    /**
     * Looks up a getter method by its property name.
     *
     * <p>getter("foo",Integer) --&gt; Integer getFoo();
     *
     * @param property The property.
     * @param type The type of the property.
     * @return The getter for the property, or null if it does not exist.
     */
    public Method getter(String property, Class type) {
        Collection<Method> methods = getters.get(property);
        if (methods != null) {
            for (Method getter : methods) {
                if (type == null) {
                    return getter;
                } else {
                    Class target = getter.getReturnType();
                    if (type.isAssignableFrom(target)
                            || (target.isPrimitive() && type == wrapper(target))
                            || (type.isPrimitive() && target == wrapper(type))) {
                        return getter;
                    }
                }
            }
        }

        // could not be found, try again with a more lax match
        String lax = lax(property);
        if (!lax.equals(property)) {
            return getter(lax, type);
        }

        return null;
    }

    /**
     * Does some checks on the property name to turn it into a java bean property.
     *
     * <p>Checks include collapsing any "_" characters.
     */
    static String lax(String property) {
        return property.replaceAll("_", "");
    }

    /**
     * Returns the wrapper class for a primitive class.
     *
     * @param primitive A primtive class, like int.class, double.class, etc...
     */
    static Class wrapper(Class primitive) {
        if (boolean.class == primitive) {
            return Boolean.class;
        }
        if (char.class == primitive) {
            return Character.class;
        }
        if (byte.class == primitive) {
            return Byte.class;
        }
        if (short.class == primitive) {
            return Short.class;
        }
        if (int.class == primitive) {
            return Integer.class;
        }
        if (long.class == primitive) {
            return Long.class;
        }

        if (float.class == primitive) {
            return Float.class;
        }
        if (double.class == primitive) {
            return Double.class;
        }

        return null;
    }

    /** Looks up a method by name. */
    public Method method(String name) {
        Collection<Method> results = methods.get(name);
        if (results.isEmpty()) {
            return null;
        } else {
            return results.iterator().next();
        }
    }

    /** Returns the name of the property corresponding to the getter method. */
    String gp(Method getter) {
        String name = getter.getName();
        if (COMMON_DERIVED_PROPERTIES.contains(name)) {
            return name;
        }
        return name.substring(name.startsWith("get") ? 3 : 2);
    }
}

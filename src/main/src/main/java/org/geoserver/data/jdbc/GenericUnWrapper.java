/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.data.jdbc.datasource.UnWrapper;
import org.geotools.util.logging.Logging;

// import org.springframework.jdbc.support.nativejdbc.XAPoolNativeJdbcExtractor;

/**
 * Generic UnWrapper using reflection to access original JDBC connection.
 *
 * <p>This implementation is a stand-in for SpringUnWrapper which is not currently able to unwrap
 * JBoss WrappedConnectionJDK6 connections. While a list of well-known Connection implementations is
 * catered for (with both the class name and unwrap method recorded) the GenericUnWrapper is willing
 * to search through the available fields looking for an implementation of Connection to return.
 *
 * @author Jody Garnett - OpenGeo
 */
public class GenericUnWrapper implements UnWrapper {
    private static final Logger LOGGER =
            Logging.getLogger("org.geoserver.data.jdbc.GenericUnWrapper");

    private static final Method IGNORE;

    static {
        try {
            IGNORE = GenericUnWrapper.class.getMethod("ignore", new Class[0]);
        } catch (Exception inconsistent) {
            throw new IllegalStateException(
                    "Expected static GenericUnWrapper ignore() method", inconsistent);
        }
    }

    /**
     * Primary record of known access methods used to unwrap conenctions in exotic deployment
     * environments such as JBoss.
     *
     * <p>This field is package visible to allow local testing.
     */
    static final Map<Class<?>, Method> CONNECTION_METHODS;

    static {
        CONNECTION_METHODS = new ConcurrentHashMap<Class<?>, Method>();

        // if the environment does not contain the classes ... skip
        methodSearch(
                "JBoss EAP 6.0",
                CONNECTION_METHODS,
                "org.jboss.jca.adapters.jdbc.jdk6.WrappedConnectionJDK6",
                "getUnderlyingConnection");
        methodSearch(
                "JBoss JCA",
                CONNECTION_METHODS,
                "org.jboss.jca.adapters.jdbc.WrappedConnection",
                "getUnderlyingConnection");
        methodSearch(
                "JBoss Resource Adapter",
                CONNECTION_METHODS,
                "org.jboss.resource.adapter.jdbc.WrappedConnection",
                "getUnderlyingConnection");
    }

    /**
     * Primary record of known access methods used to unwrap statements.
     *
     * <p>This field is package visible to allow local testing.
     */
    static final Map<Class<?>, Method> STATEMENT_METHODS;

    static {
        STATEMENT_METHODS = new ConcurrentHashMap<Class<?>, Method>();
        methodSearch(
                "JBoss Resource Adapter",
                STATEMENT_METHODS,
                "org.jboss.resource.adapter.jdbc.WrappedCallableStatement",
                "getUnderlyingStatement");
    }

    /** Look up method used for unwrapping (if supported in the application container). */
    private static void methodSearch(
            String env, Map<Class<?>, Method> methods, String className, String methodName) {
        try {
            Class<?> wrappedConnection = Class.forName(className);
            Method unwrap = wrappedConnection.getMethod(methodName, (Class[]) null);
            LOGGER.info(env + " " + className + " supported");
            methods.put(wrappedConnection, unwrap);
        } catch (ClassNotFoundException ignore) {
            LOGGER.finer(env + " " + className + " not found");
        } catch (Throwable e) {
            LOGGER.fine(env + " " + className + " not available:" + e);
        }
    }

    /** Used as reflection target of {@link #IGNORE} placeholder. */
    public static void ignore() {
        // this space is intentionally left blank
    }

    public boolean canUnwrap(Connection conn) {
        Connection unwrapped = unwrapInternal(Connection.class, conn, CONNECTION_METHODS);
        return unwrapped != null;
    }

    public Connection unwrap(Connection conn) {
        Connection unwrapped = unwrapInternal(Connection.class, conn, CONNECTION_METHODS);
        if (unwrapped != null) {
            return unwrapped;
        } else {
            throw new IllegalArgumentException(
                    "This connection is not unwrappable, "
                            + "check canUnwrap before calling unwrap");
        }
    }

    public boolean canUnwrap(Statement statement) {
        Statement unwrapped = unwrapInternal(Statement.class, statement, STATEMENT_METHODS);
        return unwrapped != null;
    }

    public Statement unwrap(Statement statement) {
        Statement unwrapped = unwrapInternal(Statement.class, statement, STATEMENT_METHODS);
        if (unwrapped != null) {
            return unwrapped;
        } else {
            throw new IllegalArgumentException(
                    "This statement is not unwrappable, "
                            + "check canUnwrap before calling unwrap");
        }
    }

    /**
     * Using provided map of methods to unwrap. For each implementation class an unwrapper method is
     * is provided, or null is sentinel (indicating no method is available). For classes that do not
     * provide an unwrapping method reflection is tried once (resulting in either a cached method to
     * next time, or null for use as a sentinel
     */
    @SuppressWarnings(
            "deprecation") // Method.isAccessible is deprecated but replacement not available in
    // Java 8
    private <T> T unwrapInternal(Class<T> target, T conn, Map<Class<?>, Method> methods) {
        Class<?> implementation = conn.getClass();
        // Check if we have a known method to use
        if (methods.containsKey(implementation)) {
            Method accessMethod = methods.get(implementation);
            if (accessMethod == IGNORE) {
                return null; // reflection has already been tried and come up empty
            }
            T unwrapped = unwrapInternal(target, conn, implementation, accessMethod);
            return unwrapped;
        } else {
            // Scan for superclass/interface method
            for (Entry<Class<?>, Method> entry : methods.entrySet()) {
                Class<?> wrapper = entry.getKey();
                Method accessMethod = entry.getValue();
                if (wrapper.isInstance(conn)) {
                    T unwrapped = unwrapInternal(target, conn, wrapper, accessMethod);
                    if (unwrapped != null) {
                        methods.put(implementation, accessMethod);
                        return unwrapped;
                    }
                }
            }
            // Use reflection to scan for an accessMethod
            for (Method method : implementation.getMethods()) {
                if (target.isAssignableFrom(method.getReturnType())
                        && method.getParameterTypes().length == 0
                        && method.isAccessible()) {
                    // possible accessor method
                    T unwrapped = unwrapInternal(target, conn, implementation, method);
                    if (unwrapped != null) {
                        methods.put(implementation, method);
                        return unwrapped;
                    }
                }
            }
            // Give up - mark this one as not possible so we can exit early next time
            methods.put(implementation, IGNORE);
        }
        return null; // not found
    }

    /**
     * Safe unwrap method using invoke on the provided accessMethod.
     *
     * <p>All errors are logged at finest detail, and null is returned.
     *
     * @return unwrapped instance of target class, or null if not available
     */
    private <T> T unwrapInternal(Class<T> target, T conn, Class<?> wrapper, Method accessMethod) {
        if (accessMethod == null) {
            LOGGER.finest(
                    "Using "
                            + wrapper.getName()
                            + " does not have accessMethod to unwrap "
                            + target.getSimpleName());
            return null; // skip inaccessible method
        }
        try {
            Object result = accessMethod.invoke(conn, (Object[]) null);
            if (result == null) {
                LOGGER.finest(
                        "Using "
                                + wrapper.getName()
                                + "."
                                + accessMethod.getName()
                                + "() to unwrap "
                                + target.getSimpleName()
                                + " produced a null");
                return null;
            }
            if (result == conn) {
                LOGGER.finest(
                        "Using "
                                + wrapper.getName()
                                + "."
                                + accessMethod.getName()
                                + "() to unwrap did not result in native "
                                + target.getSimpleName()
                                + ": "
                                + result.getClass().getSimpleName());
                return null;
            }
            if (!target.isInstance(result)) {
                LOGGER.finest(
                        "Using "
                                + wrapper.getName()
                                + "."
                                + accessMethod.getName()
                                + "() to unwrap did not result in native "
                                + target.getSimpleName()
                                + ": "
                                + result.getClass().getSimpleName());
                return null;
            }
            return target.cast(result);
        } catch (IllegalArgumentException e) {
            LOGGER.log(
                    Level.FINEST,
                    "Using "
                            + wrapper.getName()
                            + "."
                            + accessMethod.getName()
                            + "() to unwrap "
                            + target.getSimpleName()
                            + " failed: "
                            + e);
            return null; // unexpected with no arguments
        } catch (IllegalAccessException e) {
            LOGGER.log(
                    Level.FINEST,
                    "Using "
                            + wrapper.getName()
                            + "."
                            + accessMethod.getName()
                            + "() to unwrap "
                            + target.getSimpleName()
                            + " failed: "
                            + e);
            return null; // could be a visibility issue
        } catch (InvocationTargetException e) {
            LOGGER.log(
                    Level.FINEST,
                    "Using "
                            + wrapper.getName()
                            + "."
                            + accessMethod.getName()
                            + "() to unwrap "
                            + target.getSimpleName()
                            + " failed: "
                            + e);
            return null; // abort abort
        }
    }
}

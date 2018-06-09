/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * Helper class for using GeoServerExtensions in a test environment.
 * <p>
 * This class allows the insertion of beans, properties and files into the
 * GeoServerExtensions cache to facilitate testing.
 * </p>
 * <h2>Singleton Beans</h2>
 * <p>
 * As a concession to mocking test cases, a few singletons can be registered by hand:
 * <pre><code>
 * &#64;Before
 * public void before(){
 *   GeoServerResourceLoader loader = new GeoServerResourceLoader(baseDirectory);
 *   GeoServerExtensionsHelper.singleton( "resourceLoader", loader );
 * }
 * &#64;After
 * public void after(){
 *   GeoServerExtensionsHelper.clear();
 * }
 * </code><pre>
 * Warnings provided by {@link #checkContext(ApplicationContext, String)} are suppressed when using {@link #init(Object)}.
 *
 *
 * @author Jody Garnett (Boundless)
 */
public class GeoServerExtensionsHelper {

    /**
     * Flag to identify use of spring context via {@link #setApplicationContext(ApplicationContext)}
     * and enable additional consistency checks for missing extensions.
     */
    public static void setIsSpringContext(boolean isSpring) {
        GeoServerExtensions.isSpringContext = isSpring;
    }
    /** Clear caches used by GeoServerExtensions. */
    public static void clear() {
        GeoServerExtensions.extensionsCache.clear();
        GeoServerExtensions.singletonBeanCache.clear();
        GeoServerExtensions.propertyCache.clear();
        GeoServerExtensions.fileCache.clear();
    }
    /**
     * Sets the web application context to be used for looking up extensions.
     *
     * <p>This is the context that is used for methods which don't supply their own context.
     *
     * @param context ApplicationContext used to lookup extensions
     */
    public static void init(ApplicationContext context) throws BeansException {
        GeoServerExtensions.isSpringContext = false;
        GeoServerExtensions.context = context;
        clear();
    }

    /**
     * Directly register singleton for use with {@link GeoServerExtensions#bean(String)} (and {@link
     * GeoServerExtensions#bean(Class)}).
     *
     * <p>If GeoServerExtensions has been configured with a context
     *
     * @param name Singleton name
     * @param bean Singleton
     */
    public static void singleton(String name, Object bean, Class<?>... declaredClasses) {
        if (GeoServerExtensions.context != null) {
            if (GeoServerExtensions.context.containsBean(name)) {
                Object conflict = GeoServerExtensions.context.getBean(name);
                if (bean != conflict) {
                    GeoServerExtensions.LOGGER.fine(
                            "ApplicationContext override " + name + ": " + conflict);
                }
            }
        } else {
            GeoServerExtensions.isSpringContext = false;
        }
        if (name == null || bean == null) {
            return;
        }
        GeoServerExtensions.singletonBeanCache.put(name, bean);
        if (declaredClasses != null && declaredClasses.length > 0) {
            for (Class<?> clazz : declaredClasses) {
                addToCache(GeoServerExtensions.extensionsCache, clazz, name);
            }
        } else {
            Class<?> type = bean.getClass();
            addToCache(GeoServerExtensions.extensionsCache, type, name);
        }
    }

    static <T> void addToCache(Map<T, String[]> cache, T key, String name) {
        String[] cached = cache.get(key);
        if (cached != null) {
            cached = Arrays.copyOf(cached, cached.length + 1);
            cached[cached.length - 1] = name;
        } else {
            cached = new String[] {name};
        }
        cache.put(key, cached);
    }

    /** Directly register property for use with {@link GeoServerExtensions#getProperty(String)}. */
    public static void property(String propertyName, String property) {
        GeoServerExtensions.propertyCache.put(propertyName, property);
    }
    /** Directly register file for use with {@link GeoServerExtensions#file(String)}. */
    public static void file(String path, File file) {
        GeoServerExtensions.fileCache.put(path, file);
    }

    /**
     * JUnit Rule which automatically initialises and clears mocked extensions.
     *
     * @author Kevin Smith, Boundless
     */
    public static class ExtensionsHelperRule implements TestRule {
        ApplicationContext context;
        Boolean isSpringContext;
        Boolean active = false;

        public Statement apply(Statement base, Description description) {
            return statement(base);
        }

        private Statement statement(final Statement base) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    try {
                        if (context != null) {
                            GeoServerExtensionsHelper.init(context);
                        } else {
                            GeoServerExtensionsHelper.clear();
                        }
                        if (isSpringContext != null) {
                            GeoServerExtensionsHelper.setIsSpringContext(isSpringContext);
                        }

                        active = true;

                        base.evaluate();
                    } finally {
                        active = false;
                        GeoServerExtensionsHelper.clear();
                    }
                }
            };
        }

        /**
         * Directly register singleton for use with {@link GeoServerExtensions#bean(String)} (and
         * {@link GeoServerExtensions#bean(Class)}).
         *
         * <p>If GeoServerExtensions has been configured with a context
         *
         * @param name Singleton name
         * @param bean Singleton
         */
        public void singleton(String name, Object bean, Class<?>... declaredClasses) {
            if (!active) throw new IllegalStateException();
            GeoServerExtensionsHelper.singleton(name, bean, declaredClasses);
        }

        /**
         * Directly register property for use with {@link GeoServerExtensions#getProperty(String)}.
         */
        public void property(String propertyName, String property) {
            if (!active) throw new IllegalStateException();
            GeoServerExtensionsHelper.property(propertyName, property);
        }

        /** Directly register file for use with {@link GeoServerExtensions#file(String)}. */
        public void file(String path, File file) {
            if (!active) throw new IllegalStateException();
            GeoServerExtensionsHelper.file(path, file);
        }
    }
}

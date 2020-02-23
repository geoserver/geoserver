/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.geoserver.platform.resource.Paths;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.SuppressFBWarnings;
import org.geotools.util.factory.FactoryRegistry;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.context.WebApplicationContext;

/**
 * Utility class uses to process GeoServer extension points.
 *
 * <p>An instance of this class needs to be registered in spring context as follows.
 *
 * <pre><code>
 *         &lt;bean id="geoserverExtensions" class="org.geoserver.GeoServerExtensions"/&gt;
 * </code></pre>
 *
 * It must be a singleton, and must not be loaded lazily. Furthermore, this bean must be loaded
 * before any beans that use it.
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @author Andrea Aime, The Open Planning Project
 */
public class GeoServerExtensions implements ApplicationContextAware, ApplicationListener {

    /** logger */
    protected static final Logger LOGGER = Logging.getLogger("org.geoserver.platform");

    /**
     * Caches the names of the beans for a particular type, so that the lookup (expensive) wont' be
     * needed. We cache names instead of beans because doing the latter we would break the
     * "singleton=false" directive of some beans
     */
    static SoftValueHashMap<Class, String[]> extensionsCache =
            new SoftValueHashMap<Class, String[]>(40);

    static ConcurrentHashMap<String, Object> singletonBeanCache =
            new ConcurrentHashMap<String, Object>();

    /**
     * Property cache maintained by GeoServerExtensionsHelper allowing temporary override of {@link
     * #getProperty(String)} results.
     */
    static ConcurrentHashMap<String, String> propertyCache =
            new ConcurrentHashMap<String, String>();

    /**
     * File cache maintained by GeoServerExtensionsHelper allowing temporary override of {@link
     * #file(String)} results.
     */
    static ConcurrentHashMap<String, File> fileCache = new ConcurrentHashMap<String, File>();

    /** SPI lookups are very expensive, we need to cache them */
    static SoftValueHashMap<Class, List<Object>> spiCache =
            new SoftValueHashMap<Class, List<Object>>(40);

    /**
     * Flag to identify use of spring context via {@link #setApplicationContext(ApplicationContext)}
     * an enable additional consistency checks for missing extensions.
     */
    static boolean isSpringContext = true;
    /** A static application context */
    static ApplicationContext context;

    /**
     * Sets the web application context to be used for looking up extensions.
     *
     * <p>This method is called by the spring container, and should never be called by client code.
     * If client needs to supply a particular context, methods which take a context are available.
     *
     * <p>This is the context that is used for methods which don't supply their own context.
     *
     * @param context ApplicationContext used to lookup extensions
     */
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        isSpringContext = true;
        GeoServerExtensions.context = context;
        extensionsCache.clear();
        singletonBeanCache.clear();
        propertyCache.clear();
    }

    /**
     * Loads all extensions implementing or extending <code>extensionPoint</code>.
     *
     * @param extensionPoint The class or interface of the extensions.
     * @param context The context in which to perform the lookup.
     * @return A collection of the extensions, or an empty collection.
     */
    @SuppressWarnings("unchecked")
    public static final <T> List<T> extensions(
            Class<T> extensionPoint, ApplicationContext context) {
        Collection<String> names;
        names = extensionNames(extensionPoint, context);

        // lookup extension filters preventing recursion
        List<ExtensionFilter> filters;
        if (ExtensionFilter.class.isAssignableFrom(extensionPoint)) {
            filters = Collections.emptyList();
        } else {
            filters = extensions(ExtensionFilter.class, context);
        }

        // look up all the beans
        List<T> result = new ArrayList<T>(names.size());
        for (String name : names) {
            Object bean = getBean(context, name);
            if (!excludeBean(name, bean, filters)) result.add((T) bean);
        }

        // load from secondary extension providers
        if (!ExtensionProvider.class.isAssignableFrom(extensionPoint)
                && !ExtensionFilter.class.isAssignableFrom(extensionPoint)) {

            List<Object> secondary = new ArrayList<Object>();
            for (ExtensionProvider xp : extensions(ExtensionProvider.class, context)) {
                try {
                    if (extensionPoint.isAssignableFrom(xp.getExtensionPoint())) {
                        secondary.addAll(xp.getExtensions(extensionPoint));
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Extension provider threw exception", e);
                }
            }
            filter(secondary, filters, result);
        }

        // load from factory spi
        List<Object> spiExtensions = spiCache.get(extensionPoint);
        if (spiExtensions == null) {
            spiExtensions = new ArrayList<Object>();
            new FactoryRegistry(extensionPoint)
                    .getFactories(extensionPoint, false)
                    .forEach(spiExtensions::add);
            spiCache.put(extensionPoint, spiExtensions);
        }
        // filter the beans coming from SPI (we don't cache the results
        // of the filtering, an extension filter can change its mind
        // from call to call
        filter(spiExtensions, filters, result);

        // sort the results based on ExtensionPriority
        Collections.sort(result, ExtensionPriority.COMPARATOR);

        return result;
    }

    public static <T> Collection<String> extensionNames(Class<T> extensionPoint) {
        return extensionNames(extensionPoint, context);
    }

    public static <T> Collection<String> extensionNames(
            Class<T> extensionPoint, ApplicationContext context) {
        String[] names;
        if (GeoServerExtensions.context == context) {
            names = extensionsCache.get(extensionPoint);
        } else {
            names = null;
        }
        if (names == null) {
            checkContext(context, extensionPoint.getSimpleName());
            if (context != null) {
                try {
                    names = context.getBeanNamesForType(extensionPoint);
                    if (names == null) {
                        names = new String[0];
                    }
                    // update cache only if dealing with the same context
                    if (GeoServerExtensions.context == context) {
                        extensionsCache.put(extensionPoint, names);
                    }
                } catch (Exception e) {
                    // JD: this can happen during testing... if the application
                    // context has been closed and a non-one time setup test is
                    // run that triggers an extension lookup
                    LOGGER.log(Level.WARNING, "bean lookup error", e);
                    return Collections.emptyList();
                }
            } else {
                return Collections.emptyList();
            }
        }
        return Arrays.asList(names);
    }

    private static Object getBean(ApplicationContext context, String name) {
        Object bean = singletonBeanCache.get(name);
        if (bean == null && context != null) {
            bean = context.getBean(name);
            if (bean != null && context.isSingleton(name)) {
                singletonBeanCache.putIfAbsent(name, bean);
            }
        }
        return bean;
    }

    private static void filter(List objects, List<ExtensionFilter> filters, List result) {
        for (Object bean : objects) {
            if (!excludeBean(null, bean, filters)) result.add(bean);
        }
    }

    /** Returns true if any of the {@link ExtensionFilter} asks to exclude the bean */
    private static boolean excludeBean(String beanId, Object bean, List<ExtensionFilter> filters) {
        for (ExtensionFilter filter : filters) {
            if (filter.exclude(beanId, bean)) return true;
        }
        return false;
    }

    /**
     * Loads all extensions implementing or extending <code>extensionPoint</code>.
     *
     * <p>This method uses the "default" application context to perform the lookup. See {@link
     * #setApplicationContext(ApplicationContext)}.
     *
     * @param extensionPoint The class or interface of the extensions.
     * @return A collection of the extensions, or an empty collection.
     */
    public static final <T> List<T> extensions(Class<T> extensionPoint) {
        return extensions(extensionPoint, context);
    }

    /** Returns a specific bean given its name */
    public static final Object bean(String name) {
        return bean(name, context);
    }

    /** Returns a specific bean given its name with a specified application context. */
    public static final Object bean(String name, ApplicationContext context) {
        checkContext(context, name);
        if (context != null) {
            return getBean(context, name);
        } else {
            Object bean = singletonBeanCache.get(name);
            return bean;
        }
    }

    /**
     * Loads a single bean by its type.
     *
     * <p>This method returns null if there is no such bean. An exception is thrown if multiple
     * beans of the specified type exist.
     *
     * @param type THe type of the bean to lookup.
     * @throws MultipleBeansException If there are multiple beans of the specified type in the
     *     context.
     */
    public static final <T> T bean(Class<T> type) throws IllegalArgumentException {
        checkContext(context, type.getSimpleName());
        return bean(type, context);
    }

    /**
     * Loads a single bean by its type from the specified application context.
     *
     * <p>This method returns null if there is no such bean. An exception is thrown if multiple
     * beans of the specified type exist.
     *
     * @param type THe type of the bean to lookup.
     * @param context The application context
     * @throws MultipleBeansException If there are multiple beans of the specified type in the
     *     context.
     */
    public static final <T> T bean(Class<T> type, ApplicationContext context)
            throws IllegalArgumentException {
        List<T> beans = extensions(type, context);
        if (beans.isEmpty()) {
            return null;
        }

        if (beans.size() > 1) {
            throw new MultipleBeansException(type, extensionNames(type, context));
        }

        return beans.get(0);
    }

    /**
     * Exception thrown when multiple beans implementing an extension point and only one is
     * expected.
     */
    public static class MultipleBeansException extends IllegalArgumentException {
        /** serialVersionUID */
        private static final long serialVersionUID = -8039187466594032626L;

        private final Class<?> extensionPoint;
        private final Collection<String> availableBeans;

        public MultipleBeansException(Class<?> extensionPoint, Collection<String> availableBeans) {
            super("Multiple beans of type " + extensionPoint.getName());
            this.extensionPoint = extensionPoint;
            this.availableBeans = availableBeans;
        }

        /** @return the extension point */
        public Class<?> getExtensionPoint() {
            return extensionPoint;
        }

        /** @return the names of the beans */
        public Collection<String> getAvailableBeans() {
            return availableBeans;
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            extensionsCache.clear();
            singletonBeanCache.clear();
        }
    }

    /** Checks the context, if null will issue a warning. */
    static void checkContext(ApplicationContext context, String bean) {
        if (context == null && isSpringContext) {
            LOGGER.warning("Extension lookup '" + bean + "', but ApplicationContext is unset.");
        }
    }

    /**
     * Looks up for a named string property in the order defined by {@link #getProperty(String,
     * ApplicationContext)} using the internally cached spring application context.
     *
     * <p>Care should be taken when using this method. It should not be called during startup or
     * from tests cases as the internal context will not have been set.
     *
     * @param propertyName The property name to lookup.
     * @return The property value, or null if not found
     */
    public static String getProperty(String propertyName) {
        return getProperty(propertyName, context);
    }

    /**
     * Looks up for a named string property into the following contexts (in order):
     *
     * <ul>
     *   <li>System Property
     *   <li>web.xml init parameters (only works if the context is a {@link WebApplicationContext}
     *   <li>Environment variable
     * </ul>
     *
     * and returns the first non null, non empty value found.
     *
     * @param propertyName The property name to be searched
     * @param context The Spring context (may be null)
     * @return The property value, or null if not found
     */
    public static String getProperty(String propertyName, ApplicationContext context) {
        if (context instanceof WebApplicationContext) {
            return getProperty(propertyName, ((WebApplicationContext) context).getServletContext());
        } else {
            return getProperty(propertyName, (ServletContext) null);
        }
    }

    /**
     * Looks up for a named string property into the following contexts (in order):
     *
     * <ul>
     *   <li>Test override supplied by GeoServerExtensionsHelper
     *   <li>System Property
     *   <li>web.xml init parameters
     *   <li>Environment variable
     * </ul>
     *
     * and returns the first non null, non empty value found.
     *
     * @param propertyName The property name to be searched
     * @param context The servlet context used to look into web.xml (may be null)
     * @return The property value, or null if not found
     */
    public static String getProperty(String propertyName, ServletContext context) {
        // TODO: this code comes from the data directory lookup and it's useful
        // until we provide a way for the user to manually inspect the three contexts
        // (when trying to debug why the variable they think they've set, and so on, see also
        // https://osgeo-org.atlassian.net/browse/GEOS-2343
        // Once that is fixed, we can remove the logging code that makes this method more complex
        // than strictly necessary

        final String[] typeStrs = {
            "Property override",
            "Java environment variable ",
            "Servlet context parameter ",
            "System environment variable "
        };

        String result = null;
        for (int j = 0; j < typeStrs.length; j++) {
            // Lookup section
            switch (j) {
                case 0:
                    result = propertyCache.get(propertyName);
                    break;

                case 1:
                    result = System.getProperty(propertyName);
                    break;
                case 2:
                    if (context != null) {
                        result = context.getInitParameter(propertyName);
                    }
                    break;
                case 3:
                    result = System.getenv(propertyName);
                    break;
            }

            if (result == null || result.equalsIgnoreCase("")) {
                LOGGER.finer("Found " + typeStrs[j] + ": '" + propertyName + "' to be unset");
            } else {
                break;
            }
        }

        return result;
    }

    /**
     * Search the context for indicated file.
     *
     * <p>Example:
     *
     * <pre><code>
     * File webXML = GeoServerExtensions.file("WEB-INF/web.xml");
     * </code></pre>
     *
     * @param path File name to search for
     * @return Requested file, or null if not found
     */
    public static File file(String path) {
        if (fileCache.containsKey(path)) {
            return fileCache.get(path); // override provided by GeoServerExtensionsHelper
        }
        ServletContext servletContext;
        if (context instanceof WebApplicationContext
                && (servletContext = ((WebApplicationContext) context).getServletContext())
                        != null) {
            String filepath = servletContext.getRealPath(path);
            if (filepath != null) {
                File file = new File(filepath);
                if (file.exists()) {
                    return file;
                }
            } else {
                List<String> items = Paths.names(path);
                int index = 0;
                if (index < items.size()) {

                    filepath = servletContext.getRealPath(items.get(index));
                    index++;
                    if (filepath != null) {
                        File file = new File(filepath);
                        while (index < items.size()) {
                            file = new File(file, items.get(index));
                            index++;
                        }
                        return file;
                    }
                }
            }
        }
        return null; // unavaialble
    }
}

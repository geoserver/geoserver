/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import org.geotools.factory.FactoryRegistry;
import org.geotools.util.SoftValueHashMap;
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
 * <p>
 * An instance of this class needs to be registered in spring context as follows.
 * <code>
 *         <pre>
 *         &lt;bean id="geoserverExtensions" class="org.geoserver.GeoServerExtensions"/&gt;
 *         </pre>
 * </code>
 * It must be a singleton, and must not be loaded lazily. Furthermore, this
 * bean must be loaded before any beans that use it.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project
 * @author Andrea Aime, The Open Planning Project
 *
 */
public class GeoServerExtensions implements ApplicationContextAware, ApplicationListener {
    
    /**
     * logger 
     */
    private static final Logger LOGGER = Logging.getLogger( "org.geoserver.platform" );
    
    /**
     * Caches the names of the beans for a particular type, so that the lookup (expensive)
     * wont' be needed. We cache names instead of beans because doing the latter we would
     * break the "singleton=false" directive of some beans
     */
    static SoftValueHashMap<Class, String[]> extensionsCache = new SoftValueHashMap<Class, String[]>(40);
    
    static ConcurrentHashMap<String, Object> singletonBeanCache = new ConcurrentHashMap<String, Object>();
    
    /**
     * SPI lookups are very  expensive, we need to cache them
     */
    static SoftValueHashMap<Class, List<Object>> spiCache = new SoftValueHashMap<Class, List<Object>>(40);
    
    /**
     * A static application context
     */
    static ApplicationContext context;

    /**
     * Sets the web application context to be used for looking up extensions.
     * <p>
     * This method is called by the spring container, and should never be called
     * by client code. If client needs to supply a particular context, methods
     * which take a context are available.
     * </p>
     * <p>
     * This is the context that is used for methods which dont supply their
     * own context.
     * </p>
     */
    public void setApplicationContext(ApplicationContext context)
        throws BeansException {
        GeoServerExtensions.context = context;
        extensionsCache.clear();
        singletonBeanCache.clear();
    }

    /**
     * Loads all extensions implementing or extending <code>extensionPoint</code>.
     *
     * @param extensionPoint The class or interface of the extensions.
     * @param context The context in which to perform the lookup.
     *
     * @return A collection of the extensions, or an empty collection.
     */
    public static final <T> List<T> extensions(Class<T> extensionPoint, ApplicationContext context) {
        String[] names;
        if(GeoServerExtensions.context == context){
            names = extensionsCache.get(extensionPoint);
        }else{
            names = null;
        }
        if(names == null) {
            checkContext(context);
            if ( context != null ) {
                try {
                    names = context.getBeanNamesForType(extensionPoint);
                    //update cache only if dealing with the same context
                    if(GeoServerExtensions.context == context){
                        extensionsCache.put(extensionPoint, names);
                    }
                }
                catch( Exception e ) {
                    //JD: this can happen during testing... if the application 
                    // context has been closed and a non-one time setup test is
                    // run that triggers an extension lookup
                    LOGGER.log( Level.WARNING, "bean lookup error", e );
                    return Collections.EMPTY_LIST;
                }
            }
            else {
                return Collections.EMPTY_LIST;
            }
        }
        
        // lookup extension filters preventing recursion
        List<ExtensionFilter> filters;
        if(ExtensionFilter.class.isAssignableFrom(extensionPoint)) {
            filters = Collections.emptyList();
        } else {
            filters = extensions(ExtensionFilter.class, context);
        }
        
        // look up all the beans
        List result = new ArrayList(names.length);
        for(String name : names) {
            Object bean = getBean(context, name);
            if(!excludeBean(name, bean, filters))
                result.add(bean);
        }
        
        // load from secondary extension providers
        if (!ExtensionProvider.class.isAssignableFrom(extensionPoint) && 
            !ExtensionFilter.class.isAssignableFrom(extensionPoint)) {
            
            List secondary = new ArrayList();
            for (ExtensionProvider xp : extensions(ExtensionProvider.class, context)) {
                try {
                    if (extensionPoint.isAssignableFrom(xp.getExtensionPoint())) {
                        secondary.addAll(xp.getExtensions(extensionPoint));
                    }
                }
                catch(Exception e) {
                    LOGGER.log(Level.WARNING, "Extension provider threw exception", e);
                }
            }
            filter(secondary, filters, result);
        }
        
        // load from factory spi
        List<Object> spiExtensions = spiCache.get(extensionPoint);
        if(spiExtensions == null) {
            spiExtensions = new ArrayList<Object>();
            Iterator i = FactoryRegistry.lookupProviders(extensionPoint);
            while( i.hasNext() ) {
                spiExtensions.add( i.next() );
            }
            spiCache.put(extensionPoint, spiExtensions);
        }
        // filter the beans coming from SPI (we don't cache the results
        // of the filtering, an extension filter can change its mind 
        // from call to call
        filter(spiExtensions, filters, result);
        
        //sort the results based on ExtensionPriority
        Collections.sort( result, new Comparator() {

            public int compare(Object o1, Object o2) {
                int p1 = ExtensionPriority.LOWEST;
                if ( o1 instanceof ExtensionPriority ) {
                    p1 = ((ExtensionPriority)o1).getPriority();
                }
                
                int p2 = ExtensionPriority.LOWEST;
                if ( o2 instanceof ExtensionPriority ) {
                    p2 = ((ExtensionPriority)o2).getPriority();
                }
                
                return p1 - p2;
            }
        });
        
        return result;
    }

    private static Object getBean(ApplicationContext context, String name) {
        Object bean = singletonBeanCache.get(name);
        if(bean == null) {
            bean = context.getBean(name);
            if(bean != null && context.isSingleton(name)) {
                singletonBeanCache.put(name, bean);
            } 
        }
        return bean;
    }

    private static void filter(List objects, List<ExtensionFilter> filters, List result) {
        for (Object bean : objects) {
            if(!excludeBean(null, bean, filters))
                result.add(bean);
        }
    }
    
    /**
     * Returns true if any of the {@link ExtensionFilter} asks to exclude the bean
     */
    private static boolean excludeBean(String beanId, Object bean, List<ExtensionFilter> filters) {
        for (ExtensionFilter filter : filters) {
            if(filter.exclude(beanId, bean))
                return true;
        }
        return false;
    }

    /**
     * Loads all extensions implementing or extending <code>extensionPoint</code>.
     * <p>
     * This method uses the "default" application context to perform the lookup.
     * See {@link #setApplicationContext(ApplicationContext)}.
     * </p>
     * @param extensionPoint The class or interface of the extensions.
     *
     * @return A collection of the extensions, or an empty collection.
     */
    public static final <T> List<T> extensions(Class<T> extensionPoint) {
        return extensions(extensionPoint, context);
    }
    
    /**
     * Returns a specific bean given its name
     * @param name
     * @return
     */
    public static final Object bean(String name) {
        return bean(name, context);
    }
    
    /**
     * Returns a specific bean given its name with a specified application context.
     *
     */
    public static final Object bean(String name, ApplicationContext context) {
        checkContext(context);
        return context != null ? getBean(context, name) : null;
    }

    /**
     * Loads a single bean by its type.
     * <p>
     * This method returns null if there is no such bean. An exception is thrown
     * if multiple beans of the specified type exist.
     * </p>
     *
     * @param type THe type of the bean to lookup.
     * 
     * @throws IllegalArgumentException If there are multiple beans of the specified
     * type in the context. 
     */
    public static final <T> T bean(Class<T> type) throws IllegalArgumentException {
        checkContext(context);
        return context != null ? bean( type, context ) : null;
    }
    
    /**
     * Loads a single bean by its type from the specified application context.
     * <p>
     * This method returns null if there is no such bean. An exception is thrown
     * if multiple beans of the specified type exist.
     * </p>
     *
     * @param type THe type of the bean to lookup.
     * @param context The application context
     * 
     * @throws IllegalArgumentException If there are multiple beans of the specified
     * type in the context. 
     */
    public static final <T> T bean(Class<T> type, ApplicationContext context) throws IllegalArgumentException {
        List<T> beans = extensions(type,context);
        if ( beans.isEmpty() ) {
            return null;
        }
        
        if ( beans.size() > 1 ) {
            throw new IllegalArgumentException( "Multiple beans of type " + type.getName() );
        }
        
        return beans.get( 0 );
    }
    
    public void onApplicationEvent(ApplicationEvent event) {
        if(event instanceof ContextRefreshedEvent) { 
            extensionsCache.clear();
            singletonBeanCache.clear();
        }
    }
    
    /**
     * Checks the context, if null will issue a warning.
     */
    static void checkContext(ApplicationContext context) {
        if ( context == null ) {
            LOGGER.warning( "Extension lookup occured, but ApplicationContext is unset.");
        }
    }
    
    /**
     * Looks up for a named string property in the order defined by 
     * {@link #getProperty(String, ApplicationContext)} using the internally cached spring 
     * application context.
     * <p>
     * Care should be taken when using this method. It should not be called during startup or from 
     * tests cases as the internal context will not have been set.
     * </p>
     * @param propertyName The property name to lookup.
     * 
     * @return The property value, or null if not found 
     */
    public static String getProperty(String propertyName) {
        return getProperty(propertyName, context);
    }
    
    /**
     * Looks up for a named string property into the following contexts (in order):
     * <ul>
     * <li>System Property</li>
     * <li>web.xml init parameters (only works if the context is a {@link WebApplicationContext}</li>
     * <li>Environment variable</li>
     * </ul>
     * and returns the first non null, non empty value found.
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
     * <ul>
     * <li>System Property</li>
     * <li>web.xml init parameters</li>
     * <li>Environment variable</li>
     * </ul>
     * and returns the first non null, non empty value found.
     * @param propertyName The property name to be searched
     * @param context The servlet context used to look into web.xml (may be null)
     * @return The property value, or null if not found
     */
    public static String getProperty(String propertyName, ServletContext context) {
        // TODO: this code comes from the data directory lookup and it's useful as 
        // long as we don't provide a way for the user to manually inspect the three contexts
        // (when trying to debug why the variable they thing they've set, and so on, see also
        // http://jira.codehaus.org/browse/GEOS-2343
        // Once that is fixed, we can remove the logging code that makes this method more complex
        // than strictly necessary

        final String[] typeStrs = { "Java environment variable ", "Servlet context parameter ",
                "System environment variable " };

        String result = null;
        for (int j = 0; j < typeStrs.length; j++) {
            // Lookup section
            switch (j) {
            case 0:
                result = System.getProperty(propertyName);
                break;
            case 1:
                if (context != null) {
                    result = context.getInitParameter(propertyName);
                }
                break;
            case 2:
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
    
}

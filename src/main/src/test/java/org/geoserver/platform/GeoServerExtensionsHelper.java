package org.geoserver.platform;

import java.io.File;

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
 * @author Jody Garnett (Boundless)
 */
public class GeoServerExtensionsHelper {
    
    /**
     * Flag to identify use of spring context via {@link #setApplicationContext(ApplicationContext)} and
     * enable additional consistency checks for missing extensions.
     */
    public static void setIsSpringContext(boolean isSpring){
        GeoServerExtensions.isSpringContext = isSpring;
    }
    /**
     * Clear caches used by GeoServerExtensions.
     */
    public static void clear(){
        GeoServerExtensions.extensionsCache.clear();
        GeoServerExtensions.singletonBeanCache.clear();
        GeoServerExtensions.propertyCache.clear();
        GeoServerExtensions.fileCache.clear();
    }
    /**
     * Sets the web application context to be used for looking up extensions.
     * <p>
     * This is the context that is used for methods which don't supply their
     * own context.
     * </p>
     * @param context ApplicationContext used to lookup extensions
     */
    public static void init(ApplicationContext context)
        throws BeansException {
        GeoServerExtensions.isSpringContext = false;
        GeoServerExtensions.context = context;
        clear();
    }
    
    /**
     * Directly register singleton for use with {@link GeoServerExtensions#bean(String)} (and {@link GeoServerExtensions#bean(Class)}).
     * <p>
     * If GeoServerExtensions has been configured with a context
     * @param name Singleton name
     * @param bean Singleton
     */
    public static void singleton( String name, Object bean ){
        if( GeoServerExtensions.context != null ){
            if( GeoServerExtensions.context.containsBean(name) ){
                Object conflict = GeoServerExtensions.context.getBean(name);
                if( bean != conflict ){
                    GeoServerExtensions.LOGGER.fine("ApplicationContext override "+name+": "+conflict);
                }
            }
        }
        else {
            GeoServerExtensions.isSpringContext = false;
        }
        if( name == null || bean == null ){
            return;
        }
        GeoServerExtensions.singletonBeanCache.put( name,  bean );
        Class<?> type = bean.getClass();
        GeoServerExtensions.extensionsCache.put( type, new String[]{ name } );
    }
    
    /**
     * Directly register property for use with {@link GeoServerExtensions#getProperty(String)}.
     */    
    public static void property(String propertyName, String property ){
        GeoServerExtensions.propertyCache.put(propertyName,  property );
    }
    /**
     * Directly register file for use with {@link GeoServerExtensions#file(String)}.
     */ 
    public static void file(String path, File file ){
        GeoServerExtensions.fileCache.put(path, file);
    }
}

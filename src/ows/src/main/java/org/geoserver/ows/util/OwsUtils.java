/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.geoserver.platform.ServiceException;
import org.geotools.util.SoftValueHashMap;


/**
 * Utility class for performing reflective operations and other ows utility functions.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class OwsUtils {
    
    /**
     * Reflectively sets a property on an object.
     * <p>
     * This method uses {@link #setter(Class, String, Class)} to locate teh setter 
     * method for the property and then invokes it with teh specified <tt>value</tt>.
     * </p>
     * @param object The target object. 
     * @param property The property to set.
     * @param value The value to set, may be <code>null</code>.
     * 
     * @throws IllegalArgumentException If no such property exists.
     * @throws RuntimeException If an error occurs setting the property
     */
    public static void set( Object object, String property, Object value ) throws IllegalArgumentException {
        Method s = setter( object.getClass(), property, value != null ? value.getClass() : null );
        if ( s == null ) {
            throw new IllegalArgumentException( "No such property '" + property + "' for object " + object ); 
        }
        
        try {
            s.invoke( object, value );
        } 
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
    }
    
    /**
     * Cache of reflection information about a class, keyed by class.
     */
    static Map<Class, ClassProperties> classPropertiesCache = new SoftValueHashMap<Class, ClassProperties>();
    
    /**
     * Accessor for the class to property info cache.
     */
    static ClassProperties classProperties(Class clazz) {
        // SoftValueHashMap is thread safe, no need to synch
        ClassProperties properties = classPropertiesCache.get(clazz);
        if(properties == null) {
            properties = new ClassProperties(clazz);
            classPropertiesCache.put(clazz, properties);
        }
        return properties;
    }
    
    /**
     * Returns the properties object describing the properties of a class.
     */
    public static ClassProperties getClassProperties( Class clazz ) {
        return classProperties(clazz);
    }

    /**
     * Returns a setter method for a property of java bean.
     * <p>
     * The <tt>type</tt> parameter may be <code>null</code> to indicate the 
     * the setter for the property should be returned regardless of the type. If
     * not null it will be used to filter the returned method.
     * </p>
     * @param clazz The type of the bean.
     * @param property The property name.
     * @param type The type of the property, may be <code>null</code>.
     *
     * @return The setter method, or <code>null</code> if not found.
     */
    public static Method setter(Class clazz, String property, Class type) {
        return classProperties(clazz).setter(property, type);
    }

    /**
     * Reflectively determines if an object has a specified property.

     * @param object The target object. 
     * @param property The property to lookup.
     * 
     * @return True if the property exists, otherwise false.
     */
    public static boolean has(Object object, String property) {
        return getter(object.getClass(), property, null) != null;
    }
    
    /**
     * Reflectively gets a property from an object.
     * <p>
     * This method uses {@link #getter(Class, String, Class)} to locate the getter 
     * method for the property and then invokes it.
     * </p>
     * @param object The target object. 
     * @param property The property to set.
     * 
     * @throws IllegalArgumentException If no such property exists.
     * @throws RuntimeException If an error occurs getting the property
     */
    public static Object get(Object object, String property) {
        Method g = getter( object.getClass(), property, null );
        if ( g == null ) {
            throw new IllegalArgumentException("No such property '" + property + "' for object " + object );
        }
      
        try {
            return g.invoke( object, null );
        } 
        catch( Exception e ) {
            throw new RuntimeException( e );
        }
    }
    
    /**
     * Returns a getter method for a property of java bean.
     *
     * @param clazz The type of the bean.
     * @param property The property name.
     * @param type The type of the property, may be null.
     *
     * @return The setter method, or <code>null</code> if not found.
     */
    public static Method getter(Class clazz, String property, Class type) {
        return classProperties(clazz).getter(property, type);
    }

    /**
     * Reflectivley retreives a propety from a java bean.
     *
     * @param object The java bean.
     * @param property The property to retreive.
     * @param type Teh type of the property to retreive.
     *
     * @return The property, or null if it could not be found..
     */
    public static Object property(Object object, String property, Class type) {
        Method getter = getter(object.getClass(), property, type);

        if (getter != null) {
            try {
                return getter.invoke(object, null);
            } catch (Exception e) {
                //TODO: log this
            }
        }

        return null;
    }

    /**
     * Returns a method with a pariticular name of a class, ignoring method
     * paramters.
     *
     * @param clazz The class
     * @param name The name of the method.
     *
     * @return The method, or <code>null</code> if it could not be found.
     */
    public static Method method(Class clazz, String name) {
        return classProperties(clazz).method( name );
    }

    /**
     * Returns an object of a particular type in a list of objects of
     * various types.
     *
     * @param parameters A list of objects, of various types.
     * @param type The type of paramter to be returned.
     *
     * @return The object of the specified type, or <code>null</code>
     */
    @SuppressWarnings("unchecked")
    public static <T extends Object> T parameter(Object[] parameters, Class<T> type) {
        for (int i = 0; i < parameters.length; i++) {
            Object parameter = parameters[i];

            if ((parameter != null) && type.isAssignableFrom(parameter.getClass())) {
                return (T) parameter;
            }
        }

        return null;
    }
    
    /**
     * Dumps a stack of service exception messages to a string buffer.
     *
     */
    public static void dumpExceptionMessages(ServiceException e, StringBuffer s, boolean xmlEscape) {
        Throwable ex = e;
        do {
            Throwable cause = ex.getCause();
            final String message = ex.getMessage();
            String lastMessage = message;
            if(!"".equals(message)) {
                if(xmlEscape)
                    s.append(ResponseUtils.encodeXML(message));
                else
                    s.append(message);
                if(ex instanceof ServiceException) {
                    for ( Iterator t = ((ServiceException) ex).getExceptionText().iterator(); t.hasNext(); ) {
                        s.append("\n");
                        String msg = (String) t.next();
                        if(!lastMessage.equals(msg)) {
                            if(xmlEscape)
                                s.append(ResponseUtils.encodeXML(msg));
                            else
                                s.append(msg);
                            lastMessage = msg;
                        }
                        
                    }
                }
                if(cause != null)
                    s.append("\n");
            }
            
            // avoid infinite loop if someone did the very stupid thing of setting
            // the cause as the exception itself (I only found this situation once, but...)
            if(ex == cause || cause == null)
                break;
            else
                ex = cause;
        } while(true);
    }
    
    /**
     * Copies properties from one object to another.
     * 
     * @param source The source object. 
     * @param target The target object.
     * @param clazz The class of source and target.
     */
    public static <T> void copy(T source, T target, Class<T> clazz) {
        ClassProperties properties = getClassProperties(clazz);
        for (String p : properties.properties()) {
            Method getter = properties.getter(p, null);
            if (getter == null) {
                continue; // should not really happen
            }

            Class type = getter.getReturnType();
            Method setter = properties.setter(p, type);

            // do a check for read only before calling the getter to avoid an uneccesary call
            if (setter == null
                    && !(Collection.class.isAssignableFrom(type) || Map.class
                            .isAssignableFrom(type))) {
                // read only
                continue;
            }

            try {
                Object newValue = getter.invoke(source, null);
                if (newValue == null) {
                    continue;
                    // TODO: make this a flag whether to overwrite with null values
                }
                if (setter == null) {
                    if (Collection.class.isAssignableFrom(type)) {
                        updateCollectionProperty(target, (Collection) newValue, getter);
                    } else if (Map.class.isAssignableFrom(type)) {
                        updateMapProperty(target, (Map) newValue, getter);
                    }
                    continue;
                }

                setter.invoke(target, newValue);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
   
    /**
     * Helper method for updating a collection based property.
     */
    static void updateCollectionProperty(Object object, Collection newValue, Method getter)
            throws Exception {
        Collection oldValue = (Collection) getter.invoke(object, null);
        oldValue.clear();
        oldValue.addAll(newValue);
    }

    /**
     * Helper method for updating a map based property.
     */
    static void updateMapProperty(Object object, Map newValue, Method getter) throws Exception {
        Map oldValue = (Map) getter.invoke(object, null);
        oldValue.clear();
        oldValue.putAll(newValue);
    }

}

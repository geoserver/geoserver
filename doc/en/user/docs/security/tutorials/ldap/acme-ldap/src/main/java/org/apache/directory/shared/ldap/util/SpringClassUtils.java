/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */

package org.apache.directory.shared.ldap.util;


import java.beans.Introspector;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;


/**
 * Miscellaneous class utility methods. Mainly for internal use within the
 * framework; consider Jakarta's Commons Lang for a more comprehensive suite
 * of utilities.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class SpringClassUtils
{

    /** Suffix for array class names */
    public static final String ARRAY_SUFFIX = "[]";

    /** All primitive classes */
    private static Class[] PRIMITIVE_CLASSES =
        { boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class };

    /** The package separator character '.' */
    private static final char PACKAGE_SEPARATOR_CHAR = '.';

    /** The inner class separator character '$' */
    private static final char INNER_CLASS_SEPARATOR_CHAR = '$';

    /** The CGLIB class separator character "$$" */
    private static final String CGLIB_CLASS_SEPARATOR_CHAR = "$$";


    /**
     * Return a default ClassLoader to use (never <code>null</code>).
     * Returns the thread context ClassLoader, if available.
     * The ClassLoader that loaded the ClassUtils class will be used as fallback.
     * <p>Call this method if you intend to use the thread context ClassLoader
     * in a scenario where you absolutely need a non-null ClassLoader reference:
     * for example, for class path resource loading (but not necessarily for
     * <code>Class.forName</code>, which accepts a <code>null</code> ClassLoader
     * reference as well).
     * @see java.lang.Thread#getContextClassLoader()
     */
    public static ClassLoader getDefaultClassLoader()
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if ( cl == null )
        {
            // No thread context class loader -> use class loader of this class.
            cl = SpringClassUtils.class.getClassLoader();
        }
        return cl;
    }


    /**
     * Replacement for <code>Class.forName()</code> that also returns Class instances
     * for primitives (like "int") and array class names (like "String[]").
     * <p>Always uses the thread context class loader.
     * @param name the name of the Class
     * @return Class instance for the supplied name
     * @see java.lang.Class#forName(String, boolean, ClassLoader)
     * @see java.lang.Thread#getContextClassLoader()
     */
    public static Class forName( String name ) throws ClassNotFoundException
    {
        return forName( name, Thread.currentThread().getContextClassLoader() );
    }


    /**
     * Replacement for <code>Class.forName()</code> that also returns Class instances
     * for primitives (like "int") and array class names (like "String[]").
     * @param name the name of the Class
     * @param classLoader the class loader to use
     * @return Class instance for the supplied name
     * @see java.lang.Class#forName(String, boolean, ClassLoader)
     * @see java.lang.Thread#getContextClassLoader()
     */
    public static Class forName( String name, ClassLoader classLoader ) throws ClassNotFoundException
    {
        Class clazz = resolvePrimitiveClassName( name );
        if ( clazz != null )
        {
            return clazz;
        }
        if ( name.endsWith( ARRAY_SUFFIX ) )
        {
            // special handling for array class names
            String elementClassName = name.substring( 0, name.length() - ARRAY_SUFFIX.length() );
            Class elementClass = SpringClassUtils.forName( elementClassName, classLoader );
            return Array.newInstance( elementClass, 0 ).getClass();
        }
        return Class.forName( name, true, classLoader );
    }


    /**
     * Resolve the given class name as primitive class, if appropriate.
     * @param name the name of the potentially primitive class
     * @return the primitive class, or <code>null</code> if the name does not denote
     * a primitive class
     */
    public static Class resolvePrimitiveClassName( String name )
    {
        // Most class names will be quite long, considering that they
        // SHOULD sit in a package, so a length check is worthwhile.
        if ( name.length() <= 8 )
        {
            // could be a primitive - likely
            for ( int i = 0; i < PRIMITIVE_CLASSES.length; i++ )
            {
                Class clazz = PRIMITIVE_CLASSES[i];
                if ( clazz.getName().equals( name ) )
                {
                    return clazz;
                }
            }
        }
        return null;
    }


    /**
     * Return the short string name of a Java class in decapitalized
     * JavaBeans property format.
     * @param clazz the class
     * @return the short name rendered in a standard JavaBeans property format
     * @see java.beans.Introspector#decapitalize(String)
     */
    public static String getShortNameAsProperty( Class clazz )
    {
        return Introspector.decapitalize( getShortName( clazz ) );
    }


    /**
     * Get the class name without the qualified package name.
     * @param clazz the class to get the short name for
     * @return the class name of the class without the package name
     * @throws IllegalArgumentException if the class is null
     */
    public static String getShortName( Class clazz )
    {
        return getShortName( clazz.getName() );
    }


    /**
     * Get the class name without the qualified package name.
     * @param className the className to get the short name for
     * @return the class name of the class without the package name
     * @throws IllegalArgumentException if the className is empty
     */
    public static String getShortName( String className )
    {
        //Assert.hasLength(className, "class name must not be empty");
        int lastDotIndex = className.lastIndexOf( PACKAGE_SEPARATOR_CHAR );
        int nameEndIndex = className.indexOf( CGLIB_CLASS_SEPARATOR_CHAR );
        if ( nameEndIndex == -1 )
        {
            nameEndIndex = className.length();
        }
        String shortName = className.substring( lastDotIndex + 1, nameEndIndex );
        shortName = shortName.replace( INNER_CLASS_SEPARATOR_CHAR, PACKAGE_SEPARATOR_CHAR );
        return shortName;
    }


    /**
     * Return the qualified name of the given method, consisting of
     * fully qualified interface/class name + "." + method name.
     * @param method the method
     * @return the qualified name of the method
     */
    public static String getQualifiedMethodName( Method method )
    {
        //Assert.notNull(method, "Method must not be empty");
        return method.getDeclaringClass().getName() + "." + method.getName();
    }


    /**
     * Determine whether the given class has a method with the given signature.
     * Essentially translates <code>NoSuchMethodException</code> to "false".
     * @param clazz the clazz to analyze
     * @param methodName the name of the method
     * @param paramTypes the parameter types of the method
     */
    public static boolean hasMethod( Class clazz, String methodName, Class[] paramTypes )
    {
        try
        {
            clazz.getMethod( methodName, paramTypes );
            return true;
        }
        catch ( NoSuchMethodException ex )
        {
            return false;
        }
    }


    /**
     * Return the number of methods with a given name (with any argument types),
     * for the given class and/or its superclasses. Includes non-public methods.
     * @param clazz the clazz to check
     * @param methodName the name of the method
     * @return the number of methods with the given name
     */
    public static int getMethodCountForName( Class clazz, String methodName )
    {
        int count = 0;
        do
        {
            for ( int i = 0; i < clazz.getDeclaredMethods().length; i++ )
            {
                Method method = clazz.getDeclaredMethods()[i];
                if ( methodName.equals( method.getName() ) )
                {
                    count++;
                }
            }
            clazz = clazz.getSuperclass();
        }
        while ( clazz != null );
        return count;
    }


    /**
     * Does the given class and/or its superclasses at least have one or more
     * methods (with any argument types)? Includes non-public methods.
     * @param clazz the clazz to check
     * @param methodName the name of the method
     * @return whether there is at least one method with the given name
     */
    public static boolean hasAtLeastOneMethodWithName( Class clazz, String methodName )
    {
        do
        {
            for ( int i = 0; i < clazz.getDeclaredMethods().length; i++ )
            {
                Method method = clazz.getDeclaredMethods()[i];
                
                if ( methodName.equals( method.getName() ) )
                {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        }
        while ( clazz != null );
        
        return false;
    }


    /**
     * Return a static method of a class.
     * @param methodName the static method name
     * @param clazz the class which defines the method
     * @param args the parameter types to the method
     * @return the static method, or <code>null</code> if no static method was found
     * @throws IllegalArgumentException if the method name is blank or the clazz is null
     */
    public static Method getStaticMethod( Class clazz, String methodName, Class[] args )
    {
        try
        {
            Method method = clazz.getDeclaredMethod( methodName, args );
            
            if ( ( method.getModifiers() & Modifier.STATIC ) != 0 )
            {
                return method;
            }
        }
        catch ( NoSuchMethodException ex )
        {
        }
        
        return null;
    }


    /**
     * Return a path suitable for use with ClassLoader.getResource (also
     * suitable for use with Class.getResource by prepending a slash ('/') to
     * the return value. Built by taking the package of the specified class
     * file, converting all dots ('.') to slashes ('/'), adding a trailing slash
     * if necesssary, and concatenating the specified resource name to this.
     * <br/>As such, this function may be used to build a path suitable for
     * loading a resource file that is in the same package as a class file,
     * although {link org.springframework.core.io.ClassPathResource} is usually
     * even more convenient.
     * @param clazz the Class whose package will be used as the base
     * @param resourceName the resource name to append. A leading slash is optional.
     * @return the built-up resource path
     * @see java.lang.ClassLoader#getResource
     * @see java.lang.Class#getResource
     */
    public static String addResourcePathToPackagePath( Class clazz, String resourceName )
    {
        if ( !resourceName.startsWith( "/" ) )
        {
            return classPackageAsResourcePath( clazz ) + "/" + resourceName;
        }

        return classPackageAsResourcePath( clazz ) + resourceName;
    }


    /**
     * Given an input class object, return a string which consists of the
     * class's package name as a pathname, i.e., all dots ('.') are replaced by
     * slashes ('/'). Neither a leading nor trailing slash is added. The result
     * could be concatenated with a slash and the name of a resource, and fed
     * directly to ClassLoader.getResource(). For it to be fed to Class.getResource,
     * a leading slash would also have to be prepended to the return value.
     * @param clazz the input class. A null value or the default (empty) package
     * will result in an empty string ("") being returned.
     * @return a path which represents the package name
     * @see java.lang.ClassLoader#getResource
     * @see java.lang.Class#getResource
     */
    public static String classPackageAsResourcePath( Class clazz )
    {
        if ( clazz == null || clazz.getPackage() == null )
        {
            return "";
        }
        
        return clazz.getPackage().getName().replace( '.', '/' );
    }


    /**
     * Return all interfaces that the given object implements as array,
     * including ones implemented by superclasses.
     * @param object the object to analyse for interfaces
     * @return all interfaces that the given object implements as array
     */
    public static Class[] getAllInterfaces( Object object )
    {
        Set<Class> interfaces = getAllInterfacesAsSet( object );
        return interfaces.toArray( new Class[interfaces.size()] );
    }


    /**
     * Return all interfaces that the given class implements as array,
     * including ones implemented by superclasses.
     * @param clazz the class to analyse for interfaces
     * @return all interfaces that the given object implements as array
     */
    public static Class[] getAllInterfacesForClass( Class clazz )
    {
        Set<Class> interfaces = getAllInterfacesForClassAsSet( clazz );
        return interfaces.toArray( new Class[interfaces.size()] );
    }


    /**
     * Return all interfaces that the given object implements as List,
     * including ones implemented by superclasses.
     * @param object the object to analyse for interfaces
     * @return all interfaces that the given object implements as List
     */
    public static Set<Class> getAllInterfacesAsSet( Object object )
    {
        return getAllInterfacesForClassAsSet( object.getClass() );
    }


    /**
     * Return all interfaces that the given class implements as Set,
     * including ones implemented by superclasses.
     * @param clazz the class to analyse for interfaces
     * @return all interfaces that the given object implements as Set
     */
    public static Set<Class> getAllInterfacesForClassAsSet( Class clazz )
    {
        Set<Class> interfaces = new HashSet<Class>();

        while ( clazz != null )
        {
            for ( int i = 0; i < clazz.getInterfaces().length; i++ )
            {
                Class ifc = clazz.getInterfaces()[i];
                interfaces.add( ifc );
            }

            clazz = clazz.getSuperclass();
        }

        return interfaces;
    }

}

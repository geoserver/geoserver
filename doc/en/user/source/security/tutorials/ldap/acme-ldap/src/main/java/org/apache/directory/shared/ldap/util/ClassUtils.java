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


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * Operates on classes without using reflection.
 * </p>
 * <p>
 * This class handles invalid <code>null</code> inputs as best it can. Each
 * method documents its behaviour in more detail.
 * </p>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ClassUtils
{
    private static final String EMPTY = "";

    /**
     * <p>
     * The package separator character: <code>'&#x2e;' == .</code>.
     * </p>
     */
    public static final char PACKAGE_SEPARATOR_CHAR = '.';

    /**
     * <p>
     * The package separator String: <code>"&#x2e;"</code>.
     * </p>
     */
    public static final String PACKAGE_SEPARATOR = String.valueOf( PACKAGE_SEPARATOR_CHAR );

    /**
     * <p>
     * The inner class separator character: <code>'$' == $</code>.
     * </p>
     */
    public static final char INNER_CLASS_SEPARATOR_CHAR = '$';

    /**
     * <p>
     * The inner class separator String: <code>"$"</code>.
     * </p>
     */
    public static final String INNER_CLASS_SEPARATOR = String.valueOf( INNER_CLASS_SEPARATOR_CHAR );

    /**
     * Maps primitive <code>Class</code>es to their corresponding wrapper
     * <code>Class</code>.
     */
    private static Map<Class<?>,Class<?>> primitiveWrapperMap = new HashMap<Class<?>,Class<?>>();
    
    static
    {
        primitiveWrapperMap.put( Boolean.TYPE, Boolean.class );
        primitiveWrapperMap.put( Byte.TYPE, Byte.class );
        primitiveWrapperMap.put( Character.TYPE, Character.class );
        primitiveWrapperMap.put( Short.TYPE, Short.class );
        primitiveWrapperMap.put( Integer.TYPE, Integer.class );
        primitiveWrapperMap.put( Long.TYPE, Long.class );
        primitiveWrapperMap.put( Double.TYPE, Double.class );
        primitiveWrapperMap.put( Float.TYPE, Float.class );
    }


    /**
     * <p>
     * ClassUtils instances should NOT be constructed in standard programming.
     * Instead, the class should be used as
     * <code>ClassUtils.getShortClassName(cls)</code>.
     * </p>
     * <p>
     * This constructor is public to permit tools that require a JavaBean
     * instance to operate.
     * </p>
     */
    public ClassUtils()
    {
    }


    // Short class name
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Gets the class name minus the package name for an <code>Object</code>.
     * </p>
     * 
     * @param object
     *            the class to get the short name for, may be null
     * @param valueIfNull
     *            the value to return if null
     * @return the class name of the object without the package name, or the
     *         null value
     */
    public static String getShortClassName( Object object, String valueIfNull )
    {
        if ( object == null )
        {
            return valueIfNull;
        }
        return getShortClassName( object.getClass().getName() );
    }


    /**
     * <p>
     * Gets the class name minus the package name from a <code>Class</code>.
     * </p>
     * 
     * @param cls
     *            the class to get the short name for.
     * @return the class name without the package name or an empty string
     */
    public static String getShortClassName( Class<?> cls )
    {
        if ( cls == null )
        {
            return EMPTY;
        }
        
        return getShortClassName( cls.getName() );
    }


    /**
     * <p>
     * Gets the class name minus the package name from a String.
     * </p>
     * <p>
     * The string passed in is assumed to be a class name - it is not checked.
     * </p>
     * 
     * @param className
     *            the className to get the short name for
     * @return the class name of the class without the package name or an empty
     *         string
     */
    public static String getShortClassName( String className )
    {
        if ( className == null )
        {
            return EMPTY;
        }
        
        if ( className.length() == 0 )
        {
            return EMPTY;
        }
        
        char[] chars = className.toCharArray();
        int lastDot = 0;
        
        for ( int i = 0; i < chars.length; i++ )
        {
            if ( chars[i] == PACKAGE_SEPARATOR_CHAR )
            {
                lastDot = i + 1;
            }
            else if ( chars[i] == INNER_CLASS_SEPARATOR_CHAR )
            { // handle inner classes
                chars[i] = PACKAGE_SEPARATOR_CHAR;
            }
        }
        
        return new String( chars, lastDot, chars.length - lastDot );
    }


    // Package name
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Gets the package name of an <code>Object</code>.
     * </p>
     * 
     * @param object
     *            the class to get the package name for, may be null
     * @param valueIfNull
     *            the value to return if null
     * @return the package name of the object, or the null value
     */
    public static String getPackageName( Object object, String valueIfNull )
    {
        if ( object == null )
        {
            return valueIfNull;
        }
        
        return getPackageName( object.getClass().getName() );
    }


    /**
     * <p>
     * Gets the package name of a <code>Class</code>.
     * </p>
     * 
     * @param cls
     *            the class to get the package name for, may be
     *            <code>null</code>.
     * @return the package name or an empty string
     */
    public static String getPackageName( Class<?> cls )
    {
        if ( cls == null )
        {
            return EMPTY;
        }
        
        return getPackageName( cls.getName() );
    }


    /**
     * <p>
     * Gets the package name from a <code>String</code>.
     * </p>
     * <p>
     * The string passed in is assumed to be a class name - it is not checked.
     * </p>
     * <p>
     * If the class is unpackaged, return an empty string.
     * </p>
     * 
     * @param className
     *            the className to get the package name for, may be
     *            <code>null</code>
     * @return the package name or an empty string
     */
    public static String getPackageName( String className )
    {
        if ( className == null )
        {
            return EMPTY;
        }
        
        int i = className.lastIndexOf( PACKAGE_SEPARATOR_CHAR );
        
        if ( i == -1 )
        {
            return EMPTY;
        }
        
        return className.substring( 0, i );
    }


    // Superclasses/Superinterfaces
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Gets a <code>List</code> of superclasses for the given class.
     * </p>
     * 
     * @param cls
     *            the class to look up, may be <code>null</code>
     * @return the <code>List</code> of superclasses in order going up from
     *         this one <code>null</code> if null input
     */
    public static List<Class<?>> getAllSuperclasses( Class<?> cls )
    {
        if ( cls == null )
        {
            return null;
        }
        
        List<Class<?>> classes = new ArrayList<Class<?>>();
        
        Class<?> superclass = cls.getSuperclass();
        
        while ( superclass != null )
        {
            classes.add( superclass );
            superclass = superclass.getSuperclass();
        }
        
        return classes;
    }


    /**
     * <p>
     * Gets a <code>List</code> of all interfaces implemented by the given
     * class and its superclasses.
     * </p>
     * <p>
     * The order is determined by looking through each interface in turn as
     * declared in the source file and following its hierarchy up. Then each
     * superclass is considered in the same way. Later duplicates are ignored,
     * so the order is maintained.
     * </p>
     * 
     * @param cls
     *            the class to look up, may be <code>null</code>
     * @return the <code>List</code> of interfaces in order, <code>null</code>
     *         if null input
     */
    public static List<Class<?>> getAllInterfaces( Class<?> cls )
    {
        if ( cls == null )
        {
            return null;
        }
        
        List<Class<?>> list = new ArrayList<Class<?>>();
        
        while ( cls != null )
        {
            Class<?>[] interfaces = cls.getInterfaces();
            
            for ( Class<?> interf:interfaces )
            {
                if ( list.contains( interf ) == false )
                {
                    list.add( interf );
                }
                
                for ( Class<?> superIntf:getAllInterfaces( interf ) )
                {
                    if ( list.contains( superIntf ) == false )
                    {
                        list.add( superIntf );
                    }
                }
            }
            
            cls = cls.getSuperclass();
        }
        
        return list;
    }


    // Convert list
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Given a <code>List</code> of class names, this method converts them
     * into classes.
     * </p>
     * <p>
     * A new <code>List</code> is returned. If the class name cannot be found,
     * <code>null</code> is stored in the <code>List</code>. If the class
     * name in the <code>List</code> is <code>null</code>,
     * <code>null</code> is stored in the output <code>List</code>.
     * </p>
     * 
     * @param classNames
     *            the classNames to change
     * @return a <code>List</code> of Class objects corresponding to the class
     *         names, <code>null</code> if null input
     * @throws ClassCastException
     *             if classNames contains a non String entry
     */
    public static List<Class<?>> convertClassNamesToClasses( List<String> classNames )
    {
        if ( classNames == null )
        {
            return null;
        }
        
        List<Class<?>> classes = new ArrayList<Class<?>>( classNames.size() );
        
        for ( String className:classNames )
        {
            try
            {
                classes.add( Class.forName( className ) );
            }
            catch ( Exception ex )
            {
                classes.add( null );
            }
        }
        
        return classes;
    }


    /**
     * <p>
     * Given a <code>List</code> of <code>Class</code> objects, this method
     * converts them into class names.
     * </p>
     * <p>
     * A new <code>List</code> is returned. <code>null</code> objects will
     * be copied into the returned list as <code>null</code>.
     * </p>
     * 
     * @param classes
     *            the classes to change
     * @return a <code>List</code> of class names corresponding to the Class
     *         objects, <code>null</code> if null input
     * @throws ClassCastException
     *             if <code>classes</code> contains a non-<code>Class</code>
     *             entry
     */
    public static List<String> convertClassesToClassNames( List<Class<?>> classes )
    {
        if ( classes == null )
        {
            return null;
        }
        
        List<String> classNames = new ArrayList<String>( classes.size() );
        
        for ( Class<?> clazz:classes )
        {
            if ( clazz == null )
            {
                classNames.add( null );
            }
            else
            {
                classNames.add( clazz.getName() );
            }
        }
        
        return classNames;
    }


    // Is assignable
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Checks if an array of Classes can be assigned to another array of
     * Classes.
     * </p>
     * <p>
     * This method calls {@link #isAssignable(Class, Class) isAssignable} for
     * each Class pair in the input arrays. It can be used to check if a set of
     * arguments (the first parameter) are suitably compatible with a set of
     * method parameter types (the second parameter).
     * </p>
     * <p>
     * Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method, this
     * method takes into account widenings of primitive classes and
     * <code>null</code>s.
     * </p>
     * <p>
     * Primitive widenings allow an int to be assigned to a <code>long</code>,
     * <code>float</code> or <code>double</code>. This method returns the
     * correct result for these cases.
     * </p>
     * <p>
     * <code>Null</code> may be assigned to any reference type. This method
     * will return <code>true</code> if <code>null</code> is passed in and
     * the toClass is non-primitive.
     * </p>
     * <p>
     * Specifically, this method tests whether the type represented by the
     * specified <code>Class</code> parameter can be converted to the type
     * represented by this <code>Class</code> object via an identity
     * conversion widening primitive or widening reference conversion. See
     * <em><a href="http://java.sun.com/docs/books/jls/">The Java Language Specification</a></em>,
     * sections 5.1.1, 5.1.2 and 5.1.4 for details.
     * </p>
     * 
     * @param classArray
     *            the array of Classes to check, may be <code>null</code>
     * @param toClassArray
     *            the array of Classes to try to assign into, may be
     *            <code>null</code>
     * @return <code>true</code> if assignment possible
     */
    public static boolean isAssignable( Class<?>[] classArray, Class<?>[] toClassArray )
    {
        if ( ArrayUtils.isSameLength( classArray, toClassArray ) == false )
        {
            return false;
        }
        
        if ( classArray == null )
        {
            classArray = ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        
        if ( toClassArray == null )
        {
            toClassArray = ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        
        for ( int i = 0; i < classArray.length; i++ )
        {
            if ( isAssignable( classArray[i], toClassArray[i] ) == false )
            {
                return false;
            }
        }
        
        return true;
    }


    /**
     * <p>
     * Checks if one <code>Class</code> can be assigned to a variable of
     * another <code>Class</code>.
     * </p>
     * <p>
     * Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method, this
     * method takes into account widenings of primitive classes and
     * <code>null</code>s.
     * </p>
     * <p>
     * Primitive widenings allow an int to be assigned to a long, float or
     * double. This method returns the correct result for these cases.
     * </p>
     * <p>
     * <code>Null</code> may be assigned to any reference type. This method
     * will return <code>true</code> if <code>null</code> is passed in and
     * the toClass is non-primitive.
     * </p>
     * <p>
     * Specifically, this method tests whether the type represented by the
     * specified <code>Class</code> parameter can be converted to the type
     * represented by this <code>Class</code> object via an identity
     * conversion widening primitive or widening reference conversion. See
     * <em><a href="http://java.sun.com/docs/books/jls/">The Java Language Specification</a></em>,
     * sections 5.1.1, 5.1.2 and 5.1.4 for details.
     * </p>
     * 
     * @param cls
     *            the Class to check, may be null
     * @param toClass
     *            the Class to try to assign into, returns false if null
     * @return <code>true</code> if assignment possible
     */
    public static boolean isAssignable( Class<?> cls, Class<?> toClass )
    {
        if ( toClass == null )
        {
            return false;
        }
        
        // have to check for null, as isAssignableFrom doesn't
        if ( cls == null )
        {
            return !( toClass.isPrimitive() );
        }
        
        if ( cls.equals( toClass ) )
        {
            return true;
        }
        
        if ( cls.isPrimitive() )
        {
            if ( toClass.isPrimitive() == false )
            {
                return false;
            }
            
            if ( Integer.TYPE.equals( cls ) )
            {
                return Long.TYPE.equals( toClass ) || Float.TYPE.equals( toClass ) || Double.TYPE.equals( toClass );
            }
            
            if ( Long.TYPE.equals( cls ) )
            {
                return Float.TYPE.equals( toClass ) || Double.TYPE.equals( toClass );
            }
            
            if ( Boolean.TYPE.equals( cls ) )
            {
                return false;
            }
            
            if ( Double.TYPE.equals( cls ) )
            {
                return false;
            }
            
            if ( Float.TYPE.equals( cls ) )
            {
                return Double.TYPE.equals( toClass );
            }
            
            if ( Character.TYPE.equals( cls ) )
            {
                return Integer.TYPE.equals( toClass ) || Long.TYPE.equals( toClass ) || Float.TYPE.equals( toClass )
                    || Double.TYPE.equals( toClass );
            }
            
            if ( Short.TYPE.equals( cls ) )
            {
                return Integer.TYPE.equals( toClass ) || Long.TYPE.equals( toClass ) || Float.TYPE.equals( toClass )
                    || Double.TYPE.equals( toClass );
            }
            
            if ( Byte.TYPE.equals( cls ) )
            {
                return Short.TYPE.equals( toClass ) || Integer.TYPE.equals( toClass ) || Long.TYPE.equals( toClass )
                    || Float.TYPE.equals( toClass ) || Double.TYPE.equals( toClass );
            }
            
            // should never get here
            return false;
        }
        
        return toClass.isAssignableFrom( cls );
    }


    /**
     * <p>
     * Converts the specified primitive Class object to its corresponding
     * wrapper Class object.
     * </p>
     * 
     * @param cls
     *            the class to convert, may be null
     * @return the wrapper class for <code>cls</code> or <code>cls</code> if
     *         <code>cls</code> is not a primitive. <code>null</code> if
     *         null input.
     */
    public static Class<?> primitiveToWrapper( Class<?> cls )
    {
        Class<?> convertedClass = cls;
        
        if ( cls != null && cls.isPrimitive() )
        {
            convertedClass = primitiveWrapperMap.get( cls );
        }
        
        return convertedClass;
    }


    /**
     * <p>
     * Converts the specified array of primitive Class objects to an array of
     * its corresponding wrapper Class objects.
     * </p>
     * 
     * @param classes
     *            the class array to convert, may be null or empty
     * @return an array which contains for each given class, the wrapper class
     *         or the original class if class is not a primitive.
     *         <code>null</code> if null input. Empty array if an empty array
     *         passed in.
     */
    public static Class<?>[] primitivesToWrappers( Class<?>[] classes )
    {
        if ( classes == null )
        {
            return null;
        }

        if ( classes.length == 0 )
        {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }

        Class<?>[] convertedClasses = new Class[classes.length];
        
        for ( int i = 0; i < classes.length; i++ )
        {
            convertedClasses[i] = primitiveToWrapper( classes[i] );
        }
        
        return convertedClasses;
    }


    // Inner class
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Is the specified class an inner class or static nested class.
     * </p>
     * 
     * @param cls
     *            the class to check, may be null
     * @return <code>true</code> if the class is an inner or static nested
     *         class, false if not or <code>null</code>
     */
    public static boolean isInnerClass( Class<?> cls )
    {
        if ( cls == null )
        {
            return false;
        }
        
        return ( cls.getName().indexOf( INNER_CLASS_SEPARATOR_CHAR ) >= 0 );
    }

    // -----------------------------------------------------------------------
    /**
     * Compares two <code>Class</code>s by name.
     */
    private static class ClassNameComparator implements Comparator<Class<?>>
    {
        /**
         * Compares two <code>Class</code>s by name.
         * 
         * @throws ClassCastException
         *             If <code>o1</code> or <code>o2</code> are not
         *             <code>Class</code> instances.
         */
        public int compare( Class<?> class1, Class<?> class2 )
        {
            if ( class1 == null )
            {
                return class2 == null ? 0 : -1;
            }
            
            if ( class2 == null )
            {
                return 1;
            }
            
            return class1.getName().compareTo( class2.getName() );
        }
    }

    /**
     * Compares two <code>Class</code>s by name.
     */
    public static final Comparator<Class<?>> CLASS_NAME_COMPARATOR = new ClassNameComparator();

    /**
     * Compares two <code>Package</code>s by name.
     */
    private static class PackageNameComparator implements Comparator<Package>
    {

        /**
         * Compares two <code>Package</code>s by name.
         * 
         * @throws ClassCastException
         *             If <code>o1</code> or <code>o2</code> are not
         *             <code>Package</code> instances.
         */
        public int compare( Package package1, Package package2 )
        {
            if ( package1 == null )
            {
                return package2 == null ? 0 : -1;
            }

            if ( package2 == null )
            {
                return 1;
            }
            
            return package1.getName().compareTo( package2.getName() );
        }
    }

    /**
     * Compares two <code>Package</code>s by name.
     */
    public static final Comparator<Package> PACKAGE_NAME_COMPARATOR = new PackageNameComparator();

}

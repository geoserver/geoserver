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


import java.io.Serializable;


/**
 * <p>
 * Operations on <code>Object</code>.
 * </p>
 * <p>
 * This class tries to handle <code>null</code> input gracefully. An exception
 * will generally not be thrown for a <code>null</code> input. Each method
 * documents its behaviour in more detail.
 * </p>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ObjectUtils
{

    /**
     * <p>
     * Singleton used as a <code>null</code> placeholder where
     * <code>null</code> has another meaning.
     * </p>
     * <p>
     * For example, in a <code>HashMap</code> the
     * {@link java.util.HashMap#get(java.lang.Object)} method returns
     * <code>null</code> if the <code>Map</code> contains <code>null</code>
     * or if there is no matching key. The <code>Null</code> placeholder can
     * be used to distinguish between these two cases.
     * </p>
     * <p>
     * Another example is <code>Hashtable</code>, where <code>null</code>
     * cannot be stored.
     * </p>
     * <p>
     * This instance is Serializable.
     * </p>
     */
    public static final Null NULL = new Null();


    /**
     * <p>
     * <code>ObjectUtils</code> instances should NOT be constructed in
     * standard programming. Instead, the class should be used as
     * <code>ObjectUtils.defaultIfNull("a","b");</code>.
     * </p>
     * <p>
     * This constructor is public to permit tools that require a JavaBean
     * instance to operate.
     * </p>
     */
    public ObjectUtils()
    {
    }


    // Defaulting
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Returns a default value if the object passed is <code>null</code>.
     * </p>
     * 
     * <pre>
     *  ObjectUtils.defaultIfNull(null, null)      = null
     *  ObjectUtils.defaultIfNull(null, &quot;&quot;)        = &quot;&quot;
     *  ObjectUtils.defaultIfNull(null, &quot;zz&quot;)      = &quot;zz&quot;
     *  ObjectUtils.defaultIfNull(&quot;abc&quot;, *)        = &quot;abc&quot;
     *  ObjectUtils.defaultIfNull(Boolean.TRUE, *) = Boolean.TRUE
     * </pre>
     * 
     * @param object
     *            the <code>Object</code> to test, may be <code>null</code>
     * @param defaultValue
     *            the default value to return, may be <code>null</code>
     * @return <code>object</code> if it is not <code>null</code>,
     *         defaultValue otherwise
     */
    public static Object defaultIfNull( Object object, Object defaultValue )
    {
        return ( object != null ? object : defaultValue );
    }


    /**
     * <p>
     * Compares two objects for equality, where either one or both objects may
     * be <code>null</code>.
     * </p>
     * 
     * <pre>
     *  ObjectUtils.equals(null, null)                  = true
     *  ObjectUtils.equals(null, &quot;&quot;)                    = false
     *  ObjectUtils.equals(&quot;&quot;, null)                    = false
     *  ObjectUtils.equals(&quot;&quot;, &quot;&quot;)                      = true
     *  ObjectUtils.equals(Boolean.TRUE, null)          = false
     *  ObjectUtils.equals(Boolean.TRUE, &quot;true&quot;)        = false
     *  ObjectUtils.equals(Boolean.TRUE, Boolean.TRUE)  = true
     *  ObjectUtils.equals(Boolean.TRUE, Boolean.FALSE) = false
     * </pre>
     * 
     * @param object1
     *            the first object, may be <code>null</code>
     * @param object2
     *            the second object, may be <code>null</code>
     * @return <code>true</code> if the values of both objects are the same
     */
    public static boolean equals( Object object1, Object object2 )
    {
        if ( object1 == object2 )
        {
            return true;
        }
        if ( ( object1 == null ) || ( object2 == null ) )
        {
            return false;
        }
        return object1.equals( object2 );
    }


    /**
     * <p>
     * Gets the hash code of an object returning zero when the object is
     * <code>null</code>.
     * </p>
     * 
     * <pre>
     *  ObjectUtils.hashCode(null)   = 0
     *  ObjectUtils.hashCode(obj)    = obj.hashCode()
     * </pre>
     * 
     * @param obj
     *            the object to obtain the hash code of, may be
     *            <code>null</code>
     * @return the hash code of the object, or zero if null
     * @since 2.1
     */
    public static int hashCode( Object obj )
    {
        return ( ( obj == null ) ? 0 : obj.hashCode() );
    }


    // Identity ToString
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Gets the toString that would be produced by <code>Object</code> if a
     * class did not override toString itself. <code>null</code> will return
     * <code>null</code>.
     * </p>
     * 
     * <pre>
     *  ObjectUtils.identityToString(null)         = null
     *  ObjectUtils.identityToString(&quot;&quot;)           = &quot;java.lang.String@1e23&quot;
     *  ObjectUtils.identityToString(Boolean.TRUE) = &quot;java.lang.Boolean@7fa&quot;
     * </pre>
     * 
     * @param object
     *            the object to create a toString for, may be <code>null</code>
     * @return the default toString text, or <code>null</code> if
     *         <code>null</code> passed in
     */
    public static String identityToString( Object object )
    {
        if ( object == null )
        {
            return null;
        }
        return appendIdentityToString( null, object ).toString();
    }


    /**
     * <p>
     * Appends the toString that would be produced by <code>Object</code> if a
     * class did not override toString itself. <code>null</code> will return
     * <code>null</code>.
     * </p>
     * 
     * <pre>
     *  ObjectUtils.appendIdentityToString(*, null)            = null
     *  ObjectUtils.appendIdentityToString(null, &quot;&quot;)           = &quot;java.lang.String@1e23&quot;
     *  ObjectUtils.appendIdentityToString(null, Boolean.TRUE) = &quot;java.lang.Boolean@7fa&quot;
     *  ObjectUtils.appendIdentityToString(buf, Boolean.TRUE)  = buf.append(&quot;java.lang.Boolean@7fa&quot;)
     * </pre>
     * 
     * @param buffer
     *            the buffer to append to, may be <code>null</code>
     * @param object
     *            the object to create a toString for, may be <code>null</code>
     * @return the default toString text, or <code>null</code> if
     *         <code>null</code> passed in
     * @since 2.0
     */
    public static StringBuffer appendIdentityToString( StringBuffer buffer, Object object )
    {
        if ( object == null )
        {
            return null;
        }
        if ( buffer == null )
        {
            buffer = new StringBuffer();
        }
        return buffer.append( object.getClass().getName() ).append( '@' ).append(
            Integer.toHexString( System.identityHashCode( object ) ) );
    }


    // ToString
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Gets the <code>toString</code> of an <code>Object</code> returning an
     * empty string ("") if <code>null</code> input.
     * </p>
     * 
     * <pre>
     *  ObjectUtils.toString(null)         = &quot;&quot;
     *  ObjectUtils.toString(&quot;&quot;)           = &quot;&quot;
     *  ObjectUtils.toString(&quot;bat&quot;)        = &quot;bat&quot;
     *  ObjectUtils.toString(Boolean.TRUE) = &quot;true&quot;
     * </pre>
     * 
     * @see String#valueOf(Object)
     * @param obj
     *            the Object to <code>toString</code>, may be null
     * @return the passed in Object's toString, or nullStr if <code>null</code>
     *         input
     * @since 2.0
     */
    public static String toString( Object obj )
    {
        return ( obj == null ? "" : obj.toString() );
    }


    /**
     * <p>
     * Gets the <code>toString</code> of an <code>Object</code> returning a
     * specified text if <code>null</code> input.
     * </p>
     * 
     * <pre>
     *  ObjectUtils.toString(null, null)           = null
     *  ObjectUtils.toString(null, &quot;null&quot;)         = &quot;null&quot;
     *  ObjectUtils.toString(&quot;&quot;, &quot;null&quot;)           = &quot;&quot;
     *  ObjectUtils.toString(&quot;bat&quot;, &quot;null&quot;)        = &quot;bat&quot;
     *  ObjectUtils.toString(Boolean.TRUE, &quot;null&quot;) = &quot;true&quot;
     * </pre>
     * 
     * @see String#valueOf(Object)
     * @param obj
     *            the Object to <code>toString</code>, may be null
     * @param nullStr
     *            the String to return if <code>null</code> input, may be null
     * @return the passed in Object's toString, or nullStr if <code>null</code>
     *         input
     * @since 2.0
     */
    public static String toString( Object obj, String nullStr )
    {
        return ( obj == null ? nullStr : obj.toString() );
    }

    // Null
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Class used as a null placeholder where <code>null</code> has another
     * meaning.
     * </p>
     * <p>
     * For example, in a <code>HashMap</code> the
     * {@link java.util.HashMap#get(java.lang.Object)} method returns
     * <code>null</code> if the <code>Map</code> contains <code>null</code>
     * or if there is no matching key. The <code>Null</code> placeholder can
     * be used to distinguish between these two cases.
     * </p>
     * <p>
     * Another example is <code>Hashtable</code>, where <code>null</code>
     * cannot be stored.
     * </p>
     */
    public static class Null implements Serializable
    {
        // declare serialization compatibility with Commons Lang 1.0
        private static final long serialVersionUID = 7092611880189329093L;


        /**
         * Restricted constructor - singleton.
         */
        Null()
        {
        }


        /**
         * <p>
         * Ensure singleton.
         * </p>
         * 
         * @return the singleton value
         */
        private Object readResolve()
        {
            return ObjectUtils.NULL;
        }
    }

}

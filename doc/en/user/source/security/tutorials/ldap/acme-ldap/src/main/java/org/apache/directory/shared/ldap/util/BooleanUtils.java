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

import org.apache.directory.shared.i18n.I18n;


/**
 * <p>
 * Operations on boolean primitives and Boolean objects.
 * </p>
 * <p>
 * This class tries to handle <code>null</code> input gracefully. An exception
 * will not be thrown for a <code>null</code> input. Each method documents its
 * behaviour in more detail.
 * </p>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @since 2.0
 * @version $Id: BooleanUtils.java 919765 2010-03-06 13:44:54Z felixk $
 */
public class BooleanUtils
{
    private static final Integer INTEGER_ZERO = Integer.valueOf( 0 );

    private static final Integer INTEGER_ONE = Integer.valueOf( 1 );


    /**
     * <p>
     * <code>BooleanUtils</code> instances should NOT be constructed in
     * standard programming. Instead, the class should be used as
     * <code>BooleanUtils.toBooleanObject(true);</code>.
     * </p>
     * <p>
     * This constructor is public to permit tools that require a JavaBean
     * instance to operate.
     * </p>
     */
    public BooleanUtils()
    {
    }


    // Boolean utilities
    // --------------------------------------------------------------------------
    /**
     * <p>
     * Negates the specified boolean.
     * </p>
     * <p>
     * If <code>null</code> is passed in, <code>null</code> will be
     * returned.
     * </p>
     * 
     * <pre>
     * BooleanUtils.negate( Boolean.TRUE ) = Boolean.FALSE;
     * BooleanUtils.negate( Boolean.FALSE ) = Boolean.TRUE;
     * BooleanUtils.negate( null ) = null;
     * </pre>
     * 
     * @param bool
     *            the Boolean to negate, may be null
     * @return the negated Boolean, or <code>null</code> if <code>null</code>
     *         input
     */
    public static Boolean negate( Boolean bool )
    {
        if ( bool == null )
        {
            return null;
        }
        return ( bool.booleanValue() ? Boolean.FALSE : Boolean.TRUE );
    }


    // boolean Boolean methods
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Boolean factory that avoids creating new Boolean objecs all the time.
     * </p>
     * <p>
     * This method was added to JDK1.4 but is available here for earlier JDKs.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBooleanObject(false) = Boolean.FALSE
     *    BooleanUtils.toBooleanObject(true)  = Boolean.TRUE
     * </pre>
     * 
     * @param bool
     *            the boolean to convert
     * @return Boolean.TRUE or Boolean.FALSE as appropriate
     */
    public static Boolean toBooleanObject( boolean bool )
    {
        return ( bool ? Boolean.TRUE : Boolean.FALSE );
    }


    /**
     * <p>
     * Converts a Boolean to a boolean handling <code>null</code> by returning
     * <code>false</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBoolean(Boolean.TRUE)  = true
     *    BooleanUtils.toBoolean(Boolean.FALSE) = false
     *    BooleanUtils.toBoolean(null)          = false
     * </pre>
     * 
     * @param bool
     *            the boolean to convert
     * @return <code>true</code> or <code>false</code>, <code>null</code>
     *         returns <code>false</code>
     */
    public static boolean toBoolean( Boolean bool )
    {
        if ( bool == null )
        {
            return false;
        }
        return ( bool.booleanValue() ? true : false );
    }


    /**
     * <p>
     * Converts a Boolean to a boolean handling <code>null</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBooleanDefaultIfNull(Boolean.TRUE, false) = true
     *    BooleanUtils.toBooleanDefaultIfNull(Boolean.FALSE, true) = false
     *    BooleanUtils.toBooleanDefaultIfNull(null, true)          = true
     * </pre>
     * 
     * @param bool
     *            the boolean to convert
     * @param valueIfNull
     *            the boolean value to return if <code>null</code>
     * @return <code>true</code> or <code>false</code>
     */
    public static boolean toBooleanDefaultIfNull( Boolean bool, boolean valueIfNull )
    {
        if ( bool == null )
        {
            return valueIfNull;
        }
        return ( bool.booleanValue() ? true : false );
    }


    // Integer to Boolean methods
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Converts an int to a boolean using the convention that <code>zero</code>
     * is <code>false</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBoolean(0) = false
     *    BooleanUtils.toBoolean(1) = true
     *    BooleanUtils.toBoolean(2) = true
     * </pre>
     * 
     * @param value
     *            the int to convert
     * @return <code>true</code> if non-zero, <code>false</code> if zero
     */
    public static boolean toBoolean( int value )
    {
        return ( value == 0 ? false : true );
    }


    /**
     * <p>
     * Converts an int to a Boolean using the convention that <code>zero</code>
     * is <code>false</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBoolean(0) = Boolean.FALSE
     *    BooleanUtils.toBoolean(1) = Boolean.TRUE
     *    BooleanUtils.toBoolean(2) = Boolean.TRUE
     * </pre>
     * 
     * @param value
     *            the int to convert
     * @return Boolean.TRUE if non-zero, Boolean.FALSE if zero,
     *         <code>null</code> if <code>null</code>
     */
    public static Boolean toBooleanObject( int value )
    {
        return ( value == 0 ? Boolean.FALSE : Boolean.TRUE );
    }


    /**
     * <p>
     * Converts an Integer to a Boolean using the convention that
     * <code>zero</code> is <code>false</code>.
     * </p>
     * <p>
     * <code>null</code> will be converted to <code>null</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBoolean(new Integer(0))    = Boolean.FALSE
     *    BooleanUtils.toBoolean(new Integer(1))    = Boolean.TRUE
     *    BooleanUtils.toBoolean(new Integer(null)) = null
     * </pre>
     * 
     * @param value
     *            the Integer to convert
     * @return Boolean.TRUE if non-zero, Boolean.FALSE if zero,
     *         <code>null</code> if <code>null</code> input
     */
    public static Boolean toBooleanObject( Integer value )
    {
        if ( value == null )
        {
            return null;
        }
        return ( value.intValue() == 0 ? Boolean.FALSE : Boolean.TRUE );
    }


    /**
     * <p>
     * Converts an int to a boolean specifying the conversion values.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBoolean(0, 1, 0) = false
     *    BooleanUtils.toBoolean(1, 1, 0) = true
     *    BooleanUtils.toBoolean(2, 1, 2) = false
     *    BooleanUtils.toBoolean(2, 2, 0) = true
     * </pre>
     * 
     * @param value
     *            the Integer to convert
     * @param trueValue
     *            the value to match for <code>true</code>
     * @param falseValue
     *            the value to match for <code>false</code>
     * @return <code>true</code> or <code>false</code>
     * @throws IllegalArgumentException
     *             if no match
     */
    public static boolean toBoolean( int value, int trueValue, int falseValue )
    {
        if ( value == trueValue )
        {
            return true;
        }
        else if ( value == falseValue )
        {
            return false;
        }
        // no match
        throw new IllegalArgumentException( I18n.err( I18n.ERR_04349 ) );
    }


    /**
     * <p>
     * Converts an Integer to a boolean specifying the conversion values.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBoolean(new Integer(0), new Integer(1), new Integer(0)) = false
     *    BooleanUtils.toBoolean(new Integer(1), new Integer(1), new Integer(0)) = true
     *    BooleanUtils.toBoolean(new Integer(2), new Integer(1), new Integer(2)) = false
     *    BooleanUtils.toBoolean(new Integer(2), new Integer(2), new Integer(0)) = true
     *    BooleanUtils.toBoolean(null, null, new Integer(0))                     = true
     * </pre>
     * 
     * @param value
     *            the Integer to convert
     * @param trueValue
     *            the value to match for <code>true</code>, may be
     *            <code>null</code>
     * @param falseValue
     *            the value to match for <code>false</code>, may be
     *            <code>null</code>
     * @return <code>true</code> or <code>false</code>
     * @throws IllegalArgumentException
     *             if no match
     */
    public static boolean toBoolean( Integer value, Integer trueValue, Integer falseValue )
    {
        if ( value == null )
        {
            if ( trueValue == null )
            {
                return true;
            }
            else if ( falseValue == null )
            {
                return false;
            }
        }
        else if ( value.equals( trueValue ) )
        {
            return true;
        }
        else if ( value.equals( falseValue ) )
        {
            return false;
        }
        // no match
        throw new IllegalArgumentException( I18n.err( I18n.ERR_04349 ) );
    }


    /**
     * <p>
     * Converts an int to a Boolean specifying the conversion values.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBooleanObject(0, 0, 2, 3) = Boolean.TRUE
     *    BooleanUtils.toBooleanObject(2, 1, 2, 3) = Boolean.FALSE
     *    BooleanUtils.toBooleanObject(3, 1, 2, 3) = null
     * </pre>
     * 
     * @param value
     *            the Integer to convert
     * @param trueValue
     *            the value to match for <code>true</code>
     * @param falseValue
     *            the value to match for <code>false</code>
     * @param nullValue
     *            the value to to match for <code>null</code>
     * @return Boolean.TRUE, Boolean.FALSE, or <code>null</code>
     * @throws IllegalArgumentException
     *             if no match
     */
    public static Boolean toBooleanObject( int value, int trueValue, int falseValue, int nullValue )
    {
        if ( value == trueValue )
        {
            return Boolean.TRUE;
        }
        else if ( value == falseValue )
        {
            return Boolean.FALSE;
        }
        else if ( value == nullValue )
        {
            return null;
        }
        // no match
        throw new IllegalArgumentException( I18n.err( I18n.ERR_04349 ) );
    }


    /**
     * <p>
     * Converts an Integer to a Boolean specifying the conversion values.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBooleanObject(new Integer(0), new Integer(0), new Integer(2), new Integer(3)) = Boolean.TRUE
     *    BooleanUtils.toBooleanObject(new Integer(2), new Integer(1), new Integer(2), new Integer(3)) = Boolean.FALSE
     *    BooleanUtils.toBooleanObject(new Integer(3), new Integer(1), new Integer(2), new Integer(3)) = null
     * </pre>
     * 
     * @param value
     *            the Integer to convert
     * @param trueValue
     *            the value to match for <code>true</code>, may be
     *            <code>null</code>
     * @param falseValue
     *            the value to match for <code>false</code>, may be
     *            <code>null</code>
     * @param nullValue
     *            the value to to match for <code>null</code>, may be
     *            <code>null</code>
     * @return Boolean.TRUE, Boolean.FALSE, or <code>null</code>
     * @throws IllegalArgumentException
     *             if no match
     */
    public static Boolean toBooleanObject( Integer value, Integer trueValue, Integer falseValue, Integer nullValue )
    {
        if ( value == null )
        {
            if ( trueValue == null )
            {
                return Boolean.TRUE;
            }
            else if ( falseValue == null )
            {
                return Boolean.FALSE;
            }
            else if ( nullValue == null )
            {
                return null;
            }
        }
        else if ( value.equals( trueValue ) )
        {
            return Boolean.TRUE;
        }
        else if ( value.equals( falseValue ) )
        {
            return Boolean.FALSE;
        }
        else if ( value.equals( nullValue ) )
        {
            return null;
        }
        // no match
        throw new IllegalArgumentException( I18n.err( I18n.ERR_04349 ) );
    }


    // Boolean to Integer methods
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Converts a boolean to an int using the convention that <code>zero</code>
     * is <code>false</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toInteger(true)  = 1
     *    BooleanUtils.toInteger(false) = 0
     * </pre>
     * 
     * @param bool
     *            the boolean to convert
     * @return one if <code>true</code>, zero if <code>false</code>
     */
    public static int toInteger( boolean bool )
    {
        return ( bool ? 1 : 0 );
    }


    /**
     * <p>
     * Converts a boolean to an Integer using the convention that
     * <code>zero</code> is <code>false</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toIntegerObject(true)  = new Integer(1)
     *    BooleanUtils.toIntegerObject(false) = new Integer(0)
     * </pre>
     * 
     * @param bool
     *            the boolean to convert
     * @return one if <code>true</code>, zero if <code>false</code>
     */
    public static Integer toIntegerObject( boolean bool )
    {
        return ( bool ? INTEGER_ONE : INTEGER_ZERO );
    }


    /**
     * <p>
     * Converts a Boolean to a Integer using the convention that
     * <code>zero</code> is <code>false</code>.
     * </p>
     * <p>
     * <code>null</code> will be converted to <code>null</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toIntegerObject(Boolean.TRUE)  = new Integer(1)
     *    BooleanUtils.toIntegerObject(Boolean.FALSE) = new Integer(0)
     * </pre>
     * 
     * @param bool
     *            the Boolean to convert
     * @return one if Boolean.TRUE, zero if Boolean.FALSE, <code>null</code>
     *         if <code>null</code>
     */
    public static Integer toIntegerObject( Boolean bool )
    {
        if ( bool == null )
        {
            return null;
        }
        return ( bool.booleanValue() ? INTEGER_ONE : INTEGER_ZERO );
    }


    /**
     * <p>
     * Converts a boolean to an int specifying the conversion values.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toInteger(true, 1, 0)  = 1
     *    BooleanUtils.toInteger(false, 1, 0) = 0
     * </pre>
     * 
     * @param bool
     *            the to convert
     * @param trueValue
     *            the value to return if <code>true</code>
     * @param falseValue
     *            the value to return if <code>false</code>
     * @return the appropriate value
     */
    public static int toInteger( boolean bool, int trueValue, int falseValue )
    {
        return ( bool ? trueValue : falseValue );
    }


    /**
     * <p>
     * Converts a Boolean to an int specifying the conversion values.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toInteger(Boolean.TRUE, 1, 0, 2)  = 1
     *    BooleanUtils.toInteger(Boolean.FALSE, 1, 0, 2) = 0
     *    BooleanUtils.toInteger(null, 1, 0, 2)          = 2
     * </pre>
     * 
     * @param bool
     *            the Boolean to convert
     * @param trueValue
     *            the value to return if <code>true</code>
     * @param falseValue
     *            the value to return if <code>false</code>
     * @param nullValue
     *            the value to return if <code>null</code>
     * @return the appropriate value
     */
    public static int toInteger( Boolean bool, int trueValue, int falseValue, int nullValue )
    {
        if ( bool == null )
        {
            return nullValue;
        }
        return ( bool.booleanValue() ? trueValue : falseValue );
    }


    /**
     * <p>
     * Converts a boolean to an Integer specifying the conversion values.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toIntegerObject(true, new Integer(1), new Integer(0))  = new Integer(1)
     *    BooleanUtils.toIntegerObject(false, new Integer(1), new Integer(0)) = new Integer(0)
     * </pre>
     * 
     * @param bool
     *            the to convert
     * @param trueValue
     *            the value to return if <code>true</code>, may be
     *            <code>null</code>
     * @param falseValue
     *            the value to return if <code>false</code>, may be
     *            <code>null</code>
     * @return the appropriate value
     */
    public static Integer toIntegerObject( boolean bool, Integer trueValue, Integer falseValue )
    {
        return ( bool ? trueValue : falseValue );
    }


    /**
     * <p>
     * Converts a Boolean to an Integer specifying the conversion values.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toIntegerObject(Boolean.TRUE, new Integer(1), new Integer(0), new Integer(2))  = new Integer(1)
     *    BooleanUtils.toIntegerObject(Boolean.FALSE, new Integer(1), new Integer(0), new Integer(2)) = new Integer(0)
     *    BooleanUtils.toIntegerObject(null, new Integer(1), new Integer(0), new Integer(2))          = new Integer(2)
     * </pre>
     * 
     * @param bool
     *            the Boolean to convert
     * @param trueValue
     *            the value to return if <code>true</code>, may be
     *            <code>null</code>
     * @param falseValue
     *            the value to return if <code>false</code>, may be
     *            <code>null</code>
     * @param nullValue
     *            the value to return if <code>null</code>, may be
     *            <code>null</code>
     * @return the appropriate value
     */
    public static Integer toIntegerObject( Boolean bool, Integer trueValue, Integer falseValue, Integer nullValue )
    {
        if ( bool == null )
        {
            return nullValue;
        }
        return ( bool.booleanValue() ? trueValue : falseValue );
    }


    // String to Boolean methods
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Converts a String to a Boolean.
     * </p>
     * <p>
     * <code>'true'</code>, <code>'on'</code> or <code>'yes'</code> (case
     * insensitive) will return <code>true</code>. <code>'false'</code>,
     * <code>'off'</code> or <code>'no'</code> (case insensitive) will
     * return <code>false</code>. Otherwise, <code>null</code> is returned.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBooleanObject(null)    = null
     *    BooleanUtils.toBooleanObject(&quot;true&quot;)  = Boolean.TRUE
     *    BooleanUtils.toBooleanObject(&quot;false&quot;) = Boolean.FALSE
     *    BooleanUtils.toBooleanObject(&quot;on&quot;)    = Boolean.TRUE
     *    BooleanUtils.toBooleanObject(&quot;ON&quot;)    = Boolean.TRUE
     *    BooleanUtils.toBooleanObject(&quot;off&quot;)   = Boolean.FALSE
     *    BooleanUtils.toBooleanObject(&quot;oFf&quot;)   = Boolean.FALSE
     *    BooleanUtils.toBooleanObject(&quot;blue&quot;)  = null
     * </pre>
     * 
     * @param str
     *            the String to check
     * @return the Boolean value of the string, <code>null</code> if no match
     *         or <code>null</code> input
     */
    public static Boolean toBooleanObject( String str )
    {
        if ( "true".equalsIgnoreCase( str ) )
        {
            return Boolean.TRUE;
        }
        else if ( "false".equalsIgnoreCase( str ) )
        {
            return Boolean.FALSE;
        }
        else if ( "on".equalsIgnoreCase( str ) )
        {
            return Boolean.TRUE;
        }
        else if ( "off".equalsIgnoreCase( str ) )
        {
            return Boolean.FALSE;
        }
        else if ( "yes".equalsIgnoreCase( str ) )
        {
            return Boolean.TRUE;
        }
        else if ( "no".equalsIgnoreCase( str ) )
        {
            return Boolean.FALSE;
        }
        // no match
        return null;
    }


    /**
     * <p>
     * Converts a String to a Boolean throwing an exception if no match.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBooleanObject(&quot;true&quot;, &quot;true&quot;, &quot;false&quot;, &quot;null&quot;)  = Boolean.TRUE
     *    BooleanUtils.toBooleanObject(&quot;false&quot;, &quot;true&quot;, &quot;false&quot;, &quot;null&quot;) = Boolean.FALSE
     *    BooleanUtils.toBooleanObject(&quot;null&quot;, &quot;true&quot;, &quot;false&quot;, &quot;null&quot;)  = null
     * </pre>
     * 
     * @param str
     *            the String to check
     * @param trueString
     *            the String to match for <code>true</code> (case sensitive),
     *            may be <code>null</code>
     * @param falseString
     *            the String to match for <code>false</code> (case sensitive),
     *            may be <code>null</code>
     * @param nullString
     *            the String to match for <code>null</code> (case sensitive),
     *            may be <code>null</code>
     * @return the Boolean value of the string, <code>null</code> if no match
     *         or <code>null</code> input
     */
    public static Boolean toBooleanObject( String str, String trueString, String falseString, String nullString )
    {
        if ( str == null )
        {
            if ( trueString == null )
            {
                return Boolean.TRUE;
            }
            else if ( falseString == null )
            {
                return Boolean.FALSE;
            }
            else if ( nullString == null )
            {
                return null;
            }
        }
        else if ( str.equals( trueString ) )
        {
            return Boolean.TRUE;
        }
        else if ( str.equals( falseString ) )
        {
            return Boolean.FALSE;
        }
        else if ( str.equals( nullString ) )
        {
            return null;
        }
        // no match
        throw new IllegalArgumentException( I18n.err( I18n.ERR_04350 ) );
    }


    // String to boolean methods
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Converts a String to a boolean (optimised for performance).
     * </p>
     * <p>
     * <code>'true'</code>, <code>'on'</code> or <code>'yes'</code> (case
     * insensitive) will return <code>true</code>. Otherwise,
     * <code>false</code> is returned.
     * </p>
     * <p>
     * This method performs 4 times faster (JDK1.4) than
     * <code>Boolean.valueOf(String)</code>. However, this method accepts
     * 'on' and 'yes' as true values.
     * 
     * <pre>
     *    BooleanUtils.toBoolean(null)    = false
     *    BooleanUtils.toBoolean(&quot;true&quot;)  = true
     *    BooleanUtils.toBoolean(&quot;TRUE&quot;)  = true
     *    BooleanUtils.toBoolean(&quot;tRUe&quot;)  = true
     *    BooleanUtils.toBoolean(&quot;on&quot;)    = true
     *    BooleanUtils.toBoolean(&quot;yes&quot;)   = true
     *    BooleanUtils.toBoolean(&quot;false&quot;) = false
     *    BooleanUtils.toBoolean(&quot;x gti&quot;) = false
     * </pre>
     * 
     * @param str
     *            the String to check
     * @return the boolean value of the string, <code>false</code> if no match
     */
    public static boolean toBoolean( String str )
    {
        // Previously used equalsIgnoreCase, which was fast for interned 'true'.
        // Non interned 'true' matched 15 times slower.
        // 
        // Optimisation provides same performance as before for interned 'true'.
        // Similar performance for null, 'false', and other strings not length
        // 2/3/4.
        // 'true'/'TRUE' match 4 times slower, 'tRUE'/'True' 7 times slower.
        if ( "true".equals( str ) )
        {
            return true;
        }
        if ( str == null )
        {
            return false;
        }
        switch ( str.length() )
        {
            case 2:
            {
                char ch0 = str.charAt( 0 );
                char ch1 = str.charAt( 1 );
                return ( ch0 == 'o' || ch0 == 'O' ) && ( ch1 == 'n' || ch1 == 'N' );
            }
            case 3:
            {
                char ch = str.charAt( 0 );
                if ( ch == 'y' )
                {
                    return ( str.charAt( 1 ) == 'e' || str.charAt( 1 ) == 'E' )
                        && ( str.charAt( 2 ) == 's' || str.charAt( 2 ) == 'S' );
                }
                if ( ch == 'Y' )
                {
                    return ( str.charAt( 1 ) == 'E' || str.charAt( 1 ) == 'e' )
                        && ( str.charAt( 2 ) == 'S' || str.charAt( 2 ) == 's' );
                }
            }
            case 4:
            {
                char ch = str.charAt( 0 );
                if ( ch == 't' )
                {
                    return ( str.charAt( 1 ) == 'r' || str.charAt( 1 ) == 'R' )
                        && ( str.charAt( 2 ) == 'u' || str.charAt( 2 ) == 'U' )
                        && ( str.charAt( 3 ) == 'e' || str.charAt( 3 ) == 'E' );
                }
                if ( ch == 'T' )
                {
                    return ( str.charAt( 1 ) == 'R' || str.charAt( 1 ) == 'r' )
                        && ( str.charAt( 2 ) == 'U' || str.charAt( 2 ) == 'u' )
                        && ( str.charAt( 3 ) == 'E' || str.charAt( 3 ) == 'e' );
                }
            }
        }
        return false;
    }


    /**
     * <p>
     * Converts a String to a Boolean throwing an exception if no match found.
     * </p>
     * <p>
     * null is returned if there is no match.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toBoolean(&quot;true&quot;, &quot;true&quot;, &quot;false&quot;)  = true
     *    BooleanUtils.toBoolean(&quot;false&quot;, &quot;true&quot;, &quot;false&quot;) = false
     * </pre>
     * 
     * @param str
     *            the String to check
     * @param trueString
     *            the String to match for <code>true</code> (case sensitive),
     *            may be <code>null</code>
     * @param falseString
     *            the String to match for <code>false</code> (case sensitive),
     *            may be <code>null</code>
     * @return the boolean value of the string
     * @throws IllegalArgumentException
     *             if the String doesn't match
     */
    public static boolean toBoolean( String str, String trueString, String falseString )
    {
        if ( str == null )
        {
            if ( trueString == null )
            {
                return true;
            }
            else if ( falseString == null )
            {
                return false;
            }
        }
        else if ( str.equals( trueString ) )
        {
            return true;
        }
        else if ( str.equals( falseString ) )
        {
            return false;
        }
        // no match
        throw new IllegalArgumentException( I18n.err( I18n.ERR_04350 ) );
    }


    // Boolean to String methods
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Converts a Boolean to a String returning <code>'true'</code>,
     * <code>'false'</code>, or <code>null</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toStringTrueFalse(Boolean.TRUE)  = &quot;true&quot;
     *    BooleanUtils.toStringTrueFalse(Boolean.FALSE) = &quot;false&quot;
     *    BooleanUtils.toStringTrueFalse(null)          = null;
     * </pre>
     * 
     * @param bool
     *            the Boolean to check
     * @return <code>'true'</code>, <code>'false'</code>, or
     *         <code>null</code>
     */
    public static String toStringTrueFalse( Boolean bool )
    {
        return toString( bool, "true", "false", null );
    }


    /**
     * <p>
     * Converts a Boolean to a String returning <code>'on'</code>,
     * <code>'off'</code>, or <code>null</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toStringOnOff(Boolean.TRUE)  = &quot;on&quot;
     *    BooleanUtils.toStringOnOff(Boolean.FALSE) = &quot;off&quot;
     *    BooleanUtils.toStringOnOff(null)          = null;
     * </pre>
     * 
     * @param bool
     *            the Boolean to check
     * @return <code>'on'</code>, <code>'off'</code>, or <code>null</code>
     */
    public static String toStringOnOff( Boolean bool )
    {
        return toString( bool, "on", "off", null );
    }


    /**
     * <p>
     * Converts a Boolean to a String returning <code>'yes'</code>,
     * <code>'no'</code>, or <code>null</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toStringYesNo(Boolean.TRUE)  = &quot;yes&quot;
     *    BooleanUtils.toStringYesNo(Boolean.FALSE) = &quot;no&quot;
     *    BooleanUtils.toStringYesNo(null)          = null;
     * </pre>
     * 
     * @param bool
     *            the Boolean to check
     * @return <code>'yes'</code>, <code>'no'</code>, or <code>null</code>
     */
    public static String toStringYesNo( Boolean bool )
    {
        return toString( bool, "yes", "no", null );
    }


    /**
     * <p>
     * Converts a Boolean to a String returning one of the input Strings.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toString(Boolean.TRUE, &quot;true&quot;, &quot;false&quot;, null)   = &quot;true&quot;
     *    BooleanUtils.toString(Boolean.FALSE, &quot;true&quot;, &quot;false&quot;, null)  = &quot;false&quot;
     *    BooleanUtils.toString(null, &quot;true&quot;, &quot;false&quot;, null)           = null;
     * </pre>
     * 
     * @param bool
     *            the Boolean to check
     * @param trueString
     *            the String to return if <code>true</code>, may be
     *            <code>null</code>
     * @param falseString
     *            the String to return if <code>false</code>, may be
     *            <code>null</code>
     * @param nullString
     *            the String to return if <code>null</code>, may be
     *            <code>null</code>
     * @return one of the three input Strings
     */
    public static String toString( Boolean bool, String trueString, String falseString, String nullString )
    {
        if ( bool == null )
        {
            return nullString;
        }
        return ( bool.booleanValue() ? trueString : falseString );
    }


    // boolean to String methods
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Converts a boolean to a String returning <code>'true'</code> or
     * <code>'false'</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toStringTrueFalse(true)   = &quot;true&quot;
     *    BooleanUtils.toStringTrueFalse(false)  = &quot;false&quot;
     * </pre>
     * 
     * @param bool
     *            the Boolean to check
     * @return <code>'true'</code>, <code>'false'</code>, or
     *         <code>null</code>
     */
    public static String toStringTrueFalse( boolean bool )
    {
        return toString( bool, "true", "false" );
    }


    /**
     * <p>
     * Converts a boolean to a String returning <code>'on'</code> or
     * <code>'off'</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toStringOnOff(true)   = &quot;on&quot;
     *    BooleanUtils.toStringOnOff(false)  = &quot;off&quot;
     * </pre>
     * 
     * @param bool
     *            the Boolean to check
     * @return <code>'on'</code>, <code>'off'</code>, or <code>null</code>
     */
    public static String toStringOnOff( boolean bool )
    {
        return toString( bool, "on", "off" );
    }


    /**
     * <p>
     * Converts a boolean to a String returning <code>'yes'</code> or
     * <code>'no'</code>.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toStringYesNo(true)   = &quot;yes&quot;
     *    BooleanUtils.toStringYesNo(false)  = &quot;no&quot;
     * </pre>
     * 
     * @param bool
     *            the Boolean to check
     * @return <code>'yes'</code>, <code>'no'</code>, or <code>null</code>
     */
    public static String toStringYesNo( boolean bool )
    {
        return toString( bool, "yes", "no" );
    }


    /**
     * <p>
     * Converts a boolean to a String returning one of the input Strings.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.toString(true, &quot;true&quot;, &quot;false&quot;)   = &quot;true&quot;
     *    BooleanUtils.toString(false, &quot;true&quot;, &quot;false&quot;)  = &quot;false&quot;
     * </pre>
     * 
     * @param bool
     *            the Boolean to check
     * @param trueString
     *            the String to return if <code>true</code>, may be
     *            <code>null</code>
     * @param falseString
     *            the String to return if <code>false</code>, may be
     *            <code>null</code>
     * @return one of the two input Strings
     */
    public static String toString( boolean bool, String trueString, String falseString )
    {
        return ( bool ? trueString : falseString );
    }


    // xor methods
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Performs an xor on a set of booleans.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.xor(new boolean[] { true, true })   = false
     *    BooleanUtils.xor(new boolean[] { false, false }) = false
     *    BooleanUtils.xor(new boolean[] { true, false })  = true
     * </pre>
     * 
     * @param array
     *            an array of <code>boolean<code>s
     * @return <code>true</code> if the xor is successful.
     * @throws IllegalArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty.
     */
    public static boolean xor( boolean[] array )
    {
        // Validates input
        if ( array == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04352 ) );
        }
        else if ( array.length == 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04352 ) );
        }

        // Loops through array, comparing each item
        int trueCount = 0;
        for ( int i = 0; i < array.length; i++ )
        {
            // If item is true, and trueCount is < 1, increments count
            // Else, xor fails
            if ( array[i] )
            {
                if ( trueCount < 1 )
                {
                    trueCount++;
                }
                else
                {
                    return false;
                }
            }
        }

        // Returns true if there was exactly 1 true item
        return trueCount == 1;
    }


    /**
     * <p>
     * Performs an xor on an array of Booleans.
     * </p>
     * 
     * <pre>
     *    BooleanUtils.xor(new Boolean[] { Boolean.TRUE, Boolean.TRUE })   = Boolean.FALSE
     *    BooleanUtils.xor(new Boolean[] { Boolean.FALSE, Boolean.FALSE }) = Boolean.FALSE
     *    BooleanUtils.xor(new Boolean[] { Boolean.TRUE, Boolean.FALSE })  = Boolean.TRUE
     * </pre>
     * 
     * @param array
     *            an array of <code>Boolean<code>s
     * @return <code>true</code> if the xor is successful.
     * @throws IllegalArgumentException if <code>array</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>array</code> is empty.
     * @throws IllegalArgumentException if <code>array</code> contains a <code>null</code>
     */
    public static Boolean xor( Boolean[] array )
    {
        if ( array == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04351 ) );
        }
        else if ( array.length == 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04352 ) );
        }
        boolean[] primitive = null;
        try
        {
            primitive = ArrayUtils.toPrimitive( array );
        }
        catch ( NullPointerException ex )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04353 ) );
        }
        return ( xor( primitive ) ? Boolean.TRUE : Boolean.FALSE );
    }

}

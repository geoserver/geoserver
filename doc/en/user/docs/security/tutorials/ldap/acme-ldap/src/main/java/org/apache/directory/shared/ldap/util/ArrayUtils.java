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


import java.lang.reflect.Array;

import org.apache.directory.shared.i18n.I18n;


/**
 * <p>
 * Operations on arrays, primitive arrays (like <code>int[]</code>) and
 * primitive wrapper arrays (like <code>Integer[]</code>).
 * </p>
 * <p>
 * This class tries to handle <code>null</code> input gracefully. An exception
 * will not be thrown for a <code>null</code> array input. However, an Object
 * array that contains a <code>null</code> element may throw an exception.
 * Each method documents its behaviour.
 * </p>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @since 2.0
 */
public class ArrayUtils
{

    /**
     * An empty immutable <code>Object</code> array.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    /**
     * An empty immutable <code>Class</code> array.
     */
    public static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    /**
     * An empty immutable <code>String</code> array.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * An empty immutable <code>long</code> array.
     */
    public static final long[] EMPTY_LONG_ARRAY = new long[0];

    /**
     * An empty immutable <code>Long</code> array.
     */
    public static final Long[] EMPTY_LONG_OBJECT_ARRAY = new Long[0];

    /**
     * An empty immutable <code>int</code> array.
     */
    public static final int[] EMPTY_INT_ARRAY = new int[0];

    /**
     * An empty immutable <code>Integer</code> array.
     */
    public static final Integer[] EMPTY_INTEGER_OBJECT_ARRAY = new Integer[0];

    /**
     * An empty immutable <code>short</code> array.
     */
    public static final short[] EMPTY_SHORT_ARRAY = new short[0];

    /**
     * An empty immutable <code>Short</code> array.
     */
    public static final Short[] EMPTY_SHORT_OBJECT_ARRAY = new Short[0];

    /**
     * An empty immutable <code>byte</code> array.
     */
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    /**
     * An empty immutable <code>Byte</code> array.
     */
    public static final Byte[] EMPTY_BYTE_OBJECT_ARRAY = new Byte[0];

    /**
     * An empty immutable <code>double</code> array.
     */
    public static final double[] EMPTY_DOUBLE_ARRAY = new double[0];

    /**
     * An empty immutable <code>Double</code> array.
     */
    public static final Double[] EMPTY_DOUBLE_OBJECT_ARRAY = new Double[0];

    /**
     * An empty immutable <code>float</code> array.
     */
    public static final float[] EMPTY_FLOAT_ARRAY = new float[0];

    /**
     * An empty immutable <code>Float</code> array.
     */
    public static final Float[] EMPTY_FLOAT_OBJECT_ARRAY = new Float[0];

    /**
     * An empty immutable <code>boolean</code> array.
     */
    public static final boolean[] EMPTY_BOOLEAN_ARRAY = new boolean[0];

    /**
     * An empty immutable <code>Boolean</code> array.
     */
    public static final Boolean[] EMPTY_BOOLEAN_OBJECT_ARRAY = new Boolean[0];

    /**
     * An empty immutable <code>char</code> array.
     */
    public static final char[] EMPTY_CHAR_ARRAY = new char[0];

    /**
     * An empty immutable <code>Character</code> array.
     */
    public static final Character[] EMPTY_CHARACTER_OBJECT_ARRAY = new Character[0];


    /**
     * <p>
     * ArrayUtils instances should NOT be constructed in standard programming.
     * Instead, the class should be used as
     * <code>ArrayUtils.clone(new int[] {2})</code>.
     * </p>
     * <p>
     * This constructor is public to permit tools that require a JavaBean
     * instance to operate.
     * </p>
     */
    public ArrayUtils()
    {
    }


    // Basic methods handling multi-dimensional arrays
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Outputs an array as a String, treating <code>null</code> as an empty
     * array.
     * </p>
     * <p>
     * Multi-dimensional arrays are handled correctly, including
     * multi-dimensional primitive arrays.
     * </p>
     * <p>
     * The format is that of Java source code, for example <code>{a,b}</code>.
     * </p>
     * 
     * @param array
     *            the array to get a toString for, may be <code>null</code>
     * @return a String representation of the array, '{}' if null array input
     */
    public static String toString( Object array )
    {
        return toString( array, "{}" );
    }


    /**
     * <p>
     * Outputs an array as a String handling <code>null</code>s.
     * </p>
     * <p>
     * Multi-dimensional arrays are handled correctly, including
     * multi-dimensional primitive arrays.
     * </p>
     * <p>
     * The format is that of Java source code, for example <code>{a,b}</code>.
     * </p>
     * 
     * @param array
     *            the array to get a toString for, may be <code>null</code>
     * @param stringIfNull
     *            the String to return if the array is <code>null</code>
     * @return a String representation of the array
     */
    public static String toString( Object array, String stringIfNull )
    {
        if ( array == null )
        {
            return stringIfNull;
        }
        
        return new ToStringBuilder( array, ToStringStyle.SIMPLE_STYLE ).append( array ).toString();
    }


    /**
     * <p>
     * Get a hashCode for an array handling multi-dimensional arrays correctly.
     * </p>
     * <p>
     * Multi-dimensional primitive arrays are also handled correctly by this
     * method.
     * </p>
     * 
     * @param array
     *            the array to get a hashCode for, may be <code>null</code>
     * @return a hashCode for the array, zero if null array input
     */
    public static int hashCode( Object array )
    {
        return new HashCodeBuilder().append( array ).toHashCode();
    }


    /**
     * <p>
     * Compares two arrays, using equals(), handling multi-dimensional arrays
     * correctly.
     * </p>
     * <p>
     * Multi-dimensional primitive arrays are also handled correctly by this
     * method.
     * </p>
     * 
     * @param array1
     *            the left hand array to compare, may be <code>null</code>
     * @param array2
     *            the right hand array to compare, may be <code>null</code>
     * @return <code>true</code> if the arrays are equal
     */
    public static boolean isEquals( Object array1, Object array2 )
    {
        return new EqualsBuilder().append( array1, array2 ).isEquals();
    }

    // Clone
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Shallow clones an array returning a typecast result and handling
     * <code>null</code>.
     * </p>
     * <p>
     * The objects in the array are not cloned, thus there is no special
     * handling for multi-dimensional arrays.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            the array to shallow clone, may be <code>null</code>
     * @return the cloned array, <code>null</code> if <code>null</code>
     *         input
     */
    public static Object[] clone( Object[] array )
    {
        if ( array == null )
        {
            return null;
        }
        return array.clone();
    }


    /**
     * <p>
     * Clones an array returning a typecast result and handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            the array to clone, may be <code>null</code>
     * @return the cloned array, <code>null</code> if <code>null</code>
     *         input
     */
    public static long[] clone( long[] array )
    {
        if ( array == null )
        {
            return null;
        }
        return array.clone();
    }


    /**
     * <p>
     * Clones an array returning a typecast result and handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            the array to clone, may be <code>null</code>
     * @return the cloned array, <code>null</code> if <code>null</code>
     *         input
     */
    public static int[] clone( int[] array )
    {
        if ( array == null )
        {
            return null;
        }
        return array.clone();
    }


    /**
     * <p>
     * Clones an array returning a typecast result and handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            the array to clone, may be <code>null</code>
     * @return the cloned array, <code>null</code> if <code>null</code>
     *         input
     */
    public static short[] clone( short[] array )
    {
        if ( array == null )
        {
            return null;
        }
        return array.clone();
    }


    /**
     * <p>
     * Clones an array returning a typecast result and handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            the array to clone, may be <code>null</code>
     * @return the cloned array, <code>null</code> if <code>null</code>
     *         input
     */
    public static char[] clone( char[] array )
    {
        if ( array == null )
        {
            return null;
        }
        return array.clone();
    }


    /**
     * <p>
     * Clones an array returning a typecast result and handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            the array to clone, may be <code>null</code>
     * @return the cloned array, <code>null</code> if <code>null</code>
     *         input
     */
    public static byte[] clone( byte[] array )
    {
        if ( array == null )
        {
            return null;
        }
        return array.clone();
    }


    /**
     * <p>
     * Clones an array returning a typecast result and handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            the array to clone, may be <code>null</code>
     * @return the cloned array, <code>null</code> if <code>null</code>
     *         input
     */
    public static double[] clone( double[] array )
    {
        if ( array == null )
        {
            return null;
        }
        return array.clone();
    }


    /**
     * <p>
     * Clones an array returning a typecast result and handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            the array to clone, may be <code>null</code>
     * @return the cloned array, <code>null</code> if <code>null</code>
     *         input
     */
    public static float[] clone( float[] array )
    {
        if ( array == null )
        {
            return null;
        }
        return array.clone();
    }


    /**
     * <p>
     * Clones an array returning a typecast result and handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            the array to clone, may be <code>null</code>
     * @return the cloned array, <code>null</code> if <code>null</code>
     *         input
     */
    public static boolean[] clone( boolean[] array )
    {
        if ( array == null )
        {
            return null;
        }
        return array.clone();
    }


    // Subarrays
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Produces a new array containing the elements between the start and end
     * indices.
     * </p>
     * <p>
     * The start index is inclusive, the end index exclusive. Null array input
     * produces null output.
     * </p>
     * <p>
     * The component type of the subarray is always the same as that of the
     * input array. Thus, if the input is an array of type <code>Date</code>,
     * the following usage is envisaged:
     * </p>
     * 
     * <pre>
     * Date[] someDates = ( Date[] ) ArrayUtils.subarray( allDates, 2, 5 );
     * </pre>
     * 
     * @param array
     *            the array
     * @param startIndexInclusive
     *            the starting index. Undervalue (&lt;0) is promoted to 0,
     *            overvalue (&gt;array.length) results in an empty array.
     * @param endIndexExclusive
     *            elements up to endIndex-1 are present in the returned
     *            subarray. Undervalue (&lt; startIndex) produces empty array,
     *            overvalue (&gt;array.length) is demoted to array length.
     * @return a new array containing the elements between the start and end
     *         indices.
     */
    public static Object[] subarray( Object[] array, int startIndexInclusive, int endIndexExclusive )
    {
        if ( array == null )
        {
            return null;
        }
        if ( startIndexInclusive < 0 )
        {
            startIndexInclusive = 0;
        }
        if ( endIndexExclusive > array.length )
        {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        Class<?> type = array.getClass().getComponentType();
        if ( newSize <= 0 )
        {
            return ( Object[] ) Array.newInstance( type, 0 );
        }
        Object[] subarray = ( Object[] ) Array.newInstance( type, newSize );
        System.arraycopy( array, startIndexInclusive, subarray, 0, newSize );
        return subarray;
    }


    /**
     * <p>
     * Produces a new <code>long</code> array containing the elements between
     * the start and end indices.
     * </p>
     * <p>
     * The start index is inclusive, the end index exclusive. Null array input
     * produces null output.
     * </p>
     * 
     * @param array
     *            the array
     * @param startIndexInclusive
     *            the starting index. Undervalue (&lt;0) is promoted to 0,
     *            overvalue (&gt;array.length) results in an empty array.
     * @param endIndexExclusive
     *            elements up to endIndex-1 are present in the returned
     *            subarray. Undervalue (&lt; startIndex) produces empty array,
     *            overvalue (&gt;array.length) is demoted to array length.
     * @return a new array containing the elements between the start and end
     *         indices.
     */
    public static long[] subarray( long[] array, int startIndexInclusive, int endIndexExclusive )
    {
        if ( array == null )
        {
            return null;
        }
        if ( startIndexInclusive < 0 )
        {
            startIndexInclusive = 0;
        }
        if ( endIndexExclusive > array.length )
        {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if ( newSize <= 0 )
        {
            return EMPTY_LONG_ARRAY;
        }

        long[] subarray = new long[newSize];
        System.arraycopy( array, startIndexInclusive, subarray, 0, newSize );
        return subarray;
    }


    /**
     * <p>
     * Produces a new <code>int</code> array containing the elements between
     * the start and end indices.
     * </p>
     * <p>
     * The start index is inclusive, the end index exclusive. Null array input
     * produces null output.
     * </p>
     * 
     * @param array
     *            the array
     * @param startIndexInclusive
     *            the starting index. Undervalue (&lt;0) is promoted to 0,
     *            overvalue (&gt;array.length) results in an empty array.
     * @param endIndexExclusive
     *            elements up to endIndex-1 are present in the returned
     *            subarray. Undervalue (&lt; startIndex) produces empty array,
     *            overvalue (&gt;array.length) is demoted to array length.
     * @return a new array containing the elements between the start and end
     *         indices.
     */
    public static int[] subarray( int[] array, int startIndexInclusive, int endIndexExclusive )
    {
        if ( array == null )
        {
            return null;
        }
        if ( startIndexInclusive < 0 )
        {
            startIndexInclusive = 0;
        }
        if ( endIndexExclusive > array.length )
        {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if ( newSize <= 0 )
        {
            return EMPTY_INT_ARRAY;
        }

        int[] subarray = new int[newSize];
        System.arraycopy( array, startIndexInclusive, subarray, 0, newSize );
        return subarray;
    }


    /**
     * <p>
     * Produces a new <code>short</code> array containing the elements between
     * the start and end indices.
     * </p>
     * <p>
     * The start index is inclusive, the end index exclusive. Null array input
     * produces null output.
     * </p>
     * 
     * @param array
     *            the array
     * @param startIndexInclusive
     *            the starting index. Undervalue (&lt;0) is promoted to 0,
     *            overvalue (&gt;array.length) results in an empty array.
     * @param endIndexExclusive
     *            elements up to endIndex-1 are present in the returned
     *            subarray. Undervalue (&lt; startIndex) produces empty array,
     *            overvalue (&gt;array.length) is demoted to array length.
     * @return a new array containing the elements between the start and end
     *         indices.
     */
    public static short[] subarray( short[] array, int startIndexInclusive, int endIndexExclusive )
    {
        if ( array == null )
        {
            return null;
        }
        if ( startIndexInclusive < 0 )
        {
            startIndexInclusive = 0;
        }
        if ( endIndexExclusive > array.length )
        {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if ( newSize <= 0 )
        {
            return EMPTY_SHORT_ARRAY;
        }

        short[] subarray = new short[newSize];
        System.arraycopy( array, startIndexInclusive, subarray, 0, newSize );
        return subarray;
    }


    /**
     * <p>
     * Produces a new <code>char</code> array containing the elements between
     * the start and end indices.
     * </p>
     * <p>
     * The start index is inclusive, the end index exclusive. Null array input
     * produces null output.
     * </p>
     * 
     * @param array
     *            the array
     * @param startIndexInclusive
     *            the starting index. Undervalue (&lt;0) is promoted to 0,
     *            overvalue (&gt;array.length) results in an empty array.
     * @param endIndexExclusive
     *            elements up to endIndex-1 are present in the returned
     *            subarray. Undervalue (&lt; startIndex) produces empty array,
     *            overvalue (&gt;array.length) is demoted to array length.
     * @return a new array containing the elements between the start and end
     *         indices.
     */
    public static char[] subarray( char[] array, int startIndexInclusive, int endIndexExclusive )
    {
        if ( array == null )
        {
            return null;
        }
        if ( startIndexInclusive < 0 )
        {
            startIndexInclusive = 0;
        }
        if ( endIndexExclusive > array.length )
        {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if ( newSize <= 0 )
        {
            return EMPTY_CHAR_ARRAY;
        }

        char[] subarray = new char[newSize];
        System.arraycopy( array, startIndexInclusive, subarray, 0, newSize );
        return subarray;
    }


    /**
     * <p>
     * Produces a new <code>byte</code> array containing the elements between
     * the start and end indices.
     * </p>
     * <p>
     * The start index is inclusive, the end index exclusive. Null array input
     * produces null output.
     * </p>
     * 
     * @param array
     *            the array
     * @param startIndexInclusive
     *            the starting index. Undervalue (&lt;0) is promoted to 0,
     *            overvalue (&gt;array.length) results in an empty array.
     * @param endIndexExclusive
     *            elements up to endIndex-1 are present in the returned
     *            subarray. Undervalue (&lt; startIndex) produces empty array,
     *            overvalue (&gt;array.length) is demoted to array length.
     * @return a new array containing the elements between the start and end
     *         indices.
     */
    public static byte[] subarray( byte[] array, int startIndexInclusive, int endIndexExclusive )
    {
        if ( array == null )
        {
            return null;
        }
        if ( startIndexInclusive < 0 )
        {
            startIndexInclusive = 0;
        }
        if ( endIndexExclusive > array.length )
        {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if ( newSize <= 0 )
        {
            return EMPTY_BYTE_ARRAY;
        }

        byte[] subarray = new byte[newSize];
        System.arraycopy( array, startIndexInclusive, subarray, 0, newSize );
        return subarray;
    }


    /**
     * <p>
     * Produces a new <code>double</code> array containing the elements
     * between the start and end indices.
     * </p>
     * <p>
     * The start index is inclusive, the end index exclusive. Null array input
     * produces null output.
     * </p>
     * 
     * @param array
     *            the array
     * @param startIndexInclusive
     *            the starting index. Undervalue (&lt;0) is promoted to 0,
     *            overvalue (&gt;array.length) results in an empty array.
     * @param endIndexExclusive
     *            elements up to endIndex-1 are present in the returned
     *            subarray. Undervalue (&lt; startIndex) produces empty array,
     *            overvalue (&gt;array.length) is demoted to array length.
     * @return a new array containing the elements between the start and end
     *         indices.
     */
    public static double[] subarray( double[] array, int startIndexInclusive, int endIndexExclusive )
    {
        if ( array == null )
        {
            return null;
        }
        if ( startIndexInclusive < 0 )
        {
            startIndexInclusive = 0;
        }
        if ( endIndexExclusive > array.length )
        {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if ( newSize <= 0 )
        {
            return EMPTY_DOUBLE_ARRAY;
        }

        double[] subarray = new double[newSize];
        System.arraycopy( array, startIndexInclusive, subarray, 0, newSize );
        return subarray;
    }


    /**
     * <p>
     * Produces a new <code>float</code> array containing the elements between
     * the start and end indices.
     * </p>
     * <p>
     * The start index is inclusive, the end index exclusive. Null array input
     * produces null output.
     * </p>
     * 
     * @param array
     *            the array
     * @param startIndexInclusive
     *            the starting index. Undervalue (&lt;0) is promoted to 0,
     *            overvalue (&gt;array.length) results in an empty array.
     * @param endIndexExclusive
     *            elements up to endIndex-1 are present in the returned
     *            subarray. Undervalue (&lt; startIndex) produces empty array,
     *            overvalue (&gt;array.length) is demoted to array length.
     * @return a new array containing the elements between the start and end
     *         indices.
     */
    public static float[] subarray( float[] array, int startIndexInclusive, int endIndexExclusive )
    {
        if ( array == null )
        {
            return null;
        }
        if ( startIndexInclusive < 0 )
        {
            startIndexInclusive = 0;
        }
        if ( endIndexExclusive > array.length )
        {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if ( newSize <= 0 )
        {
            return EMPTY_FLOAT_ARRAY;
        }

        float[] subarray = new float[newSize];
        System.arraycopy( array, startIndexInclusive, subarray, 0, newSize );
        return subarray;
    }


    /**
     * <p>
     * Produces a new <code>boolean</code> array containing the elements
     * between the start and end indices.
     * </p>
     * <p>
     * The start index is inclusive, the end index exclusive. Null array input
     * produces null output.
     * </p>
     * 
     * @param array
     *            the array
     * @param startIndexInclusive
     *            the starting index. Undervalue (&lt;0) is promoted to 0,
     *            overvalue (&gt;array.length) results in an empty array.
     * @param endIndexExclusive
     *            elements up to endIndex-1 are present in the returned
     *            subarray. Undervalue (&lt; startIndex) produces empty array,
     *            overvalue (&gt;array.length) is demoted to array length.
     * @return a new array containing the elements between the start and end
     *         indices.
     */
    public static boolean[] subarray( boolean[] array, int startIndexInclusive, int endIndexExclusive )
    {
        if ( array == null )
        {
            return null;
        }
        if ( startIndexInclusive < 0 )
        {
            startIndexInclusive = 0;
        }
        if ( endIndexExclusive > array.length )
        {
            endIndexExclusive = array.length;
        }
        int newSize = endIndexExclusive - startIndexInclusive;
        if ( newSize <= 0 )
        {
            return EMPTY_BOOLEAN_ARRAY;
        }

        boolean[] subarray = new boolean[newSize];
        System.arraycopy( array, startIndexInclusive, subarray, 0, newSize );
        return subarray;
    }


    // Is same length
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Checks whether two arrays are the same length, treating <code>null</code>
     * arrays as length <code>0</code>.
     * <p>
     * Any multi-dimensional aspects of the arrays are ignored.
     * </p>
     * 
     * @param array1
     *            the first array, may be <code>null</code>
     * @param array2
     *            the second array, may be <code>null</code>
     * @return <code>true</code> if length of arrays matches, treating
     *         <code>null</code> as an empty array
     */
    public static boolean isSameLength( Object[] array1, Object[] array2 )
    {
        if ( ( array1 == null && array2 != null && array2.length > 0 )
            || ( array2 == null && array1 != null && array1.length > 0 )
            || ( array1 != null && array2 != null && array1.length != array2.length ) )
        {
            return false;
        }
        return true;
    }


    /**
     * <p>
     * Checks whether two arrays are the same length, treating <code>null</code>
     * arrays as length <code>0</code>.
     * </p>
     * 
     * @param array1
     *            the first array, may be <code>null</code>
     * @param array2
     *            the second array, may be <code>null</code>
     * @return <code>true</code> if length of arrays matches, treating
     *         <code>null</code> as an empty array
     */
    public static boolean isSameLength( long[] array1, long[] array2 )
    {
        if ( ( array1 == null && array2 != null && array2.length > 0 )
            || ( array2 == null && array1 != null && array1.length > 0 )
            || ( array1 != null && array2 != null && array1.length != array2.length ) )
        {
            return false;
        }
        return true;
    }


    /**
     * <p>
     * Checks whether two arrays are the same length, treating <code>null</code>
     * arrays as length <code>0</code>.
     * </p>
     * 
     * @param array1
     *            the first array, may be <code>null</code>
     * @param array2
     *            the second array, may be <code>null</code>
     * @return <code>true</code> if length of arrays matches, treating
     *         <code>null</code> as an empty array
     */
    public static boolean isSameLength( int[] array1, int[] array2 )
    {
        if ( ( array1 == null && array2 != null && array2.length > 0 )
            || ( array2 == null && array1 != null && array1.length > 0 )
            || ( array1 != null && array2 != null && array1.length != array2.length ) )
        {
            return false;
        }
        return true;
    }


    /**
     * <p>
     * Checks whether two arrays are the same length, treating <code>null</code>
     * arrays as length <code>0</code>.
     * </p>
     * 
     * @param array1
     *            the first array, may be <code>null</code>
     * @param array2
     *            the second array, may be <code>null</code>
     * @return <code>true</code> if length of arrays matches, treating
     *         <code>null</code> as an empty array
     */
    public static boolean isSameLength( short[] array1, short[] array2 )
    {
        if ( ( array1 == null && array2 != null && array2.length > 0 )
            || ( array2 == null && array1 != null && array1.length > 0 )
            || ( array1 != null && array2 != null && array1.length != array2.length ) )
        {
            return false;
        }
        return true;
    }


    /**
     * <p>
     * Checks whether two arrays are the same length, treating <code>null</code>
     * arrays as length <code>0</code>.
     * </p>
     * 
     * @param array1
     *            the first array, may be <code>null</code>
     * @param array2
     *            the second array, may be <code>null</code>
     * @return <code>true</code> if length of arrays matches, treating
     *         <code>null</code> as an empty array
     */
    public static boolean isSameLength( char[] array1, char[] array2 )
    {
        if ( ( array1 == null && array2 != null && array2.length > 0 )
            || ( array2 == null && array1 != null && array1.length > 0 )
            || ( array1 != null && array2 != null && array1.length != array2.length ) )
        {
            return false;
        }
        return true;
    }


    /**
     * <p>
     * Checks whether two arrays are the same length, treating <code>null</code>
     * arrays as length <code>0</code>.
     * </p>
     * 
     * @param array1
     *            the first array, may be <code>null</code>
     * @param array2
     *            the second array, may be <code>null</code>
     * @return <code>true</code> if length of arrays matches, treating
     *         <code>null</code> as an empty array
     */
    public static boolean isSameLength( byte[] array1, byte[] array2 )
    {
        if ( ( array1 == null && array2 != null && array2.length > 0 )
            || ( array2 == null && array1 != null && array1.length > 0 )
            || ( array1 != null && array2 != null && array1.length != array2.length ) )
        {
            return false;
        }
        return true;
    }


    /**
     * <p>
     * Checks whether two arrays are the same length, treating <code>null</code>
     * arrays as length <code>0</code>.
     * </p>
     * 
     * @param array1
     *            the first array, may be <code>null</code>
     * @param array2
     *            the second array, may be <code>null</code>
     * @return <code>true</code> if length of arrays matches, treating
     *         <code>null</code> as an empty array
     */
    public static boolean isSameLength( double[] array1, double[] array2 )
    {
        if ( ( array1 == null && array2 != null && array2.length > 0 )
            || ( array2 == null && array1 != null && array1.length > 0 )
            || ( array1 != null && array2 != null && array1.length != array2.length ) )
        {
            return false;
        }
        return true;
    }


    /**
     * <p>
     * Checks whether two arrays are the same length, treating <code>null</code>
     * arrays as length <code>0</code>.
     * </p>
     * 
     * @param array1
     *            the first array, may be <code>null</code>
     * @param array2
     *            the second array, may be <code>null</code>
     * @return <code>true</code> if length of arrays matches, treating
     *         <code>null</code> as an empty array
     */
    public static boolean isSameLength( float[] array1, float[] array2 )
    {
        if ( ( array1 == null && array2 != null && array2.length > 0 )
            || ( array2 == null && array1 != null && array1.length > 0 )
            || ( array1 != null && array2 != null && array1.length != array2.length ) )
        {
            return false;
        }
        return true;
    }


    /**
     * <p>
     * Checks whether two arrays are the same length, treating <code>null</code>
     * arrays as length <code>0</code>.
     * </p>
     * 
     * @param array1
     *            the first array, may be <code>null</code>
     * @param array2
     *            the second array, may be <code>null</code>
     * @return <code>true</code> if length of arrays matches, treating
     *         <code>null</code> as an empty array
     */
    public static boolean isSameLength( boolean[] array1, boolean[] array2 )
    {
        if ( ( array1 == null && array2 != null && array2.length > 0 )
            || ( array2 == null && array1 != null && array1.length > 0 )
            || ( array1 != null && array2 != null && array1.length != array2.length ) )
        {
            return false;
        }
        return true;
    }


    // -----------------------------------------------------------------------
    /**
     * <p>
     * Returns the length of the specified array. This method can deal with
     * <code>Object</code> arrays and with primitive arrays.
     * </p>
     * <p>
     * If the input array is <code>null</code>, <code>0</code> is returned.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.getLength(null)            = 0
     *  ArrayUtils.getLength([])              = 0
     *  ArrayUtils.getLength([null])          = 1
     *  ArrayUtils.getLength([true, false])   = 2
     *  ArrayUtils.getLength([1, 2, 3])       = 3
     *  ArrayUtils.getLength([&quot;a&quot;, &quot;b&quot;, &quot;c&quot;]) = 3
     * </pre>
     * 
     * @param array
     *            the array to retrieve the length from, may be null
     * @return The length of the array, or <code>0</code> if the array is
     *         <code>null</code>
     * @throws IllegalArgumentException
     *             if the object arguement is not an array.
     */
    public static int getLength( Object array )
    {
        if ( array == null )
        {
            return 0;
        }
        else
        {
            return Array.getLength( array );
        }
    }


    /**
     * Returns the last index of the given array or -1 if empty or null. This
     * method can deal with <code>Object</code> arrays and with primitive
     * arrays. This value is one less than the size since arrays indices are
     * 0-based.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.lastIndex(null)            = -1
     *  ArrayUtils.lastIndex([])              = -1
     *  ArrayUtils.lastIndex([null])          = 0
     *  ArrayUtils.lastIndex([true, false])   = 1
     *  ArrayUtils.lastIndex([1, 2, 3])       = 2
     *  ArrayUtils.lastIndex([&quot;a&quot;, &quot;b&quot;, &quot;c&quot;]) = 2
     * </pre>
     * 
     * @param array
     *            the array to return the last index for, may be null
     * @return the last index, -1 if empty or null
     * @throws IllegalArgumentException
     *             if the object arguement is not an array.
     */
    public static int lastIndex( Object array )
    {
        return ArrayUtils.getLength( array ) - 1;
    }


    /**
     * <p>
     * Checks whether two arrays are the same type taking into account
     * multi-dimensional arrays.
     * </p>
     * 
     * @param array1
     *            the first array, must not be <code>null</code>
     * @param array2
     *            the second array, must not be <code>null</code>
     * @return <code>true</code> if type of arrays matches
     * @throws IllegalArgumentException
     *             if either array is <code>null</code>
     */
    public static boolean isSameType( Object array1, Object array2 )
    {
        if ( array1 == null || array2 == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04337 ) );
        }
        return array1.getClass().getName().equals( array2.getClass().getName() );
    }


    // Reverse
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     * <p>
     * There is no special handling for multi-dimensional arrays.
     * </p>
     * <p>
     * This method does nothing if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to reverse, may be <code>null</code>
     */
    public static void reverse( Object[] array )
    {
        if ( array == null )
        {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        Object tmp;
        while ( j > i )
        {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }


    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     * <p>
     * This method does nothing if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to reverse, may be <code>null</code>
     */
    public static void reverse( long[] array )
    {
        if ( array == null )
        {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        long tmp;
        while ( j > i )
        {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }


    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     * <p>
     * This method does nothing if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to reverse, may be <code>null</code>
     */
    public static void reverse( int[] array )
    {
        if ( array == null )
        {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        int tmp;
        while ( j > i )
        {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }


    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     * <p>
     * This method does nothing if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to reverse, may be <code>null</code>
     */
    public static void reverse( short[] array )
    {
        if ( array == null )
        {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        short tmp;
        while ( j > i )
        {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }


    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     * <p>
     * This method does nothing if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to reverse, may be <code>null</code>
     */
    public static void reverse( char[] array )
    {
        if ( array == null )
        {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        char tmp;
        while ( j > i )
        {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }


    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     * <p>
     * This method does nothing if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to reverse, may be <code>null</code>
     */
    public static void reverse( byte[] array )
    {
        if ( array == null )
        {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while ( j > i )
        {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }


    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     * <p>
     * This method does nothing if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to reverse, may be <code>null</code>
     */
    public static void reverse( double[] array )
    {
        if ( array == null )
        {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        double tmp;
        while ( j > i )
        {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }


    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     * <p>
     * This method does nothing if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to reverse, may be <code>null</code>
     */
    public static void reverse( float[] array )
    {
        if ( array == null )
        {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        float tmp;
        while ( j > i )
        {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }


    /**
     * <p>
     * Reverses the order of the given array.
     * </p>
     * <p>
     * This method does nothing if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to reverse, may be <code>null</code>
     */
    public static void reverse( boolean[] array )
    {
        if ( array == null )
        {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        boolean tmp;
        while ( j > i )
        {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }


    // IndexOf search
    // ----------------------------------------------------------------------

    // Object IndexOf
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Find the index of the given object in the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param objectToFind
     *            the object to find, may be <code>null</code>
     * @return the index of the object within the array, <code>-1</code> if
     *         not found or <code>null</code> array input
     */
    public static int indexOf( Object[] array, Object objectToFind )
    {
        return indexOf( array, objectToFind, 0 );
    }


    /**
     * <p>
     * Find the index of the given object in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex is treated as zero. A startIndex larger than the
     * array length will return <code>-1</code>.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param objectToFind
     *            the object to find, may be <code>null</code>
     * @param startIndex
     *            the index to start searching at
     * @return the index of the object within the array starting at the index,
     *         <code>-1</code> if not found or <code>null</code> array input
     */
    public static int indexOf( Object[] array, Object objectToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            startIndex = 0;
        }
        if ( objectToFind == null )
        {
            for ( int i = startIndex; i < array.length; i++ )
            {
                if ( array[i] == null )
                {
                    return i;
                }
            }
        }
        else
        {
            for ( int i = startIndex; i < array.length; i++ )
            {
                if ( objectToFind.equals( array[i] ) )
                {
                    return i;
                }
            }
        }
        return -1;
    }


    /**
     * <p>
     * Find the last index of the given object within the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to travers backwords looking for the object, may be
     *            <code>null</code>
     * @param objectToFind
     *            the object to find, may be <code>null</code>
     * @return the last index of the object within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( Object[] array, Object objectToFind )
    {
        return lastIndexOf( array, objectToFind, Integer.MAX_VALUE );
    }


    /**
     * <p>
     * Find the last index of the given object in the array starting at the
     * given index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex will return <code>-1</code>. A startIndex larger
     * than the array length will search from the end of the array.
     * </p>
     * 
     * @param array
     *            the array to traverse for looking for the object, may be
     *            <code>null</code>
     * @param objectToFind
     *            the object to find, may be <code>null</code>
     * @param startIndex
     *            the start index to travers backwards from
     * @return the last index of the object within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( Object[] array, Object objectToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            return -1;
        }
        else if ( startIndex >= array.length )
        {
            startIndex = array.length - 1;
        }
        if ( objectToFind == null )
        {
            for ( int i = startIndex; i >= 0; i-- )
            {
                if ( array[i] == null )
                {
                    return i;
                }
            }
        }
        else
        {
            for ( int i = startIndex; i >= 0; i-- )
            {
                if ( objectToFind.equals( array[i] ) )
                {
                    return i;
                }
            }
        }
        return -1;
    }


    /**
     * <p>
     * Checks if the object is in the given array.
     * </p>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     * 
     * @param array
     *            the array to search through
     * @param objectToFind
     *            the object to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains( Object[] array, Object objectToFind )
    {
        return ( indexOf( array, objectToFind ) != -1 );
    }


    // long IndexOf
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Find the index of the given value in the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( long[] array, long valueToFind )
    {
        return indexOf( array, valueToFind, 0 );
    }


    /**
     * <p>
     * Find the index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex is treated as zero. A startIndex larger than the
     * array length will return -1.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the index to start searching at
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( long[] array, long valueToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            startIndex = 0;
        }
        for ( int i = startIndex; i < array.length; i++ )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Find the last index of the given value within the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to travers backwords looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the object to find
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( long[] array, long valueToFind )
    {
        return lastIndexOf( array, valueToFind, Integer.MAX_VALUE );
    }


    /**
     * <p>
     * Find the last index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex will return -1. A startIndex larger than the array
     * length will search from the end of the array.
     * </p>
     * 
     * @param array
     *            the array to traverse for looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the start index to travers backwards from
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( long[] array, long valueToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            return -1;
        }
        else if ( startIndex >= array.length )
        {
            startIndex = array.length - 1;
        }
        for ( int i = startIndex; i >= 0; i-- )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     * 
     * @param array
     *            the array to search through
     * @param valueToFind
     *            the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains( long[] array, long valueToFind )
    {
        return ( indexOf( array, valueToFind ) != -1 );
    }


    // int IndexOf
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Find the index of the given value in the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( int[] array, int valueToFind )
    {
        return indexOf( array, valueToFind, 0 );
    }


    /**
     * <p>
     * Find the index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex is treated as zero. A startIndex larger than the
     * array length will return -1.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the index to start searching at
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( int[] array, int valueToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            startIndex = 0;
        }
        for ( int i = startIndex; i < array.length; i++ )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Find the last index of the given value within the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to travers backwords looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the object to find
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( int[] array, int valueToFind )
    {
        return lastIndexOf( array, valueToFind, Integer.MAX_VALUE );
    }


    /**
     * <p>
     * Find the last index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex will return -1. A startIndex larger than the array
     * length will search from the end of the array.
     * </p>
     * 
     * @param array
     *            the array to traverse for looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the start index to travers backwards from
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( int[] array, int valueToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            return -1;
        }
        else if ( startIndex >= array.length )
        {
            startIndex = array.length - 1;
        }
        for ( int i = startIndex; i >= 0; i-- )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     * 
     * @param array
     *            the array to search through
     * @param valueToFind
     *            the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains( int[] array, int valueToFind )
    {
        return ( indexOf( array, valueToFind ) != -1 );
    }


    // short IndexOf
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Find the index of the given value in the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( short[] array, short valueToFind )
    {
        return indexOf( array, valueToFind, 0 );
    }


    /**
     * <p>
     * Find the index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex is treated as zero. A startIndex larger than the
     * array length will return -1.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the index to start searching at
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( short[] array, short valueToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            startIndex = 0;
        }
        for ( int i = startIndex; i < array.length; i++ )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Find the last index of the given value within the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to travers backwords looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the object to find
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( short[] array, short valueToFind )
    {
        return lastIndexOf( array, valueToFind, Integer.MAX_VALUE );
    }


    /**
     * <p>
     * Find the last index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex will return -1. A startIndex larger than the array
     * length will search from the end of the array.
     * </p>
     * 
     * @param array
     *            the array to traverse for looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the start index to travers backwards from
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( short[] array, short valueToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            return -1;
        }
        else if ( startIndex >= array.length )
        {
            startIndex = array.length - 1;
        }
        for ( int i = startIndex; i >= 0; i-- )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     * 
     * @param array
     *            the array to search through
     * @param valueToFind
     *            the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains( short[] array, short valueToFind )
    {
        return ( indexOf( array, valueToFind ) != -1 );
    }


    // char IndexOf
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Find the index of the given value in the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( char[] array, char valueToFind )
    {
        return indexOf( array, valueToFind, 0 );
    }


    /**
     * <p>
     * Find the index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex is treated as zero. A startIndex larger than the
     * array length will return -1.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the index to start searching at
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( char[] array, char valueToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            startIndex = 0;
        }
        for ( int i = startIndex; i < array.length; i++ )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Find the last index of the given value within the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to travers backwords looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the object to find
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( char[] array, char valueToFind )
    {
        return lastIndexOf( array, valueToFind, Integer.MAX_VALUE );
    }


    /**
     * <p>
     * Find the last index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex will return -1. A startIndex larger than the array
     * length will search from the end of the array.
     * </p>
     * 
     * @param array
     *            the array to traverse for looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the start index to travers backwards from
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( char[] array, char valueToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            return -1;
        }
        else if ( startIndex >= array.length )
        {
            startIndex = array.length - 1;
        }
        for ( int i = startIndex; i >= 0; i-- )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     * 
     * @param array
     *            the array to search through
     * @param valueToFind
     *            the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains( char[] array, char valueToFind )
    {
        return ( indexOf( array, valueToFind ) != -1 );
    }


    // byte IndexOf
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Find the index of the given value in the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( byte[] array, byte valueToFind )
    {
        return indexOf( array, valueToFind, 0 );
    }


    /**
     * <p>
     * Find the index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex is treated as zero. A startIndex larger than the
     * array length will return -1.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the index to start searching at
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( byte[] array, byte valueToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            startIndex = 0;
        }
        for ( int i = startIndex; i < array.length; i++ )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Find the last index of the given value within the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to travers backwords looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the object to find
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( byte[] array, byte valueToFind )
    {
        return lastIndexOf( array, valueToFind, Integer.MAX_VALUE );
    }


    /**
     * <p>
     * Find the last index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex will return -1. A startIndex larger than the array
     * length will search from the end of the array.
     * </p>
     * 
     * @param array
     *            the array to traverse for looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the start index to travers backwards from
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( byte[] array, byte valueToFind, int startIndex )
    {
        if ( array == null )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            return -1;
        }
        else if ( startIndex >= array.length )
        {
            startIndex = array.length - 1;
        }
        for ( int i = startIndex; i >= 0; i-- )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     * 
     * @param array
     *            the array to search through
     * @param valueToFind
     *            the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains( byte[] array, byte valueToFind )
    {
        return ( indexOf( array, valueToFind ) != -1 );
    }


    // double IndexOf
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Find the index of the given value in the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( double[] array, double valueToFind )
    {
        return indexOf( array, valueToFind, 0 );
    }


    /**
     * <p>
     * Find the index of the given value within a given tolerance in the array.
     * This method will return the index of the first value which falls between
     * the region defined by valueToFind - tolerance and valueToFind +
     * tolerance.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param tolerance
     *            tolerance of the search
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( double[] array, double valueToFind, double tolerance )
    {
        return indexOf( array, valueToFind, 0, tolerance );
    }


    /**
     * <p>
     * Find the index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex is treated as zero. A startIndex larger than the
     * array length will return -1.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the index to start searching at
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( double[] array, double valueToFind, int startIndex )
    {
        if ( ArrayUtils.isEmpty( array ) )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            startIndex = 0;
        }
        for ( int i = startIndex; i < array.length; i++ )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Find the index of the given value in the array starting at the given
     * index. This method will return the index of the first value which falls
     * between the region defined by valueToFind - tolerance and valueToFind +
     * tolerance.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex is treated as zero. A startIndex larger than the
     * array length will return -1.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the index to start searching at
     * @param tolerance
     *            tolerance of the search
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( double[] array, double valueToFind, int startIndex, double tolerance )
    {
        if ( ArrayUtils.isEmpty( array ) )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            startIndex = 0;
        }
        double min = valueToFind - tolerance;
        double max = valueToFind + tolerance;
        for ( int i = startIndex; i < array.length; i++ )
        {
            if ( array[i] >= min && array[i] <= max )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Find the last index of the given value within the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to travers backwords looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the object to find
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( double[] array, double valueToFind )
    {
        return lastIndexOf( array, valueToFind, Integer.MAX_VALUE );
    }


    /**
     * <p>
     * Find the last index of the given value within a given tolerance in the
     * array. This method will return the index of the last value which falls
     * between the region defined by valueToFind - tolerance and valueToFind +
     * tolerance.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param tolerance
     *            tolerance of the search
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int lastIndexOf( double[] array, double valueToFind, double tolerance )
    {
        return lastIndexOf( array, valueToFind, Integer.MAX_VALUE, tolerance );
    }


    /**
     * <p>
     * Find the last index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex will return -1. A startIndex larger than the array
     * length will search from the end of the array.
     * </p>
     * 
     * @param array
     *            the array to traverse for looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the start index to travers backwards from
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( double[] array, double valueToFind, int startIndex )
    {
        if ( ArrayUtils.isEmpty( array ) )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            return -1;
        }
        else if ( startIndex >= array.length )
        {
            startIndex = array.length - 1;
        }
        for ( int i = startIndex; i >= 0; i-- )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Find the last index of the given value in the array starting at the given
     * index. This method will return the index of the last value which falls
     * between the region defined by valueToFind - tolerance and valueToFind +
     * tolerance.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex will return -1. A startIndex larger than the array
     * length will search from the end of the array.
     * </p>
     * 
     * @param array
     *            the array to traverse for looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the start index to travers backwards from
     * @param tolerance
     *            search for value within plus/minus this amount
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( double[] array, double valueToFind, int startIndex, double tolerance )
    {
        if ( ArrayUtils.isEmpty( array ) )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            return -1;
        }
        else if ( startIndex >= array.length )
        {
            startIndex = array.length - 1;
        }
        double min = valueToFind - tolerance;
        double max = valueToFind + tolerance;
        for ( int i = startIndex; i >= 0; i-- )
        {
            if ( array[i] >= min && array[i] <= max )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     * 
     * @param array
     *            the array to search through
     * @param valueToFind
     *            the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains( double[] array, double valueToFind )
    {
        return ( indexOf( array, valueToFind ) != -1 );
    }


    /**
     * <p>
     * Checks if a value falling within the given tolerance is in the given
     * array. If the array contains a value within the inclusive range defined
     * by (value - tolerance) to (value + tolerance).
     * </p>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     * 
     * @param array
     *            the array to search
     * @param valueToFind
     *            the value to find
     * @param tolerance
     *            the array contains the tolerance of the search
     * @return true if value falling within tolerance is in array
     */
    public static boolean contains( double[] array, double valueToFind, double tolerance )
    {
        return ( indexOf( array, valueToFind, 0, tolerance ) != -1 );
    }


    // float IndexOf
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Find the index of the given value in the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( float[] array, float valueToFind )
    {
        return indexOf( array, valueToFind, 0 );
    }


    /**
     * <p>
     * Find the index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex is treated as zero. A startIndex larger than the
     * array length will return -1.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the index to start searching at
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( float[] array, float valueToFind, int startIndex )
    {
        if ( ArrayUtils.isEmpty( array ) )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            startIndex = 0;
        }
        for ( int i = startIndex; i < array.length; i++ )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Find the last index of the given value within the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to travers backwords looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the object to find
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( float[] array, float valueToFind )
    {
        return lastIndexOf( array, valueToFind, Integer.MAX_VALUE );
    }


    /**
     * <p>
     * Find the last index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex will return -1. A startIndex larger than the array
     * length will search from the end of the array.
     * </p>
     * 
     * @param array
     *            the array to traverse for looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the start index to travers backwards from
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( float[] array, float valueToFind, int startIndex )
    {
        if ( ArrayUtils.isEmpty( array ) )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            return -1;
        }
        else if ( startIndex >= array.length )
        {
            startIndex = array.length - 1;
        }
        for ( int i = startIndex; i >= 0; i-- )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     * 
     * @param array
     *            the array to search through
     * @param valueToFind
     *            the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains( float[] array, float valueToFind )
    {
        return ( indexOf( array, valueToFind ) != -1 );
    }


    // boolean IndexOf
    // -----------------------------------------------------------------------
    /**
     * <p>
     * Find the index of the given value in the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( boolean[] array, boolean valueToFind )
    {
        return indexOf( array, valueToFind, 0 );
    }


    /**
     * <p>
     * Find the index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex is treated as zero. A startIndex larger than the
     * array length will return -1.
     * </p>
     * 
     * @param array
     *            the array to search through for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the index to start searching at
     * @return the index of the value within the array, <code>-1</code> if not
     *         found or <code>null</code> array input
     */
    public static int indexOf( boolean[] array, boolean valueToFind, int startIndex )
    {
        if ( ArrayUtils.isEmpty( array ) )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            startIndex = 0;
        }
        for ( int i = startIndex; i < array.length; i++ )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Find the last index of the given value within the array.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * 
     * @param array
     *            the array to travers backwords looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the object to find
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( boolean[] array, boolean valueToFind )
    {
        return lastIndexOf( array, valueToFind, Integer.MAX_VALUE );
    }


    /**
     * <p>
     * Find the last index of the given value in the array starting at the given
     * index.
     * </p>
     * <p>
     * This method returns <code>-1</code> if <code>null</code> array input.
     * </p>
     * <p>
     * A negative startIndex will return -1. A startIndex larger than the array
     * length will search from the end of the array.
     * </p>
     * 
     * @param array
     *            the array to traverse for looking for the object, may be
     *            <code>null</code>
     * @param valueToFind
     *            the value to find
     * @param startIndex
     *            the start index to travers backwards from
     * @return the last index of the value within the array, <code>-1</code>
     *         if not found or <code>null</code> array input
     */
    public static int lastIndexOf( boolean[] array, boolean valueToFind, int startIndex )
    {
        if ( ArrayUtils.isEmpty( array ) )
        {
            return -1;
        }
        if ( startIndex < 0 )
        {
            return -1;
        }
        else if ( startIndex >= array.length )
        {
            startIndex = array.length - 1;
        }
        for ( int i = startIndex; i >= 0; i-- )
        {
            if ( valueToFind == array[i] )
            {
                return i;
            }
        }
        return -1;
    }


    /**
     * <p>
     * Checks if the value is in the given array.
     * </p>
     * <p>
     * The method returns <code>false</code> if a <code>null</code> array is
     * passed in.
     * </p>
     * 
     * @param array
     *            the array to search through
     * @param valueToFind
     *            the value to find
     * @return <code>true</code> if the array contains the object
     */
    public static boolean contains( boolean[] array, boolean valueToFind )
    {
        return ( indexOf( array, valueToFind ) != -1 );
    }


    // Primitive/Object array converters
    // ----------------------------------------------------------------------

    // Long array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Longs to primitives.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Long</code> array, may be <code>null</code>
     * @return a <code>long</code> array, <code>null</code> if null array
     *         input
     * @throws NullPointerException
     *             if array content is <code>null</code>
     */
    public static long[] toPrimitive( Long[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_LONG_ARRAY;
        }
        final long[] result = new long[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = array[i].longValue();
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of object Long to primitives handling <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Long</code> array, may be <code>null</code>
     * @param valueForNull
     *            the value to insert if <code>null</code> found
     * @return a <code>long</code> array, <code>null</code> if null array
     *         input
     */
    public static long[] toPrimitive( Long[] array, long valueForNull )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_LONG_ARRAY;
        }
        final long[] result = new long[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            Long b = array[i];
            result[i] = ( b == null ? valueForNull : b.longValue() );
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of primitive longs to objects.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>long</code> array
     * @return a <code>Long</code> array, <code>null</code> if null array
     *         input
     */
    public static Long[] toObject( long[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_LONG_OBJECT_ARRAY;
        }
        final Long[] result = new Long[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = new Long( array[i] );
        }
        return result;
    }


    // Int array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Integers to primitives.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Integer</code> array, may be <code>null</code>
     * @return an <code>int</code> array, <code>null</code> if null array
     *         input
     * @throws NullPointerException
     *             if array content is <code>null</code>
     */
    public static int[] toPrimitive( Integer[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_INT_ARRAY;
        }
        final int[] result = new int[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = array[i].intValue();
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of object Integer to primitives handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Integer</code> array, may be <code>null</code>
     * @param valueForNull
     *            the value to insert if <code>null</code> found
     * @return an <code>int</code> array, <code>null</code> if null array
     *         input
     */
    public static int[] toPrimitive( Integer[] array, int valueForNull )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_INT_ARRAY;
        }
        final int[] result = new int[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            Integer b = array[i];
            result[i] = ( b == null ? valueForNull : b.intValue() );
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of primitive ints to objects.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            an <code>int</code> array
     * @return an <code>Integer</code> array, <code>null</code> if null
     *         array input
     */
    public static Integer[] toObject( int[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_INTEGER_OBJECT_ARRAY;
        }
        final Integer[] result = new Integer[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = new Integer( array[i] );
        }
        return result;
    }


    // Short array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Shorts to primitives.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Short</code> array, may be <code>null</code>
     * @return a <code>byte</code> array, <code>null</code> if null array
     *         input
     * @throws NullPointerException
     *             if array content is <code>null</code>
     */
    public static short[] toPrimitive( Short[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_SHORT_ARRAY;
        }
        final short[] result = new short[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = array[i].shortValue();
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of object Short to primitives handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Short</code> array, may be <code>null</code>
     * @param valueForNull
     *            the value to insert if <code>null</code> found
     * @return a <code>byte</code> array, <code>null</code> if null array
     *         input
     */
    public static short[] toPrimitive( Short[] array, short valueForNull )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_SHORT_ARRAY;
        }
        final short[] result = new short[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            Short b = array[i];
            result[i] = ( b == null ? valueForNull : b.shortValue() );
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of primitive shorts to objects.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>short</code> array
     * @return a <code>Short</code> array, <code>null</code> if null array
     *         input
     */
    public static Short[] toObject( short[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_SHORT_OBJECT_ARRAY;
        }
        final Short[] result = new Short[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = new Short( array[i] );
        }
        return result;
    }


    // Byte array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Bytes to primitives.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Byte</code> array, may be <code>null</code>
     * @return a <code>byte</code> array, <code>null</code> if null array
     *         input
     * @throws NullPointerException
     *             if array content is <code>null</code>
     */
    public static byte[] toPrimitive( Byte[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_BYTE_ARRAY;
        }
        final byte[] result = new byte[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = array[i].byteValue();
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of object Bytes to primitives handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Byte</code> array, may be <code>null</code>
     * @param valueForNull
     *            the value to insert if <code>null</code> found
     * @return a <code>byte</code> array, <code>null</code> if null array
     *         input
     */
    public static byte[] toPrimitive( Byte[] array, byte valueForNull )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_BYTE_ARRAY;
        }
        final byte[] result = new byte[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            Byte b = array[i];
            result[i] = ( b == null ? valueForNull : b.byteValue() );
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of primitive bytes to objects.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>byte</code> array
     * @return a <code>Byte</code> array, <code>null</code> if null array
     *         input
     */
    public static Byte[] toObject( byte[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_BYTE_OBJECT_ARRAY;
        }
        final Byte[] result = new Byte[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = new Byte( array[i] );
        }
        return result;
    }


    // Double array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Doubles to primitives.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Double</code> array, may be <code>null</code>
     * @return a <code>double</code> array, <code>null</code> if null array
     *         input
     * @throws NullPointerException
     *             if array content is <code>null</code>
     */
    public static double[] toPrimitive( Double[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_DOUBLE_ARRAY;
        }
        final double[] result = new double[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = array[i].doubleValue();
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of object Doubles to primitives handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Double</code> array, may be <code>null</code>
     * @param valueForNull
     *            the value to insert if <code>null</code> found
     * @return a <code>double</code> array, <code>null</code> if null array
     *         input
     */
    public static double[] toPrimitive( Double[] array, double valueForNull )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_DOUBLE_ARRAY;
        }
        final double[] result = new double[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            Double b = array[i];
            result[i] = ( b == null ? valueForNull : b.doubleValue() );
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of primitive doubles to objects.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>double</code> array
     * @return a <code>Double</code> array, <code>null</code> if null array
     *         input
     */
    public static Double[] toObject( double[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_DOUBLE_OBJECT_ARRAY;
        }
        final Double[] result = new Double[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = Double.valueOf( array[i] );
        }
        return result;
    }


    // Float array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Floats to primitives.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Float</code> array, may be <code>null</code>
     * @return a <code>float</code> array, <code>null</code> if null array
     *         input
     * @throws NullPointerException
     *             if array content is <code>null</code>
     */
    public static float[] toPrimitive( Float[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_FLOAT_ARRAY;
        }
        final float[] result = new float[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = array[i].floatValue();
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of object Floats to primitives handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Float</code> array, may be <code>null</code>
     * @param valueForNull
     *            the value to insert if <code>null</code> found
     * @return a <code>float</code> array, <code>null</code> if null array
     *         input
     */
    public static float[] toPrimitive( Float[] array, float valueForNull )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_FLOAT_ARRAY;
        }
        final float[] result = new float[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            Float b = array[i];
            result[i] = ( b == null ? valueForNull : b.floatValue() );
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of primitive floats to objects.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>float</code> array
     * @return a <code>Float</code> array, <code>null</code> if null array
     *         input
     */
    public static Float[] toObject( float[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_FLOAT_OBJECT_ARRAY;
        }
        final Float[] result = new Float[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = Float.valueOf( array[i] );
        }
        return result;
    }


    // Boolean array converters
    // ----------------------------------------------------------------------
    /**
     * <p>
     * Converts an array of object Booleans to primitives.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Boolean</code> array, may be <code>null</code>
     * @return a <code>boolean</code> array, <code>null</code> if null array
     *         input
     * @throws NullPointerException
     *             if array content is <code>null</code>
     */
    public static boolean[] toPrimitive( Boolean[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_BOOLEAN_ARRAY;
        }
        final boolean[] result = new boolean[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = array[i].booleanValue();
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of object Booleans to primitives handling
     * <code>null</code>.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>Boolean</code> array, may be <code>null</code>
     * @param valueForNull
     *            the value to insert if <code>null</code> found
     * @return a <code>boolean</code> array, <code>null</code> if null array
     *         input
     */
    public static boolean[] toPrimitive( Boolean[] array, boolean valueForNull )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_BOOLEAN_ARRAY;
        }
        final boolean[] result = new boolean[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            Boolean b = array[i];
            result[i] = ( b == null ? valueForNull : b.booleanValue() );
        }
        return result;
    }


    /**
     * <p>
     * Converts an array of primitive booleans to objects.
     * </p>
     * <p>
     * This method returns <code>null</code> if <code>null</code> array
     * input.
     * </p>
     * 
     * @param array
     *            a <code>boolean</code> array
     * @return a <code>Boolean</code> array, <code>null</code> if null array
     *         input
     */
    public static Boolean[] toObject( boolean[] array )
    {
        if ( array == null )
        {
            return null;
        }
        else if ( array.length == 0 )
        {
            return EMPTY_BOOLEAN_OBJECT_ARRAY;
        }
        final Boolean[] result = new Boolean[array.length];
        for ( int i = 0; i < array.length; i++ )
        {
            result[i] = ( array[i] ? Boolean.TRUE : Boolean.FALSE );
        }
        return result;
    }


    // ----------------------------------------------------------------------
    /**
     * <p>
     * Checks if an array of Objects is empty or <code>null</code>.
     * </p>
     * 
     * @param array
     *            the array to test
     * @return <code>true</code> if the array is empty or <code>null</code>
     * @since 2.1
     */
    public static boolean isEmpty( Object[] array )
    {
        if ( array == null || array.length == 0 )
        {
            return true;
        }
        return false;
    }


    /**
     * <p>
     * Checks if an array of primitive longs is empty or <code>null</code>.
     * </p>
     * 
     * @param array
     *            the array to test
     * @return <code>true</code> if the array is empty or <code>null</code>
     * @since 2.1
     */
    public static boolean isEmpty( long[] array )
    {
        if ( array == null || array.length == 0 )
        {
            return true;
        }
        return false;
    }


    /**
     * <p>
     * Checks if an array of primitive ints is empty or <code>null</code>.
     * </p>
     * 
     * @param array
     *            the array to test
     * @return <code>true</code> if the array is empty or <code>null</code>
     * @since 2.1
     */
    public static boolean isEmpty( int[] array )
    {
        if ( array == null || array.length == 0 )
        {
            return true;
        }
        return false;
    }


    /**
     * <p>
     * Checks if an array of primitive shorts is empty or <code>null</code>.
     * </p>
     * 
     * @param array
     *            the array to test
     * @return <code>true</code> if the array is empty or <code>null</code>
     * @since 2.1
     */
    public static boolean isEmpty( short[] array )
    {
        if ( array == null || array.length == 0 )
        {
            return true;
        }
        return false;
    }


    /**
     * <p>
     * Checks if an array of primitive chars is empty or <code>null</code>.
     * </p>
     * 
     * @param array
     *            the array to test
     * @return <code>true</code> if the array is empty or <code>null</code>
     * @since 2.1
     */
    public static boolean isEmpty( char[] array )
    {
        if ( array == null || array.length == 0 )
        {
            return true;
        }
        return false;
    }


    /**
     * <p>
     * Checks if an array of primitive bytes is empty or <code>null</code>.
     * </p>
     * 
     * @param array
     *            the array to test
     * @return <code>true</code> if the array is empty or <code>null</code>
     * @since 2.1
     */
    public static boolean isEmpty( byte[] array )
    {
        if ( array == null || array.length == 0 )
        {
            return true;
        }
        return false;
    }


    /**
     * <p>
     * Checks if an array of primitive doubles is empty or <code>null</code>.
     * </p>
     * 
     * @param array
     *            the array to test
     * @return <code>true</code> if the array is empty or <code>null</code>
     * @since 2.1
     */
    public static boolean isEmpty( double[] array )
    {
        if ( array == null || array.length == 0 )
        {
            return true;
        }
        return false;
    }


    /**
     * <p>
     * Checks if an array of primitive floats is empty or <code>null</code>.
     * </p>
     * 
     * @param array
     *            the array to test
     * @return <code>true</code> if the array is empty or <code>null</code>
     * @since 2.1
     */
    public static boolean isEmpty( float[] array )
    {
        if ( array == null || array.length == 0 )
        {
            return true;
        }
        return false;
    }


    /**
     * <p>
     * Checks if an array of primitive booleans is empty or <code>null</code>.
     * </p>
     * 
     * @param array
     *            the array to test
     * @return <code>true</code> if the array is empty or <code>null</code>
     * @since 2.1
     */
    public static boolean isEmpty( boolean[] array )
    {
        if ( array == null || array.length == 0 )
        {
            return true;
        }
        return false;
    }


    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>array1</code>
     * followed by all of the elements <code>array2</code>. When an array is
     * returned, it is always a new array.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.addAll(null, null)     = null
     *  ArrayUtils.addAll(array1, null)   = cloned copy of array1
     *  ArrayUtils.addAll(null, array2)   = cloned copy of array2
     *  ArrayUtils.addAll([], [])         = []
     *  ArrayUtils.addAll([null], [null]) = [null, null]
     *  ArrayUtils.addAll([&quot;a&quot;, &quot;b&quot;, &quot;c&quot;], [&quot;1&quot;, &quot;2&quot;, &quot;3&quot;]) = [&quot;a&quot;, &quot;b&quot;, &quot;c&quot;, &quot;1&quot;, &quot;2&quot;, &quot;3&quot;]
     * </pre>
     * 
     * @param array1
     *            the first array whose elements are added to the new array, may
     *            be <code>null</code>
     * @param array2
     *            the second array whose elements are added to the new array,
     *            may be <code>null</code>
     * @return The new array, <code>null</code> if <code>null</code> array
     *         inputs. The type of the new array is the type of the first array.
     * @since 2.1
     */
    public static Object[] addAll( Object[] array1, Object[] array2 )
    {
        if ( array1 == null )
        {
            return clone( array2 );
        }
        else if ( array2 == null )
        {
            return clone( array1 );
        }
        else
        {
            Object[] joinedArray = ( Object[] ) Array.newInstance( array1.getClass().getComponentType(), array1.length
                + array2.length );
            System.arraycopy( array1, 0, joinedArray, 0, array1.length );
            System.arraycopy( array2, 0, joinedArray, array1.length, array2.length );
            return joinedArray;
        }
    }


    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.add(null, null)      = [null]
     *  ArrayUtils.add(null, &quot;a&quot;)       = [&quot;a&quot;]
     *  ArrayUtils.add([&quot;a&quot;], null)     = [&quot;a&quot;, null]
     *  ArrayUtils.add([&quot;a&quot;], &quot;b&quot;)      = [&quot;a&quot;, &quot;b&quot;]
     *  ArrayUtils.add([&quot;a&quot;, &quot;b&quot;], &quot;c&quot;) = [&quot;a&quot;, &quot;b&quot;, &quot;c&quot;]
     * </pre>
     * 
     * @param array
     *            the array to "add" the element to, may be <code>null</code>
     * @param element
     *            the object to add
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static Object[] add( Object[] array, Object element )
    {
        Object newArray = copyArrayGrow1( array, element != null ? element.getClass() : Object.class );
        Array.set( newArray, lastIndex( newArray ), element );
        return ( Object[] ) newArray;
    }


    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.add(null, true)          = [true]
     *  ArrayUtils.add([true], false)       = [true, false]
     *  ArrayUtils.add([true, false], true) = [true, false, true]
     * </pre>
     * 
     * @param array
     *            the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element
     *            the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static boolean[] add( boolean[] array, boolean element )
    {
        boolean[] newArray = ( boolean[] ) copyArrayGrow1( array, Boolean.TYPE );
        newArray[lastIndex( newArray )] = element;
        return newArray;
    }


    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.add(null, 0)   = [0]
     *  ArrayUtils.add([1], 0)    = [1, 0]
     *  ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     * 
     * @param array
     *            the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element
     *            the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static byte[] add( byte[] array, byte element )
    {
        byte[] newArray = ( byte[] ) copyArrayGrow1( array, Byte.TYPE );
        newArray[lastIndex( newArray )] = element;
        return newArray;
    }


    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.add(null, '0')       = ['0']
     *  ArrayUtils.add(['1'], '0')      = ['1', '0']
     *  ArrayUtils.add(['1', '0'], '1') = ['1', '0', '1']
     * </pre>
     * 
     * @param array
     *            the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element
     *            the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static char[] add( char[] array, char element )
    {
        char[] newArray = ( char[] ) copyArrayGrow1( array, Character.TYPE );
        newArray[lastIndex( newArray )] = element;
        return newArray;
    }


    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.add(null, 0)   = [0]
     *  ArrayUtils.add([1], 0)    = [1, 0]
     *  ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     * 
     * @param array
     *            the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element
     *            the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static double[] add( double[] array, double element )
    {
        double[] newArray = ( double[] ) copyArrayGrow1( array, Double.TYPE );
        newArray[lastIndex( newArray )] = element;
        return newArray;
    }


    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.add(null, 0)   = [0]
     *  ArrayUtils.add([1], 0)    = [1, 0]
     *  ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     * 
     * @param array
     *            the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element
     *            the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static float[] add( float[] array, float element )
    {
        float[] newArray = ( float[] ) copyArrayGrow1( array, Float.TYPE );
        newArray[lastIndex( newArray )] = element;
        return newArray;
    }


    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.add(null, 0)   = [0]
     *  ArrayUtils.add([1], 0)    = [1, 0]
     *  ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     * 
     * @param array
     *            the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element
     *            the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static int[] add( int[] array, int element )
    {
        int[] newArray = ( int[] ) copyArrayGrow1( array, Integer.TYPE );
        newArray[lastIndex( newArray )] = element;
        return newArray;
    }


    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.add(null, 0)   = [0]
     *  ArrayUtils.add([1], 0)    = [1, 0]
     *  ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     * 
     * @param array
     *            the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element
     *            the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static long[] add( long[] array, long element )
    {
        long[] newArray = ( long[] ) copyArrayGrow1( array, Long.TYPE );
        newArray[lastIndex( newArray )] = element;
        return newArray;
    }


    /**
     * <p>
     * Copies the given array and adds the given element at the end of the new
     * array.
     * </p>
     * <p>
     * The new array contains the same elements of the input array plus the
     * given element in the last position. The component type of the new array
     * is the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.add(null, 0)   = [0]
     *  ArrayUtils.add([1], 0)    = [1, 0]
     *  ArrayUtils.add([1, 0], 1) = [1, 0, 1]
     * </pre>
     * 
     * @param array
     *            the array to copy and add the element to, may be
     *            <code>null</code>
     * @param element
     *            the object to add at the last index of the new array
     * @return A new array containing the existing elements plus the new element
     * @since 2.1
     */
    public static short[] add( short[] array, short element )
    {
        short[] newArray = ( short[] ) copyArrayGrow1( array, Short.TYPE );
        newArray[lastIndex( newArray )] = element;
        return newArray;
    }


    /**
     * Returns a copy of the given array of size 1 greater than the argument.
     * The last value of the array is left to the default value.
     * 
     * @param array
     *            The array to copy, must not be <code>null</code>.
     * @param newArrayComponentType
     *            If <code>array</code> is <code>null</code>, create a size
     *            1 array of this type.
     * @return A new copy of the array of size 1 greater than the input.
     */
    private static Object copyArrayGrow1( Object array, Class<?> newArrayComponentType )
    {
        if ( array != null )
        {
            int arrayLength = Array.getLength( array );
            Object newArray = Array.newInstance( array.getClass().getComponentType(), arrayLength + 1 );
            System.arraycopy( array, 0, newArray, 0, arrayLength );
            return newArray;
        }
        else
        {
            return Array.newInstance( newArrayComponentType, 1 );
        }
    }


    /**
     * <p>
     * Inserts the specified element at the specified position in the array.
     * Shifts the element currently at that position (if any) and any subsequent
     * elements to the right (adds one to their indices).
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * plus the given element on the specified position. The component type of
     * the returned array is always the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, a new one element array is
     * returned whose component type is the same as the element.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.add(null, 0, null)      = [null]
     *  ArrayUtils.add(null, 0, &quot;a&quot;)       = [&quot;a&quot;]
     *  ArrayUtils.add([&quot;a&quot;], 1, null)     = [&quot;a&quot;, null]
     *  ArrayUtils.add([&quot;a&quot;], 1, &quot;b&quot;)      = [&quot;a&quot;, &quot;b&quot;]
     *  ArrayUtils.add([&quot;a&quot;, &quot;b&quot;], 3, &quot;c&quot;) = [&quot;a&quot;, &quot;b&quot;, &quot;c&quot;]
     * </pre>
     * 
     * @param array
     *            the array to add the element to, may be <code>null</code>
     * @param index
     *            the position of the new object
     * @param element
     *            the object to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >
     *             array.length).
     */
    public static Object[] add( Object[] array, int index, Object element )
    {
        if ( array == null )
        {
            if ( index != 0 )
            {
                throw new IndexOutOfBoundsException( I18n.err( I18n.ERR_04338, index ,0 ) );
            }
            Object joinedArray = Array.newInstance( element != null ? element.getClass() : Object.class, 1 );
            Array.set( joinedArray, 0, element );
            return ( Object[] ) joinedArray;
        }
        int length = array.length;
        if ( index > length || index < 0 )
        {
            throw new IndexOutOfBoundsException( I18n.err( I18n.ERR_04338, index, length ) );
        }
        Object result = Array.newInstance( array.getClass().getComponentType(), length + 1 );
        System.arraycopy( array, 0, result, 0, index );
        Array.set( result, index, element );
        if ( index < length )
        {
            System.arraycopy( array, index, result, index + 1, length - index );
        }
        return ( Object[] ) result;
    }


    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.remove([&quot;a&quot;], 0)           = []
     *  ArrayUtils.remove([&quot;a&quot;, &quot;b&quot;], 0)      = [&quot;b&quot;]
     *  ArrayUtils.remove([&quot;a&quot;, &quot;b&quot;], 1)      = [&quot;a&quot;]
     *  ArrayUtils.remove([&quot;a&quot;, &quot;b&quot;, &quot;c&quot;], 1) = [&quot;a&quot;, &quot;c&quot;]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may not be
     *            <code>null</code>
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is <code>null</code>.
     * @since 2.1
     */
    public static Object[] remove( Object[] array, int index )
    {
        return ( Object[] ) remove( ( Object ) array, index );
    }


    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.removeElement(null, &quot;a&quot;)            = null
     *  ArrayUtils.removeElement([], &quot;a&quot;)              = []
     *  ArrayUtils.removeElement([&quot;a&quot;], &quot;b&quot;)           = [&quot;a&quot;]
     *  ArrayUtils.removeElement([&quot;a&quot;, &quot;b&quot;], &quot;a&quot;)      = [&quot;b&quot;]
     *  ArrayUtils.removeElement([&quot;a&quot;, &quot;b&quot;, &quot;a&quot;], &quot;a&quot;) = [&quot;b&quot;, &quot;a&quot;]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may be <code>null</code>
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static Object[] removeElement( Object[] array, Object element )
    {
        int index = indexOf( array, element );
        if ( index == -1 )
        {
            return clone( array );
        }
        return remove( array, index );
    }


    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.remove([true], 0)              = []
     *  ArrayUtils.remove([true, false], 0)       = [false]
     *  ArrayUtils.remove([true, false], 1)       = [true]
     *  ArrayUtils.remove([true, true, false], 1) = [true, false]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may not be
     *            <code>null</code>
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is <code>null</code>.
     * @since 2.1
     */
    public static boolean[] remove( boolean[] array, int index )
    {
        return ( boolean[] ) remove( ( Object ) array, index );
    }


    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.removeElement(null, true)                = null
     *  ArrayUtils.removeElement([], true)                  = []
     *  ArrayUtils.removeElement([true], false)             = [true]
     *  ArrayUtils.removeElement([true, false], false)      = [true]
     *  ArrayUtils.removeElement([true, false, true], true) = [false, true]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may be <code>null</code>
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static boolean[] removeElement( boolean[] array, boolean element )
    {
        int index = indexOf( array, element );
        if ( index == -1 )
        {
            return clone( array );
        }
        return remove( array, index );
    }


    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.remove([1], 0)          = []
     *  ArrayUtils.remove([1, 0], 0)       = [0]
     *  ArrayUtils.remove([1, 0], 1)       = [1]
     *  ArrayUtils.remove([1, 0, 1], 1)    = [1, 1]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may not be
     *            <code>null</code>
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is <code>null</code>.
     * @since 2.1
     */
    public static byte[] remove( byte[] array, int index )
    {
        return ( byte[] ) remove( ( Object ) array, index );
    }


    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.removeElement(null, 1)        = null
     *  ArrayUtils.removeElement([], 1)          = []
     *  ArrayUtils.removeElement([1], 0)         = [1]
     *  ArrayUtils.removeElement([1, 0], 0)      = [1]
     *  ArrayUtils.removeElement([1, 0, 1], 1)   = [0, 1]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may be <code>null</code>
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static byte[] removeElement( byte[] array, byte element )
    {
        int index = indexOf( array, element );
        if ( index == -1 )
        {
            return clone( array );
        }
        return remove( array, index );
    }


    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.remove(['a'], 0)           = []
     *  ArrayUtils.remove(['a', 'b'], 0)      = ['b']
     *  ArrayUtils.remove(['a', 'b'], 1)      = ['a']
     *  ArrayUtils.remove(['a', 'b', 'c'], 1) = ['a', 'c']
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may not be
     *            <code>null</code>
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is <code>null</code>.
     * @since 2.1
     */
    public static char[] remove( char[] array, int index )
    {
        return ( char[] ) remove( ( Object ) array, index );
    }


    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.removeElement(null, 'a')            = null
     *  ArrayUtils.removeElement([], 'a')              = []
     *  ArrayUtils.removeElement(['a'], 'b')           = ['a']
     *  ArrayUtils.removeElement(['a', 'b'], 'a')      = ['b']
     *  ArrayUtils.removeElement(['a', 'b', 'a'], 'a') = ['b', 'a']
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may be <code>null</code>
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static char[] removeElement( char[] array, char element )
    {
        int index = indexOf( array, element );
        if ( index == -1 )
        {
            return clone( array );
        }
        return remove( array, index );
    }


    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.remove([1.1], 0)           = []
     *  ArrayUtils.remove([2.5, 6.0], 0)      = [6.0]
     *  ArrayUtils.remove([2.5, 6.0], 1)      = [2.5]
     *  ArrayUtils.remove([2.5, 6.0, 3.8], 1) = [2.5, 3.8]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may not be
     *            <code>null</code>
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is <code>null</code>.
     * @since 2.1
     */
    public static double[] remove( double[] array, int index )
    {
        return ( double[] ) remove( ( Object ) array, index );
    }


    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.removeElement(null, 1.1)            = null
     *  ArrayUtils.removeElement([], 1.1)              = []
     *  ArrayUtils.removeElement([1.1], 1.2)           = [1.1]
     *  ArrayUtils.removeElement([1.1, 2.3], 1.1)      = [2.3]
     *  ArrayUtils.removeElement([1.1, 2.3, 1.1], 1.1) = [2.3, 1.1]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may be <code>null</code>
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static double[] removeElement( double[] array, double element )
    {
        int index = indexOf( array, element );
        if ( index == -1 )
        {
            return clone( array );
        }
        return remove( array, index );
    }


    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.remove([1.1], 0)           = []
     *  ArrayUtils.remove([2.5, 6.0], 0)      = [6.0]
     *  ArrayUtils.remove([2.5, 6.0], 1)      = [2.5]
     *  ArrayUtils.remove([2.5, 6.0, 3.8], 1) = [2.5, 3.8]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may not be
     *            <code>null</code>
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is <code>null</code>.
     * @since 2.1
     */
    public static float[] remove( float[] array, int index )
    {
        return ( float[] ) remove( ( Object ) array, index );
    }


    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.removeElement(null, 1.1)            = null
     *  ArrayUtils.removeElement([], 1.1)              = []
     *  ArrayUtils.removeElement([1.1], 1.2)           = [1.1]
     *  ArrayUtils.removeElement([1.1, 2.3], 1.1)      = [2.3]
     *  ArrayUtils.removeElement([1.1, 2.3, 1.1], 1.1) = [2.3, 1.1]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may be <code>null</code>
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static float[] removeElement( float[] array, float element )
    {
        int index = indexOf( array, element );
        if ( index == -1 )
        {
            return clone( array );
        }
        return remove( array, index );
    }


    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.remove([1], 0)         = []
     *  ArrayUtils.remove([2, 6], 0)      = [6]
     *  ArrayUtils.remove([2, 6], 1)      = [2]
     *  ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may not be
     *            <code>null</code>
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is <code>null</code>.
     * @since 2.1
     */
    public static int[] remove( int[] array, int index )
    {
        return ( int[] ) remove( ( Object ) array, index );
    }


    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.removeElement(null, 1)      = null
     *  ArrayUtils.removeElement([], 1)        = []
     *  ArrayUtils.removeElement([1], 2)       = [1]
     *  ArrayUtils.removeElement([1, 3], 1)    = [3]
     *  ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may be <code>null</code>
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static int[] removeElement( int[] array, int element )
    {
        int index = indexOf( array, element );
        if ( index == -1 )
        {
            return clone( array );
        }
        return remove( array, index );
    }


    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.remove([1], 0)         = []
     *  ArrayUtils.remove([2, 6], 0)      = [6]
     *  ArrayUtils.remove([2, 6], 1)      = [2]
     *  ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may not be
     *            <code>null</code>
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is <code>null</code>.
     * @since 2.1
     */
    public static long[] remove( long[] array, int index )
    {
        return ( long[] ) remove( ( Object ) array, index );
    }


    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.removeElement(null, 1)      = null
     *  ArrayUtils.removeElement([], 1)        = []
     *  ArrayUtils.removeElement([1], 2)       = [1]
     *  ArrayUtils.removeElement([1, 3], 1)    = [3]
     *  ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may be <code>null</code>
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static long[] removeElement( long[] array, long element )
    {
        int index = indexOf( array, element );
        if ( index == -1 )
        {
            return clone( array );
        }
        return remove( array, index );
    }


    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.remove([1], 0)         = []
     *  ArrayUtils.remove([2, 6], 0)      = [6]
     *  ArrayUtils.remove([2, 6], 1)      = [2]
     *  ArrayUtils.remove([2, 6, 3], 1)   = [2, 3]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may not be
     *            <code>null</code>
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is <code>null</code>.
     * @since 2.1
     */
    public static short[] remove( short[] array, int index )
    {
        return ( short[] ) remove( ( Object ) array, index );
    }


    /**
     * <p>
     * Removes the first occurrence of the specified element from the specified
     * array. All subsequent elements are shifted to the left (substracts one
     * from their indices). If the array doesn't contains such an element, no
     * elements are removed from the array.
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the first occurrence of the specified element. The component type
     * of the returned array is always the same as that of the input array.
     * </p>
     * 
     * <pre>
     *  ArrayUtils.removeElement(null, 1)      = null
     *  ArrayUtils.removeElement([], 1)        = []
     *  ArrayUtils.removeElement([1], 2)       = [1]
     *  ArrayUtils.removeElement([1, 3], 1)    = [3]
     *  ArrayUtils.removeElement([1, 3, 1], 1) = [3, 1]
     * </pre>
     * 
     * @param array
     *            the array to remove the element from, may be <code>null</code>
     * @param element
     *            the element to be removed
     * @return A new array containing the existing elements except the first
     *         occurrence of the specified element.
     * @since 2.1
     */
    public static short[] removeElement( short[] array, short element )
    {
        int index = indexOf( array, element );
        if ( index == -1 )
        {
            return clone( array );
        }
        return remove( array, index );
    }


    /**
     * <p>
     * Removes the element at the specified position from the specified array.
     * All subsequent elements are shifted to the left (substracts one from
     * their indices).
     * </p>
     * <p>
     * This method returns a new array with the same elements of the input array
     * except the element on the specified position. The component type of the
     * returned array is always the same as that of the input array.
     * </p>
     * <p>
     * If the input array is <code>null</code>, an IndexOutOfBoundsException
     * will be thrown, because in that case no valid index can be specified.
     * </p>
     * 
     * @param array
     *            the array to remove the element from, may not be
     *            <code>null</code>
     * @param index
     *            the position of the element to be removed
     * @return A new array containing the existing elements except the element
     *         at the specified position.
     * @throws IndexOutOfBoundsException
     *             if the index is out of range (index < 0 || index >=
     *             array.length), or if the array is <code>null</code>.
     * @since 2.1
     */
    private static Object remove( Object array, int index )
    {
        int length = getLength( array );
        if ( index < 0 || index >= length )
        {
            throw new IndexOutOfBoundsException( I18n.err( I18n.ERR_04338, index, length ) );
        }

        Object result = Array.newInstance( array.getClass().getComponentType(), length - 1 );
        System.arraycopy( array, 0, result, 0, index );
        if ( index < length - 1 )
        {
            System.arraycopy( array, index + 1, result, index, length - index - 1 );
        }

        return result;
    }

}

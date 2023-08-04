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


import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.directory.shared.i18n.I18n;


/**
 * <p>
 * Provides utilities for manipulating and examining <code>Throwable</code>
 * objects.
 * </p>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 919765 $
 */
public class ExceptionUtils
{

    /**
     * <p>
     * Used when printing stack frames to denote the start of a wrapped
     * exception.
     * </p>
     * <p/>
     * <p>
     * Package private for accessibility by test suite.
     * </p>
     */
    static final String WRAPPED_MARKER = " [wrapped] ";

    /**
     * <p>
     * The names of methods commonly used to access a wrapped exception.
     * </p>
     */
    private static String[] CAUSE_METHOD_NAMES =
        { "getCause", "getNextException", "getTargetException", "getException", "getSourceException", "getRootCause",
            "getCausedByException", "getNested", "getLinkedException", "getNestedException", "getLinkedCause",
            "getThrowable", };

    /**
     * <p>
     * The Method object for JDK1.4 getCause.
     * </p>
     */
    private static final Method THROWABLE_CAUSE_METHOD;

    static
    {
        Method getCauseMethod;
        try
        {
            getCauseMethod = Throwable.class.getMethod( "getCause", (Class[])null );
        }
        catch ( Exception e )
        {
            getCauseMethod = null;
        }
        THROWABLE_CAUSE_METHOD = getCauseMethod;
    }


    /**
     * <p>
     * Public constructor allows an instance of <code>ExceptionUtils</code> to
     * be created, although that is not normally necessary.
     * </p>
     */
    public ExceptionUtils()
    {
    }


    /**
     * <p>
     * Checks if a String is not empty ("") and not null.
     * </p>
     * 
     * @param str
     *            the String to check, may be null
     * @return <code>true</code> if the String is not empty and not null
     */
    private static boolean isNotEmpty( String str )
    {
        return ( str != null && str.length() > 0 );
    }


    // -----------------------------------------------------------------------
    /**
     * <p>
     * Adds to the list of method names used in the search for
     * <code>Throwable</code> objects.
     * </p>
     * 
     * @param methodName
     *            the methodName to add to the list, <code>null</code> and
     *            empty strings are ignored
     * @since 2.0
     */
    public static void addCauseMethodName( String methodName )
    {
        if ( isNotEmpty( methodName ) )
        {
            List<String> list = new ArrayList<String>( Arrays.asList( CAUSE_METHOD_NAMES ) );
            list.add( methodName );
            CAUSE_METHOD_NAMES = list.toArray( new String[list.size()] );
        }
    }


    /**
     * <p>
     * Introspects the <code>Throwable</code> to obtain the cause.
     * </p>
     * <p/>
     * <p>
     * The method searches for methods with specific names that return a
     * <code>Throwable</code> object. This will pick up most wrapping
     * exceptions, including those from JDK 1.4, and The method names can be
     * added to using {@link #addCauseMethodName(String)}.
     * </p>
     * <p/>
     * <p>
     * The default list searched for are:
     * </p>
     * <ul>
     * <li><code>getCause()</code></li>
     * <li><code>getNextException()</code></li>
     * <li><code>getTargetException()</code></li>
     * <li><code>getException()</code></li>
     * <li><code>getSourceException()</code></li>
     * <li><code>getRootCause()</code></li>
     * <li><code>getCausedByException()</code></li>
     * <li><code>getNested()</code></li>
     * </ul>
     * <p/>
     * <p>
     * In the absence of any such method, the object is inspected for a
     * <code>detail</code> field assignable to a <code>Throwable</code>.
     * </p>
     * <p/>
     * <p>
     * If none of the above is found, returns <code>null</code>.
     * </p>
     * 
     * @param throwable
     *            the throwable to introspect for a cause, may be null
     * @return the cause of the <code>Throwable</code>, <code>null</code>
     *         if none found or null throwable input
     * @since 1.0
     */
    public static Throwable getCause( Throwable throwable )
    {
        return getCause( throwable, CAUSE_METHOD_NAMES );
    }


    /**
     * <p>
     * Introspects the <code>Throwable</code> to obtain the cause.
     * </p>
     * <p/>
     * <ol>
     * <li>Try known exception types.</li>
     * <li>Try the supplied array of method names.</li>
     * <li>Try the field 'detail'.</li>
     * </ol>
     * <p/>
     * <p>
     * A <code>null</code> set of method names means use the default set. A
     * <code>null</code> in the set of method names will be ignored.
     * </p>
     * 
     * @param throwable
     *            the throwable to introspect for a cause, may be null
     * @param methodNames
     *            the method names, null treated as default set
     * @return the cause of the <code>Throwable</code>, <code>null</code>
     *         if none found or null throwable input
     * @since 1.0
     */
    public static Throwable getCause( Throwable throwable, String[] methodNames )
    {
        if ( throwable == null )
        {
            return null;
        }
        Throwable cause = getCauseUsingWellKnownTypes( throwable );
        if ( cause == null )
        {
            if ( methodNames == null )
            {
                methodNames = CAUSE_METHOD_NAMES;
            }
            for ( int i = 0; i < methodNames.length; i++ )
            {
                String methodName = methodNames[i];
                if ( methodName != null )
                {
                    cause = getCauseUsingMethodName( throwable, methodName );
                    if ( cause != null )
                    {
                        break;
                    }
                }
            }

            if ( cause == null )
            {
                cause = getCauseUsingFieldName( throwable, "detail" );
            }
        }
        return cause;
    }


    /**
     * <p>
     * Introspects the <code>Throwable</code> to obtain the root cause.
     * </p>
     * <p/>
     * <p>
     * This method walks through the exception chain to the last element, "root"
     * of the tree, using {@link #getCause(Throwable)}, and returns that
     * exception.
     * </p>
     * 
     * @param throwable
     *            the throwable to get the root cause for, may be null
     * @return the root cause of the <code>Throwable</code>,
     *         <code>null</code> if none found or null throwable input
     */
    public static Throwable getRootCause( Throwable throwable )
    {
        Throwable cause = getCause( throwable );
        if ( cause != null )
        {
            throwable = cause;
            while ( ( throwable = getCause( throwable ) ) != null )
            {
                cause = throwable;
            }
        }
        return cause;
    }


    /**
     * <p>
     * Finds a <code>Throwable</code> for known types.
     * </p>
     * <p/>
     * <p>
     * Uses <code>instanceof</code> checks to examine the exception, looking
     * for well known types which could contain chained or wrapped exceptions.
     * </p>
     * 
     * @param throwable
     *            the exception to examine
     * @return the wrapped exception, or <code>null</code> if not found
     */
    private static Throwable getCauseUsingWellKnownTypes( Throwable throwable )
    {
        if ( throwable instanceof Nestable )
        {
            return ( ( Nestable ) throwable ).getCause();
        }
        else if ( throwable instanceof SQLException )
        {
            return ( ( SQLException ) throwable ).getNextException();
        }
        else if ( throwable instanceof InvocationTargetException )
        {
            return ( ( InvocationTargetException ) throwable ).getTargetException();
        }
        else
        {
            return null;
        }
    }


    /**
     * <p>
     * Finds a <code>Throwable</code> by method name.
     * </p>
     * 
     * @param throwable
     *            the exception to examine
     * @param methodName
     *            the name of the method to find and invoke
     * @return the wrapped exception, or <code>null</code> if not found
     */
    private static Throwable getCauseUsingMethodName( Throwable throwable, String methodName )
    {
        Method method = null;
        try
        {
            method = throwable.getClass().getMethod( methodName, (Class[])null );
        }
        catch ( NoSuchMethodException ignored )
        {
        }
        catch ( SecurityException ignored )
        {
        }

        if ( method != null && Throwable.class.isAssignableFrom( method.getReturnType() ) )
        {
            try
            {
                return ( Throwable ) method.invoke( throwable, ArrayUtils.EMPTY_OBJECT_ARRAY );
            }
            catch ( IllegalAccessException ignored )
            {
            }
            catch ( IllegalArgumentException ignored )
            {
            }
            catch ( InvocationTargetException ignored )
            {
            }
        }
        return null;
    }


    /**
     * <p>
     * Finds a <code>Throwable</code> by field name.
     * </p>
     * 
     * @param throwable
     *            the exception to examine
     * @param fieldName
     *            the name of the attribute to examine
     * @return the wrapped exception, or <code>null</code> if not found
     */
    private static Throwable getCauseUsingFieldName( Throwable throwable, String fieldName )
    {
        Field field = null;
        try
        {
            field = throwable.getClass().getField( fieldName );
        }
        catch ( NoSuchFieldException ignored )
        {
        }
        catch ( SecurityException ignored )
        {
        }

        if ( field != null && Throwable.class.isAssignableFrom( field.getType() ) )
        {
            try
            {
                return ( Throwable ) field.get( throwable );
            }
            catch ( IllegalAccessException ignored )
            {
            }
            catch ( IllegalArgumentException ignored )
            {
            }
        }
        return null;
    }


    // -----------------------------------------------------------------------
    /**
     * <p>
     * Checks if the Throwable class has a <code>getCause</code> method.
     * </p>
     * <p/>
     * <p>
     * This is true for JDK 1.4 and above.
     * </p>
     * 
     * @return true if Throwable is nestable
     * @since 2.0
     */
    public static boolean isThrowableNested()
    {
        return ( THROWABLE_CAUSE_METHOD != null );
    }


    /**
     * <p>
     * Checks whether this <code>Throwable</code> class can store a cause.
     * </p>
     * <p/>
     * <p>
     * This method does <b>not</b> check whether it actually does store a
     * cause.
     * <p>
     * 
     * @param throwable
     *            the <code>Throwable</code> to examine, may be null
     * @return boolean <code>true</code> if nested otherwise
     *         <code>false</code>
     * @since 2.0
     */
    public static boolean isNestedThrowable( Throwable throwable )
    {
        if ( throwable == null )
        {
            return false;
        }

        if ( throwable instanceof Nestable )
        {
            return true;
        }
        else if ( throwable instanceof SQLException )
        {
            return true;
        }
        else if ( throwable instanceof InvocationTargetException )
        {
            return true;
        }
        else if ( isThrowableNested() )
        {
            return true;
        }

        Class cls = throwable.getClass();
        for ( int i = 0, isize = CAUSE_METHOD_NAMES.length; i < isize; i++ )
        {
            try
            {
                Method method = cls.getMethod( CAUSE_METHOD_NAMES[i], (Class[])null );
                if ( method != null && Throwable.class.isAssignableFrom( method.getReturnType() ) )
                {
                    return true;
                }
            }
            catch ( NoSuchMethodException ignored )
            {
            }
            catch ( SecurityException ignored )
            {
            }
        }

        try
        {
            Field field = cls.getField( "detail" );
            if ( field != null )
            {
                return true;
            }
        }
        catch ( NoSuchFieldException ignored )
        {
        }
        catch ( SecurityException ignored )
        {
        }

        return false;
    }


    // -----------------------------------------------------------------------
    /**
     * <p>
     * Counts the number of <code>Throwable</code> objects in the exception
     * chain.
     * </p>
     * <p/>
     * <p>
     * A throwable without cause will return <code>1</code>. A throwable with
     * one cause will return <code>2</code> and so on. A <code>null</code>
     * throwable will return <code>0</code>.
     * </p>
     * 
     * @param throwable
     *            the throwable to inspect, may be null
     * @return the count of throwables, zero if null input
     */
    public static int getThrowableCount( Throwable throwable )
    {
        int count = 0;
        while ( throwable != null )
        {
            count++;
            throwable = ExceptionUtils.getCause( throwable );
        }
        return count;
    }


    /**
     * <p>
     * Returns the list of <code>Throwable</code> objects in the exception
     * chain.
     * </p>
     * <p/>
     * <p>
     * A throwable without cause will return an array containing one element -
     * the input throwable. A throwable with one cause will return an array
     * containing two elements. - the input throwable and the cause throwable. A
     * <code>null</code> throwable will return an array size zero.
     * </p>
     * 
     * @param throwable
     *            the throwable to inspect, may be null
     * @return the array of throwables, never null
     */
    public static Throwable[] getThrowables( Throwable throwable )
    {
        List<Throwable> list = new ArrayList<Throwable>();
        
        while ( throwable != null )
        {
            list.add( throwable );
            throwable = ExceptionUtils.getCause( throwable );
        }
        
        return list.toArray( new Throwable[list.size()] );
    }


    // -----------------------------------------------------------------------
    /**
     * <p>
     * Returns the (zero based) index of the first <code>Throwable</code> that
     * matches the specified type in the exception chain.
     * </p>
     * <p/>
     * <p>
     * A <code>null</code> throwable returns <code>-1</code>. A
     * <code>null</code> type returns <code>-1</code>. No match in the
     * chain returns <code>-1</code>.
     * </p>
     * 
     * @param throwable
     *            the throwable to inspect, may be null
     * @param type
     *            the type to search for
     * @return the index into the throwable chain, -1 if no match or null input
     */
    public static int indexOfThrowable( Throwable throwable, Class type )
    {
        return indexOfThrowable( throwable, type, 0 );
    }


    /**
     * <p>
     * Returns the (zero based) index of the first <code>Throwable</code> that
     * matches the specified type in the exception chain from a specified index.
     * </p>
     * <p/>
     * <p>
     * A <code>null</code> throwable returns <code>-1</code>. A
     * <code>null</code> type returns <code>-1</code>. No match in the
     * chain returns <code>-1</code>. A negative start index is treated as
     * zero. A start index greater than the number of throwables returns
     * <code>-1</code>.
     * </p>
     * 
     * @param throwable
     *            the throwable to inspect, may be null
     * @param type
     *            the type to search for
     * @param fromIndex
     *            the (zero based) index of the starting position, negative
     *            treated as zero, larger than chain size returns -1
     * @return the index into the throwable chain, -1 if no match or null input
     */
    public static int indexOfThrowable( Throwable throwable, Class type, int fromIndex )
    {
        if ( throwable == null )
        {
            return -1;
        }
        if ( fromIndex < 0 )
        {
            fromIndex = 0;
        }
        Throwable[] throwables = ExceptionUtils.getThrowables( throwable );
        if ( fromIndex >= throwables.length )
        {
            return -1;
        }
        for ( int i = fromIndex; i < throwables.length; i++ )
        {
            if ( throwables[i].getClass().equals( type ) )
            {
                return i;
            }
        }
        return -1;
    }


    // -----------------------------------------------------------------------
    /**
     * <p>
     * Prints a compact stack trace for the root cause of a throwable to
     * <code>System.err</code>.
     * </p>
     * <p/>
     * <p>
     * The compact stack trace starts with the root cause and prints stack
     * frames up to the place where it was caught and wrapped. Then it prints
     * the wrapped exception and continues with stack frames until the wrapper
     * exception is caught and wrapped again, etc.
     * </p>
     * <p/>
     * <p>
     * The method is equivalent to <code>printStackTrace</code> for throwables
     * that don't have nested causes.
     * </p>
     * 
     * @param throwable
     *            the throwable to output
     * @since 2.0
     */
    public static void printRootCauseStackTrace( Throwable throwable )
    {
        printRootCauseStackTrace( throwable, System.err );
    }


    /**
     * <p>
     * Prints a compact stack trace for the root cause of a throwable.
     * </p>
     * <p/>
     * <p>
     * The compact stack trace starts with the root cause and prints stack
     * frames up to the place where it was caught and wrapped. Then it prints
     * the wrapped exception and continues with stack frames until the wrapper
     * exception is caught and wrapped again, etc.
     * </p>
     * <p/>
     * <p>
     * The method is equivalent to <code>printStackTrace</code> for throwables
     * that don't have nested causes.
     * </p>
     * 
     * @param throwable
     *            the throwable to output, may be null
     * @param stream
     *            the stream to output to, may not be null
     * @throws IllegalArgumentException
     *             if the stream is <code>null</code>
     * @since 2.0
     */
    public static void printRootCauseStackTrace( Throwable throwable, PrintStream stream )
    {
        if ( throwable == null )
        {
            return;
        }
        if ( stream == null )
        {
            throw new IllegalArgumentException( "The PrintStream must not be null" );
        }
        String trace[] = getRootCauseStackTrace( throwable );
        for ( int i = 0; i < trace.length; i++ )
        {
            stream.println( trace[i] );
        }
        stream.flush();
    }


    /**
     * <p>
     * Prints a compact stack trace for the root cause of a throwable.
     * </p>
     * <p/>
     * <p>
     * The compact stack trace starts with the root cause and prints stack
     * frames up to the place where it was caught and wrapped. Then it prints
     * the wrapped exception and continues with stack frames until the wrapper
     * exception is caught and wrapped again, etc.
     * </p>
     * <p/>
     * <p>
     * The method is equivalent to <code>printStackTrace</code> for throwables
     * that don't have nested causes.
     * </p>
     * 
     * @param throwable
     *            the throwable to output, may be null
     * @param writer
     *            the writer to output to, may not be null
     * @throws IllegalArgumentException
     *             if the writer is <code>null</code>
     * @since 2.0
     */
    public static void printRootCauseStackTrace( Throwable throwable, PrintWriter writer )
    {
        if ( throwable == null )
        {
            return;
        }
        if ( writer == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04356 ) );
        }
        String trace[] = getRootCauseStackTrace( throwable );
        for ( int i = 0; i < trace.length; i++ )
        {
            writer.println( trace[i] );
        }
        writer.flush();
    }


    // -----------------------------------------------------------------------
    /**
     * <p>
     * Creates a compact stack trace for the root cause of the supplied
     * <code>Throwable</code>.
     * </p>
     * 
     * @param throwable
     *            the throwable to examine, may be null
     * @return an array of stack trace frames, never null
     * @since 2.0
     */
    public static String[] getRootCauseStackTrace( Throwable throwable )
    {
        if ( throwable == null )
        {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        
        Throwable throwables[] = getThrowables( throwable );
        int count = throwables.length;
        List<String> frames = new ArrayList<String>();
        List<String> nextTrace = getStackFrameList( throwables[count - 1] );
        
        for ( int i = count; --i >= 0; )
        {
            List<String> trace = nextTrace;
            
            if ( i != 0 )
            {
                nextTrace = getStackFrameList( throwables[i - 1] );
                removeCommonFrames( trace, nextTrace );
            }
            if ( i == count - 1 )
            {
                frames.add( throwables[i].toString() );
            }
            else
            {
                frames.add( WRAPPED_MARKER + throwables[i].toString() );
            }
            for ( int j = 0; j < trace.size(); j++ )
            {
                frames.add( trace.get( j ) );
            }
        }
        return frames.toArray( new String[0] );
    }


    /**
     * <p>
     * Removes common frames from the cause trace given the two stack traces.
     * </p>
     * 
     * @param causeFrames
     *            stack trace of a cause throwable
     * @param wrapperFrames
     *            stack trace of a wrapper throwable
     * @throws IllegalArgumentException
     *             if either argument is null
     * @since 2.0
     */
    public static void removeCommonFrames( List causeFrames, List wrapperFrames )
    {
        if ( causeFrames == null || wrapperFrames == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04357 ) );
        }
        int causeFrameIndex = causeFrames.size() - 1;
        int wrapperFrameIndex = wrapperFrames.size() - 1;
        while ( causeFrameIndex >= 0 && wrapperFrameIndex >= 0 )
        {
            // Remove the frame from the cause trace if it is the same
            // as in the wrapper trace
            String causeFrame = ( String ) causeFrames.get( causeFrameIndex );
            String wrapperFrame = ( String ) wrapperFrames.get( wrapperFrameIndex );
            if ( causeFrame.equals( wrapperFrame ) )
            {
                causeFrames.remove( causeFrameIndex );
            }
            causeFrameIndex--;
            wrapperFrameIndex--;
        }
    }


    // -----------------------------------------------------------------------
    /**
     * <p>
     * Gets the stack trace from a Throwable as a String.
     * </p>
     * 
     * @param throwable
     *            the <code>Throwable</code> to be examined
     * @return the stack trace as generated by the exception's
     *         <code>printStackTrace(PrintWriter)</code> method
     */
    public static String getStackTrace( Throwable throwable )
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter( sw, true );
        throwable.printStackTrace( pw );
        return sw.getBuffer().toString();
    }


    /**
     * <p>
     * A way to get the entire nested stack-trace of an throwable.
     * </p>
     * 
     * @param throwable
     *            the <code>Throwable</code> to be examined
     * @return the nested stack trace, with the root cause first
     * @since 2.0
     */
    public static String getFullStackTrace( Throwable throwable )
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter( sw, true );
        Throwable[] ts = getThrowables( throwable );
        for ( int i = 0; i < ts.length; i++ )
        {
            ts[i].printStackTrace( pw );
            if ( isNestedThrowable( ts[i] ) )
            {
                break;
            }
        }
        return sw.getBuffer().toString();
    }


    // -----------------------------------------------------------------------
    /**
     * <p>
     * Captures the stack trace associated with the specified
     * <code>Throwable</code> object, decomposing it into a list of stack
     * frames.
     * </p>
     * 
     * @param throwable
     *            the <code>Throwable</code> to examine, may be null
     * @return an array of strings describing each stack frame, never null
     */
    public static String[] getStackFrames( Throwable throwable )
    {
        if ( throwable == null )
        {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        return getStackFrames( getStackTrace( throwable ) );
    }


    /**
     * <p>
     * Functionality shared between the <code>getStackFrames(Throwable)</code>
     * methods of this and the
     */
    static String[] getStackFrames( String stackTrace )
    {
        String linebreak = SystemUtils.LINE_SEPARATOR;
        StringTokenizer frames = new StringTokenizer( stackTrace, linebreak );
        List<String> list = new LinkedList<String>();
        
        while ( frames.hasMoreTokens() )
        {
            list.add( frames.nextToken() );
        }
        
        return list.toArray( new String[list.size()] );
    }


    /**
     * <p>
     * Produces a <code>List</code> of stack frames - the message is not
     * included.
     * </p>
     * <p/>
     * <p>
     * This works in most cases - it will only fail if the exception message
     * contains a line that starts with:
     * <code>&quot;&nbsp;&nbsp;&nbsp;at&quot;.</code>
     * </p>
     * 
     * @param t
     *            is any throwable
     * @return List of stack frames
     */
    static List<String> getStackFrameList( Throwable t )
    {
        String stackTrace = getStackTrace( t );
        String linebreak = SystemUtils.LINE_SEPARATOR;
        StringTokenizer frames = new StringTokenizer( stackTrace, linebreak );
        List<String> list = new LinkedList<String>();
        boolean traceStarted = false;
        
        while ( frames.hasMoreTokens() )
        {
            String token = frames.nextToken();
            // Determine if the line starts with <whitespace>at
            int at = token.indexOf( "at" );
            
            if ( at != -1 && token.substring( 0, at ).trim().length() == 0 )
            {
                traceStarted = true;
                list.add( token );
            }
            else if ( traceStarted )
            {
                break;
            }
        }
        return list;
    }
    
    
    public static String printErrors( List<Throwable> errors )
    {
        StringBuilder sb = new StringBuilder();
        
        for ( Throwable error:errors )
        {
            sb.append( "Error : " ).append( error.getMessage() ).append( "\n" );
        }
        
        return sb.toString();
    }
}

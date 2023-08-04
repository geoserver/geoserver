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


/**
 * The base class of all exceptions which can contain other exceptions. It is
 * intended to ease the debugging by carrying on the information about the
 * exception which was caught and provoked throwing the current exception.
 * Catching and rethrowing may occur multiple times, and provided that all
 * exceptions except the first one are descendants of
 * <code>NestedException</code>, when the exception is finally printed out
 * using any of the <code>
 * printStackTrace()</code> methods, the stack trace
 * will contain the information about all exceptions thrown and caught on the
 * way.
 * <p>
 * Running the following program
 * <p>
 * <blockquote>
 * 
 * <pre>
 *   1 import org.apache.commons.lang.exception.NestableException;
 *   2
 *   3 public class Test {
 *   4     public static void main( String[] args ) {
 *   5         try {
 *   6             a();
 *   7         } catch(Exception e) {
 *   8             e.printStackTrace();
 *   9         }
 *  10      }
 *  11
 *  12      public static void a() throws Exception {
 *  13          try {
 *  14              b();
 *  15          } catch(Exception e) {
 *  16              throw new NestableException(&quot;foo&quot;, e);
 *  17          }
 *  18      }
 *  19
 *  20      public static void b() throws Exception {
 *  21          try {
 *  22              c();
 *  23          } catch(Exception e) {
 *  24              throw new NestableException(&quot;bar&quot;, e);
 *  25          }
 *  26      }
 *  27
 *  28      public static void c() throws Exception {
 *  29          throw new Exception(&quot;baz&quot;);
 *  30      }
 *  31 }
 * </pre>
 * 
 * </blockquote>
 * <p>
 * Yields the following stack trace:
 * <p>
 * <blockquote>
 * 
 * <pre>
 *  org.apache.commons.lang.exception.NestableException: foo
 *          at Test.a(Test.java:16)
 *          at Test.main(Test.java:6)
 *  Caused by: org.apache.commons.lang.exception.NestableException: bar
 *          at Test.b(Test.java:24)
 *          at Test.a(Test.java:14)
 *          ... 1 more
 *  Caused by: java.lang.Exception: baz
 *          at Test.c(Test.java:29)
 *          at Test.b(Test.java:22)
 *          ... 2 more
 * </pre>
 * 
 * </blockquote><br>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NestableException extends Exception implements Nestable
{

    static final long serialVersionUID = 3485795588970325053L;

    /**
     * The helper instance which contains much of the code which we delegate to.
     */
    protected NestableDelegate delegate = new NestableDelegate( this );

    /**
     * Holds the reference to the exception or error that caused this exception
     * to be thrown.
     */
    private Throwable cause = null;


    /**
     * Constructs a new <code>NestableException</code> without specified
     * detail message.
     */
    public NestableException()
    {
        super();
    }


    /**
     * Constructs a new <code>NestableException</code> with specified detail
     * message.
     * 
     * @param msg
     *            The error message.
     */
    public NestableException(String msg)
    {
        super( msg );
    }


    /**
     * Constructs a new <code>NestableException</code> with specified nested
     * <code>Throwable</code>.
     * 
     * @param cause
     *            the exception or error that caused this exception to be thrown
     */
    public NestableException(Throwable cause)
    {
        super();
        this.cause = cause;
    }


    /**
     * Constructs a new <code>NestableException</code> with specified detail
     * message and nested <code>Throwable</code>.
     * 
     * @param msg
     *            the error message
     * @param cause
     *            the exception or error that caused this exception to be thrown
     */
    public NestableException(String msg, Throwable cause)
    {
        super( msg );
        this.cause = cause;
    }


    public Throwable getCause()
    {
        return cause;
    }


    /**
     * Returns the detail message string of this throwable. If it was created
     * with a null message, returns the following: (cause==null ? null :
     * cause.toString()).
     */
    public String getMessage()
    {
        if ( super.getMessage() != null )
        {
            return super.getMessage();
        }
        else if ( cause != null )
        {
            return cause.toString();
        }
        else
        {
            return null;
        }
    }


    public String getMessage( int index )
    {
        if ( index == 0 )
        {
            return super.getMessage();
        }
        else
        {
            return delegate.getMessage( index );
        }
    }


    public String[] getMessages()
    {
        return delegate.getMessages();
    }


    public Throwable getThrowable( int index )
    {
        return delegate.getThrowable( index );
    }


    public int getThrowableCount()
    {
        return delegate.getThrowableCount();
    }


    public Throwable[] getThrowables()
    {
        return delegate.getThrowables();
    }


    public int indexOfThrowable( Class type )
    {
        return delegate.indexOfThrowable( type, 0 );
    }


    public int indexOfThrowable( Class type, int fromIndex )
    {
        return delegate.indexOfThrowable( type, fromIndex );
    }


    public void printStackTrace()
    {
        delegate.printStackTrace();
    }


    public void printStackTrace( PrintStream out )
    {
        delegate.printStackTrace( out );
    }


    public void printStackTrace( PrintWriter out )
    {
        delegate.printStackTrace( out );
    }


    public final void printPartialStackTrace( PrintWriter out )
    {
        super.printStackTrace( out );
    }

}

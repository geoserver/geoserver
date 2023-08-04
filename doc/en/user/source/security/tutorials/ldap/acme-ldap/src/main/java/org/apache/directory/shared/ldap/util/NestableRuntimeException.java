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
 * The base class of all runtime exceptions which can contain other exceptions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NestableRuntimeException extends RuntimeException implements Nestable
{

    static final long serialVersionUID = -833907851887600575L;

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
     * Constructs a new <code>NestableRuntimeException</code> without
     * specified detail message.
     */
    public NestableRuntimeException()
    {
        super();
    }


    /**
     * Constructs a new <code>NestableRuntimeException</code> with specified
     * detail message.
     * 
     * @param msg
     *            the error message
     */
    public NestableRuntimeException(String msg)
    {
        super( msg );
    }


    /**
     * Constructs a new <code>NestableRuntimeException</code> with specified
     * nested <code>Throwable</code>.
     * 
     * @param cause
     *            the exception or error that caused this exception to be thrown
     */
    public NestableRuntimeException(Throwable cause)
    {
        super();
        this.cause = cause;
    }


    /**
     * Constructs a new <code>NestableRuntimeException</code> with specified
     * detail message and nested <code>Throwable</code>.
     * 
     * @param msg
     *            the error message
     * @param cause
     *            the exception or error that caused this exception to be thrown
     */
    public NestableRuntimeException(String msg, Throwable cause)
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

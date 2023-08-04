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
 * A dynamically growing byte[]. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ByteBuffer
{
    /** the default initial buffer size */
    private static final int DEFAULT_INITIAL_SIZE = 10;
    
    /** the initial size of the buffer in number of bytes: also increment for allocations */
    private final int initialSize;
    /** the position into the buffer */
    private int pos = 0;
    /** the bytes of the buffer */
    private byte[] buf;
    
    
    public ByteBuffer()
    {
        this ( DEFAULT_INITIAL_SIZE );
    }
    
    
    public ByteBuffer( int initialSize )
    {
        if ( initialSize <= 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04354 ) );
        }
        this.initialSize = initialSize;
        this.buf = new byte[initialSize];
    }
    
    
    public final void clear()
    {
        pos = 0;
    }
    
    
    public final int position()
    {
        return pos;
    }
    
    
    public final int capacity()
    {
        return buf.length;
    }
    
    
    public final byte get( int ii )
    {
        return buf[ii];
    }
    
    
    /**
     * Get's the bytes, the backing store for this buffer.  Note
     * that you need to use the position index to determine where
     * to stop reading from this buffer.
     */
    public final byte[] buffer()
    {
        return buf;
    }
    
    
    /**
     * Get's a copy of the bytes used.
     */
    public final byte[] copyOfUsedBytes()
    {
        byte[] copy = new byte[pos];
        System.arraycopy( buf, 0, copy, 0, pos );
        return copy;
    }
    
    
    /**
     * Appends the bytes to this buffer.
     */
    public final void append( byte[] bytes )
    {
        for ( byte b : bytes )
        {
            append( b );
        }
    }
    
    
    /**
     * Appends a byte to this buffer.
     */
    public final void append( byte bite )
    {
        if ( pos >= buf.length )
        {
            growBuffer();
        }
        
        buf[pos] = bite;
        pos++;
    }
    
    
    /**
     * Appends an int to this buffer.  WARNING: the int is truncated to 
     * a byte value.
     */
    public final void append( int val )
    {
        if ( pos >= buf.length )
        {
            growBuffer();
        }
        
        buf[pos] = ( byte ) val;
        pos++;
    }
    
    
    private final void growBuffer()
    {
        byte[] copy = new byte[buf.length+initialSize];
        System.arraycopy( buf, 0, copy, 0, pos );
        this.buf = copy;
    }
}

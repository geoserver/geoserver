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


import java.io.PipedInputStream;
import java.io.IOException;


/**
 * A piped input stream that fixes the "Read end Dead" issue when a single
 * thread is used.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public class ParserPipedInputStream extends PipedInputStream
{
    protected synchronized void receive( int b ) throws IOException
    {
        while ( in == out )
        {
            /* full: kick any waiting readers */
            notifyAll();
            try
            {
                wait( 1000 );
            }
            catch ( InterruptedException ex )
            {
                throw new java.io.InterruptedIOException();
            }
        }

        if ( in < 0 )
        {
            in = 0;
            out = 0;
        }

        buffer[in++] = ( byte ) ( b & 0xFF );

        if ( in >= buffer.length )
        {
            in = 0;
        }
    }
}

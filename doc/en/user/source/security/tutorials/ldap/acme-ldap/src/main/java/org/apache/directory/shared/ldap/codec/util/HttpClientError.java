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

package org.apache.directory.shared.ldap.codec.util;


/**
 * Signals that an error has occurred.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 664290 $ $Date: 2005-02-26 08:01:52 -0500 (Sat, 26 Feb
 *          2005) $
 * @since 3.0
 */
public class HttpClientError extends Error
{
    private static final long serialVersionUID = 1L;


    /**
     * Creates a new HttpClientError with a <tt>null</tt> detail message.
     */
    public HttpClientError()
    {
        super();
    }


    /**
     * Creates a new HttpClientError with the specified detail message.
     * 
     * @param message
     *            The error message
     */
    public HttpClientError(String message)
    {
        super( message );
    }

}

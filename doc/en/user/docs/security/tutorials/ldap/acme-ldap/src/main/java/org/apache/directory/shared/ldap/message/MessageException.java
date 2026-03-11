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

package org.apache.directory.shared.ldap.message;


import org.apache.directory.shared.ldap.RuntimeMultiException;


/**
 * This exception is thrown when a message processing error occurs.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 437007 $
 */
public class MessageException extends RuntimeMultiException
{
    static final long serialVersionUID = -155089078576745029L;


    /**
     * Constructs an Exception without a message.
     */
    public MessageException()
    {
        super();
    }


    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param a_message
     *            The message associated with the exception.
     */
    public MessageException(String a_message)
    {
        super( a_message );
    }
}

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

package org.apache.directory.shared.ldap.message.spi;


import org.apache.directory.shared.ldap.message.MessageException;


/**
 * This exception is thrown when provider specific errors occur.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 437007 $
 */
public class ProviderException extends MessageException
{
    static final long serialVersionUID = 8915445282948982052L;

    /** The provider this exception resulted on */
    private final Provider m_provider;


    /**
     * Gets the provider that caused this exception.
     * 
     * @return the offensive Provider.
     */
    public Provider getProvider()
    {
        return m_provider;
    }


    /**
     * Constructs an Exception without a message.
     * 
     * @param a_provider
     *            The offending Provider that caused the exception.
     */
    public ProviderException(final Provider a_provider)
    {
        super();
        m_provider = a_provider;
    }


    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param a_provider
     *            The offending Provider that caused the exception.
     * @param a_message
     *            The message associated with the exception.
     */
    public ProviderException(final Provider a_provider, String a_message)
    {
        super( a_message );
        m_provider = a_provider;
    }
}

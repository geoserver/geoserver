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
package org.apache.directory.shared.ldap.message.internal;

import org.apache.directory.shared.ldap.codec.MessageTypeEnum;


/**
 * Abstract base for a Lockable Response message.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev: 910150 $
 */
public abstract class InternalAbstractResponse extends InternalAbstractMessage implements InternalResponse
{
    // ------------------------------------------------------------------------
    // Response Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Allows subclasses based on the abstract type to create a response to a
     * request.
     * 
     * @param id
     *            the response eliciting this Request
     * @param type
     *            the message type of the response
     */
    protected InternalAbstractResponse(final int id, final MessageTypeEnum type)
    {
        super( id, type );
    }
}

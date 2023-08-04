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
 * The base request message class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 910150 $
 */
public class InternalAbstractRequest extends InternalAbstractMessage implements InternalRequest
{
    static final long serialVersionUID = -4511116249089399040L;

    /** Flag indicating whether or not this request returns a response. */
    private final boolean hasResponse;


    /**
     * Subclasses must provide these parameters via a super constructor call.
     * 
     * @param id
     *            the sequential message identifier
     * @param type
     *            the request type enum
     * @param hasResponse
     *            flag indicating if this request generates a response
     */
    protected InternalAbstractRequest(final int id, final MessageTypeEnum type, boolean hasResponse)
    {
        super( id, type );

        this.hasResponse = hasResponse;
    }


    /**
     * Indicator flag used to determine whether or not this type of request
     * produces a reply.
     * 
     * @return true if any reply is generated, false if no response is generated
     */
    public boolean hasResponse()
    {
        return hasResponse;
    }
    
    /**
     * @see Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int hash = 37;
        hash = hash*17 + (hasResponse ? 0 : 1 );
        hash = hash*17 + super.hashCode();
        
        return hash;
    }
}

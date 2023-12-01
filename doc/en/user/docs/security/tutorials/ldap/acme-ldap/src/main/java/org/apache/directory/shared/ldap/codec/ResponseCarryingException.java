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

package org.apache.directory.shared.ldap.codec;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.internal.InternalMessage;
import org.apache.directory.shared.ldap.message.internal.InternalResultResponse;
import org.apache.directory.shared.ldap.name.DN;


/**
 * Thrown when a Decoder has encountered a failure condition during a decode.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class ResponseCarryingException extends DecoderException
{
    /**
     * Declares the Serial Version Uid.
     * 
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;

    /** The response with the error cause */
    private transient InternalMessage response;
    
    /**
     * Creates a DecoderException
     * 
     * @param message A message with meaning to a human
     */
    public ResponseCarryingException(String message)
    {
        super( message );
    }


    /**
     * Creates a DecoderException
     * 
     * @param message A message with meaning to a human
     * @param cause The Exception which caused the error
     */
    public ResponseCarryingException(String message, InternalResultResponse response, ResultCodeEnum code, 
        DN matchedDn, Throwable cause)
    {
        super( message, cause );

        response.getLdapResult().setErrorMessage( message );
        response.getLdapResult().setResultCode( code );
        response.getLdapResult().setMatchedDn( matchedDn );
    
        this.response = response; 
    }
    
    /**
     * Set a response if we get an exception while parsing the message
     * @param response the constructed response
     */
    public void setResponse( InternalMessage response ) 
    {
        this.response = response;
    }
    
    /**
     * Get the constructed response
     * @return The constructed response
     */
    public InternalMessage getResponse()
    {
        return response;
    }

}

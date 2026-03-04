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
package org.apache.directory.shared.ldap.message.extended;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.message.ExtendedResponseImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.util.StringTools;
/**
 * 
 * The response sent back from the server after the Cancel extended operation is performed.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CancelResponse extends ExtendedResponseImpl
{
    /** The serial version UUID */
    private static final long serialVersionUID = 1L;

    /**
     * Create a new CancelResponse object
     * @param messageId The messageId
     * @param rcode the result code
     */
    public CancelResponse( int messageId, ResultCodeEnum rcode )
    {
        super( messageId, null );

        switch ( rcode )
        {
            case SUCCESS :
            case CANCELED:
            case CANNOT_CANCEL :
            case NO_SUCH_OPERATION :
            case TOO_LATE :
                break;
            
            default:
                throw new IllegalArgumentException( I18n.err( I18n.ERR_04166, ResultCodeEnum.SUCCESS,
                		ResultCodeEnum.OPERATIONS_ERROR, ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS ) );
        }
        
        super.getLdapResult().setMatchedDn( null );
        super.getLdapResult().setResultCode( rcode );
    }


    public CancelResponse( int messageId )
    {
        super( messageId, null );
        super.getLdapResult().setMatchedDn( null );
        super.getLdapResult().setResultCode( ResultCodeEnum.SUCCESS );
    }


    // ------------------------------------------------------------------------
    // ExtendedResponse Interface Method Implementations
    // ------------------------------------------------------------------------
    /**
     * Gets the response OID specific encoded response values. It's a null
     * value for a CancelResponse
     * 
     * @return the response specific encoded response values.
     */
    public byte[] getResponse()
    {
        return StringTools.EMPTY_BYTES;
    }


    /**
     * Gets the OID uniquely identifying this extended response (a.k.a. its
     * name). It's a null value for the Cancel response
     * 
     * @return the OID of the extended response type.
     */
    public String getResponseName()
    {
        return "";
    }


    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( obj instanceof CancelResponse )
        {
            return true;
        }

        return false;
    }
}

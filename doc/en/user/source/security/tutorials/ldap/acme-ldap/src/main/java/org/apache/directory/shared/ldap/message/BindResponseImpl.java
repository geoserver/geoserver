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


import java.util.Arrays;

import org.apache.directory.shared.ldap.message.internal.InternalAbstractResultResponse;
import org.apache.directory.shared.ldap.message.internal.InternalBindResponse;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * BindResponse implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev: 905344 $
 */
public class BindResponseImpl extends InternalAbstractResultResponse implements InternalBindResponse
{
    static final long serialVersionUID = -5146809476518669755L;

    /** optional property holding SASL authentication response parameters */
    private byte[] serverSaslCreds;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a Lockable AddResponse as a reply to an AddRequest.
     * 
     * @param id
     *            the session unique message id
     */
    public BindResponseImpl(final int id)
    {
        super( id, TYPE );
    }


    // ------------------------------------------------------------------------
    // BindResponse Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the optional property holding SASL authentication response paramters
     * that are SASL mechanism specific. Will return null if the authentication
     * is simple.
     * 
     * @return the sasl mech. specific credentials or null of auth. is simple
     */
    public byte[] getServerSaslCreds()
    {
        if ( serverSaslCreds == null )
        {
            return null;
        }

        final byte[] copy = new byte[ serverSaslCreds.length ];
        System.arraycopy( serverSaslCreds, 0, copy, 0, serverSaslCreds.length );
        return copy;
    }


    /**
     * Sets the optional property holding SASL authentication response paramters
     * that are SASL mechanism specific. Leave null if authentication mode is
     * simple.
     * 
     * @param serverSaslCreds
     *            the sasl auth. mech. specific credentials
     */
    public void setServerSaslCreds( byte[] serverSaslCreds )
    {
        if ( serverSaslCreds != null )
        {
            this.serverSaslCreds = new byte[ serverSaslCreds.length ];
            System.arraycopy( serverSaslCreds, 0, this.serverSaslCreds, 0, serverSaslCreds.length );
        } else {
            this.serverSaslCreds = null;
        }
    }


    /**
     * Checks to see if this BindResponse is equal to another BindResponse. The
     * implementation and lockable properties are not factored into the
     * evaluation of equality. Only the messageId, saslCredentials and the
     * LdapResults of this BindResponse PDU and the compared object are taken
     * into account if that object also implements the BindResponse interface.
     * 
     * @param obj
     *            the object to test for equality with this BindResponse
     * @return true if obj equals this BindResponse false otherwise
     */
    public boolean equals( Object obj )
    {
        // quickly return true if obj is this one
        if ( obj == this )
        {
            return true;
        }

        if ( ( obj == null ) || !( obj instanceof InternalBindResponse ) )
        {
            return false;
        }

        if ( !super.equals( obj ) )
        {
            return false;
        }

        InternalBindResponse response = ( InternalBindResponse ) obj;
        byte[] creds = response.getServerSaslCreds();
        
        if ( serverSaslCreds == null )
        {
            if ( creds != null )
            {
                return false;
            }
        }
        else if ( creds == null )
        {
            return false;
        }
        
        return Arrays.equals( serverSaslCreds, creds );
    }


    /**
     * Get a String representation of a BindResponse
     * 
     * @return A BindResponse String
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "    BindResponse\n" );
        sb.append( super.toString() );

        if ( serverSaslCreds != null )
        {
            sb.append( "        Server sasl credentials : '" ).append( StringTools.dumpBytes( serverSaslCreds ) ).append( "'\n" );
        }

        return sb.toString();
    }

}

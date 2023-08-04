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

import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;




/**
 * A generic LdapResponse Object. It will contain the LdapResult.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public abstract class LdapResponseCodec extends LdapMessageCodec
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The LdapResult element */
    private LdapResultCodec ldapResult;

    /** The response length */
    private int ldapResponseLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new LdapResponse object.
     */
    public LdapResponseCodec()
    {
        super();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the LdapResult
     * 
     * @return Returns the ldapResult.
     */
    public LdapResultCodec getLdapResult()
    {
        return ldapResult;
    }


    /**
     * Set the ldap result
     * 
     * @param ldapResult The ldapResult to set.
     */
    public void setLdapResult( LdapResultCodec ldapResult )
    {
        this.ldapResult = ldapResult;
    }


    /**
     * @return Returns the ldapResponseLength.
     */
    public int getLdapResponseLength()
    {
        return ldapResponseLength;
    }


    /**
     * Compute the LdapResponse length
     */
    public int computeLdapResultLength()
    {
        ldapResponseLength = 0;

        if ( ldapResult != null )
        {
            ldapResponseLength = ldapResult.computeLength();
        }

        return ldapResponseLength;
    }


    /**
     * Encode the AddResponse message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04023 ) );
        }

        // The ldapResult
        if ( ldapResult != null )
        {
            ldapResult.encode( buffer );
        }

        return buffer;
    }


    /**
     * Get a String representation of an Response
     * 
     * @return An Response String
     */
    public String toString()
    {
        return ( ldapResult != null ? ldapResult.toString() : "" );
    }
}

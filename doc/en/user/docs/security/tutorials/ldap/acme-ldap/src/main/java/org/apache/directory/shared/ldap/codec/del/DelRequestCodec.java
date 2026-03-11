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
package org.apache.directory.shared.ldap.codec.del;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.name.DN;


/**
 * A DelRequest Message. 
 * 
 * Its syntax is : 
 * 
 * DelRequest ::= [APPLICATION 10] LDAPDN
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 918756 $, $Date: 2010-03-04 01:05:29 +0200 (Thu, 04 Mar 2010) $, 
 */
public class DelRequestCodec extends LdapMessageCodec
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The entry to be deleted */
    private DN entry;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new DelRequest object.
     */
    public DelRequestCodec()
    {
        super();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the message type
     * 
     * @return Returns the type.
     */
    public MessageTypeEnum getMessageType()
    {
        return MessageTypeEnum.DEL_REQUEST;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "DEL_REQUEST";
    }


    /**
     * Get the entry to be deleted
     * 
     * @return Returns the entry.
     */
    public DN getEntry()
    {
        return entry;
    }


    /**
     * Set the entry to be deleted
     * 
     * @param entry The entry to set.
     */
    public void setEntry( DN entry )
    {
        this.entry = entry;
    }


    /**
     * Compute the DelRequest length 
     * 
     * DelRequest : 
     * 0x4A L1 entry 
     * 
     * L1 = Length(entry) 
     * Length(DelRequest) = Length(0x4A) + Length(L1) + L1
     */
    protected int computeLengthProtocolOp()
    {
        // The entry
        return 1 + TLV.getNbBytes( DN.getNbBytes( entry ) ) + DN.getNbBytes( entry );
    }


    /**
     * Encode the DelRequest message to a PDU. 
     * 
     * DelRequest : 
     * 0x4A LL entry
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The DelRequest Tag
            buffer.put( LdapConstants.DEL_REQUEST_TAG );

            // The entry
            buffer.put( TLV.getBytes( DN.getNbBytes( entry ) ) );
            buffer.put( DN.getBytes( entry ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Return a String representing a DelRequest
     * 
     * @return A DelRequest String
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "    Del request\n" );
        sb.append( "        Entry : '" ).append( entry ).append( '\'' );

        return toString( sb.toString() );
    }
}

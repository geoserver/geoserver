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
package org.apache.directory.shared.ldap.codec.abandon;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A AbandonRequest Message. 
 * 
 * Its syntax is : 
 * AbandonRequest ::= [APPLICATION 16] MessageID 
 * 
 * MessageID ::= INTEGER (0 .. maxInt) 
 * 
 * maxInt INTEGER ::= 2147483647 -- (2^^31 - 1) --
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class AbandonRequestCodec extends LdapMessageCodec
{
    /** The logger */
    private static Logger LOGGER = LoggerFactory.getLogger( AbandonRequestCodec.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOGGER.isDebugEnabled();

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The abandoned message ID */
    private int abandonedMessageId;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new AbandonRequest object.
     */
    public AbandonRequestCodec()
    {
        super();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the abandoned message ID
     * 
     * @return Returns the abandoned MessageId.
     */
    public int getAbandonedMessageId()
    {
        return abandonedMessageId;
    }


    /**
     * Get the message type
     * 
     * @return Returns the type.
     */
    public MessageTypeEnum getMessageType()
    {
        return MessageTypeEnum.ABANDON_REQUEST;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "ABANDON_REQUEST";
    }


    /**
     * Set the abandoned message ID
     * 
     * @param abandonedMessageId The abandoned messageID to set.
     */
    public void setAbandonedMessageId( int abandonedMessageId )
    {
        this.abandonedMessageId = abandonedMessageId;
    }

    
    /**
     * Compute the AbandonRequest length 
     * 
     * AbandonRequest : 
     * 0x50 0x0(1..4) abandoned MessageId 
     * 
     * Length(AbandonRequest) = Length(0x50) + 1 + Length(abandoned MessageId)
     */
    protected int computeLengthProtocolOp()
    {
        int length = 1 + 1 + Value.getNbBytes( abandonedMessageId );

        if ( IS_DEBUG )
        {
            LOGGER.debug( "Message length : {}", Integer.valueOf( length ) );
        }

        return length;
    }


    /**
     * Encode the Abandon protocolOp part
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The tag
            buffer.put( LdapConstants.ABANDON_REQUEST_TAG );

            // The length. It has to be evaluated depending on
            // the abandoned messageId value.
            buffer.put( ( byte ) Value.getNbBytes( abandonedMessageId ) );

            // The abandoned messageId
            buffer.put( Value.getBytes( abandonedMessageId ) );
        }
        catch ( BufferOverflowException boe )
        {
            String msg = I18n.err( I18n.ERR_04005 );
            LOGGER.error( msg );
            throw new EncoderException( msg );
        }
    }


    /**
     * Return a String representing an AbandonRequest
     * 
     * @return A String representing the AbandonRequest
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "    Abandon Request :\n" );
        sb.append( "        Message Id : " ).append( abandonedMessageId );

        return toString( sb.toString() );
    }
}

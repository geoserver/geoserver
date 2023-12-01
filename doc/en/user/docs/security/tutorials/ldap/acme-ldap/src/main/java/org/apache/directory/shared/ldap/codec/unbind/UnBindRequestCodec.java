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
package org.apache.directory.shared.ldap.codec.unbind;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A UnBindRequest ldapObject. 
 * 
 * Its syntax is : 
 * UnbindRequest ::= [APPLICATION 2] NULL 
 * 
 * This ldapObject is empty.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class UnBindRequestCodec extends LdapMessageCodec
{
    /** The logger */
    private static Logger LOGGER = LoggerFactory.getLogger( UnBindRequestCodec.class );

    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new BindRequest object.
     */
    public UnBindRequestCodec()
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
        return MessageTypeEnum.UNBIND_REQUEST;
    }


    /**
     * Compute the UnBindRequest length 
     * 
     * UnBindRequest : 
     * 0x42 00
     */
    protected int computeLengthProtocolOp()
    {
        return 2; // Always 2
    }


    /**
     * Encode the Unbind protocolOp part
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The tag
            buffer.put( LdapConstants.UNBIND_REQUEST_TAG );

            // The length is always null.
            buffer.put( ( byte ) 0 );
        }
        catch ( BufferOverflowException boe )
        {
            String msg = I18n.err( I18n.ERR_04005 );
            LOGGER.error( msg );
            throw new EncoderException( msg );
        }
    }


    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "UNBIND_REQUEST";
    }


    /**
     * Get a String representation of a UnBindRequest
     * 
     * @return A UnBindRequest String
     */
    public String toString()
    {
        return super.toString( "    UnBind Request" );
    }
}

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
package org.apache.directory.shared.ldap.codec.intermediate;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapResponseCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A IntermediateResponse Message. Its syntax is :
 *   IntermediateResponse ::= [APPLICATION 25] SEQUENCE {
 *              responseName     [0] LDAPOID OPTIONAL,
 *              responseValue    [1] OCTET STRING OPTIONAL }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664290 $, $Date: 2008-06-07 08:28:06 +0200 (Sat, 07 Jun 2008) $, 
 */
public class IntermediateResponseCodec extends LdapResponseCodec
{
    /** The logger */
    private static Logger LOGGER = LoggerFactory.getLogger( IntermediateResponseCodec.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOGGER.isDebugEnabled();

    /** The name */
    private OID responseName;

    /** The response */
    private byte[] responseValue;

    /** The extended response length */
    private int intermediateResponseLength;

    /** The OID length */
    private int responseNameLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new IntermediateResponse object.
     */
    public IntermediateResponseCodec()
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
        return MessageTypeEnum.INTERMEDIATE_RESPONSE;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "INTERMEDIATE_RESPONSE";
    }


    /**
     * Get the intermediate response name
     * 
     * @return Returns the name.
     */
    public String getResponseName()
    {
        return ( ( responseName == null ) ? "" : responseName.toString() );
    }


    /**
     * Set the intermediate response name
     * 
     * @param responseName The name to set.
     */
    public void setResponseName( OID responseName )
    {
        this.responseName = responseName;
    }


    /**
     * Get the intermediate response value
     * 
     * @return Returns the intermediate response value.
     */
    public byte[] getResponseValue()
    {
        return responseValue;
    }


    /**
     * Set the intermediate response value
     * 
     * @param responseValue The intermediate response value to set.
     */
    public void setResponseValue( byte[] responseValue )
    {
        this.responseValue = responseValue;
    }


    /**
     * Compute the intermediateResponse length
     * 
     * intermediateResponse :
     * 
     * 0x79 L1
     *  |
     * [+--> 0x80 L2 name
     * [+--> 0x81 L3 response]]
     * 
     * L1 = [ + Length(0x80) + Length(L2) + L2
     *      [ + Length(0x81) + Length(L3) + L3]]
     * 
     * Length(IntermediateResponse) = Length(0x79) + Length(L1) + L1
     * 
     * @return The IntermediateResponse length
     */
    protected int computeLengthProtocolOp()
    {
        intermediateResponseLength = 0;

        if ( responseName != null )
        {
            responseNameLength = responseName.toString().length();
            intermediateResponseLength += 1 + TLV.getNbBytes( responseNameLength ) + responseNameLength;
        }

        if ( responseValue != null )
        {
            intermediateResponseLength += 1 + TLV.getNbBytes( responseValue.length )
                    + responseValue.length;
        }

        int length = 1 + TLV.getNbBytes( intermediateResponseLength ) + intermediateResponseLength;

        if ( IS_DEBUG )
        {
            LOGGER.debug( "Intermediate response length : {}", Integer.valueOf( length ) );
        }

        return length;
    }


    /**
     * Encode the IntermediateResponse message to a PDU. 
     * IntermediateResponse :
     *   0x79 LL
     *     [0x80 LL response name]
     *     [0x81 LL responseValue]
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The IntermediateResponse Tag
            buffer.put( LdapConstants.INTERMEDIATE_RESPONSE_TAG );
            buffer.put( TLV.getBytes( intermediateResponseLength ) );

            // The responseName, if any
            if ( responseName != null )
            {
                buffer.put( ( byte ) LdapConstants.INTERMEDIATE_RESPONSE_NAME_TAG );
                buffer.put( TLV.getBytes( responseNameLength ) );

                if ( responseName.getOIDLength() != 0 )
                {
                    buffer.put( StringTools.getBytesUtf8( responseName.toString() ) );
                }
            }

            // The response, if any
            if ( responseValue != null )
            {
                buffer.put( ( byte ) LdapConstants.INTERMEDIATE_RESPONSE_VALUE_TAG );

                buffer.put( TLV.getBytes( responseValue.length ) );

                if ( responseValue.length != 0 )
                {
                    buffer.put( responseValue );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            String msg = I18n.err( I18n.ERR_04005 );
            LOGGER.error( msg );
            throw new EncoderException( msg );
        }
    }

    
    /**
     * Get a String representation of an IntermediateResponse
     * 
     * @return An IntermediateResponse String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Intermediate Response\n" );
        sb.append( super.toString() );

        if ( responseName != null )
        {
            sb.append( "        Response name :'" ).append( responseName ).append( "'\n" );
        }

        if ( responseValue != null )
        {
            sb.append( "        ResponseValue :'" );
            sb.append( StringTools.dumpBytes( responseValue ) );
            sb.append( "'\n" );
        }

        return sb.toString();
    }
}

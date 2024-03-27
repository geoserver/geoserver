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
package org.apache.directory.shared.ldap.codec.compare;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A CompareRequest Message. Its syntax is :
 * CompareRequest ::= [APPLICATION 14] SEQUENCE {
 *              entry           LDAPDN,
 *              ava             AttributeValueAssertion }
 * 
 * AttributeValueAssertion ::= SEQUENCE {
 *              attributeDesc   AttributeDescription,
 *              assertionValue  AssertionValue }
 * 
 * AttributeDescription ::= LDAPString
 * 
 * AssertionValue ::= OCTET STRING
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 918756 $, $Date: 2010-03-04 01:05:29 +0200 (Thu, 04 Mar 2010) $, 
 */
public class CompareRequestCodec extends LdapMessageCodec
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The entry to be compared */
    private DN entry;

    /** The attribute to be compared */
    private String attributeDesc;

    /** The value to be compared */
    private Object assertionValue;

    /** The compare request length */
    private int compareRequestLength;

    /** The attribute value assertion length */
    private int avaLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new CompareRequest object.
     */
    public CompareRequestCodec()
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
        return MessageTypeEnum.COMPARE_REQUEST;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "COMPARE_REQUEST";
    }


    /**
     * Get the entry to be compared
     * 
     * @return Returns the entry.
     */
    public DN getEntry()
    {
        return entry;
    }


    /**
     * Set the entry to be compared
     * 
     * @param entry The entry to set.
     */
    public void setEntry( DN entry )
    {
        this.entry = entry;
    }


    /**
     * Get the assertion value
     * 
     * @return Returns the assertionValue.
     */
    public Object getAssertionValue()
    {
        return assertionValue;
    }


    /**
     * Set the assertion value
     * 
     * @param assertionValue The assertionValue to set.
     */
    public void setAssertionValue( Object assertionValue )
    {
        this.assertionValue = assertionValue;
    }


    /**
     * Get the attribute description
     * 
     * @return Returns the attributeDesc.
     */
    public String getAttributeDesc()
    {
        return ( ( attributeDesc == null ) ? "" : attributeDesc );
    }


    /**
     * Set the attribute description
     * 
     * @param attributeDesc The attributeDesc to set.
     */
    public void setAttributeDesc( String attributeDesc )
    {
        this.attributeDesc = attributeDesc;
    }


    /**
     * Compute the CompareRequest length 
     * 
     * CompareRequest : 
     * 0x6E L1 
     *   | 
     *   +--> 0x04 L2 entry 
     *   +--> 0x30 L3 (ava) 
     *         | 
     *         +--> 0x04 L4 attributeDesc 
     *         +--> 0x04 L5 assertionValue 
     *         
     * L3 = Length(0x04) + Length(L4) + L4 + Length(0x04) +
     *      Length(L5) + L5 
     * Length(CompareRequest) = Length(0x6E) + Length(L1) + L1 +
     *      Length(0x04) + Length(L2) + L2 + Length(0x30) + Length(L3) + L3
     * 
     * @return The CompareRequest PDU's length
     */
    protected int computeLengthProtocolOp()
    {
        // The entry
        compareRequestLength = 1 + TLV.getNbBytes( DN.getNbBytes( entry ) ) + DN.getNbBytes( entry );

        // The attribute value assertion
        int attributeDescLength = StringTools.getBytesUtf8( attributeDesc ).length;
        avaLength = 1 + TLV.getNbBytes( attributeDescLength ) + attributeDescLength;

        if ( assertionValue instanceof String )
        {
            int assertionValueLength = StringTools.getBytesUtf8( ( String ) assertionValue ).length;
            avaLength += 1 + TLV.getNbBytes( assertionValueLength ) + assertionValueLength;
        }
        else
        {
            avaLength += 1 + TLV.getNbBytes( ( ( byte[] ) assertionValue ).length )
                + ( ( byte[] ) assertionValue ).length;
        }

        compareRequestLength += 1 + TLV.getNbBytes( avaLength ) + avaLength;

        return 1 + TLV.getNbBytes( compareRequestLength ) + compareRequestLength;
    }


    /**
     * Encode the CompareRequest message to a PDU. 
     * 
     * CompareRequest : 
     *   0x6E LL 
     *     0x04 LL entry 
     *     0x30 LL attributeValueAssertion 
     *       0x04 LL attributeDesc 
     *       0x04 LL assertionValue
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The CompareRequest Tag
            buffer.put( LdapConstants.COMPARE_REQUEST_TAG );
            buffer.put( TLV.getBytes( compareRequestLength ) );

            // The entry
            Value.encode( buffer, DN.getBytes( entry ) );

            // The attributeValueAssertion sequence Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( avaLength ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        // The attributeDesc
        Value.encode( buffer, attributeDesc );

        // The assertionValue
        if ( assertionValue instanceof String )
        {
            Value.encode( buffer, ( String ) assertionValue );
        }
        else
        {
            Value.encode( buffer, ( byte[] ) assertionValue );
        }
    }


    /**
     * Get a String representation of a Compare Request
     * 
     * @return A Compare Request String
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "    Compare request\n" );
        sb.append( "        Entry : '" ).append( entry ).append( "'\n" );
        sb.append( "        Attribute description : '" ).append( attributeDesc ).append( "'\n" );
        sb.append( "        Attribute value : '" ).append( StringTools.dumpObject( assertionValue ) ).append( '\'' );

        return toString( sb.toString() );
    }
}

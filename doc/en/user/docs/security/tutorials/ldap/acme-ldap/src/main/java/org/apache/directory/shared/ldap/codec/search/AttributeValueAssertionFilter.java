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
package org.apache.directory.shared.ldap.codec.search;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.AttributeValueAssertion;
import org.apache.directory.shared.ldap.codec.LdapConstants;


/**
 * Object to store the filter. A filter is seen as a tree with a root.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class AttributeValueAssertionFilter extends Filter
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The assertion. */
    private AttributeValueAssertion assertion;

    /** The filter type */
    private int filterType;

    /** The attributeValueAssertion length */
    private int avaLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * The constructor.
     * 
     * @param filterType The filter type
     */
    public AttributeValueAssertionFilter( int tlvId, int filterType )
    {
        super( tlvId );
        this.filterType = filterType;
    }


    /**
     * The constructor.
     * 
     * @param filterType The filter type
     */
    public AttributeValueAssertionFilter( int filterType )
    {
        super();
        this.filterType = filterType;
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the assertion
     * 
     * @return Returns the assertion.
     */
    public AttributeValueAssertion getAssertion()
    {
        return assertion;
    }


    /**
     * Set the assertion
     * 
     * @param assertion The assertion to set.
     */
    public void setAssertion( AttributeValueAssertion assertion )
    {
        this.assertion = assertion;
    }


    /**
     * Get the filter type
     * 
     * @return Returns the filterType.
     */
    public int getFilterType()
    {
        return filterType;
    }


    /**
     * Set the filter type
     * 
     * @param filterType The filterType to set.
     */
    public void setFilterType( int filterType )
    {
        this.filterType = filterType;
    }


    /**
     * Compute the AttributeValueFilter length
     * 
     * AttributeValueFilter :
     * 
     * 0xA(3, 5, 6, 8) L1
     *  |
     *  +--> 0x04 L2 attributeDesc
     *  +--> 0x04 L3 assertionValue
     *  
     * 
     * L2 = Length(attributeDesc)
     * L3 = Length(assertionValue)
     * L1 = 1 + Length(L2) + L2 
     *      + 1 + Length(L3) + L3
     * 
     * Length(AttributeValueFilter) = Length(0xA?) + Length(L1)
     *                                + 1 + Length(L2) + L2 
     *                                + 1 + Length(L3) + L3 
     */
    public int computeLength()
    {
        avaLength = 0;
        int attributeDescLength = assertion.getAttributeDesc().length();

        avaLength = 1 + TLV.getNbBytes( attributeDescLength ) + attributeDescLength;

        org.apache.directory.shared.ldap.entry.Value<?> assertionValue = assertion.getAssertionValue();

        int assertionValueLength = 0;

        assertionValueLength = assertionValue.getBytes().length;

        avaLength += 1 + TLV.getNbBytes( assertionValueLength ) + assertionValueLength;

        return 1 + TLV.getNbBytes( avaLength ) + avaLength;
    }


    /**
     * Encode the AttributeValueAssertion Filters to a PDU. The 
     * following filters are to be encoded :
     *  - equality match 
     *  - greater or equal
     *  - less or equal
     *  - approx match 
     * 
     * AttributeValueAssertion filters :
     * 
     * 0xA[3, 5, 6, 8] LL 
     * 0x04 LL attributeDesc
     * 0x04 LL assertionValue
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

        try
        {
            // The AttributeValueAssertion Tag
            switch ( filterType )
            {
                case LdapConstants.EQUALITY_MATCH_FILTER:
                    buffer.put( ( byte ) LdapConstants.EQUALITY_MATCH_FILTER_TAG );
                    break;

                case LdapConstants.LESS_OR_EQUAL_FILTER:
                    buffer.put( ( byte ) LdapConstants.LESS_OR_EQUAL_FILTER_TAG );
                    break;

                case LdapConstants.GREATER_OR_EQUAL_FILTER:
                    buffer.put( ( byte ) LdapConstants.GREATER_OR_EQUAL_FILTER_TAG );
                    break;

                case LdapConstants.APPROX_MATCH_FILTER:
                    buffer.put( ( byte ) LdapConstants.APPROX_MATCH_FILTER_TAG );
                    break;
            }

            buffer.put( TLV.getBytes( avaLength ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        // The attribute desc
        Value.encode( buffer, assertion.getAttributeDesc() );

        // The assertion desc
        if ( assertion.getAssertionValue().isBinary() )
        {
            Value.encode( buffer, assertion.getAssertionValue().getString() );
        }
        else
        {
            Value.encode( buffer, assertion.getAssertionValue().getBytes() );
        }

        return buffer;
    }


    /**
     * Return a string compliant with RFC 2254 representing an item filter
     * 
     * @return The item filter string
     */
    public String toString()
    {
        return assertion != null ? assertion.toStringRFC2254( filterType ) : "";
    }
}

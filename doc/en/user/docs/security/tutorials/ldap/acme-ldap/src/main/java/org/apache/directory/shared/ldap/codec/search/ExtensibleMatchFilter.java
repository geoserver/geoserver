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
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * The search request filter Matching Rule assertion
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class ExtensibleMatchFilter extends Filter
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The expected lenth of the Matching Rule Assertion */
    private int expectedMatchingRuleLength;

    /** Matching rule */
    private String matchingRule;
    
    /** Matching rule bytes */
    private byte[] matchingRuleBytes;

    /** Matching rule type */
    private String type;
    
    private byte[] typeBytes;

    /** Matching rule value */
    private org.apache.directory.shared.ldap.entry.Value<?> matchValue;

    /** The dnAttributes flag */
    private boolean dnAttributes = false;

    /** The extensible match length */
    private int extensibleMatchLength;

    // ~ Constructors
    // -------------------------------------------------------------------------------
    /**
     * Creates a new ExtensibleMatchFilter object. The dnAttributes flag
     * defaults to false.
     */
    public ExtensibleMatchFilter( int tlvId )
    {
        super( tlvId );
    }
    
    
    /**
     * Creates a new ExtensibleMatchFilter object. The dnAttributes flag
     * defaults to false.
     */
    public ExtensibleMatchFilter()
    {
        super();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the dnAttributes flag
     * 
     * @return Returns the dnAttributes.
     */
    public boolean isDnAttributes()
    {
        return dnAttributes;
    }


    /**
     * Set the dnAttributes flag
     * 
     * @param dnAttributes The dnAttributes to set.
     */
    public void setDnAttributes( boolean dnAttributes )
    {
        this.dnAttributes = dnAttributes;
    }


    /**
     * Get the matchingRule
     * 
     * @return Returns the matchingRule.
     */
    public String getMatchingRule()
    {
        return matchingRule;
    }


    /**
     * Set the matchingRule
     * 
     * @param matchingRule The matchingRule to set.
     */
    public void setMatchingRule( String matchingRule )
    {
        this.matchingRule = matchingRule;
    }


    /**
     * Get the matchValue
     * 
     * @return Returns the matchValue.
     */
    public org.apache.directory.shared.ldap.entry.Value<?> getMatchValue()
    {
        return matchValue;
    }


    /**
     * Set the matchValue
     * 
     * @param matchValue The matchValue to set.
     */
    public void setMatchValue( org.apache.directory.shared.ldap.entry.Value<?> matchValue )
    {
        this.matchValue = matchValue;
    }


    /**
     * Get the type
     * 
     * @return Returns the type.
     */
    public String getType()
    {
        return type;
    }


    /**
     * Set the type
     * 
     * @param type The type to set.
     */
    public void setType( String type )
    {
        this.type = type;
    }


    /**
     * get the expectedMatchingRuleLength
     * 
     * @return Returns the expectedMatchingRuleLength.
     */
    public int getExpectedMatchingRuleLength()
    {
        return expectedMatchingRuleLength;
    }


    /**
     * Set the expectedMatchingRuleLength
     * 
     * @param expectedMatchingRuleLength The expectedMatchingRuleLength to set.
     */
    public void setExpectedMatchingRuleLength( int expectedMatchingRuleLength )
    {
        this.expectedMatchingRuleLength = expectedMatchingRuleLength;
    }


    /**
     * Compute the ExtensibleMatchFilter length 
     * ExtensibleMatchFilter : 
     * 0xA9 L1 
     *   |
     *  [+--> 0x81 L3 matchingRule] 
     *  [+--> 0x82 L4 type] 
     *  [+--> 0x83 L5 matchValue]
     *  [+--> 0x01 0x01 dnAttributes]
     */
    public int computeLength()
    {
        if ( matchingRule != null )
        {
            matchingRuleBytes = StringTools.getBytesUtf8( matchingRule );
            extensibleMatchLength = 1 + TLV.getNbBytes( matchingRuleBytes.length ) + matchingRuleBytes.length;
        }

        if ( type != null )
        {
            typeBytes = StringTools.getBytesUtf8( type );
            extensibleMatchLength += 1 + TLV.getNbBytes( typeBytes.length ) + typeBytes.length;
        }

        if ( matchValue != null )
        {
            int bytesLength = matchValue.getBytes().length;
            extensibleMatchLength += 1 + TLV.getNbBytes( bytesLength ) + bytesLength;
        }

        if ( dnAttributes )
        {
            extensibleMatchLength += 1 + 1 + 1;
        }

        return 1 + TLV.getNbBytes( extensibleMatchLength ) + extensibleMatchLength;
    }


    /**
     * Encode the ExtensibleMatch Filters to a PDU. 
     * 
     * ExtensibleMatch filter :
     * 
     * 0xA9 LL 
     *  |     0x81 LL matchingRule
     *  |    / |   0x82 LL Type  
     *  |   /  |  /0x83 LL matchValue
     *  +--+   +-+
     *  |   \     \
     *  |    \     0x83 LL MatchValue
     *  |     0x82 LL type
     *  |     0x83 LL matchValue
     *  +--[0x84 0x01 dnAttributes]
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
            // The ExtensibleMatch Tag
            buffer.put( ( byte ) LdapConstants.EXTENSIBLE_MATCH_FILTER_TAG );
            buffer.put( TLV.getBytes( extensibleMatchLength ) );

            if ( ( matchingRule == null ) && ( type == null ) )
            {
                throw new EncoderException( I18n.err( I18n.ERR_04056 ) );
            }

            // The matching rule
            if ( matchingRule != null )
            {
                buffer.put( ( byte ) LdapConstants.MATCHING_RULE_ID_TAG );
                buffer.put( TLV.getBytes( matchingRuleBytes.length ) );
                buffer.put( matchingRuleBytes );
            }

            // The type
            if ( type != null )
            {
                buffer.put( ( byte ) LdapConstants.MATCHING_RULE_TYPE_TAG );
                buffer.put( TLV.getBytes( typeBytes.length ) );
                buffer.put( typeBytes );
            }

            // The match value
            if ( matchValue != null )
            {
                buffer.put( ( byte ) LdapConstants.MATCH_VALUE_TAG );

                byte[] bytes = matchValue.getBytes();
                int bytesLength = bytes.length;
                buffer.put( TLV.getBytes( bytesLength ) );

                if ( bytesLength != 0 )
                {
                    buffer.put( bytes );
                }

            }

            // The dnAttributes flag, if true only
            if ( dnAttributes )
            {
                buffer.put( ( byte ) LdapConstants.DN_ATTRIBUTES_FILTER_TAG );
                buffer.put( ( byte ) 1 );
                buffer.put( Value.TRUE_VALUE );
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        return buffer;
    }


    /**
     * Return a String representing an extended filter as of RFC 2254
     * 
     * @return An Extened Filter String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        if ( type != null )
        {
            sb.append( type );
        }

        if ( dnAttributes )
        {
            sb.append( ":dn" );
        }

        if ( matchingRule == null )
        {

            if ( type == null )
            {
                return "Extended Filter wrong syntax";
            }
        }
        else
        {
            sb.append( ':' ).append( matchingRule );
        }

        sb.append( ":=" ).append( matchValue );

        return sb.toString();
    }
}

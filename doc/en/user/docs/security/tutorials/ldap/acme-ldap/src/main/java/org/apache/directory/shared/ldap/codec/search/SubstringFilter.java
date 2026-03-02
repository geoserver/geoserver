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
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A Object that stores the substring filter. 
 * 
 * A substring filter follow this
 * grammar : 
 * 
 * substring = attr "=" ( ([initial] any [final] | 
 *                        (initial [any] [final) | 
 *                        ([initial] [any] final) ) 
 *                       
 * initial = value 
 * any = "*" *(value "*")
 * final = value
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class SubstringFilter extends Filter
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The substring filter type (an attributeDescription) */
    private String type;
    
    /** The type length */
    private int typeLength;

    /**
     * This member is used to control the length of the three parts of the
     * substring filter
     */
    private int substringsLength;

    /** The initial filter */
    private String initialSubstrings;

    /** The any filter. It's a list of LdapString */
    private List<String> anySubstrings = new ArrayList<String>( 1 );

    /** The final filter */
    private String finalSubstrings;

    /** Temporary storage for substringsFilter length */
    private int substringsFilterLength;

    /** Temporary storage for substringsFilter sequence length */
    private int substringsFilterSequenceLength;


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * The constructor. We will create the 'any' subsring arraylist with only
     * one element.
     */
    public SubstringFilter( int tlvId )
    {
        super( tlvId );
    }
    
    
    /**
     * The constructor. We will create the 'any' subsring arraylist with only
     * one element.
     */
    public SubstringFilter()
    {
        super();
    }


    /**
     * Get the internal substrings
     * 
     * @return Returns the anySubstrings.
     */
    public List<String> getAnySubstrings()
    {
        return anySubstrings;
    }


    /**
     * Add a internal substring
     * 
     * @param any The anySubstrings to set.
     */
    public void addAnySubstrings( String any )
    {
        this.anySubstrings.add( any );
    }


    /**
     * Get the final substring
     * 
     * @return Returns the finalSubstrings.
     */
    public String getFinalSubstrings()
    {
        return finalSubstrings;
    }


    /**
     * Set the final substring
     * 
     * @param finalSubstrings The finalSubstrings to set.
     */
    public void setFinalSubstrings( String finalSubstrings )
    {
        this.finalSubstrings = finalSubstrings;
    }


    /**
     * Get the initial substring
     * 
     * @return Returns the initialSubstrings.
     */
    public String getInitialSubstrings()
    {
        return initialSubstrings;
    }


    /**
     * Set the initial substring
     * 
     * @param initialSubstrings The initialSubstrings to set.
     */
    public void setInitialSubstrings( String initialSubstrings )
    {
        this.initialSubstrings = initialSubstrings;
    }


    /**
     * Get the attribute
     * 
     * @return Returns the type.
     */
    public String getType()
    {
        return type;
    }


    /**
     * Set the attribute to match
     * 
     * @param type The type to set.
     */
    public void setType( String type )
    {
        this.type = type;
    }


    /**
     * @return Returns the substringsLength.
     */
    public int getSubstringsLength()
    {
        return substringsLength;
    }


    /**
     * @param substringsLength The substringsLength to set.
     */
    public void setSubstringsLength( int substringsLength )
    {
        this.substringsLength = substringsLength;
    }


    /**
     * Compute the SubstringFilter length 
     * 
     * SubstringFilter : 
     * 0xA4 L1 
     *   | 
     *   +--> 0x04 L2 type 
     *   +--> 0x30 L3 
     *          | 
     *         [+--> 0x80 L4 initial] 
     *         [+--> 0x81 L5-1 any] 
     *         [+--> 0x81 L5-2 any] 
     *         [+--> ... 
     *         [+--> 0x81 L5-i any] 
     *         [+--> ... 
     *         [+--> 0x81 L5-n any] 
     *         [+--> 0x82 L6 final]
     */
    public int computeLength()
    {
        // The type
        typeLength = StringTools.getBytesUtf8( type ).length;
        
        substringsFilterLength = 1 + TLV.getNbBytes( typeLength ) + typeLength;
        substringsFilterSequenceLength = 0;

        if ( initialSubstrings != null )
        {
            int initialLength = StringTools.getBytesUtf8( initialSubstrings ).length; 
            substringsFilterSequenceLength += 1 + TLV.getNbBytes( initialLength )
                + initialLength;
        }

        if ( anySubstrings != null )
        {
            for ( String any:anySubstrings )
            {
                int anyLength = StringTools.getBytesUtf8( any ).length; 
                substringsFilterSequenceLength += 1 + TLV.getNbBytes( anyLength ) + anyLength;
            }
        }

        if ( finalSubstrings != null )
        {
            int finalLength = StringTools.getBytesUtf8( finalSubstrings ).length; 
            substringsFilterSequenceLength += 1 + TLV.getNbBytes( finalLength )
                + finalLength;
        }

        substringsFilterLength += 1 + TLV.getNbBytes( substringsFilterSequenceLength )
            + substringsFilterSequenceLength;

        return 1 + TLV.getNbBytes( substringsFilterLength ) + substringsFilterLength;
    }


    /**
     * Encode the Substrings Filter to a PDU. 
     * 
     * Substrings Filter :
     * 
     * 0xA4 LL 
     * 0x30 LL substringsFilter
     *   0x04 LL type
     *   0x30 LL substrings sequence
     *    |  0x80 LL initial
     *    | /  [0x81 LL any]* 
     *    |/   [0x82 LL final]
     *    +--[0x81 LL any]+
     *     \   [0x82 LL final]
     *      \
     *       0x82 LL final
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
            // The SubstringFilter Tag
            buffer.put( ( byte ) LdapConstants.SUBSTRINGS_FILTER_TAG );
            buffer.put( TLV.getBytes( substringsFilterLength ) );

            // The type
            Value.encode( buffer, type.getBytes() );

            // The SubstringSequenceFilter Tag
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( substringsFilterSequenceLength ) );

            if ( ( initialSubstrings == null ) && ( ( anySubstrings == null ) || ( anySubstrings.size() == 0 ) )
                && ( finalSubstrings == null ) )
            {
                throw new EncoderException( I18n.err( I18n.ERR_04058 ) );
            }

            // The initial substring
            if ( initialSubstrings != null )
            {
                byte[] initialBytes = StringTools.getBytesUtf8( initialSubstrings );
                buffer.put( ( byte ) LdapConstants.SUBSTRINGS_FILTER_INITIAL_TAG );
                buffer.put( TLV.getBytes( initialBytes.length ) );
                buffer.put( initialBytes );
            }

            // The any substrings
            if ( anySubstrings != null )
            {
                for ( String any:anySubstrings )
                {
                    byte[] anyBytes = StringTools.getBytesUtf8( any );
                    buffer.put( ( byte ) LdapConstants.SUBSTRINGS_FILTER_ANY_TAG );
                    buffer.put( TLV.getBytes( anyBytes.length ) );
                    buffer.put( anyBytes );
                }
            }

            // The final substring
            if ( finalSubstrings != null )
            {
                byte[] finalBytes = StringTools.getBytesUtf8( finalSubstrings );
                buffer.put( ( byte ) LdapConstants.SUBSTRINGS_FILTER_FINAL_TAG );
                buffer.put( TLV.getBytes( finalBytes.length ) );
                buffer.put( finalBytes );
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        return buffer;
    }


    /**
     * Return a string compliant with RFC 2254 representing a Substring filter
     * 
     * @return The substring filter string
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        if ( initialSubstrings != null )
        {
            sb.append( initialSubstrings );
        }

        sb.append( '*' );

        if ( anySubstrings != null )
        {
            for ( String any:anySubstrings )
            {
                sb.append( any ).append( '*' );
            }
        }

        if ( finalSubstrings != null )
        {
            sb.append( finalSubstrings );
        }

        return sb.toString();
    }
}

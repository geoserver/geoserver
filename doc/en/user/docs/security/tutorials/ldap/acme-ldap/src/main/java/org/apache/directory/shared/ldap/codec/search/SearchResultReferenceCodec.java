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
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.util.LdapURL;


/**
 * A SearchResultReference Message. Its syntax is : 
 * 
 * SearchResultReference ::= [APPLICATION 19] SEQUENCE OF LDAPURL
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class SearchResultReferenceCodec extends LdapMessageCodec
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The set of LdapURLs */
    private List<LdapURL> searchResultReferences = new ArrayList<LdapURL>();

    /** The search result reference length */
    private int searchResultReferenceLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new SearchResultEntry object.
     */
    public SearchResultReferenceCodec()
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
        return MessageTypeEnum.SEARCH_RESULT_REFERENCE;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "SEARCH_RESULT_REFERENCE";
    }


    /**
     * Add a new reference to the list.
     * 
     * @param searchResultReference The search result reference
     */
    public void addSearchResultReference( LdapURL searchResultReference )
    {
        searchResultReferences.add( searchResultReference );
    }


    /**
     * Get the list of references
     * 
     * @return An ArrayList of SearchResultReferences
     */
    public List<LdapURL> getSearchResultReferences()
    {
        return searchResultReferences;
    }


    /**
     * Compute the SearchResultReference length
     * 
     * SearchResultReference :
     * <pre>
     * 0x73 L1
     *  |
     *  +--> 0x04 L2 reference
     *  +--> 0x04 L3 reference
     *  +--> ...
     *  +--> 0x04 Li reference
     *  +--> ...
     *  +--> 0x04 Ln reference
     * 
     * L1 = n*Length(0x04) + sum(Length(Li)) + sum(Length(reference[i]))
     * 
     * Length(SearchResultReference) = Length(0x73 + Length(L1) + L1
     * </pre>
     */
    protected int computeLengthProtocolOp()
    {
        searchResultReferenceLength = 0;

        // We may have more than one reference.
        for ( LdapURL url:searchResultReferences )
        {
            int ldapUrlLength = url.getNbBytes();
            searchResultReferenceLength += 1 + TLV.getNbBytes( ldapUrlLength ) + ldapUrlLength;
        }

        return 1 + TLV.getNbBytes( searchResultReferenceLength ) + searchResultReferenceLength;
    }


    /**
     * Encode the SearchResultReference message to a PDU.
     * 
     * SearchResultReference :
     * <pre>
     * 0x73 LL
     *   0x04 LL reference
     *   [0x04 LL reference]*
     * </pre>
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The SearchResultReference Tag
            buffer.put( LdapConstants.SEARCH_RESULT_REFERENCE_TAG );
            buffer.put( TLV.getBytes( searchResultReferenceLength ) );

            // The references. We must at least have one reference
            for ( LdapURL reference:searchResultReferences )
            {
                // Encode the reference
                Value.encode( buffer, reference.getString() );
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Returns the Search Result Reference string
     * 
     * @return The Search Result Reference string
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Search Result Reference\n" );

        if ( ( searchResultReferences == null ) || ( searchResultReferences.size() == 0 ) )
        {
            sb.append( "        No Reference\n" );
        }
        else
        {
            sb.append( "        References\n" );

            for ( LdapURL url:searchResultReferences )
            {
                sb.append( "            '" ).append( url ).append(
                    "'\n" );
            }
        }

        return sb.toString();
    }
}

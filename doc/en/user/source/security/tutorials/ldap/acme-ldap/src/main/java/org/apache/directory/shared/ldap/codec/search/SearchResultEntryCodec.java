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
import java.util.LinkedList;
import java.util.List;

import org.apache.directory.shared.ldap.exception.LdapException;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.util.Asn1StringUtils;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A SearchResultEntry Message. Its syntax is :
 *   SearchResultEntry ::= [APPLICATION 4] SEQUENCE {
 *       objectName      LDAPDN,
 *       attributes      PartialAttributeList }
 * 
 *   PartialAttributeList ::= SEQUENCE OF SEQUENCE {
 *       type    AttributeDescription,
 *       vals    SET OF AttributeValue }
 * 
 *   AttributeDescription ::= LDAPString
 * 
 *   AttributeValue ::= OCTET STRING
 * 
 * It contains an entry, with all its attributes, and all the attributes
 * values. If a search request is submited, all the results are sent one
 * by one, followed by a searchResultDone message.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923524 $, $Date: 2010-03-16 02:31:36 +0200 (Tue, 16 Mar 2010) $, 
 */
public class SearchResultEntryCodec extends LdapMessageCodec
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** A temporary storage for the byte[] representing the objectName */
    private byte[] objectNameBytes;

    /** The entry */
    private Entry entry = new DefaultClientEntry();

    /** The current attribute being decoded */
    private EntryAttribute currentAttributeValue;

    /** The search result entry length */
    private int searchResultEntryLength;

    /** The partial attributes length */
    private int attributesLength;

    /** The list of all attributes length */
    private List<Integer> attributeLength;

    /** The list of all vals length */
    private List<Integer> valsLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new SearchResultEntry object.
     */
    public SearchResultEntryCodec()
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
        return MessageTypeEnum.SEARCH_RESULT_ENTRY;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "SEARCH_RESULT_ENTRY";
    }


    /**
     * Get the entry DN
     * 
     * @return Returns the objectName.
     */
    public DN getObjectName()
    {
        return entry.getDn();
    }


    /**
     * Set the entry DN.
     * 
     * @param objectName The objectName to set.
     */
    public void setObjectName( DN objectName )
    {
        entry.setDn( objectName );
    }


    /**
     * Get the entry.
     * 
     * @return Returns the entry
     */
    public Entry getEntry()
    {
        return entry;
    }


    /**
     * Sets the entry.
     *
     * @param entry
     *      the entry
     */
    public void setEntry( Entry entry )
    {
        this.entry = entry;
    }


    /**
     * Create a new attributeValue
     * 
     * @param type The attribute's name
     */
    public void addAttributeValues( String type )
    {
        currentAttributeValue = new DefaultClientAttribute( type );

        try
        {
            entry.put( currentAttributeValue );
        }
        catch ( LdapException ne )
        {
            // Too bad... But there is nothing we can do.
        }
    }


    /**
     * Add a new value to the current attribute
     * 
     * @param value
     */
    public void addAttributeValue( Object value )
    {
        if ( value instanceof String )
        {
            currentAttributeValue.add( ( String ) value );
        }
        else
        {
            currentAttributeValue.add( ( byte[] ) value );
        }
    }


    /**
     * Compute the SearchResultEntry length
     * 
     * SearchResultEntry :
     * <pre>
     * 0x64 L1
     *  |
     *  +--> 0x04 L2 objectName
     *  +--> 0x30 L3 (attributes)
     *        |
     *        +--> 0x30 L4-1 (partial attributes list)
     *        |     |
     *        |     +--> 0x04 L5-1 type
     *        |     +--> 0x31 L6-1 (values)
     *        |           |
     *        |           +--> 0x04 L7-1-1 value
     *        |           +--> ...
     *        |           +--> 0x04 L7-1-n value
     *        |
     *        +--> 0x30 L4-2 (partial attributes list)
     *        |     |
     *        |     +--> 0x04 L5-2 type
     *        |     +--> 0x31 L6-2 (values)
     *        |           |
     *        |           +--> 0x04 L7-2-1 value
     *        |           +--> ...
     *        |           +--> 0x04 L7-2-n value
     *        |
     *        +--> ...
     *        |
     *        +--> 0x30 L4-m (partial attributes list)
     *              |
     *              +--> 0x04 L5-m type
     *              +--> 0x31 L6-m (values)
     *                    |
     *                    +--> 0x04 L7-m-1 value
     *                    +--> ...
     *                    +--> 0x04 L7-m-n value
     * </pre>
     */
    protected int computeLengthProtocolOp()
    {
        objectNameBytes = StringTools.getBytesUtf8( entry.getDn().getName() );

        // The entry
        searchResultEntryLength = 1 + TLV.getNbBytes( objectNameBytes.length ) + objectNameBytes.length;

        // The attributes sequence
        attributesLength = 0;

        if ( ( entry != null ) && ( entry.size() != 0 ) )
        {
            attributeLength = new LinkedList<Integer>();
            valsLength = new LinkedList<Integer>();

            // Compute the attributes length
            for ( EntryAttribute attribute : entry )
            {
                int localAttributeLength = 0;
                int localValuesLength = 0;

                // Get the type length
                int idLength = attribute.getId().getBytes().length;
                localAttributeLength = 1 + TLV.getNbBytes( idLength ) + idLength;

                if ( attribute.size() != 0 )
                {
                    // The values
                    if ( attribute.size() > 0 )
                    {
                        localValuesLength = 0;

                        for ( org.apache.directory.shared.ldap.entry.Value<?> value : attribute )
                        {
                            byte[] binaryValue = value.getBytes();
                            localValuesLength += 1 + TLV.getNbBytes( binaryValue.length ) + binaryValue.length;
                        }

                        localAttributeLength += 1 + TLV.getNbBytes( localValuesLength ) + localValuesLength;
                    }
                    else
                    {
                        // We have to deal with the special wase where
                        // we don't have a value.
                        // It will be encoded as an empty OCTETSTRING,
                        // so it will be two byte slong (0x04 0x00)
                        localAttributeLength += 1 + 1;
                    }
                }
                else
                {
                    // We have no values. We will just have an empty SET OF :
                    // 0x31 0x00
                    localAttributeLength += 1 + 1;
                }

                // add the attribute length to the attributes length
                attributesLength += 1 + TLV.getNbBytes( localAttributeLength ) + localAttributeLength;

                attributeLength.add( localAttributeLength );
                valsLength.add( localValuesLength );
            }
        }

        searchResultEntryLength += 1 + TLV.getNbBytes( attributesLength ) + attributesLength;

        // Return the result.
        return 1 + TLV.getNbBytes( searchResultEntryLength ) + searchResultEntryLength;
    }


    /**
     * Encode the SearchResultEntry message to a PDU.
     * 
     * SearchResultEntry :
     * <pre>
     * 0x64 LL
     *   0x04 LL objectName
     *   0x30 LL attributes
     *     0x30 LL partialAttributeList
     *       0x04 LL type
     *       0x31 LL vals
     *         0x04 LL attributeValue
     *         ... 
     *         0x04 LL attributeValue
     *     ... 
     *     0x30 LL partialAttributeList
     *       0x04 LL type
     *       0x31 LL vals
     *         0x04 LL attributeValue
     *         ... 
     *         0x04 LL attributeValue 
     * </pre>
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The SearchResultEntry Tag
            buffer.put( LdapConstants.SEARCH_RESULT_ENTRY_TAG );
            buffer.put( TLV.getBytes( searchResultEntryLength ) );

            // The objectName
            Value.encode( buffer, objectNameBytes );

            // The attributes sequence
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( attributesLength ) );

            // The partial attribute list
            if ( ( entry != null ) && ( entry.size() != 0 ) )
            {
                int attributeNumber = 0;

                // Compute the attributes length
                for ( EntryAttribute attribute : entry )
                {
                    // The partial attribute list sequence
                    buffer.put( UniversalTag.SEQUENCE_TAG );
                    int localAttributeLength = attributeLength.get( attributeNumber );
                    buffer.put( TLV.getBytes( localAttributeLength ) );

                    // The attribute type
                    Value.encode( buffer, Asn1StringUtils.asciiStringToByte( attribute.getUpId() ) );

                    // The values
                    buffer.put( UniversalTag.SET_TAG );
                    int localValuesLength = valsLength.get( attributeNumber );
                    buffer.put( TLV.getBytes( localValuesLength ) );

                    if ( attribute.size() != 0 )
                    {
                        if ( attribute.size() > 0 )
                        {
                            for ( org.apache.directory.shared.ldap.entry.Value<?> value : attribute )
                            {
                                if ( !value.isBinary() )
                                {
                                    Value.encode( buffer, value.getString() );
                                }
                                else
                                {
                                    Value.encode( buffer, value.getBytes() );
                                }
                            }
                        }
                    }

                    // Go to the next attribute number;
                    attributeNumber++;
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Returns the Search Result Entry string
     * 
     * @return The Search Result Entry string
     */
    public String toString()
    {

        StringBuilder sb = new StringBuilder();

        sb.append( "    Search Result Entry\n" );
        sb.append( "        entry\n" );

        if ( ( entry == null ) || ( entry.size() == 0 ) )
        {
            sb.append( "            No entry\n" );
        }
        else
        {
            sb.append( entry );
        }

        return sb.toString();
    }


    /**
     * @return Returns the currentAttributeValue.
     */
    public String getCurrentAttributeValueType()
    {
        return currentAttributeValue.getId();
    }
}

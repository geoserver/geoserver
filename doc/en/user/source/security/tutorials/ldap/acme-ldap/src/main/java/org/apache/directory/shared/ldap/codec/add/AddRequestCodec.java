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
package org.apache.directory.shared.ldap.codec.add;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.apache.directory.shared.ldap.exception.LdapException;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An AddRequest Message. Its syntax is : 
 *   AddRequest ::= [APPLICATION 8] SEQUENCE {
 *              entry           LDAPDN,
 *              attributes      AttributeList }
 *
 *   AttributeList ::= SEQUENCE OF SEQUENCE {
 *              type    AttributeDescription,
 *              vals    SET OF AttributeValue }
 * 
 *   AttributeValue ::= OCTET STRING
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923524 $, $Date: 2010-03-16 02:31:36 +0200 (Tue, 16 Mar 2010) $, 
 */
public class AddRequestCodec extends LdapMessageCodec
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( AddRequestCodec.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The attributes list. */
    private Entry entry;

    /** The current attribute being decoded */
    private EntryAttribute currentAttribute;

    /** The add request length */
    private int addRequestLength;

    /** The attributes length */
    private int attributesLength;

    /** The list of all attributes length */
    private List<Integer> attributeLength;

    /** The list of all vals length */
    private List<Integer> valuesLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new AddRequest object.
     */
    public AddRequestCodec()
    {
        super();
        entry = new DefaultClientEntry();
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
        return MessageTypeEnum.ADD_REQUEST;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "ADD_REQUEST";
    }


    /**
     * Initialize the Entry.
     */
    public void initEntry()
    {
        entry = new DefaultClientEntry();
    }


    /**
     * Get the entry to be added
     * 
     * @return Returns the entry.
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
     * @param type The attribute's name (called 'type' in the grammar)
     */
    public void addAttributeType( String type ) throws LdapException
    {
        // do not create a new attribute if we have seen this attributeType before
        if ( entry.get( type ) != null )
        {
            currentAttribute = entry.get( type );
            return;
        }

        // fix this to use AttributeImpl(type.getString().toLowerCase())
        currentAttribute = new DefaultClientAttribute( type );
        entry.put( currentAttribute );
    }


    /**
     * Add a new value to the current attribute
     * 
     * @param value The value to add
     */
    public void addAttributeValue( String value )
    {
        currentAttribute.add( value );
    }


    /**
     * Add a new value to the current attribute
     * 
     * @param value The value to add
     */
    public void addAttributeValue( org.apache.directory.shared.ldap.entry.Value<?> value )
    {
        currentAttribute.add( value );
    }


    /**
     * Add a new value to the current attribute
     * 
     * @param value The value to add
     */
    public void addAttributeValue( byte[] value )
    {
        currentAttribute.add( value );
    }


    /**
     * Get the added DN
     * 
     * @return Returns the entry DN.
     */
    public DN getEntryDn()
    {
        return entry.getDn();
    }


    /**
     * Set the added DN.
     * 
     * @param entry The DN to set.
     */
    public void setEntryDn( DN entryDn )
    {
        entry.setDn( entryDn );
    }


    /**
     * Compute the AddRequest length
     * 
     * AddRequest :
     * 
     * 0x68 L1
     *  |
     *  +--> 0x04 L2 entry
     *  +--> 0x30 L3 (attributes)
     *        |
     *        +--> 0x30 L4-1 (attribute)
     *        |     |
     *        |     +--> 0x04 L5-1 type
     *        |     +--> 0x31 L6-1 (values)
     *        |           |
     *        |           +--> 0x04 L7-1-1 value
     *        |           +--> ...
     *        |           +--> 0x04 L7-1-n value
     *        |
     *        +--> 0x30 L4-2 (attribute)
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
     *        +--> 0x30 L4-m (attribute)
     *              |
     *              +--> 0x04 L5-m type
     *              +--> 0x31 L6-m (values)
     *                    |
     *                    +--> 0x04 L7-m-1 value
     *                    +--> ...
     *                    +--> 0x04 L7-m-n value
     */
    protected int computeLengthProtocolOp()
    {
        // The entry
        addRequestLength = 1 + TLV.getNbBytes( DN.getNbBytes( entry.getDn() ) ) + DN.getNbBytes( entry.getDn() );

        // The attributes sequence
        attributesLength = 0;

        if ( ( entry != null ) && ( entry.size() != 0 ) )
        {
            attributeLength = new LinkedList<Integer>();
            valuesLength = new LinkedList<Integer>();

            // Compute the attributes length
            for ( EntryAttribute attribute : entry )
            {
                int localAttributeLength = 0;
                int localValuesLength = 0;

                // Get the type length
                int idLength = attribute.getId().getBytes().length;
                localAttributeLength = 1 + TLV.getNbBytes( idLength ) + idLength;

                // The values
                if ( attribute.size() != 0 )
                {
                    localValuesLength = 0;

                    for ( org.apache.directory.shared.ldap.entry.Value<?> value : attribute )
                    {
                        int valueLength = value.getBytes().length;
                        localValuesLength += 1 + TLV.getNbBytes( valueLength ) + valueLength;
                    }

                    localAttributeLength += 1 + TLV.getNbBytes( localValuesLength ) + localValuesLength;
                }

                // add the attribute length to the attributes length
                attributesLength += 1 + TLV.getNbBytes( localAttributeLength ) + localAttributeLength;

                attributeLength.add( localAttributeLength );
                valuesLength.add( localValuesLength );
            }
        }

        addRequestLength += 1 + TLV.getNbBytes( attributesLength ) + attributesLength;

        // Return the result.
        int result = 1 + TLV.getNbBytes( addRequestLength ) + addRequestLength;

        if ( IS_DEBUG )
        {
            log.debug( "AddRequest PDU length = {}", Integer.valueOf( result ) );
        }

        return result;
    }


    /**
     * Encode the AddRequest message to a PDU. 
     * 
     * AddRequest :
     * 
     * 0x68 LL
     *   0x04 LL entry
     *   0x30 LL attributesList
     *     0x30 LL attributeList
     *       0x04 LL attributeDescription
     *       0x31 LL attributeValues
     *         0x04 LL attributeValue
     *         ... 
     *         0x04 LL attributeValue
     *     ... 
     *     0x30 LL attributeList
     *       0x04 LL attributeDescription
     *       0x31 LL attributeValue
     *         0x04 LL attributeValue
     *         ... 
     *         0x04 LL attributeValue 
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The AddRequest Tag
            buffer.put( LdapConstants.ADD_REQUEST_TAG );
            buffer.put( TLV.getBytes( addRequestLength ) );

            // The entry
            Value.encode( buffer, DN.getBytes( entry.getDn() ) );

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
                    // The attributes list sequence
                    buffer.put( UniversalTag.SEQUENCE_TAG );
                    int localAttributeLength = attributeLength.get( attributeNumber );
                    buffer.put( TLV.getBytes( localAttributeLength ) );

                    // The attribute type
                    Value.encode( buffer, attribute.getId() );

                    // The values
                    buffer.put( UniversalTag.SET_TAG );
                    int localValuesLength = valuesLength.get( attributeNumber );
                    buffer.put( TLV.getBytes( localValuesLength ) );

                    if ( attribute.size() != 0 )
                    {
                        for ( org.apache.directory.shared.ldap.entry.Value<?> value : attribute )
                        {
                            if ( value.isBinary() )
                            {
                                Value.encode( buffer, value.getBytes() );
                            }
                            else
                            {
                                Value.encode( buffer, value.getString() );
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
            throw new EncoderException( "The PDU buffer size is too small !" );
        }

        if ( IS_DEBUG )
        {
            log.debug( "AddRequest encoding : {}", StringTools.dumpBytes( buffer.array() ) );
            log.debug( "AddRequest initial value : {}", toString() );
        }
    }


    /**
     * @return Returns the currentAttribute type.
     */
    public String getCurrentAttributeType()
    {
        return currentAttribute.getId();
    }


    /**
     * Return a String representing an AddRequest
     * 
     * @return A String representing the AddRequest
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "    Add Request\n" );
        sb.append( "        Attributes\n" );

        if ( entry == null )
        {
            sb.append( "            No attributes" );
        }
        else
        {
            sb.append( entry );
        }

        return toString( sb.toString() );
    }
}

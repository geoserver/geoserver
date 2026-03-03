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
package org.apache.directory.shared.ldap.codec.modify;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.name.DN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A ModifyRequest Message. 
 * 
 * Its syntax is : 
 * 
 * ModifyRequest ::= [APPLICATION 6] SEQUENCE { 
 *     object LDAPDN, 
 *     modification SEQUENCE OF SEQUENCE { 
 *         operation ENUMERATED { 
 *             add (0), 
 *             delete (1), 
 *             replace (2) 
 *         }, 
 *         modification AttributeTypeAndValues 
 *     } 
 * } 
 * 
 * AttributeTypeAndValues ::= SEQUENCE {
 *     type AttributeDescription, 
 *     vals SET OF AttributeValue 
 * } 
 * 
 * AttributeValue ::= OCTET STRING
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 918756 $, $Date: 2010-03-04 01:05:29 +0200 (Thu, 04 Mar 2010) $, 
 */
public class ModifyRequestCodec extends LdapMessageCodec
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( ModifyRequestCodec.class );

    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The DN to be modified. */
    private DN object;

    /** The modifications list. This is an array of Modification. */
    private List<Modification> modifications;

    /** The current attribute being decoded */
    private EntryAttribute currentAttribute;

    /** A local storage for the operation */
    private ModificationOperation currentOperation;

    /** The modify request length */
    private int modifyRequestLength;

    /** The modifications length */
    private int modificationsLength;

    /** The modification sequence length */
    private List<Integer> modificationSequenceLength;

    /** The list of all modification length */
    private List<Integer> modificationLength;

    /** The list of all vals length */
    private List<Integer> valuesLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new ModifyRequest object.
     */
    public ModifyRequestCodec()
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
        return MessageTypeEnum.MODIFY_REQUEST;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "MODIFY_REQUEST";
    }


    /**
     * Initialize the ArrayList for modifications.
     */
    public void initModifications()
    {
        modifications = new ArrayList<Modification>();
    }


    /**
     * Get the entry's attributes
     * 
     * @return Returns the modifications.
     */
    public List<Modification> getModifications()
    {
        return modifications;
    }


    /**
     * Add a new modification to the list
     * 
     * @param operation The type of operation (add, delete or replace)
     */
    public void addModification( int operation )
    {
        currentOperation = ModificationOperation.getOperation( operation );

        if ( currentAttribute == null )
        {
            modifications = new ArrayList<Modification>();
        }
    }


    /**
     * Add a new attributeTypeAndValue
     * 
     * @param type The attribute's name
     */
    public void addAttributeTypeAndValues( String type )
    {
        currentAttribute = new DefaultClientAttribute( type );

        Modification modification = new ClientModification( currentOperation, currentAttribute );
        modifications.add( modification );
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
     * Return the current attribute's type
     */
    public String getCurrentAttributeType()
    {
        return currentAttribute.getId();
    }


    /**
     * Get the modification's DN
     * 
     * @return Returns the object.
     */
    public DN getObject()
    {
        return object;
    }


    /**
     * Set the modification DN.
     * 
     * @param object The DN to set.
     */
    public void setObject( DN object )
    {
        this.object = object;
    }


    /**
     * Get the current operation
     * 
     * @return Returns the currentOperation.
     */
    public int getCurrentOperation()
    {
        return currentOperation.getValue();
    }


    /**
     * Store the current operation
     * 
     * @param currentOperation The currentOperation to set.
     */
    public void setCurrentOperation( int currentOperation )
    {
        this.currentOperation = ModificationOperation.getOperation( currentOperation );
    }


    /**
     * Store the current operation
     * 
     * @param currentOperation The currentOperation to set.
     */
    public void setCurrentOperation( ModificationOperation currentOperation )
    {
        this.currentOperation = currentOperation;
    }


    /**
     * sets the modifications
     * 
     * @param modifications the list of modifications
     */
    public void setModifications( List<Modification> modifications )
    {
        this.modifications = modifications;
    }


    /**
     * Compute the ModifyRequest length 
     * 
     * ModifyRequest :
     * 
     * 0x66 L1
     *  |
     *  +--> 0x04 L2 object
     *  +--> 0x30 L3 modifications
     *        |
     *        +--> 0x30 L4-1 modification sequence
     *        |     |
     *        |     +--> 0x0A 0x01 (0..2) operation
     *        |     +--> 0x30 L5-1 modification
     *        |           |
     *        |           +--> 0x04 L6-1 type
     *        |           +--> 0x31 L7-1 vals
     *        |                 |
     *        |                 +--> 0x04 L8-1-1 attributeValue
     *        |                 +--> 0x04 L8-1-2 attributeValue
     *        |                 +--> ...
     *        |                 +--> 0x04 L8-1-i attributeValue
     *        |                 +--> ...
     *        |                 +--> 0x04 L8-1-n attributeValue
     *        |
     *        +--> 0x30 L4-2 modification sequence
     *        .     |
     *        .     +--> 0x0A 0x01 (0..2) operation
     *        .     +--> 0x30 L5-2 modification
     *                    |
     *                    +--> 0x04 L6-2 type
     *                    +--> 0x31 L7-2 vals
     *                          |
     *                          +--> 0x04 L8-2-1 attributeValue
     *                          +--> 0x04 L8-2-2 attributeValue
     *                          +--> ...
     *                          +--> 0x04 L8-2-i attributeValue
     *                          +--> ...
     *                          +--> 0x04 L8-2-n attributeValue
     */
    protected int computeLengthProtocolOp()
    {
        // Initialized with object
        modifyRequestLength = 1 + TLV.getNbBytes( DN.getNbBytes( object ) ) + DN.getNbBytes( object );

        // Modifications
        modificationsLength = 0;

        if ( ( modifications != null ) && ( modifications.size() != 0 ) )
        {
            modificationSequenceLength = new LinkedList<Integer>();
            modificationLength = new LinkedList<Integer>();
            valuesLength = new LinkedList<Integer>();

            for ( Modification modification:modifications )
            {
                // Modification sequence length initialized with the operation
                int localModificationSequenceLength = 1 + 1 + 1;
                int localValuesLength = 0;

                // Modification length initialized with the type
                int typeLength = modification.getAttribute().getId().length();
                int localModificationLength = 1 + TLV.getNbBytes( typeLength ) + typeLength;

                // Get all the values
                if ( modification.getAttribute().size() != 0 )
                {
                    for ( org.apache.directory.shared.ldap.entry.Value<?> value:modification.getAttribute() )
                    {
                        localValuesLength += 1 + TLV.getNbBytes( value.getBytes().length )
                            + value.getBytes().length;
                    }
                }

                localModificationLength += 1 + TLV.getNbBytes( localValuesLength ) + localValuesLength;

                // Compute the modificationSequenceLength
                localModificationSequenceLength += 1 + TLV.getNbBytes( localModificationLength )
                    + localModificationLength;

                // Add the tag and the length
                modificationsLength += 1 + TLV.getNbBytes( localModificationSequenceLength )
                    + localModificationSequenceLength;

                // Store the arrays of values
                valuesLength.add( localValuesLength );
                modificationLength.add( localModificationLength );
                modificationSequenceLength.add( localModificationSequenceLength );
            }

            // Add the modifications length to the modificationRequestLength
            modifyRequestLength += 1 + TLV.getNbBytes( modificationsLength ) + modificationsLength;
        }

        return 1 + TLV.getNbBytes( modifyRequestLength ) + modifyRequestLength;
    }


    /**
     * Encode the ModifyRequest message to a PDU. 
     * 
     * ModifyRequest : 
     * <pre>
     * 0x66 LL
     *   0x04 LL object
     *   0x30 LL modifiations
     *     0x30 LL modification sequence
     *       0x0A 0x01 operation
     *       0x30 LL modification
     *         0x04 LL type
     *         0x31 LL vals
     *           0x04 LL attributeValue
     *           ... 
     *           0x04 LL attributeValue
     *     ... 
     *     0x30 LL modification sequence
     *       0x0A 0x01 operation
     *       0x30 LL modification
     *         0x04 LL type
     *         0x31 LL vals
     *           0x04 LL attributeValue
     *           ... 
     *           0x04 LL attributeValue
     * </pre>
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The AddRequest Tag
            buffer.put( LdapConstants.MODIFY_REQUEST_TAG );
            buffer.put( TLV.getBytes( modifyRequestLength ) );

            // The entry
            Value.encode( buffer, DN.getBytes( object ) );

            // The modifications sequence
            buffer.put( UniversalTag.SEQUENCE_TAG );
            buffer.put( TLV.getBytes( modificationsLength ) );

            // The modifications list
            if ( ( modifications != null ) && ( modifications.size() != 0 ) )
            {
                int modificationNumber = 0;

                // Compute the modifications length
                for ( Modification modification:modifications )
                {
                    // The modification sequence
                    buffer.put( UniversalTag.SEQUENCE_TAG );
                    int localModificationSequenceLength = modificationSequenceLength
                        .get( modificationNumber );
                    buffer.put( TLV.getBytes( localModificationSequenceLength ) );

                    // The operation. The value has to be changed, it's not
                    // the same value in DirContext and in RFC 2251.
                    buffer.put( UniversalTag.ENUMERATED_TAG );
                    buffer.put( ( byte ) 1 );
                    buffer.put( ( byte ) modification.getOperation().getValue() );

                    // The modification
                    buffer.put( UniversalTag.SEQUENCE_TAG );
                    int localModificationLength = modificationLength.get( modificationNumber );
                    buffer.put( TLV.getBytes( localModificationLength ) );

                    // The modification type
                    Value.encode( buffer, modification.getAttribute().getId() );

                    // The values
                    buffer.put( UniversalTag.SET_TAG );
                    int localValuesLength = valuesLength.get( modificationNumber );
                    buffer.put( TLV.getBytes( localValuesLength ) );

                    if ( modification.getAttribute().size() != 0 )
                    {
                        for ( org.apache.directory.shared.ldap.entry.Value<?> value:modification.getAttribute() )
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

                    // Go to the next modification number;
                    modificationNumber++;
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Get a String representation of a ModifyRequest
     * 
     * @return A ModifyRequest String
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Modify Request\n" );
        sb.append( "        Object : '" ).append( object ).append( "'\n" );

        if ( modifications != null )
        {
            int i = 0;
            
            for ( Modification modification:modifications )
            {
                sb.append( "            Modification[" ).append( i ).append( "]\n" );
                sb.append( "                Operation : " );

                if ( modification != null )
                {
                    switch ( modification.getOperation() )
                    {
    
                        case ADD_ATTRIBUTE:
                            sb.append( " add\n" );
                            break;
    
                        case REPLACE_ATTRIBUTE:
                            sb.append( " replace\n" );
                            break;
    
                        case REMOVE_ATTRIBUTE:
                            sb.append( " delete\n" );
                            break;
                    }

                    sb.append( "                Modification\n" );
    
                    EntryAttribute attribute = modification.getAttribute();
    
                    if ( attribute != null )
                    {
                        sb.append( attribute );
                    }
                }
                else
                {
                    sb.append( " unknown modification operation\n" );
                }

            }
        }

        return sb.toString();
    }
}

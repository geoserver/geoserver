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
package org.apache.directory.shared.ldap.codec.search.controls.persistentSearch;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.controls.AbstractControl;
import org.apache.directory.shared.ldap.codec.search.controls.ChangeType;


/**
 * A persistence search object
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class PersistentSearchControl extends AbstractControl
{
    /** This control OID */
    public static final String CONTROL_OID = "2.16.840.1.113730.3.4.3";

    /**
     * If changesOnly is TRUE, the server MUST NOT return any existing entries
     * that match the search criteria. Entries are only returned when they are
     * changed (added, modified, deleted, or subject to a modifyDN operation).
     */
    private boolean changesOnly = true;

    /**
     * If returnECs is TRUE, the server MUST return an Entry Change Notification
     * control with each entry returned as the result of changes.
     */
    private boolean returnECs = false;

    /**
     * As changes are made to the server, the effected entries MUST be returned
     * to the client if they match the standard search criteria and if the
     * operation that caused the change is included in the changeTypes field.
     * The changeTypes field is the logical OR of one or more of these values:
     * add    (1), 
     * delete (2), 
     * modify (4), 
     * modDN  (8).
     */
    private int changeTypes = CHANGE_TYPES_MAX;
    
    /** Definition of the change types */
    public static final int CHANGE_TYPE_ADD     = 1;
    public static final int CHANGE_TYPE_DELETE  = 2;
    public static final int CHANGE_TYPE_MODIFY  = 4;
    public static final int CHANGE_TYPE_MODDN   = 8;
    
    /** Min and Max values for the possible combined change types */
    public static final int CHANGE_TYPES_MIN = CHANGE_TYPE_ADD;
    public static final int CHANGE_TYPES_MAX = CHANGE_TYPE_ADD | CHANGE_TYPE_DELETE | CHANGE_TYPE_MODIFY | CHANGE_TYPE_MODDN;

    /** A temporary storage for a psearch length */
    private int psearchSeqLength;

    /**
     * Default constructor
     *
     */
    public PersistentSearchControl()
    {
        super( CONTROL_OID );
        
        decoder = new PersistentSearchControlDecoder();
    }

    public void setChangesOnly( boolean changesOnly )
    {
        this.changesOnly = changesOnly;
    }


    public boolean isChangesOnly()
    {
        return changesOnly;
    }


    public void setReturnECs( boolean returnECs )
    {
        this.returnECs = returnECs;
    }


    public boolean isReturnECs()
    {
        return returnECs;
    }


    public void setChangeTypes( int changeTypes )
    {
        this.changeTypes = changeTypes;
    }


    public int getChangeTypes()
    {
        return changeTypes;
    }

    /**
     * Compute the PagedSearchControl length, which is the sum
     * of the control length and the value length.
     * 
     * <pre>
     * PersistentSearchControl value length :
     * 
     * 0x30 L1 
     *   | 
     *   +--> 0x02 0x0(1-4) [0..2^31-1] (changeTypes) 
     *   +--> 0x01 0x01 [0x00 | 0xFF] (changeOnly) 
     *   +--> 0x01 0x01 [0x00 | 0xFF] (returnRCs)
     * </pre> 
     */
    public int computeLength()
    {
        int changeTypesLength = 1 + 1 + Value.getNbBytes( changeTypes );
        int changesOnlyLength = 1 + 1 + 1;
        int returnRCsLength = 1 + 1 + 1;

        psearchSeqLength = changeTypesLength + changesOnlyLength + returnRCsLength;
        int valueLength = 1 + TLV.getNbBytes( psearchSeqLength ) + psearchSeqLength;

        // Call the super class to compute the global control length
        return super.computeLength( valueLength );
    }


    /**
     * Encodes the persistent search control.
     * 
     * @param buffer The encoded sink
     * @return A ByteBuffer that contains the encoded PDU
     * @throws EncoderException If anything goes wrong.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04023 ) );
        }

        // Encode the Control envelop
        super.encode( buffer );
        
        // Encode the OCTET_STRING tag
        buffer.put( UniversalTag.OCTET_STRING_TAG );
        buffer.put( TLV.getBytes( valueLength ) );

        // Now encode the PagedSearch specific part
        buffer.put( UniversalTag.SEQUENCE_TAG );
        buffer.put( TLV.getBytes( psearchSeqLength ) );

        Value.encode( buffer, changeTypes );
        Value.encode( buffer, changesOnly );
        Value.encode( buffer, returnECs );
        
        return buffer;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public byte[] getValue()
    {
        if ( value == null )
        {
            try
            { 
                computeLength();
                ByteBuffer buffer = ByteBuffer.allocate( valueLength );
                
                // Now encode the PagedSearch specific part
                buffer.put( UniversalTag.SEQUENCE_TAG );
                buffer.put( TLV.getBytes( psearchSeqLength ) );

                Value.encode( buffer, changeTypes );
                Value.encode( buffer, changesOnly );
                Value.encode( buffer, returnECs );
                
                value = buffer.array();
            }
            catch ( Exception e )
            {
                return null;
            }
        }
        
        return value;
    }

    
    public boolean isNotificationEnabled( ChangeType changeType )
    {
        return ( changeType.getValue() & changeTypes ) > 0;
    }


    public void enableNotification( ChangeType changeType )
    {
        changeTypes |= changeType.getValue();
    }


    /**
     * Return a String representing this PSearchControl.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Persistant Search Control\n" );
        sb.append( "        oid : " ).append( getOid() ).append( '\n' );
        sb.append( "        critical : " ).append( isCritical() ).append( '\n' );
        sb.append( "        changeTypes : '" ).append( changeTypes ).append( "'\n" );
        sb.append( "        changesOnly : '" ).append( changesOnly ).append( "'\n" );
        sb.append( "        returnECs   : '" ).append( returnECs ).append( "'\n" );

        return sb.toString();
    }
}

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
package org.apache.directory.shared.ldap.codec.search.controls.entryChange;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.controls.AbstractControl;
import org.apache.directory.shared.ldap.codec.search.controls.ChangeType;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A response control that may be returned by Persistent Search entry responses.
 * It contains addition change information to describe the exact change that
 * occurred to an entry. The exact details of this control are covered in section
 * 5 of this (yes) expired draft: <a
 * href="http://www3.ietf.org/proceedings/01aug/I-D/draft-ietf-ldapext-psearch-03.txt">
 * Persistent Search Draft v03</a> which is printed out below for convenience:
 * 
 * <pre>
 *    5.  Entry Change Notification Control
 *    
 *    This control provides additional information about the change the caused
 *    a particular entry to be returned as the result of a persistent search.
 *    The controlType is &quot;2.16.840.1.113730.3.4.7&quot;.  If the client set the
 *    returnECs boolean to TRUE in the PersistentSearch control, servers MUST
 *    include an EntryChangeNotification control in the Controls portion of
 *    each SearchResultEntry that is returned due to an entry being added,
 *    deleted, or modified.
 *    
 *               EntryChangeNotification ::= SEQUENCE 
 *               {
 *                         changeType ENUMERATED 
 *                         {
 *                                 add             (1),
 *                                 delete          (2),
 *                                 modify          (4),
 *                                 modDN           (8)
 *                         },
 *                         previousDN   LDAPDN OPTIONAL,     -- modifyDN ops. only
 *                         changeNumber INTEGER OPTIONAL     -- if supported
 *               }
 *    
 *    changeType indicates what LDAP operation caused the entry to be
 *    returned.
 *    
 *    previousDN is present only for modifyDN operations and gives the DN of
 *    the entry before it was renamed and/or moved.  Servers MUST include this
 *    optional field only when returning change notifications as a result of
 *    modifyDN operations.
 * 
 *    changeNumber is the change number [CHANGELOG] assigned by a server for
 *    the change.  If a server supports an LDAP Change Log it SHOULD include
 *    this field.
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 918756 $, $Date: 2010-03-04 01:05:29 +0200 (Thu, 04 Mar 2010) $, 
 */
public class EntryChangeControl extends AbstractControl
{
    /** The EntryChange control */
    public static final String CONTROL_OID = "2.16.840.1.113730.3.4.7";

    public static final int UNDEFINED_CHANGE_NUMBER = -1;

    private ChangeType changeType = ChangeType.ADD;

    private long changeNumber = UNDEFINED_CHANGE_NUMBER;

    /** The previous DN */
    private DN previousDn = null;
    
    /** A temporary storage for the previous DN */
    private byte[] previousDnBytes = null;

    /** The entry change global length */
    private int eccSeqLength;


    /**
     * @see Asn1Object#Asn1Object
     */
    public EntryChangeControl()
    {
        super( CONTROL_OID );
        
        decoder = new EntryChangeControlDecoder();
    }

    
    /**
     * Compute the EntryChangeControl length 
     * 
     * 0x30 L1 
     *   | 
     *   +--> 0x0A 0x0(1-4) [1|2|4|8] (changeType) 
     *  [+--> 0x04 L2 previousDN] 
     *  [+--> 0x02 0x0(1-4) [0..2^63-1] (changeNumber)]
     */
    public int computeLength()
    {
        int changeTypesLength = 1 + 1 + 1;

        int previousDnLength = 0;
        int changeNumberLength = 0;

        if ( previousDn != null )
        {
            previousDnBytes = StringTools.getBytesUtf8( previousDn.getName() );
            previousDnLength = 1 + TLV.getNbBytes( previousDnBytes.length ) + previousDnBytes.length;
        }

        if ( changeNumber != UNDEFINED_CHANGE_NUMBER )
        {
            changeNumberLength = 1 + 1 + Value.getNbBytes( changeNumber );
        }

        eccSeqLength = changeTypesLength + previousDnLength + changeNumberLength;
        valueLength = 1 + TLV.getNbBytes( eccSeqLength ) + eccSeqLength;

        // Call the super class to compute the global control length
        return super.computeLength( valueLength );
    }


    /**
     * Encodes the entry change control.
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

        buffer.put( UniversalTag.SEQUENCE_TAG );
        buffer.put( TLV.getBytes( eccSeqLength ) );

        buffer.put( UniversalTag.ENUMERATED_TAG );
        buffer.put( ( byte ) 1 );
        buffer.put( Value.getBytes( changeType.getValue() ) );

        if ( previousDn != null )
        {
            Value.encode( buffer, previousDnBytes );
        }
        
        if ( changeNumber != UNDEFINED_CHANGE_NUMBER )
        {
            Value.encode( buffer, changeNumber );
        }
        
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
                
                buffer.put( UniversalTag.SEQUENCE_TAG );
                buffer.put( TLV.getBytes( eccSeqLength ) );
        
                buffer.put( UniversalTag.ENUMERATED_TAG );
                buffer.put( ( byte ) 1 );
                buffer.put( Value.getBytes( changeType.getValue() ) );
        
                if ( previousDn != null )
                {
                    Value.encode( buffer, previousDnBytes );
                }
                
                if ( changeNumber != UNDEFINED_CHANGE_NUMBER )
                {
                    Value.encode( buffer, changeNumber );
                }
                
                value = buffer.array();
            }
            catch ( Exception e )
            {
                return null;
            }
        }
        
        return value;
    }


    /**
     * @return The ChangeType
     */
    public ChangeType getChangeType()
    {
        return changeType;
    }


    /**
     * Set the ChangeType
     *
     * @param changeType Add, Delete; Modify or ModifyDN
     */
    public void setChangeType( ChangeType changeType )
    {
        this.changeType = changeType;
    }


    public DN getPreviousDn()
    {
        return previousDn;
    }


    public void setPreviousDn( DN previousDn )
    {
        this.previousDn = previousDn;
    }


    public long getChangeNumber()
    {
        return changeNumber;
    }


    public void setChangeNumber( long changeNumber )
    {
        this.changeNumber = changeNumber;
    }


    /**
     * Return a String representing this EntryChangeControl.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Entry Change Control\n" );
        sb.append( "        oid : " ).append( getOid() ).append( '\n' );
        sb.append( "        critical : " ).append( isCritical() ).append( '\n' );
        sb.append( "        changeType   : '" ).append( changeType ).append( "'\n" );
        sb.append( "        previousDN   : '" ).append( previousDn ).append( "'\n" );
        
        if ( changeNumber == UNDEFINED_CHANGE_NUMBER )
        {
            sb.append( "        changeNumber : '" ).append( "UNDEFINED" ).append( "'\n" );
        }
        else
        {
            sb.append( "        changeNumber : '" ).append( changeNumber ).append( "'\n" );
        }
        
        return sb.toString();
    }
}

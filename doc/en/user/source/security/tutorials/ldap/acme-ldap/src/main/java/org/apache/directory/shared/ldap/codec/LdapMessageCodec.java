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
package org.apache.directory.shared.ldap.codec;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.controls.CodecControl;
import org.apache.directory.shared.ldap.codec.controls.ManageDsaITControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue.SyncDoneValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue.SyncInfoValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue.SyncRequestValueControl;
import org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue.SyncStateValueControl;
import org.apache.directory.shared.ldap.codec.search.controls.pagedSearch.PagedResultsControl;
import org.apache.directory.shared.ldap.codec.search.controls.persistentSearch.PersistentSearchControl;
import org.apache.directory.shared.ldap.codec.search.controls.subentries.SubentriesControl;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationInfoEnum;


/**
 * The main ldapObject : every Ldap Message are encapsulated in it. It contains
 * a message Id, a operation (protocolOp) and one ore more Controls.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public abstract class LdapMessageCodec extends AbstractAsn1Object
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The message ID */
    private int messageId;

    /** The controls */
    private List<Control> controls;

    /** The current control */
    private Control currentControl;

    /** The LdapMessage length */
    protected int ldapMessageLength;

    /** The controls length */
    private int controlsLength;

    /** The controls sequence length */
    private int controlsSequenceLength;

    private Map<String, Control> codecControls = new HashMap<String, Control>();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new LdapMessage object.
     */
    public LdapMessageCodec()
    {
        super();
        // We should not create this kind of object directly
        
        // Initialize the different known Controls
        Control control = new PersistentSearchControl();
        codecControls.put( control.getOid(), control );

        control = new ManageDsaITControl();
        codecControls.put( control.getOid(), control );

        control = new SubentriesControl();
        codecControls.put( control.getOid(), control );

        control = new PagedResultsControl();
        codecControls.put( control.getOid(), control );
        
        control = new SyncDoneValueControl();
        codecControls.put( control.getOid(), control );
        
        control = new SyncInfoValueControl( SynchronizationInfoEnum.NEW_COOKIE );
        codecControls.put( control.getOid(), control );
        
        control = new SyncInfoValueControl( SynchronizationInfoEnum.REFRESH_DELETE );
        codecControls.put( control.getOid(), control );
        
        control = new SyncInfoValueControl( SynchronizationInfoEnum.REFRESH_PRESENT );
        codecControls.put( control.getOid(), control );
        
        control = new SyncInfoValueControl( SynchronizationInfoEnum.SYNC_ID_SET );
        codecControls.put( control.getOid(), control );
        
        control = new SyncRequestValueControl();
        codecControls.put( control.getOid(), control );
        
        control = new SyncStateValueControl();
        codecControls.put( control.getOid(), control );
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the Control Object at a specific index
     * 
     * @param i The index of the Control Object to get
     * @return The selected Control Object
     */
    public Control getControls( int i )
    {
        if ( controls != null )
        {
            return controls.get( i );
        }
        else
        {
            return null;
        }
    }


    /**
     * Get the Control Objects
     * 
     * @return The Control Objects
     */
    public List<Control> getControls()
    {
        return controls;
    }


    /**
     * Get the current Control Object
     * 
     * @return The current Control Object
     */
    public Control getCurrentControl()
    {
        return currentControl;
    }
    
    
    public Control getCodecControl( String oid )
    {
        return codecControls.get( oid );
    }


    /**
     * Add a control to the Controls array
     * 
     * @param control The Control to add
     */
    public void addControl( Control control )
    {
        currentControl = control;
        
        if ( controls == null )
        {
            controls = new ArrayList<Control>();
        }
        
        controls.add( control );
    }


    /**
     * Set or add a list of controls to the Controls array. If the existing
     * control array is not null then the given controls will be added
     * 
     * @param controls The list of Controls to set or add
     */
    public void addControls( List<Control> controls )
    {
        if( this.controls == null )
        {
            this.controls = controls;
        }
        else if( controls != null )
        {
            this.controls.addAll( controls );
        }
    }
    
    
    /**
     * Init the controls array
     */
    public void initControls()
    {
        controls = new ArrayList<Control>();
    }


    /**
     * Get the message ID
     * 
     * @return The message ID
     */
    public int getMessageId()
    {
        return messageId;
    }


    /**
     * Set the message ID
     * 
     * @param messageId The message ID
     */
    public void setMessageId( int messageId )
    {
        this.messageId = messageId;
    }


    /**
     * Get the message type
     * 
     * @return The message type
     */
    public abstract MessageTypeEnum getMessageType();


    /**
     * Get the message type Name
     * 
     * @return The message type name
     */
    public abstract String getMessageTypeName();

    
    protected abstract int computeLengthProtocolOp();

    
    /**
     * Compute the LdapMessage length LdapMessage : 
     * 0x30 L1 
     *   | 
     *   +--> 0x02 0x0(1-4) [0..2^31-1] (MessageId) 
     *   +--> protocolOp 
     *   [+--> Controls] 
     *   
     * MessageId length = Length(0x02) + length(MessageId) + MessageId.length 
     * L1 = length(ProtocolOp) 
     * LdapMessage length = Length(0x30) + Length(L1) + MessageId length + L1
     */
    public int computeLength()
    {
        // The length of the MessageId. It's the sum of
        // - the tag (0x02), 1 byte
        // - the length of the Id length, 1 byte
        // - the Id length, 1 to 4 bytes
        ldapMessageLength = 1 + 1 + Value.getNbBytes( messageId );

        // Get the protocolOp length
        int protocolOpLength = computeLengthProtocolOp();

        // Add the protocol length to the message length
        ldapMessageLength += protocolOpLength;

        // Do the same thing for Controls, if any.
        if ( controls != null )
        {
            // Controls :
            // 0xA0 L3
            //   |
            //   +--> 0x30 L4
            //   +--> 0x30 L5
            //   +--> ...
            //   +--> 0x30 Li
            //   +--> ...
            //   +--> 0x30 Ln
            //
            // L3 = Length(0x30) + Length(L5) + L5
            // + Length(0x30) + Length(L6) + L6
            // + ...
            // + Length(0x30) + Length(Li) + Li
            // + ...
            // + Length(0x30) + Length(Ln) + Ln
            //
            // LdapMessageLength = LdapMessageLength + Length(0x90)
            // + Length(L3) + L3
            controlsSequenceLength = 0;

            // We may have more than one control. ControlsLength is L4.
            for ( Control control:controls )
            {
                controlsSequenceLength += ((CodecControl)control).computeLength();
            }

            // Computes the controls length
            controlsLength = controlsSequenceLength; // 1 + Length.getNbBytes(
                                                     // controlsSequenceLength
                                                     // ) + controlsSequenceLength;

            // Now, add the tag and the length of the controls length
            ldapMessageLength += 1 + TLV.getNbBytes( controlsSequenceLength ) + controlsSequenceLength;
        }

        // finally, calculate the global message size :
        // length(Tag) + Length(length) + length

        return 1 + ldapMessageLength + TLV.getNbBytes( ldapMessageLength );
    }

    
    protected abstract void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException;

    /**
     * Generate the PDU which contains the encoded object. 
     * 
     * The generation is done in two phases : 
     * - first, we compute the length of each part and the
     * global PDU length 
     * - second, we produce the PDU. 
     * 
     * <pre>
     * 0x30 L1 
     *   | 
     *   +--> 0x02 L2 MessageId  
     *   +--> ProtocolOp 
     *   +--> Controls 
     *   
     * L2 = Length(MessageId)
     * L1 = Length(0x02) + Length(L2) + L2 + Length(ProtocolOp) + Length(Controls)
     * LdapMessageLength = Length(0x30) + Length(L1) + L1
     * </pre>
     * 
     * @param buffer The encoded PDU
     * @return A ByteBuffer that contaons the PDU
     * @throws EncoderException If anything goes wrong.
     */
    public ByteBuffer encode() throws EncoderException
    {
        // Allocate the bytes buffer.
        ByteBuffer bb = ByteBuffer.allocate( computeLength() );

        try
        {
            // The LdapMessage Sequence
            bb.put( UniversalTag.SEQUENCE_TAG );

            // The length has been calculated by the computeLength method
            bb.put( TLV.getBytes( ldapMessageLength ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        // The message Id
        Value.encode( bb, messageId );

        // Add the protocolOp part
        encodeProtocolOp( bb );

        // Do the same thing for Controls, if any.
        if ( controls != null )
        {
            // Encode the controls
            bb.put( ( byte ) LdapConstants.CONTROLS_TAG );
            bb.put( TLV.getBytes( controlsLength ) );

            // Encode each control
            for ( Control control:controls )
            {
                ((CodecControl)control).encode( bb );
            }
        }

        return bb;
    }


    /**
     * Get a String representation of a LdapMessage
     * 
     * @return A LdapMessage String
     */
    protected String toString( String protocolOp )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "LdapMessage\n" );
        sb.append( "    message Id : " ).append( messageId ).append( '\n' );
        
        sb.append( protocolOp ).append( '\n' );

        if ( controls != null )
        {
            for ( Control control:controls )
            {
                sb.append( control );
            }
        }

        return sb.toString();
    }
}

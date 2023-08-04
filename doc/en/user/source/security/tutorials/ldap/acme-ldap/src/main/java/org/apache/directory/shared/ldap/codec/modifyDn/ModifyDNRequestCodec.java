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
package org.apache.directory.shared.ldap.codec.modifyDn;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A ModifyDNRequest Message. Its syntax is :
 * ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
 *                 entry           LDAPDN,
 *                 newrdn          RelativeLDAPDN,
 *                 deleteoldrdn    BOOLEAN,
 *                 newSuperior     [0] LDAPDN OPTIONAL }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 926019 $, $Date: 2010-03-22 12:14:55 +0200 (Mon, 22 Mar 2010) $, 
 */
public class ModifyDNRequestCodec extends LdapMessageCodec
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The DN to be modified. */
    private DN entry;

    /** The new RDN to be added to the RDN or to the new superior, if present */
    private RDN newRDN;

    /** If the previous RDN is to be deleted, this flag will be set to true */
    private boolean deleteOldRDN;

    /** The optional superior, which will be concatened to the newRdn */
    private DN newSuperior;

    /** The modify DN request length */
    private int modifyDNRequestLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new ModifyDNRequest object.
     */
    public ModifyDNRequestCodec()
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
        return MessageTypeEnum.MODIFYDN_REQUEST;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "MODIFYDN_REQUEST";
    }


    /**
     * Get the modification's DN
     * 
     * @return Returns the entry.
     */
    public DN getEntry()
    {
        return entry;
    }


    /**
     * Set the modification DN.
     * 
     * @param entry The entry to set.
     */
    public void setEntry( DN entry )
    {
        this.entry = entry;
    }


    /**
     * Tells if the old RDN is to be deleted
     * 
     * @return Returns the deleteOldRDN.
     */
    public boolean isDeleteOldRDN()
    {
        return deleteOldRDN;
    }


    /**
     * Set the flag to delete the old RDN
     * 
     * @param deleteOldRDN The deleteOldRDN to set.
     */
    public void setDeleteOldRDN( boolean deleteOldRDN )
    {
        this.deleteOldRDN = deleteOldRDN;
    }


    /**
     * Get the new RDN
     * 
     * @return Returns the newRDN.
     */
    public RDN getNewRDN()
    {
        return newRDN;
    }


    /**
     * Set the new RDN
     * 
     * @param newRDN The newRDN to set.
     */
    public void setNewRDN( RDN newRDN )
    {
        this.newRDN = newRDN;
    }


    /**
     * Get the newSuperior
     * 
     * @return Returns the newSuperior.
     */
    public DN getNewSuperior()
    {
        return newSuperior;
    }


    /**
     * Set the new superior
     * 
     * @param newSuperior The newSuperior to set.
     */
    public void setNewSuperior( DN newSuperior )
    {
        this.newSuperior = newSuperior;
    }


    /**
     * Compute the ModifyDNRequest length
     * 
     * ModifyDNRequest :
     * <pre>
     * 0x6C L1
     *  |
     *  +--> 0x04 L2 entry
     *  +--> 0x04 L3 newRDN
     *  +--> 0x01 0x01 (true/false) deleteOldRDN (3 bytes)
     * [+--> 0x80 L4 newSuperior ] 
     * 
     * L2 = Length(0x04) + Length(Length(entry)) + Length(entry) 
     * L3 = Length(0x04) + Length(Length(newRDN)) + Length(newRDN) 
     * L4 = Length(0x80) + Length(Length(newSuperior)) + Length(newSuperior)
     * L1 = L2 + L3 + 3 [+ L4] 
     * 
     * Length(ModifyDNRequest) = Length(0x6C) + Length(L1) + L1
     * </pre>
     * 
     * @return The PDU's length of a ModifyDN Request
     */
    protected int computeLengthProtocolOp()
    {
        int newRdnlength = StringTools.getBytesUtf8( newRDN.getName() ).length;
        modifyDNRequestLength = 1 + TLV.getNbBytes( DN.getNbBytes( entry ) ) + DN.getNbBytes( entry ) + 1
            + TLV.getNbBytes( newRdnlength ) + newRdnlength + 1 + 1 + 1; // deleteOldRDN

        if ( newSuperior != null )
        {
            modifyDNRequestLength += 1 + TLV.getNbBytes( DN.getNbBytes( newSuperior ) )
                + DN.getNbBytes( newSuperior );
        }

        return 1 + TLV.getNbBytes( modifyDNRequestLength ) + modifyDNRequestLength;
    }


    /**
     * Encode the ModifyDNRequest message to a PDU. 
     * 
     * ModifyDNRequest :
     * <pre>
     * 0x6C LL
     *   0x04 LL entry
     *   0x04 LL newRDN
     *   0x01 0x01 deleteOldRDN
     *   [0x80 LL newSuperior]
     * </pre>
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The ModifyDNRequest Tag
            buffer.put( LdapConstants.MODIFY_DN_REQUEST_TAG );
            buffer.put( TLV.getBytes( modifyDNRequestLength ) );

            // The entry

            Value.encode( buffer, DN.getBytes( entry ) );

            // The newRDN
            Value.encode( buffer, newRDN.getName() );

            // The flag deleteOldRdn
            Value.encode( buffer, deleteOldRDN );

            // The new superior, if any
            if ( newSuperior != null )
            {
                // Encode the reference
                buffer.put( ( byte ) LdapConstants.MODIFY_DN_REQUEST_NEW_SUPERIOR_TAG );

                int newSuperiorLength = DN.getNbBytes( newSuperior );

                buffer.put( TLV.getBytes( newSuperiorLength ) );

                if ( newSuperiorLength != 0 )
                {
                    buffer.put( DN.getBytes( newSuperior ) );
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }
    }


    /**
     * Get a String representation of a ModifyDNRequest
     * 
     * @return A ModifyDNRequest String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    ModifyDN Response\n" );
        sb.append( "        Entry : '" ).append( entry ).append( "'\n" );
        sb.append( "        New RDN : '" ).append( newRDN ).append( "'\n" );
        sb.append( "        Delete old RDN : " ).append( deleteOldRDN ).append( "\n" );

        if ( newSuperior != null )
        {
            sb.append( "        New superior : '" ).append( newSuperior ).append( "'\n" );
        }

        return sb.toString();
    }
}

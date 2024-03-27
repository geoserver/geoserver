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
package org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.controls.AbstractControl;
import org.apache.directory.shared.ldap.message.control.replication.SyncStateTypeEnum;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A syncStateValue object, as defined in RFC 4533
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date: 
 */
public class SyncStateValueControl  extends AbstractControl
{
    /** This control OID */
    public static final String CONTROL_OID = "1.3.6.1.4.1.4203.1.9.1.2";

    /** The syncStateEnum type */
    private SyncStateTypeEnum syncStateType;

    /** The Sync cookie */
    private byte[] cookie;

    /** The entryUUID */
    private byte[] entryUUID;

    /** global length for the control */
    private int syncStateSeqLength;

    public SyncStateValueControl()
    {
        super( CONTROL_OID );
        
        decoder = new SyncStateValueControlDecoder();
    }

    /**
     * @return the cookie
     */
    public byte[] getCookie()
    {
        return cookie;
    }


    /**
     * @param cookie the cookie to set
     */
    public void setCookie( byte[] cookie )
    {
        this.cookie = cookie;
    }


    /**
     * @return the syncState's type
     */
    public SyncStateTypeEnum getSyncStateType()
    {
        return syncStateType;
    }


    /**
     * set the syncState's type
     * 
     * @param syncStateType the syncState's type
     */
    public void setSyncStateType( SyncStateTypeEnum syncStateType )
    {
        this.syncStateType = syncStateType;
    }


    /**
     * @return the entryUUID
     */
    public byte[] getEntryUUID()
    {
        return entryUUID;
    }


    /**
     * set the entryUUID
     * 
     * @param entryUUID the entryUUID
     */
    public void setEntryUUID( byte[] entryUUID )
    {
        this.entryUUID = entryUUID;
    }


    /**
     * Compute the SyncStateValue length.
     * 
     * SyncStateValue :
     * 0x30 L1
     *  | 
     *  +--> 0x0A 0x01 [0x00|0x01|0x02|0x03] (type)
     * [+--> 0x04 L2 abcd...                 (entryUUID)
     * [+--> 0x04 L3 abcd...                 (cookie)
     *   
     */
    public int computeLength()
    {
        // The sync state type length
        syncStateSeqLength = 1 + 1 + 1;

        syncStateSeqLength += 1 + TLV.getNbBytes( entryUUID.length ) + entryUUID.length;

        // The cookie length, if we have a cookie
        if ( cookie != null )
        {
            syncStateSeqLength += 1 + TLV.getNbBytes( cookie.length ) + cookie.length;
        }
        
        valueLength = 1 + TLV.getNbBytes( syncStateSeqLength ) + syncStateSeqLength;

        return super.computeLength( valueLength );
    }


    /**
     * Encode the SyncStateValue control
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

        // Encode the SEQ 
        buffer.put( UniversalTag.SEQUENCE_TAG );
        buffer.put( TLV.getBytes( syncStateSeqLength ) );

        // The mode
        buffer.put( UniversalTag.ENUMERATED_TAG );
        buffer.put( ( byte ) 0x01 );
        buffer.put( Value.getBytes( syncStateType.getValue() ) );

        // the entryUUID
        Value.encode( buffer, entryUUID );

        // The cookie
        if ( cookie != null )
        {
            Value.encode( buffer, cookie );
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
                
                // Encode the SEQ 
                buffer.put( UniversalTag.SEQUENCE_TAG );
                buffer.put( TLV.getBytes( syncStateSeqLength ) );

                // The mode
                buffer.put( UniversalTag.ENUMERATED_TAG );
                buffer.put( ( byte ) 0x01 );
                buffer.put( Value.getBytes( syncStateType.getValue() ) );

                // the entryUUID
                Value.encode( buffer, entryUUID );

                // The cookie
                if ( cookie != null )
                {
                    Value.encode( buffer, cookie );
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
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "    SyncStateValue control :\n" );
        sb.append( "        oid : " ).append( getOid() ).append( '\n' );
        sb.append( "        critical : " ).append( isCritical() ).append( '\n' );
        sb.append( "        syncStateType     : '" ).append( syncStateType ).append( "'\n" );
        sb.append( "        entryUUID         : '" ).append( StringTools.dumpBytes( entryUUID ) ).append( "'\n" );
        sb.append( "        cookie            : '" ).append( StringTools.dumpBytes( cookie ) ).append( "'\n" );

        return sb.toString();
    }
}

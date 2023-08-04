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
package org.apache.directory.shared.ldap.codec.extended.operations.gracefulDisconnect;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.extended.operations.GracefulAction;
import org.apache.directory.shared.ldap.codec.extended.operations.GracefulActionConstants;
import org.apache.directory.shared.ldap.util.LdapURL;


/**
 * An extended operation to proceed a graceful disconnect
 * 
 * <pre>
 *   GracefulDisconnect ::= SEQUENCE 
 *   {
 *       timeOffline           INTEGER (0..720) DEFAULT 0,
 *       delay             [0] INTEGER (0..86400) DEFAULT 0,
 *       replicatedContexts    Referral OPTIONAL
 *   }
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 910150 $, $Date: 2010-02-15 03:37:34 +0200 (Mon, 15 Feb 2010) $, 
 */
public class GracefulDisconnect extends GracefulAction
{
    /** List of the alternate servers to use */
    // Two urls will be enough, generally
    private List<LdapURL> replicatedContexts = new ArrayList<LdapURL>( 2 );

    /** Length of the sequence */
    private int gracefulDisconnectSequenceLength;

    /** Length of the replicated contexts */
    private int replicatedContextsLength;


    /**
     * Create a GracefulDisconnect object, with a timeOffline and a delay
     * 
     * @param timeOffline The time the server will be offline
     * @param delay The delay before the disconnection
     */
    public GracefulDisconnect( int timeOffline, int delay )
    {
        super( timeOffline, delay );
    }


    /**
     * Default constructor.
     */
    public GracefulDisconnect()
    {
        super();
    }


    /**
     * Get the list of replicated servers
     * 
     * @return The list of replicated servers
     */
    public List<LdapURL> getReplicatedContexts()
    {
        return replicatedContexts;
    }


    /**
     * Add a new URL of a replicated server
     * 
     * @param replicatedContext The replictaed server to add.
     */
    public void addReplicatedContexts( LdapURL replicatedContext )
    {
        replicatedContexts.add( replicatedContext );
    }


    /**
     * Compute the GracefulDisconnect length 
     * 
     * 0x30 L1 
     *   | 
     *   +--> [ 0x02 0x0(1-4) [0..720] ] 
     *   +--> [ 0x80 0x0(1-3) [0..86400] ] 
     *   +--> [ 0x30 L2 
     *           | 
     *           +--> (0x04 L3 value) + ]
     */
    public int computeLength()
    {
        gracefulDisconnectSequenceLength = 0;

        if ( timeOffline != 0 )
        {
            gracefulDisconnectSequenceLength += 1 + 1 + Value.getNbBytes( timeOffline );
        }

        if ( delay != 0 )
        {
            gracefulDisconnectSequenceLength += 1 + 1 + Value.getNbBytes( delay );
        }

        if ( replicatedContexts.size() > 0 )
        {
            replicatedContextsLength = 0;

            // We may have more than one reference.
            for ( LdapURL replicatedContext:replicatedContexts )
            {
                int ldapUrlLength = replicatedContext.getNbBytes();
                replicatedContextsLength += 1 + TLV.getNbBytes( ldapUrlLength ) + ldapUrlLength;
            }

            gracefulDisconnectSequenceLength += 1 + TLV.getNbBytes( replicatedContextsLength )
                + replicatedContextsLength;
        }

        return 1 + TLV.getNbBytes( gracefulDisconnectSequenceLength ) + gracefulDisconnectSequenceLength;
    }


    /**
     * Encodes the gracefulDisconnect extended operation.
     * 
     * @return A ByteBuffer that contains the encoded PDU
     * @throws EncoderException If anything goes wrong.
     */
    public ByteBuffer encode() throws EncoderException
    {
        // Allocate the bytes buffer.
        ByteBuffer bb = ByteBuffer.allocate( computeLength() );

        bb.put( UniversalTag.SEQUENCE_TAG );
        bb.put( TLV.getBytes( gracefulDisconnectSequenceLength ) );

        if ( timeOffline != 0 )
        {
            Value.encode( bb, timeOffline );
        }

        if ( delay != 0 )
        {
            bb.put( ( byte ) GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG );
            bb.put( ( byte ) TLV.getNbBytes( delay ) );
            bb.put( Value.getBytes( delay ) );
        }

        if ( replicatedContexts.size() != 0 )
        {
            bb.put( UniversalTag.SEQUENCE_TAG );
            bb.put( TLV.getBytes( replicatedContextsLength ) );

            // We may have more than one reference.
            for ( LdapURL replicatedContext:replicatedContexts )
            {
                Value.encode( bb, replicatedContext.getBytesReference() );
            }
        }

        return bb;
    }


    /**
     * Return a string representation of the graceful disconnect
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "Graceful Disconnect extended operation" );
        sb.append( "    TimeOffline : " ).append( timeOffline ).append( '\n' );
        sb.append( "    Delay : " ).append( delay ).append( '\n' );

        if ( ( replicatedContexts != null) && ( replicatedContexts.size() != 0 ) )
        {
            sb.append( "    Replicated contexts :" );

            // We may have more than one reference.
            for ( LdapURL url:replicatedContexts )
            {
                sb.append( "\n        " ).append( url );
            }
        }

        return sb.toString();
    }
}

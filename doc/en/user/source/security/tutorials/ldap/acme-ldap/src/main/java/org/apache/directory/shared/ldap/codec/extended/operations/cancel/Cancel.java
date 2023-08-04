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
package org.apache.directory.shared.ldap.codec.extended.operations.cancel;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;


/**
 * An extended operation to proceed a Cancel operation, as described 
 * in RFC 3909
 * 
 * <pre>
 *   cancelRequestValue ::= SEQUENCE {
 *       cancelID        MessageID
 *                       -- MessageID is as defined in [RFC2251]
 *   }
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 687720 $, $Date: 2008-08-21 14:05:50 +0200 (Thu, 21 Aug 2008) $, 
 */
public class Cancel extends AbstractAsn1Object
{
    /** The Id of the the message to cancel */
    private int cancelId;
    
    /** Length of the sequence */
    private int cancelSequenceLength;

    /**
     * Create a Cancel object, with a messageId
     * 
     * @param cancelId The Id of the request to cancel
     */
    public Cancel( int cancelId )
    {
        this.cancelId = cancelId;
    }


    /**
     * Default constructor.
     */
    public Cancel()
    {
        super();
    }


    /**
     * Get the message Id of the request to cancel
     * 
     * @return The id of the request to cancel
     */
    public int getCancelId()
    {
        return cancelId;
    }


    /**
     * Set the cancelId
     * 
     * @param cancelId The Id of the request to cancel
     */
    public void setCancelId( int cancelId )
    {
        this.cancelId = cancelId;
    }


    /**
     * Compute the Cancel length 
     * 
     * 0x30 L1 
     *   | 
     *   +--> 0x02 0x0(1-4) [0..2^31-1] 
     */
    public int computeLength()
    {
        // The messageId length
        cancelSequenceLength = 1 + 1 + Value.getNbBytes( cancelId );

        // Add the sequence and the length
        return 1 + 1 + cancelSequenceLength;
    }


    /**
     * Encodes the cancel extended operation.
     * 
     * @return A ByteBuffer that contains the encoded PDU
     * @throws EncoderException If anything goes wrong.
     */
    public ByteBuffer encode() throws EncoderException
    {
        // Allocate the bytes buffer.
        ByteBuffer bb = ByteBuffer.allocate( computeLength() );

        // The sequence
        bb.put( UniversalTag.SEQUENCE_TAG );
        bb.put( TLV.getBytes( cancelSequenceLength ) );

        // The messageId
        Value.encode( bb, cancelId );

        return bb;
    }


    /**
     * Return a string representation of the cancel
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "Cancel extended operation" );
        sb.append( "    cancelId : " ).append( cancelId ).append( '\n' );

        return sb.toString();
    }
}

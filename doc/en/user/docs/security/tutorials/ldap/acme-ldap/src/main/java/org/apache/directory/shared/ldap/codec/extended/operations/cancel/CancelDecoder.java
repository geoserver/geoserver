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

import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;


/**
 * A decoder for Cancel.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664290 $, $Date: 2008-06-07 08:28:06 +0200 (Sat, 07 Jun 2008) $, 
 */
public class CancelDecoder extends Asn1Decoder
{
    /** The decoder */
    private static final Asn1Decoder decoder = new Asn1Decoder();


    /**
     * Decode a PDU which must contain a Cancel extended operation.
     * Note that the stream of bytes much contain a full PDU, not a partial one.
     * 
     * @param stream The bytes to be decoded
     * @return An Cancel object
     * @throws DecoderException If the decoding failed
     */
    public Asn1Object decode( byte[] stream ) throws DecoderException
    {
        ByteBuffer bb = ByteBuffer.wrap( stream );
        CancelContainer container = new CancelContainer();
        decoder.decode( bb, container );
        Cancel cancel = container.getCancel();

        // Clean the container for the next decoding
        container.clean();

        return cancel;
    }
}

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
package org.apache.directory.shared.ldap.codec.controls;

import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.codec.EncoderException;

/**
 * Define the transform method to be implemented by all the codec Controls
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface CodecControl
{
    /**
     * Generate the PDU which contains the Control.
     * <pre> 
     * Control : 
     * 
     * 0x30 LL
     *   0x04 LL type 
     *   [0x01 0x01 criticality]
     *   [0x04 LL value]
     * </pre>
     * @param buffer The encoded PDU
     * @return A ByteBuffer that contaons the PDU
     * @throws EncoderException If anything goes wrong.
     */
    ByteBuffer encode( ByteBuffer buffer ) throws EncoderException;

    
    /**
     * Compute the Control length
     * <pre> 
     * Control :
     * 
     * 0x30 L1
     *  |
     *  +--> 0x04 L2 controlType
     * [+--> 0x01 0x01 criticality]
     * [+--> 0x04 L3 controlValue] 
     * 
     * Control length = Length(0x30) + length(L1) 
     *                  + Length(0x04) + Length(L2) + L2
     *                  [+ Length(0x01) + 1 + 1]
     *                  [+ Length(0x04) + Length(L3) + L3]
     * </pre>
     */
    int computeLength();

    /**
     * Get the associated decoder
     *
     * @return The Control decoder
     */
    ControlDecoder getDecoder();
}

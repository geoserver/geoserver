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

package org.apache.directory.shared.ldap.message.spi;


import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.codec.stateful.StatefulEncoder;


/**
 * Standard encoder service provider interface. BER encoders of ASN.1 compiler
 * stubs are expected to implement this interface.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public interface ProviderEncoder extends ProviderObject, StatefulEncoder
{
    /**
     * Encodes a compiler stub specific ASN.1 message envelope containment tree
     * onto an output stream.
     * 
     * @param lock
     *            lock object used to exclusively write to the output stream
     * @param out
     *            the OutputStream to encode the message envelope onto.
     * @param obj
     *            the top-level message envelope stub instance, i.e. for the
     *            Snacc4J service provider this would be an instance of the
     *            LDAPMessage compiler generated stub class.
     * @throws ProviderException
     *             to indicate an error while attempting to encode the message
     *             envelope onto the output stream. Provider specific exceptions
     *             encountered while encoding can be held within this subclass
     *             of MultiException.
     */
    void encodeBlocking( Object lock, OutputStream out, Object obj ) throws ProviderException;


    /**
     * Encodes a compiler stub specific ASN.1 message envelope containment tree
     * into byte array.
     * 
     * @param obj
     *            the top-level message envelope stub instance, i.e. for the
     *            Snacc4J service provider this would be an instance of the
     *            LDAPMessage compiler generated stub class.
     * @return the encoded object in a byte buffer
     * @throws ProviderException
     *             to indicate an error while attempting to encode the message
     *             envelope into a byte buffer. Provider specific exceptions
     *             encountered while encoding can be held within this subclass
     *             of MultiException.
     */
    ByteBuffer encodeBlocking( Object obj ) throws ProviderException;
}

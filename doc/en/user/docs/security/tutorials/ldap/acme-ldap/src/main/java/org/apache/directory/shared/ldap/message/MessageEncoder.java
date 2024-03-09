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
package org.apache.directory.shared.ldap.message;


import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.codec.stateful.EncoderCallback;
import org.apache.directory.shared.asn1.codec.stateful.EncoderMonitor;
import org.apache.directory.shared.ldap.codec.LdapTransformer;
import org.apache.directory.shared.ldap.message.internal.InternalMessage;
import org.apache.directory.shared.ldap.message.spi.Provider;
import org.apache.directory.shared.ldap.message.spi.ProviderEncoder;
import org.apache.directory.shared.ldap.message.spi.ProviderException;


/**
 * Encodes a Message instance into a binary message envelope using Basic
 * Encoding rules flushing the PDU out to an OutputStream.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 905344 $
 */
public final class MessageEncoder implements ProviderEncoder
{
    /** the provider */
    private final Provider provider;

    /** the provider's encoder */
    private final ProviderEncoder encoder;


    /**
     * Creates a MessageEncoder using default properties for enabling a BER
     * library provider.
     * 
     * @throws MessageException if the encoder cannot be created.
     */
    public MessageEncoder() throws MessageException
    {
        this.provider = Provider.getProvider( Provider.getEnvironment() );
        this.encoder = provider.getEncoder();
    }


    // ------------------------------------------------------------------------
    // ProviderEncoder
    // ------------------------------------------------------------------------

    /**
     * @see ProviderEncoder#encodeBlocking(Object, java.io.OutputStream, Object)
     */
    public void encodeBlocking( Object lock, OutputStream out, Object obj ) throws ProviderException
    {
        // transform to build provider specific intermediate envelope
        Object providerEnvelope = LdapTransformer.transform( ( InternalMessage ) obj );

        // now encode provider's intermediate stub into a PDU onto stream
        this.encoder.encodeBlocking( lock, out, providerEnvelope );
    }


    /**
     * @see ProviderEncoder#encodeBlocking(Object)
     */
    public ByteBuffer encodeBlocking( Object obj ) throws ProviderException
    {
        // transform to build provider specific intermediate envelope
        Object providerEnvelope = LdapTransformer.transform( ( InternalMessage ) obj );

        // now encode provider's intermediate stub into PDU in a byte buffer
        return this.encoder.encodeBlocking( providerEnvelope );
    }


    // ------------------------------------------------------------------------
    // ProviderObject Methods
    // ------------------------------------------------------------------------

    /**
     * @see org.apache.directory.shared.ldap.message.spi.ProviderObject#getProvider()
     */
    public Provider getProvider()
    {
        return this.provider;
    }


    // ------------------------------------------------------------------------
    // StatefulEncoder Methods
    // ------------------------------------------------------------------------

    /**
     * Encodes a Message object piece by piece often emitting chunks of the
     * final PDU to the callback if present.
     * 
     * @param obj the message object to encode into a PDU
     * @throws EncoderException if there are problems while encoding
     */
    public void encode( Object obj ) throws EncoderException
    {
        // transform to build provider specific intermediate envelope
        Object providerEnvelope = LdapTransformer.transform( ( InternalMessage ) obj );

        // now give intermediate envelope to provider's encoder
        this.encoder.encode( providerEnvelope );
    }


    /**
     * Sets the callback of the underlying implementation. There is no need for
     * any special callbacks because when encoding we do not need to transform
     * before a value return as we did in the decoder.
     * 
     * @param cb the callback to set on the underlying provider specific encoder
     */
    public void setCallback( EncoderCallback cb )
    {
        this.encoder.setCallback( cb );
    }


    /**
     * Sets the monitor of the underlying implementation.
     * 
     * @param monitor the monitor to set on the underlying implementation
     */
    public void setEncoderMonitor( EncoderMonitor monitor )
    {
        this.encoder.setEncoderMonitor( monitor );
    }
}

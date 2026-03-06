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


import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.stateful.DecoderCallback;
import org.apache.directory.shared.asn1.codec.stateful.DecoderMonitor;
import org.apache.directory.shared.asn1.codec.stateful.StatefulDecoder;
import org.apache.directory.shared.ldap.codec.ResponseCarryingException;
import org.apache.directory.shared.ldap.codec.LdapTransformer;
import org.apache.directory.shared.ldap.message.spi.BinaryAttributeDetector;
import org.apache.directory.shared.ldap.message.spi.Provider;
import org.apache.directory.shared.ldap.message.spi.ProviderDecoder;

import java.io.InputStream;
import java.util.Hashtable;


/**
 * Decodes a BER encoded LDAPv3 message envelope from an input stream
 * demarshaling it into a Message instance using a BER library provider.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 903719 $
 */
public final class MessageDecoder implements ProviderDecoder
{
    /** the ASN.1 provider */
    private final Provider provider;

    /** the ASN.1 provider's decoder */
    private final ProviderDecoder decoder;

    /** the Message decode operation callback */
    private DecoderCallback cb;


    /**
     * Creates a MessageDecoder using default properties for enabling a BER
     * library provider.
     * 
     * @param binaryAttributeDetector detects whether or not an attribute is binary
     * @throws MessageException if there is a problem creating this decoder.
     */
    public MessageDecoder( BinaryAttributeDetector binaryAttributeDetector ) throws MessageException
    {
        this( binaryAttributeDetector, Integer.MAX_VALUE );
    }
    
    
    /**
     * Creates a MessageDecoder using default properties for enabling a BER
     * library provider.
     * 
     * @param binaryAttributeDetector detects whether or not an attribute is binary
     * @param maxPDUSize the maximum size a PDU can be
     * @throws MessageException if there is a problem creating this decoder.
     */
    public MessageDecoder( BinaryAttributeDetector binaryAttributeDetector, int maxPDUSize ) throws MessageException
    {
        // We need to get the encoder class name
        Hashtable<Object, Object> providerEnv = Provider.getEnvironment();
        
        this.provider = Provider.getProvider( providerEnv );
        this.decoder = this.provider.getDecoder( binaryAttributeDetector, maxPDUSize );
        this.decoder.setCallback( new DecoderCallback()
        {
            public void decodeOccurred( StatefulDecoder decoder, Object decoded )
            {
                if ( decoded instanceof Asn1Object )
                {
                    cb.decodeOccurred( decoder, LdapTransformer.transform( decoded ) );
                }
                else
                {
                    cb.decodeOccurred( decoder, decoded );
                }
            }
        } );
    }


    /**
     * Reads and decodes a BER encoded LDAPv3 ASN.1 message envelope structure
     * from an input stream to build a fully populated Message object instance.
     * 
     * @param lock
     *            lock object used to exclusively read from the input stream
     * @param in
     *            the input stream to read PDU data from.
     * @return the populated Message representing the PDU envelope.
     * @throws MessageException
     *             if there is a problem decoding.
     */
    public Object decode( final Object lock, final InputStream in ) throws MessageException
    {
        Object providerEnvelope;

        try
        {
            if ( lock == null )
            {
                // Complain here somehow first then do the following w/o synch!
    
                // Call provider decoder to demarshall PDU into berlib specific form
                providerEnvelope = decoder.decode( lock, in );
            }
            else
            {
                synchronized ( lock )
                {
                    // Same as above but a synchronized read using valid lock object
                    providerEnvelope = decoder.decode( lock, in );
                    lock.notifyAll();
                }
            }
        }
        catch ( Exception e )
        {
            throw ( MessageException ) e;
        }

        // Call on transformer to convert stub based PDU into Message based PDU
        return LdapTransformer.transform( providerEnvelope );
    }


    /**
     * Decodes a chunk of stream data returning any resultant decoded PDU via a
     * callback.
     * 
     * @param chunk
     *            the chunk to decode
     * @throws MessageException
     *             if there are failures while decoding the chunk
     */
    public void decode( Object chunk ) throws MessageException
    {
        try
        {
            decoder.decode( chunk );
        }
        catch ( DecoderException e )
        {
            // transform the DecoderException message to a MessageException
            if ( e instanceof ResponseCarryingException ) 
            {
                ResponseCarryingMessageException rcme = new ResponseCarryingMessageException( e.getMessage() );
                rcme.setResponse( ((ResponseCarryingException)e).getResponse() );
                
                throw rcme;
            }
            else
            {
                // TODO : this is certainly not the way we should handle such an exception !
                throw new ResponseCarryingMessageException( e.getMessage() );
            }
        }
    }


    /**
     * Sets the callback used to deliver completly decoded PDU's.
     * 
     * @param cb
     *            the callback to use for decoded PDU delivery
     */
    public void setCallback( DecoderCallback cb )
    {
        this.cb = cb;
    }


    /**
     * Sets the monitor for this MessageDecoder which receives callbacks for
     * noteworthy events during decoding.
     * 
     * @param monitor
     *            the monitor to receive notifications via callback events
     */
    public void setDecoderMonitor( DecoderMonitor monitor )
    {
        decoder.setDecoderMonitor( monitor );
    }


    public Provider getProvider()
    {
        return this.provider;
    }
}

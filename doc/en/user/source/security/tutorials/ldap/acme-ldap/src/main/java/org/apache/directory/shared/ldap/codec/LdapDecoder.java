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


import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.tlv.TLVStateEnum;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.stateful.DecoderCallback;
import org.apache.directory.shared.asn1.codec.stateful.DecoderMonitor;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.message.spi.BinaryAttributeDetector;
import org.apache.directory.shared.ldap.message.spi.Provider;
import org.apache.directory.shared.ldap.message.spi.ProviderDecoder;
import org.apache.directory.shared.ldap.message.spi.ProviderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The LdapDecoder decodes ASN.1 BER encoded PDUs.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class LdapDecoder implements ProviderDecoder
{
    /** The logger */
    private static Logger log = LoggerFactory.getLogger( LdapDecoder.class );

    /** A speedup for logger */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The associated Provider */
    private final Provider provider;

    /** The message container for this instance */
    private final LdapMessageContainer ldapMessageContainer;

    /** The Ldap BDER decoder instance */
    private final Asn1Decoder ldapDecoder;

    /** The callback to call when the decoding is done */
    private DecoderCallback decoderCallback;


    /**
     * Creates an instance of a Ldap Decoder implementation.
     * 
     * @param provider the owning provider.
     * @param binaryAttributeDetector checks for binary attributes 
     * @param maxPDUSize the maximum size a PDU can be
     */
    public LdapDecoder( Provider provider, BinaryAttributeDetector binaryAttributeDetector, int maxPDUSize )
    {
        this.provider = provider;
        ldapMessageContainer = new LdapMessageContainer( binaryAttributeDetector );
        ldapDecoder = new Asn1Decoder();
        
        ldapMessageContainer.setMaxPDUSize( maxPDUSize );
    }


    /**
     * Decodes a PDU
     * 
     * @param encoded The PDU containing the LdapMessage to decode
     * @throws DecoderException if anything goes wrong
     */
    public void decode( Object encoded ) throws DecoderException
    {
        ByteBuffer buf;
        int position = 0;

        if ( encoded instanceof ByteBuffer )
        {
            buf = ( ByteBuffer ) encoded;
        }
        else if ( encoded instanceof byte[] )
        {
            buf = ByteBuffer.wrap( ( byte[] ) encoded );
        }
        else
        {
            throw new DecoderException( I18n.err( I18n.ERR_04059, encoded.getClass() ) );
        }

        while ( buf.hasRemaining() )
        {
            try
            {
                ldapDecoder.decode( buf, ldapMessageContainer );
    
                if ( IS_DEBUG )
                {
                    log.debug( "Decoding the PDU : " );
    
                    int size = buf.position();
                    buf.flip();
                    
                    byte[] array = new byte[ size - position ];
                    
                    for ( int i = position; i < size; i++ )
                    {
                        array[ i ] = buf.get();
                    }
    
                    position = size;
                    
                    log.debug( StringTools.dumpBytes( array ) );
                }
                
                if ( ldapMessageContainer.getState() == TLVStateEnum.PDU_DECODED )
                {
                    if ( IS_DEBUG )
                    {
                        log.debug( "Decoded LdapMessage : " + ldapMessageContainer.getLdapMessage() );
                        buf.mark();
                    }
    
                    decoderCallback.decodeOccurred( null, ldapMessageContainer.getLdapMessage() );
                    ldapMessageContainer.clean();
                }
            }
            catch ( DecoderException de )
            {
                buf.clear();
                ldapMessageContainer.clean();
                throw de;
            }
        }
    }


    /**
     * Feeds the bytes within the input stream to the digester to generate the
     * resultant decoded Message.
     * 
     * @param in The InputStream containing the PDU to be decoded
     * @throws ProviderException If the decoding went wrong
     */
    private void digest( InputStream in ) throws ProviderException
    {
        byte[] buf;

        try
        {
            int amount;

            while ( in.available() > 0 )
            {
                buf = new byte[in.available()];

                if ( ( amount = in.read( buf ) ) == -1 )
                {
                    break;
                }

                ldapDecoder.decode( ByteBuffer.wrap( buf, 0, amount ), ldapMessageContainer );
            }
        }
        catch ( Exception e )
        {
            log.error( I18n.err( I18n.ERR_04060, e.getLocalizedMessage() ) );
            ProviderException pe = new ProviderException( provider, I18n.err( I18n.ERR_04061 ) );
            pe.addThrowable( e );
            throw pe;
        }
    }


    /**
     * Decodes a PDU from an input stream into a Snickers compiler generated
     * stub envelope.
     * 
     * @param lock Lock object used to exclusively read from the input stream
     * @param in The input stream to read and decode PDU bytes from
     * @return return decoded stub
     */
    public Object decode( Object lock, InputStream in ) throws ProviderException
    {
        if ( lock == null )
        {
            digest( in );

            if ( ldapMessageContainer.getState() == TLVStateEnum.PDU_DECODED )
            {
                if ( IS_DEBUG )
                {
                    log.debug( "Decoded LdapMessage : " + ldapMessageContainer.getLdapMessage() );
                }

                return ldapMessageContainer.getLdapMessage();
            }
            else
            {
                log.error( I18n.err( I18n.ERR_04062 ) );
                ProviderException pe = new ProviderException( provider, I18n.err( I18n.ERR_04061 ) );
                //noinspection ThrowableInstanceNeverThrown
                pe.addThrowable( new DecoderException( I18n.err( I18n.ERR_04063 ) ) );
                throw pe;
            }
        }
        else
        {
            try
            {
                // Synchronize on the input lock object to prevent concurrent
                // reads
                synchronized ( lock )
                {
                    digest( in );

                    // Notify/awaken threads waiting to read from input stream
                    lock.notifyAll();
                }
            }
            catch ( Exception e )
            {
                log.error( I18n.err( I18n.ERR_04060, e.getLocalizedMessage() ) );
                ProviderException pe = new ProviderException( provider, I18n.err( I18n.ERR_04061 ) );
                pe.addThrowable( e );
                throw pe;
            }

            if ( ldapMessageContainer.getState() == TLVStateEnum.PDU_DECODED )
            {
                if ( IS_DEBUG )
                {
                    log.debug( "Decoded LdapMessage : " + ldapMessageContainer.getLdapMessage() );
                }

                return ldapMessageContainer.getLdapMessage();
            }
            else
            {
                log.error( I18n.err( I18n.ERR_04064 ) );
                ProviderException pe = new ProviderException( provider, I18n.err( I18n.ERR_04062 ) );
                //noinspection ThrowableInstanceNeverThrown
                pe.addThrowable( new DecoderException( I18n.err( I18n.ERR_04063 ) ) );
                throw pe;
            }
        }
    }


    /**
     * Gets the Provider that this Decoder implementation is part of.
     * 
     * @return the owning provider.
     */
    public Provider getProvider()
    {
        return provider;
    }


    /**
     * Not used ...
     * 
     * @deprecated
     */
    public void setDecoderMonitor( DecoderMonitor monitor )
    {
    }


    /**
     * Set the callback to call when the PDU has been decoded
     * 
     * @param cb The callback
     */
    public void setCallback( DecoderCallback cb )
    {
        decoderCallback = cb;
    }
}

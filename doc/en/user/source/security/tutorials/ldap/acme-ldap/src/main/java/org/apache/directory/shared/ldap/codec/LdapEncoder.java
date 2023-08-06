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


import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.asn1.codec.stateful.EncoderCallback;
import org.apache.directory.shared.asn1.codec.stateful.EncoderMonitor;
import org.apache.directory.shared.asn1.codec.stateful.StatefulEncoder;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.add.AddRequestCodec;
import org.apache.directory.shared.ldap.message.spi.Provider;
import org.apache.directory.shared.ldap.message.spi.ProviderEncoder;
import org.apache.directory.shared.ldap.message.spi.ProviderException;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * LDAP BER provider's encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $
 */
public class LdapEncoder implements ProviderEncoder
{
    //TM private static long cumul = 0L;
    //TM private static long count = 0L;
    //TM private Object lock = new Object();

    /** The logger */
    private static Logger log = LoggerFactory.getLogger( LdapEncoder.class );

    /** A speedup for logger */
    private static final boolean IS_DEBUG = log.isDebugEnabled();
    
    /** The associated Provider */
    final Provider provider;

    /** The callback to call when the encoding is done */
    private EncoderCallback encodeCallback;


    /**
     * Creates an instance of a Ldap Encoder implementation.
     * 
     * @param provider The associated Provider
     */
    public LdapEncoder( Provider provider )
    {
        this.provider = provider;
        encodeCallback = new OutputCallback();
    }


    /**
     * Encodes a LdapMessage, and calls the callback.
     * 
     * @param lock Not used...
     * @param out Not used ...
     * @param obj The LdapMessage to encode
     * @throws ProviderException If anything went wrong
     */
    public void encodeBlocking( Object lock, OutputStream out, Object obj ) throws ProviderException
    {
        try
        {
            if ( IS_DEBUG )
            {
                log.debug( "Encoding this LdapMessage : " + obj );
            }

            ( ( OutputCallback ) encodeCallback ).attach( out );
            encodeCallback.encodeOccurred( null, ( ( LdapMessageCodec ) obj ).encode() );
        }
        catch ( EncoderException e )
        {
            log.error( I18n.err( I18n.ERR_04065, obj, e.getLocalizedMessage() ) );
            ProviderException pe = new ProviderException( provider, I18n.err( I18n.ERR_04065, obj, e.getLocalizedMessage() ) );
            throw pe;
        }
    }


    /**
     * Encodes a LdapMessage, and return a ByteBuffer containing the resulting
     * PDU
     * 
     * @param obj The LdapMessage to encode
     * @return The ByteBuffer containing the PDU
     * @throws ProviderException If anything went wrong
     */
    public ByteBuffer encodeBlocking( Object obj ) throws ProviderException
    {
        try
        {
            if ( IS_DEBUG )
            {
                log.debug( "Encoding this LdapMessage : " + obj );
            }

            ByteBuffer pdu = ( ( LdapMessageCodec ) obj ).encode();

            if ( IS_DEBUG )
            {
                log.debug( "Encoded PDU : " + StringTools.dumpBytes( pdu.array() ) );
            }

            pdu.flip();
            return pdu;
        }
        catch ( EncoderException e )
        {
            log.error( I18n.err( I18n.ERR_04065, obj, e.getLocalizedMessage() ) );
            ProviderException pe = new ProviderException( provider, I18n.err( I18n.ERR_04065, obj, e.getLocalizedMessage() ) );
            throw pe;
        }
    }


    /**
     * Encodes a LdapMessage, and return a byte array containing the resulting
     * PDU
     * 
     * @param obj The LdapMessage to encode
     * @return The byte[] containing the PDU
     * @throws ProviderException If anything went wrong
     */
    public byte[] encodeToArray( Object obj ) throws ProviderException
    {
        try
        {
            if ( IS_DEBUG )
            {
                log.debug( "Encoding this LdapMessage : " + obj );
            }

            byte[] pdu = ( ( LdapMessageCodec ) obj ).encode().array();

            if ( IS_DEBUG )
            {
                log.debug( "Encoded PDU : " + StringTools.dumpBytes( pdu ) );
            }

            return pdu;
        }
        catch ( EncoderException e )
        {
            log.error( I18n.err( I18n.ERR_04065,  obj, e.getLocalizedMessage() ) );
            ProviderException pe = new ProviderException( provider, I18n.err( I18n.ERR_04065, obj, e.getLocalizedMessage() ) );
            throw pe;
        }
    }


    /**
     * Gets the Provider associated with this SPI implementation object.
     * 
     * @return Provider The provider
     */
    public Provider getProvider()
    {
        return provider;
    }


    /**
     * Encodes a LdapMessage, and calls the callback
     * 
     * @param obj The LdapMessage to encode
     * @throws EncoderException If anything went wrong
     */
    public void encode( Object obj ) throws EncoderException
    {
        //TM long t0 = System.nanoTime();
        ByteBuffer encoded = encodeBlocking( obj );
        encodeCallback.encodeOccurred( null, encoded );
        //TM long t1 = System.nanoTime();
        
        //TM synchronized (lock)
        //TM {
        //TM     cumul += (t1 - t0);
        //TM     count++;
        //TM    
        //TM
        //TM     if ( count % 1000L == 0)
        //TM     {
        //TM         System.out.println( "Encode cost : " + (cumul/count) );
        //TM         cumul = 0L;
        //TM     }
        //TM }
    }


    /**
     * Set the callback called when the encoding is done.
     * 
     * @param cb The callback.
     */
    public void setCallback( EncoderCallback cb )
    {
        encodeCallback = cb;
    }


    /**
     * Not used ...
     * 
     * @deprecated
     */
    public void setEncoderMonitor( EncoderMonitor monitor )
    {
    }

    /**
     * The inner class used to write the PDU to a channel.
     */
    class OutputCallback implements EncoderCallback
    {
        /** The channel in which the PDU will be written */
        private WritableByteChannel channel = null;


        /**
         * Callback to deliver a fully encoded object.
         * 
         * @param encoder the stateful encoder driving the callback
         * @param encoded the object that was encoded
         */
        public void encodeOccurred( StatefulEncoder encoder, Object encoded )
        {
            try
            {
                ( ( ByteBuffer ) encoded ).flip();
                channel.write( ( ByteBuffer ) encoded );
            }
            catch ( IOException e )
            {
                ProviderException pe = new ProviderException( provider, I18n.err( I18n.ERR_04065, "", e.getLocalizedMessage() ) );
                throw pe;
            }
        }


        /**
         * Associate a channel to the callback
         * 
         * @param channel The channel to use to write a PDU
         */
        void attach( WritableByteChannel channel )
        {
            this.channel = channel;
        }


        /**
         * Associate a OutputStream to the callback. A channel will be created.
         * 
         * @param out The OutputStream to use
         */
        void attach( OutputStream out )
        {
            this.channel = Channels.newChannel( out );
        }
    }
    
    
    private static ByteBuffer encodeAsn1( AddRequestCodec addRequest )
    {
        //int length = computeLength( addRequest );
        return null;
    }
    
    
    private static ByteBuffer encodeAsn1( LdapMessageCodec message ) throws EncoderException
    {
        ByteBuffer buffer = null;
        
        switch ( message.getMessageType() )
        {
            case ABANDON_REQUEST :
                return encodeAsn1( message );
                
            case ADD_REQUEST :
            case ADD_RESPONSE :
            case BIND_REQUEST :
            case BIND_RESPONSE :
            case COMPARE_REQUEST :
            case COMPARE_RESPONSE :
            case DEL_REQUEST :
            case DEL_RESPONSE :
            case EXTENDED_REQUEST :
            case EXTENDED_RESPONSE :
            case INTERMEDIATE_RESPONSE :
            case MODIFY_REQUEST :
            case MODIFY_RESPONSE :
            case MODIFYDN_REQUEST :
            case MODIFYDN_RESPONSE :
            case SEARCH_REQUEST :
            case SEARCH_RESULT_DONE :
            case SEARCH_RESULT_ENTRY :
            case SEARCH_RESULT_REFERENCE  :
            case UNBIND_REQUEST :
            
        }
        
        return buffer;
    }
}

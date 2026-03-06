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
package org.apache.directory.shared.ldap.message.extended;


import javax.naming.NamingException;
import javax.naming.ldap.ExtendedResponse;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.extended.operations.gracefulShutdown.GracefulShutdown;
import org.apache.directory.shared.ldap.codec.extended.operations.gracefulShutdown.GracefulShutdownDecoder;
import org.apache.directory.shared.ldap.message.ExtendedRequestImpl;
import org.apache.directory.shared.ldap.message.internal.InternalResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An extended operation requesting the server to shutdown it's LDAP service
 * port while allowing established clients to complete or abandon operations
 * already in progress. More information about this extended request is
 * available here: <a href="ahttp://docs.safehaus.org:8080/x/GR">LDAP Extensions
 * for Graceful Shutdown</a>.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912436 $
 */
public class GracefulShutdownRequest extends ExtendedRequestImpl
{
    private static final Logger log = LoggerFactory.getLogger( GracefulShutdownRequest.class );

    private static final long serialVersionUID = -4682291068700593492L;

    public static final String EXTENSION_OID = "1.3.6.1.4.1.18060.0.1.3";

    /** Undetermined value used for timeOffline */
    public static final int UNDETERMINED = 0;

    /** The shutdown is immediate */
    public static final int NOW = 0;

    /** offline Time after disconnection */
    private int timeOffline;

    /** Delay before disconnection */
    private int delay;


    public GracefulShutdownRequest(int messageId)
    {
        this( messageId, UNDETERMINED, NOW );
    }


    public GracefulShutdownRequest(int messageId, int timeOffline, int delay)
    {
        super( messageId );
        setOid( EXTENSION_OID );
        this.timeOffline = timeOffline;
        this.delay = delay;
    }


    private void encodePayload() throws EncoderException
    {
        GracefulShutdown gs = new GracefulShutdown();
        gs.setDelay( this.delay );
        gs.setTimeOffline( this.timeOffline );
        payload = gs.encode().array();
    }


    public void setPayload( byte[] payload )
    {
        GracefulShutdownDecoder decoder = new GracefulShutdownDecoder();
        try
        {
            GracefulShutdown gs = ( GracefulShutdown ) decoder.decode( payload );
            if ( payload != null )
            {
                this.payload = new byte[ payload.length ];
                System.arraycopy( payload, 0, this.payload, 0, payload.length );
            } else {
                this.payload = null;
            }
            this.timeOffline = gs.getTimeOffline();
            this.delay = gs.getDelay();
        }
        catch ( DecoderException e )
        {
            log.error( I18n.err( I18n.ERR_04165 ), e );
            throw new RuntimeException( e );
        }
    }


    public ExtendedResponse createExtendedResponse( String id, byte[] berValue, int offset, int length )
        throws NamingException
    {
        return ( ExtendedResponse ) getResultResponse();
    }


    public byte[] getEncodedValue()
    {
        return getPayload();
    }


    public byte[] getPayload()
    {
        if ( payload == null )
        {
            try
            {
                encodePayload();
            }
            catch ( EncoderException e )
            {
                log.error( I18n.err( I18n.ERR_04164 ), e );
                throw new RuntimeException( e );
            }
        }

        return super.getPayload();
    }


    public InternalResultResponse getResultResponse()
    {
        if ( response == null )
        {
            GracefulShutdownResponse gsr = new GracefulShutdownResponse( getMessageId() );
            response = gsr;
        }

        return response;
    }


    // -----------------------------------------------------------------------
    // Parameters of the Extended Request Payload
    // -----------------------------------------------------------------------

    public int getDelay()
    {
        return delay;
    }


    public void setDelay( int delay )
    {
        this.delay = delay;
    }


    public int getTimeOffline()
    {
        return timeOffline;
    }


    public void setTimeOffline( int timeOffline )
    {
        this.timeOffline = timeOffline;
    }
}

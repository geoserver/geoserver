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
package org.apache.directory.shared.ldap.codec.bind;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A ldapObject which stores the Simple authentication for a BindRequest.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class SimpleAuthentication extends LdapAuthentication
{
    /** The logger */
    private static Logger log = LoggerFactory.getLogger( SimpleAuthentication.class );

    /** A speedup for logger */
    private static final boolean IS_DEBUG = log.isDebugEnabled();
    
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The simple authentication password */
    private byte[] simple;


    /**
     * @see Asn1Object#Asn1Object
     */
    public SimpleAuthentication()
    {
        super();
    }

    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get the simple password
     * 
     * @return The password
     */
    public byte[] getSimple()
    {
        if ( simple == null )
        {
            return null;
        }

        final byte[] copy = new byte[ simple.length ];
        System.arraycopy( simple, 0, copy, 0, simple.length );
        return copy;
    }


    /**
     * Set the simple password
     * 
     * @param simple The simple password
     */
    public void setSimple( byte[] simple )
    {
        if ( simple != null )
        {
            this.simple = new byte[ simple.length ];
            System.arraycopy( simple, 0, this.simple, 0, simple.length );
        } else {
            this.simple = null;
        }
    }


    /**
     * Compute the Simple authentication length 
     * 
     * Simple authentication : 0x80 L1 simple 
     * 
     * L1 = Length(simple) 
     * Length(Simple authentication) = Length(0x80) + Length(L1) + Length(simple)
     */
    public int computeLength()
    {
        int length = 1;

        length += TLV.getNbBytes( simple.length ) + simple.length;

        if ( IS_DEBUG )
        {
            log.debug( "Simple Authentication length : {}", Integer.valueOf( length ) );
        }

        return length;
    }


    /**
     * Encode the simple authentication to a PDU. 
     * 
     * SimpleAuthentication : 0x80 LL simple
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            log.error( I18n.err( I18n.ERR_04023 ) );
            throw new EncoderException( I18n.err( I18n.ERR_04023 ) );
        }

        try
        {
            // The simpleAuthentication Tag
            buffer.put( ( byte ) LdapConstants.BIND_REQUEST_SIMPLE_TAG );
            buffer.put( TLV.getBytes( simple.length ) );

            if ( simple.length != 0 )
            {
                buffer.put( simple );
            }
        }
        catch ( BufferOverflowException boe )
        {
            log.error( I18n.err( I18n.ERR_04005 ) );
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        return buffer;
    }


    /**
     * Return the simple authentication as a string
     * 
     * @return The simple authentication string.
     */
    public String toString()
    {
        return ( ( simple == null ) ? "null" : StringTools.dumpBytes( simple) );
    }
}

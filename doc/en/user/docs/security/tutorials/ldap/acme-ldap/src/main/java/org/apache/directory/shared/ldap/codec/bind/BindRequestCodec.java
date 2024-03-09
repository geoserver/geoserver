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
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A BindRequest ldapObject. It's a sub-class of Asn1Object, and it extends the
 * Asn1Object class to be seen as a member of the LdapMessage CHOICE.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 921600 $, $Date: 2010-03-11 00:37:30 +0200 (Thu, 11 Mar 2010) $, 
 */
public class BindRequestCodec extends LdapMessageCodec
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The protocol Version to use. Should be 3 */
    private int version;

    /** The name of the user requesting a bind */
    private DN name;

    /** The authentication used to bind the user */
    private LdapAuthentication authentication;

    /** The bind request length */
    private int bindRequestLength;

    /**
     * Creates a new BindRequest object.
     */
    public BindRequestCodec()
    {
        super();
    }

    /**
     * Get the message type
     * 
     * @return Returns the type.
     */
    public MessageTypeEnum getMessageType()
    {
        return MessageTypeEnum.BIND_REQUEST;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getMessageTypeName()
    {
        return "BIND_REQUEST";
    }


    /**
     * Get the user authentication
     * 
     * @return The user authentication
     */
    public LdapAuthentication getAuthentication()
    {
        return authentication;
    }


    /**
     * Get the user simple authentication
     * 
     * @return The simple user authentication
     */
    public SimpleAuthentication getSimpleAuthentication()
    {
        return ( SimpleAuthentication ) authentication;
    }


    /**
     * Get the user sasl authentication
     * 
     * @return The sasl user authentication
     */
    public SaslCredentials getSaslAuthentication()
    {
        return ( SaslCredentials ) authentication;
    }


    /**
     * Set the user authentication
     * 
     * @param authentication The user authentication
     */
    public void setAuthentication( LdapAuthentication authentication )
    {
        this.authentication = authentication;
    }


    /**
     * Get the user name
     * 
     * @return The user name
     */
    public DN getName()
    {
        return name;
    }


    /**
     * Set the user name
     * 
     * @param name The user name
     */
    public void setName( DN name )
    {
        this.name = name;
    }


    /**
     * Get the protocol version
     * 
     * @return The protocol version
     */
    public int getVersion()
    {
        return version;
    }


    /**
     * Check if the Ldap version in use is 3
     * 
     * @return true if the ldap version is 3
     */
    public boolean isLdapV3()
    {
        return version == 3;
    }


    /**
     * Set the protocol version
     * 
     * @param version The protocol version
     */
    public void setVersion( int version )
    {
        this.version = version;
    }


    /**
     * Compute the BindRequest length 
     * 
     * BindRequest : 
     * <pre>
     * 0x60 L1 
     *   | 
     *   +--> 0x02 0x01 (1..127) version 
     *   +--> 0x04 L2 name 
     *   +--> authentication 
     *   
     * L2 = Length(name)
     * L3/4 = Length(authentication) 
     * Length(BindRequest) = Length(0x60) + Length(L1) + L1 + Length(0x02) + 1 + 1 + 
     *      Length(0x04) + Length(L2) + L2 + Length(authentication)
     * </pre>
     */
    protected int computeLengthProtocolOp()
    {
        bindRequestLength = 1 + 1 + 1; // Initialized with version

        // The name
        bindRequestLength += 1 + TLV.getNbBytes( DN.getNbBytes( name ) ) + DN.getNbBytes( name );

        // The authentication
        bindRequestLength += authentication.computeLength();

        // Return the result.
        return 1 + TLV.getNbBytes( bindRequestLength ) + bindRequestLength;
    }


    /**
     * Encode the BindRequest message to a PDU. 
     * 
     * BindRequest : 
     * <pre>
     * 0x60 LL 
     *   0x02 LL version         0x80 LL simple 
     *   0x04 LL name           /   
     *   authentication.encode() 
     *                          \ 0x83 LL mechanism [0x04 LL credential]
     * </pre>
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    protected void encodeProtocolOp( ByteBuffer buffer ) throws EncoderException
    {
        try
        {
            // The BindRequest Tag
            buffer.put( LdapConstants.BIND_REQUEST_TAG );
            buffer.put( TLV.getBytes( bindRequestLength ) );

        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        // The version
        Value.encode( buffer, version );

        // The name
        Value.encode( buffer, DN.getBytes( name ) );

        // The authentication
        authentication.encode( buffer );
    }


    /**
     * Get a String representation of a BindRequest
     * 
     * @return A BindRequest String
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "    BindRequest\n" );
        sb.append( "        Version : '" ).append( version ).append( "'\n" );

        if ( ( null == name ) || StringTools.isEmpty( name.getNormName() ) )
        {
            sb.append( "        Name : anonymous" );
        }
        else
        {
            sb.append( "        Name : '" ).append( name ).append( "'\n" );

            if ( authentication instanceof SimpleAuthentication )
            {
                sb.append( "        Simple authentication : '" ).append( authentication ).append( '\'' );
            }
            else
            {
                sb.append( authentication );
            }
        }

        return toString( sb.toString() );
    }

    /* Used only for test perfs
    public static void main( String[] args ) throws Exception
    {
        Asn1Decoder ldapDecoder = new LdapDecoder();

        ByteBuffer stream = ByteBuffer.allocate( 0x52 );
        stream.put( new byte[]
             { 
             0x30, 0x50,                 // LDAPMessage ::=SEQUENCE {
               0x02, 0x01, 0x01,         // messageID MessageID
               0x60, 0x2E,               // CHOICE { ..., bindRequest BindRequest, ...
                                         // BindRequest ::= APPLICATION[0] SEQUENCE {
                 0x02, 0x01, 0x03,       // version INTEGER (1..127),
                 0x04, 0x1F,             // name LDAPDN,
                 'u', 'i', 'd', '=', 'a', 'k', 'a', 'r', 'a', 's', 'u', 'l', 'u', ',', 'd', 'c', '=', 'e', 'x', 'a',
                 'm', 'p', 'l', 'e', ',', 'd', 'c', '=', 'c', 'o', 'm', 
                 ( byte ) 0x80, 0x08,    // authentication AuthenticationChoice
                                         // AuthenticationChoice ::= CHOICE { simple [0] OCTET STRING,
                                         // ...
                   'p', 'a', 's', 's', 'w', 'o', 'r', 'd', 
               ( byte ) 0xA0, 0x1B, // A control
                 0x30, 0x19, 
                   0x04, 0x17, 
                     0x32, 0x2E, 0x31, 0x36, 0x2E, 0x38, 0x34, 0x30, 0x2E, 0x31, 0x2E, 0x31, 0x31, 0x33, 0x37, 0x33, 
                     0x30, 0x2E, 0x33, 0x2E, 0x34, 0x2E, 0x32 
             } );

        stream.flip();

        // Allocate a LdapMessage Container
        IAsn1Container ldapMessageContainer = new LdapMessageContainer();

        // Decode the BindRequest PDU
        try
        {
            long t0 = System.currentTimeMillis();
            for ( int i = 0; i < 10000000; i++ )
            {
                ldapDecoder.decode( stream, ldapMessageContainer );
                ( ( LdapMessageContainer ) ldapMessageContainer).clean();
                stream.flip();
            }
            long t1 = System.currentTimeMillis();
            System.out.println( "Delta = " + ( t1 - t0 ) );
            
            ldapDecoder.decode( stream, ldapMessageContainer );
        }
        catch ( DecoderException de )
        {
            de.printStackTrace();
        }
        catch ( NamingException ne )
        {
            ne.printStackTrace();
        }
    }
    */
}

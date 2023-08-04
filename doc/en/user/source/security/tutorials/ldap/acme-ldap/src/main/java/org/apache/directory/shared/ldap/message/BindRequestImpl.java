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


import java.util.Arrays;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.internal.InternalBindRequest;
import org.apache.directory.shared.ldap.message.internal.InternalBindResponse;
import org.apache.directory.shared.ldap.message.internal.InternalResultResponse;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Bind protocol operation request which authenticates and begins a client
 * session. Does not yet contain interfaces for SASL authentication mechanisms.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev: 921600 $
 */
public class BindRequestImpl extends AbstractAbandonableRequest implements InternalBindRequest
{
    static final long serialVersionUID = 7945504184130380071L;

    /**
     * Distinguished name identifying the name of the authenticating subject -
     * defaults to the empty string
     */
    private DN name;

    /** The passwords, keys or tickets used to verify user identity */
    private byte[] credentials;
    
    /** A storage for credentials hashCode */
    private int hCredentials;

    /** The mechanism used to decode user identity */
    private String mechanism;

    /** Simple vs. SASL authentication mode flag */
    private boolean isSimple = true;

    /** Bind behavior exhibited by protocol version */
    private boolean isVersion3 = true;

    /** The associated response */
    public InternalBindResponse response;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates an BindRequest implementation to bind to an LDAP server.
     * 
     * @param id the sequence identifier of the BindRequest message.
     */
    public BindRequestImpl( final int id )
    {
        super( id, TYPE );
        hCredentials = 0;
    }


    // -----------------------------------------------------------------------
    // BindRequest Interface Method Implementations
    // -----------------------------------------------------------------------

    /**
     * Checks to see if the authentication mechanism is simple and not SASL
     * based.
     * 
     * @return true if the mechanism is simple false if it is SASL based.
     */
    public boolean isSimple()
    {
        return isSimple;
    }


    /**
     * Checks to see if the authentication mechanism is simple and not SASL
     * based.
     * 
     * @return true if the mechanism is simple false if it is SASL based.
     */
    public boolean getSimple()
    {
        return isSimple;
    }


    /**
     * Sets the authentication mechanism to simple or to SASL based
     * authentication.
     * 
     * @param isSimple
     *            true if authentication is simple, false otherwise.
     */
    public void setSimple( boolean isSimple )
    {
        this.isSimple = isSimple;
    }


    /**
     * Gets the simple credentials associated with a simple authentication
     * attempt or null if this request uses SASL authentication mechanisms.
     * 
     * @return null if the mechanism is SASL or the credentials if it is simple.
     */
    public byte[] getCredentials()
    {
        return credentials;
    }


    /**
     * Sets the simple credentials associated with a simple authentication
     * attempt ignored if this request uses SASL authentication mechanisms.
     * 
     * @param credentials
     *            the credentials if authentication is simple, null otherwise
     */
    public void setCredentials( byte[] credentials )
    {
        if ( credentials != null )
        {
            this.credentials = new byte[ credentials.length ];
            System.arraycopy( credentials, 0, this.credentials, 0, credentials.length );
        } 
        else 
        {
            this.credentials = null;
        }
        
        // Compute the hashcode
        if ( credentials != null )
        {
            hCredentials = 0;
            
            for ( byte b:credentials )
            {
                hCredentials = hCredentials*31 + b;
            }
        }
        else
        {
            hCredentials = 0;
        }
    }


    /**
     * Gets the mechanism if this request uses SASL authentication mechanisms.
     * 
     * @return The mechanism if SASL.
     */
    public String getSaslMechanism()
    {
        return mechanism;
    }


    /**
     * Sets the mechanism associated with a SASL authentication
     * 
     * @param mechanism
     *            the mechanism otherwise
     */
    public void setSaslMechanism( String mechanism )
    {
        this.mechanism = mechanism;
    }


    /**
     * Gets the distinguished name of the subject in this authentication
     * request. This field may take on a null value (a zero length string) for
     * the purposes of anonymous binds, when authentication has been performed
     * at a lower layer, or when using SASL credentials with a mechanism that
     * includes the DN in the credentials.
     * 
     * @return the DN of the authenticating user.
     */
    public DN getName()
    {
        return name;
    }


    /**
     * Sets the distinguished name of the subject in this authentication
     * request. This field may take on a null value (or a zero length string)
     * for the purposes of anonymous binds, when authentication has been
     * performed at a lower layer, or when using SASL credentials with a
     * mechanism that includes the DN in the credentials.
     * 
     * @param name
     *            the DN of the authenticating user - leave null for annonymous
     *            user.
     */
    public void setName( DN name )
    {
        this.name = name;
    }


    /**
     * Checks to see if the Ldap v3 protocol is used. Normally this would
     * extract a version number from the bind request sent by the client
     * indicating the version of the protocol to be used in this protocol
     * session. The integer is either a 2 or a 3 at the moment. We thought it
     * was better to just check if the protocol used is 3 or not rather than use
     * an type-safe enumeration type for a binary value. If an LDAPv4 comes out
     * then we shall convert the return type to a type safe enumeration.
     * 
     * @return true if client using version 3 false if it is version 2.
     */
    public boolean isVersion3()
    {
        return isVersion3;
    }


    /**
     * Gets whether or not the Ldap v3 protocol is used. Normally this would
     * extract a version number from the bind request sent by the client
     * indicating the version of the protocol to be used in this protocol
     * session. The integer is either a 2 or a 3 at the moment. We thought it
     * was better to just check if the protocol used is 3 or not rather than use
     * an type-safe enumeration type for a binary value. If an LDAPv4 comes out
     * then we shall convert the return type to a type safe enumeration.
     * 
     * @return true if client using version 3 false if it is version 2.
     */
    public boolean getVersion3()
    {
        return isVersion3;
    }


    /**
     * Sets whether or not the LDAP v3 or v2 protocol is used. Normally this
     * would extract a version number from the bind request sent by the client
     * indicating the version of the protocol to be used in this protocol
     * session. The integer is either a 2 or a 3 at the moment. We thought it
     * was better to just check if the protocol used is 3 or not rather than use
     * an type-safe enumeration type for a binary value. If an LDAPv4 comes out
     * then we shall convert the return type to a type safe enumeration.
     * 
     * @param isVersion3
     *            if true the client will be exhibiting version 3 bind behavoir,
     *            if false is used version 2 behavoir will be exhibited.
     */
    public void setVersion3( boolean isVersion3 )
    {
        this.isVersion3 = isVersion3;
    }


    // -----------------------------------------------------------------------
    // BindRequest Interface Method Implementations
    // -----------------------------------------------------------------------
    /**
     * Gets the protocol response message type for this request which produces
     * at least one response.
     * 
     * @return the message type of the response.
     */
    public MessageTypeEnum getResponseType()
    {
        return RESP_TYPE;
    }


    /**
     * The result containing response for this request.
     * 
     * @return the result containing response for this request
     */
    public InternalResultResponse getResultResponse()
    {
        if ( response == null )
        {
            response = new BindResponseImpl( getMessageId() );
        }

        return response;
    }
    


    /**
     * RFC 2251/4511 [Section 4.11]: Abandon, Bind, Unbind, and StartTLS operations
     * cannot be abandoned.
     */
    public void abandon()
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04185 ) );
    }


    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( ( obj == null ) || !( obj instanceof InternalBindRequest ) )
        {
            return false;
        }
        
        if ( !super.equals( obj ) )
        {
            return false;
        }

        InternalBindRequest req = ( InternalBindRequest ) obj;

        if ( req.isSimple() != isSimple() )
        {
            return false;
        }

        if ( req.isVersion3() != isVersion3() )
        {
            return false;
        }

        DN dn1 = req.getName();
        DN dn2 = getName();
        
        if ( dn1 == null )
        {
            if ( dn2 != null )
            {
                return false;
            }
        }
        else
        {
            if ( dn2 == null )
            {
                return false;
            }
            else if ( !dn1.equals( dn2 ) )
            {
                return false;
            }
                
        }
        
        if ( !Arrays.equals( req.getCredentials(), getCredentials() ) )
        {
            return false;
        }

        return true;
    }
    
    
    /**
     * @see Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int hash = 37;
        hash = hash*17 + ( credentials == null ? 0 : hCredentials );
        hash = hash*17 + ( isSimple ? 0 : 1 );
        hash = hash*17 + ( isVersion3 ? 0 : 1 );
        hash = hash*17 + ( mechanism == null ? 0 : mechanism.hashCode() );
        hash = hash*17 + ( name == null ? 0 : name.hashCode() );
        hash = hash*17 + ( response == null ? 0 : response.hashCode() );
        hash = hash*17 + super.hashCode();
        
        return hash;
    }

    
    /**
     * Get a String representation of a BindRequest
     * 
     * @return A BindRequest String
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "    BindRequest\n" );
        sb.append( "        Version : '" ).append( isVersion3 ? "3" : "2" ).append( "'\n" );

        if ( StringTools.isEmpty( name.getNormName() ) && isSimple )
        {
            sb.append( "        Name : anonymous\n" );
        }
        else
        {
            sb.append( "        Name : '" ).append( name.toString() ).append( "'\n" );

            if ( isSimple )
            {
                sb.append( "        Simple authentication : '" ).append( StringTools.utf8ToString( credentials ) )
                    .append( '/' ).append( StringTools.dumpBytes( credentials ) ).append( "'\n" );
            }
            else
            {
                sb.append( "        Sasl credentials\n" );
                sb.append( "            Mechanism :'" ).append( mechanism ).append( "'\n" );
                
                if ( credentials == null )
                {
                    sb.append( "            Credentials : null" );
                }
                else
                {
                    sb.append( "            Credentials : '" ).
                        append( StringTools.utf8ToString( credentials ) ).
                        append( '/' ).
                        append( StringTools.dumpBytes( credentials ) ).
                        append( "'\n" );
                }
            }
        }

        return sb.toString();
    }
}

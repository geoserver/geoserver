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


import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.message.ExtendedResponseImpl;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * An extended operation intended for notifying clients of upcoming
 * disconnection. Here's what <a
 * href="http://www.faqs.org/rfcs/rfc2251.html">RFC 2251</a> has to say about
 * it:
 * 
 * <pre>
 *  Section 4.1.1 (Small snippet on sending NoD)
 *  
 *     If the server receives a PDU from the client in which the LDAPMessage
 *     SEQUENCE tag cannot be recognized, the messageID cannot be parsed,
 *     the tag of the protocolOp is not recognized as a request, or the
 *     encoding structures or lengths of data fields are found to be
 *     incorrect, then the server MUST return the notice of disconnection
 *     described in section 4.4.1, with resultCode protocolError, and
 *     immediately close the connection. In other cases that the server
 *     cannot parse the request received by the client, the server MUST
 *     return an appropriate response to the request, with the resultCode
 *     set to protocolError.
 *     
 *  ...   
 *     
 *  4.4. Unsolicited Notification
 *  
 *     An unsolicited notification is an LDAPMessage sent from the server to
 *     the client which is not in response to any LDAPMessage received by
 *     the server. It is used to signal an extraordinary condition in the
 *     server or in the connection between the client and the server.  The
 *     notification is of an advisory nature, and the server will not expect
 *     any response to be returned from the client.
 *  
 *     The unsolicited notification is structured as an LDAPMessage in which
 *     the messageID is 0 and protocolOp is of the extendedResp form.  The
 *     responseName field of the ExtendedResponse is present. The LDAPOID
 *     value MUST be unique for this notification, and not be used in any
 *     other situation.
 *  
 *     One unsolicited notification is defined in this document.
 *  
 *  4.4.1. Notice of Disconnection
 *  
 *     This notification may be used by the server to advise the client that
 *     the server is about to close the connection due to an error
 *     condition. Note that this notification is NOT a response to an
 *     unbind requested by the client: the server MUST follow the procedures
 *     of section 4.3. This notification is intended to assist clients in
 *     distinguishing between an error condition and a transient network
 *     failure. As with a connection close due to network failure, the
 *     client MUST NOT assume that any outstanding requests which modified
 *     the directory have succeeded or failed.
 *  
 *     The responseName is 1.3.6.1.4.1.1466.20036, the response field is
 *     absent, and the resultCode is used to indicate the reason for the
 *     disconnection.
 *  
 *     The following resultCode values are to be used in this notification:
 *  
 *     - protocolError: The server has received data from the client in
 *       which the LDAPMessage structure could not be parsed.
 *  
 *     - strongAuthRequired: The server has detected that an established
 *       underlying security association protecting communication between
 *       the client and server has unexpectedly failed or been compromised.
 *  
 *     - unavailable: This server will stop accepting new connections and
 *       operations on all existing connections, and be unavailable for an
 *       extended period of time. The client may make use of an alternative
 *       server.
 *  
 *     After sending this notice, the server MUST close the connection.
 *     After receiving this notice, the client MUST NOT transmit any further
 *     on the connection, and may abruptly close the connection.
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912436 $
 */
public class NoticeOfDisconnect extends ExtendedResponseImpl
{
    private static final long serialVersionUID = -4682291068700593492L;

    public static final String EXTENSION_OID = "1.3.6.1.4.1.1466.20036";

    private static final byte[] EMPTY_RESPONSE = new byte[0];

    public static final NoticeOfDisconnect UNAVAILABLE = new NoticeOfDisconnect( ResultCodeEnum.UNAVAILABLE );

    public static final NoticeOfDisconnect PROTOCOLERROR = new NoticeOfDisconnect( ResultCodeEnum.PROTOCOL_ERROR );

    public static final NoticeOfDisconnect STRONGAUTHREQUIRED = new NoticeOfDisconnect( ResultCodeEnum.STRONG_AUTH_REQUIRED );


    private NoticeOfDisconnect( ResultCodeEnum rcode )
    {
        super( 0, EXTENSION_OID );

        switch ( rcode )
        {
            case UNAVAILABLE :
                break;
                
            case PROTOCOL_ERROR :
                break;
                
            case STRONG_AUTH_REQUIRED :
                break;
                
            default:
                throw new IllegalArgumentException( I18n.err( I18n.ERR_04166, ResultCodeEnum.UNAVAILABLE,
                    ResultCodeEnum.PROTOCOL_ERROR, ResultCodeEnum.STRONG_AUTH_REQUIRED ) );
        }
        
        super.getLdapResult().setErrorMessage( rcode.toString() + ": The server will disconnect!" );
        super.getLdapResult().setMatchedDn( null );
        super.getLdapResult().setResultCode( rcode );
    }


    // ------------------------------------------------------------------------
    // ExtendedResponse Interface Method Implementations
    // ------------------------------------------------------------------------

    
    /**
     * Gets the reponse OID specific encoded response values.
     * 
     * @return the response specific encoded response values.
     */
    public byte[] getResponse()
    {
        return EMPTY_RESPONSE;
    }


    /**
     * Sets the reponse OID specific encoded response values.
     * 
     * @param value
     *            the response specific encoded response values.
     */
    public void setResponse( byte[] value )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04173 ) );
    }


    /**
     * Gets the OID uniquely identifying this extended response (a.k.a. its
     * name).
     * 
     * @return the OID of the extended response type.
     */
    public String getResponseName()
    {
        return EXTENSION_OID;
    }


    /**
     * Sets the OID uniquely identifying this extended response (a.k.a. its
     * name).
     * 
     * @param oid
     *            the OID of the extended response type.
     */
    public void setResponseName( String oid )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04168, EXTENSION_OID ) );
    }


    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( obj instanceof NoticeOfDisconnect )
        {
            return true;
        }

        return false;
    }
}

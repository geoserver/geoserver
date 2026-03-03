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
package org.apache.directory.shared.ldap.message.internal;

import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.SingleReplyRequest;


/**
 * Extended protocol request message used to add to more operations to the
 * protocol. Here's what <a href="http://www.faqs.org/rfcs/rfc2251.html"> RFC
 * 2251</a> says about it:
 * 
 * <pre>
 *  4.12. Extended Operation
 * 
 *   An extension mechanism has been added in this version of LDAP, in
 *   order to allow additional operations to be defined for services not
 *   available elsewhere in this protocol, for instance digitally signed
 *   operations and results.
 * 
 *   The extended operation allows clients to make requests and receive
 *   responses with predefined syntaxes and semantics.  These may be
 *   defined in RFCs or be private to particular implementations.  Each
 *   request MUST have a unique OBJECT IDENTIFIER assigned to it.
 * 
 *        ExtendedRequest ::= [APPLICATION 23] SEQUENCE {
 *                requestName      [0] LDAPOID,
 *                requestValue     [1] OCTET STRING OPTIONAL }
 * 
 *   The requestName is a dotted-decimal representation of the OBJECT
 *   IDENTIFIER corresponding to the request. The requestValue is
 *   information in a form defined by that request, encapsulated inside an
 *   OCTET STRING.
 *  &lt;pre&gt;
 * <br>
 *  
 *  @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 *  @version $Revision: 910150 $
 * 
 */
public interface InternalExtendedRequest extends SingleReplyRequest, javax.naming.ldap.ExtendedRequest
{
    /** Extended request message type enumeration value */
    MessageTypeEnum TYPE = MessageTypeEnum.EXTENDED_REQUEST;

    /** Extended response message type enumeration value */
    MessageTypeEnum RESP_TYPE = InternalExtendedResponse.TYPE;


    /**
     * Gets the Object Idendifier corresponding to the extended request type.
     * This is the <b>requestName</b> portion of the ext. req. PDU.
     * 
     * @return the dotted-decimal representation as a String of the OID
     */
    String getOid();


    /**
     * Sets the Object Idendifier corresponding to the extended request type.
     * 
     * @param oid
     *            the dotted-decimal representation as a String of the OID
     */
    void setOid( String oid );


    /**
     * Gets the extended request's <b>requestValue</b> portion of the PDU. The
     * form of the data is request specific and is determined by the extended
     * request OID.
     * 
     * @return byte array of data
     */
    byte[] getPayload();


    /**
     * Sets the extended request's <b>requestValue</b> portion of the PDU.
     * 
     * @param payload
     *            byte array of data encapsulating ext. req. parameters
     */
    void setPayload( byte[] payload );
}

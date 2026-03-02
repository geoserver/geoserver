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


import javax.naming.ldap.ExtendedResponse;

import org.apache.directory.shared.ldap.message.ExtendedRequestImpl;


/**
 * An extended operation which launches an internal diagnostic UI. Only the
 * administrator is authorized to execute this request. All other requestors
 * will have a response with result code of insufficientAccessRights(50) sent.
 * Any failures to launch the UI will return a operationsError(1) result code.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 482294 $
 */
public class LaunchDiagnosticUiRequest extends ExtendedRequestImpl
{
    private static final long serialVersionUID = -7481749915684864433L;

    public static final String EXTENSION_OID = "1.3.6.1.4.1.18060.0.1.1";

    private static final byte[] EMPTY_PAYLOAD = new byte[0];


    public LaunchDiagnosticUiRequest(int messageId)
    {
        super( messageId );
        setOid( EXTENSION_OID );
        setPayload( EMPTY_PAYLOAD );
    }


    public ExtendedResponse createExtendedResponse( String id, byte[] berValue, int offset, int length )
    {
        return new LaunchDiagnosticUiResponse( getMessageId() );
    }
}

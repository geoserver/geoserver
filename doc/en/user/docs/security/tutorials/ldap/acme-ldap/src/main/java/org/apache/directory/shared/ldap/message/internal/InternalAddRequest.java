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
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.message.SingleReplyRequest;
import org.apache.directory.shared.ldap.name.DN;


/**
 * Add protocol operation request used to add a new entry to the DIT.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 918756 $
 */
public interface InternalAddRequest extends SingleReplyRequest, InternalAbandonableRequest
{
    /** LDAPv3 add request type enum code */
    MessageTypeEnum TYPE = MessageTypeEnum.ADD_REQUEST;

    /** LDAPv3 add response type enum code */
    MessageTypeEnum RESP_TYPE = InternalAddResponse.TYPE;


    /**
     * Gets the distinguished name of the entry to add.
     * 
     * @return the Dn of the added entry.
     */
    DN getEntryDn();


    /**
     * Sets the distinguished name of the entry to add.
     * 
     * @param entry the Dn of the added entry.
     */
    void setEntryDn( DN entry );


    /**
     * Gets the entry to add.
     * 
     * @return the added Entry
     */
    Entry getEntry();


    /**
     * Sets the Entry to add.
     * 
     * @param entry the added Entry
     */
    void setEntry( Entry entry );
}

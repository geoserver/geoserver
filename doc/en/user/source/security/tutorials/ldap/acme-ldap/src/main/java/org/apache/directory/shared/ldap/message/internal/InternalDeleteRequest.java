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
import org.apache.directory.shared.ldap.name.DN;


/**
 * Delete request protocol message used to remove an existing leaf entry from
 * the directory.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 918756 $
 */
public interface InternalDeleteRequest extends SingleReplyRequest, InternalAbandonableRequest
{
    /** Delete request message type enumeration value */
    MessageTypeEnum TYPE = MessageTypeEnum.DEL_REQUEST;

    /** Delete response message type enumeration value */
    MessageTypeEnum RESP_TYPE = InternalDeleteResponse.TYPE;


    /**
     * Gets the distinguished name of the leaf entry to be deleted by this
     * request.
     * 
     * @return the DN of the leaf entry to delete.
     */
    DN getName();


    /**
     * Sets the distinguished name of the leaf entry to be deleted by this
     * request.
     * 
     * @param name the DN of the leaf entry to delete.
     */
    void setName( DN name );
}

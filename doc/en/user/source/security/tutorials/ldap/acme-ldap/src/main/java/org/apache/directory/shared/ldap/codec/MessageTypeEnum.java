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

/**
 * An enum to store the Ldap message type.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum MessageTypeEnum
{
    ABANDON_REQUEST(0),
    ADD_REQUEST (1),
    ADD_RESPONSE(2),
    BIND_REQUEST(3),
    BIND_RESPONSE(4),
    COMPARE_REQUEST(5),
    COMPARE_RESPONSE(6),
    DEL_REQUEST(7),
    DEL_RESPONSE(8),
    EXTENDED_REQUEST(9),
    EXTENDED_RESPONSE(10),
    MODIFYDN_REQUEST(11),
    MODIFYDN_RESPONSE(12),
    MODIFY_REQUEST(13),
    MODIFY_RESPONSE(14),
    SEARCH_REQUEST(15),
    SEARCH_RESULT_DONE(16),
    SEARCH_RESULT_ENTRY(17),
    SEARCH_RESULT_REFERENCE(18),
    UNBIND_REQUEST(19),
    INTERMEDIATE_RESPONSE(20);

    /** The internal value */
    private int value;
    
    /** The message Type name */
    private String name;
    
    private MessageTypeEnum( int value )
    {
        this.value = value;
    }
}

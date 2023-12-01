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
package org.apache.directory.shared.ldap.message.control.replication;

/**
 * This class describes the four possible synchronization mode, out of
 * which only two are presently valid :
 * 
 * syncRequestValue ::= SEQUENCE {
 *     mode ENUMERATED {
 *         -- 0 unused
 *         refreshOnly       (1),
 *         -- 2 reserved
 *         refreshAndPersist (3)
 * ...
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc4533.html">RFC 4533</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: $
 *
 */
public enum SynchronizationModeEnum
{
    UNUSED(0),
    REFRESH_ONLY(1),
    RESERVED(2),
    REFRESH_AND_PERSIST(3);
    
    /** The internal value */
    private int value;
    

    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param value the integer value of the enumeration.
     */
    private SynchronizationModeEnum( int value )
    {
        this.value = value;
    }

    
    /**
     * @return The value associated with the current element.
     */
    public int getValue()
    {
        return value;
    }
    
    
    /**
     * Get the {@link SynchronizationModeEnum} instance from an integer value.
     * 
     * @param value The value we want the enum element from
     * @return The enum element associated with this integer
     */
    public static SynchronizationModeEnum getSyncMode( int value )
    {
        if ( value == REFRESH_AND_PERSIST.getValue() )
        {
            return REFRESH_AND_PERSIST;
        }
        else if ( value == REFRESH_ONLY.getValue() )
        {
            return REFRESH_ONLY;
        }
        else if ( value == UNUSED.getValue() )
        {
            return UNUSED;
        }
        else
        {
            return RESERVED;
        }
    }
}

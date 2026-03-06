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
 * This class describes the four possible Info values :
 * <ul>
 * <li>newcookie</li>
 * <li>refreshDelete</li>
 * <li>refreshpresent</li>
 * <li>syncIdSet</li>
 * </ul>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc4533.html">RFC 4533</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: $
 *
 */
public enum SynchronizationInfoEnum
{
    NEW_COOKIE(0),
    REFRESH_DELETE(1),
    REFRESH_PRESENT(2),
    SYNC_ID_SET(3);
    
    /** The internal value */
    private int value;
    

    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param value the integer value of the enumeration.
     */
    private SynchronizationInfoEnum( int value )
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
     * Get the {@link SynchronizationInfoEnum} instance from an integer value.
     * 
     * @param value The value we want the enum element from
     * @return The enum element associated with this integer
     */
    public static SynchronizationInfoEnum getSyncMode( int value )
    {
        if ( value == NEW_COOKIE.getValue() )
        {
            return NEW_COOKIE;
        }
        else if ( value == REFRESH_DELETE.getValue() )
        {
            return REFRESH_DELETE;
        }
        else if ( value == REFRESH_PRESENT.getValue() )
        {
            return REFRESH_PRESENT;
        }
        else
        {
            return SYNC_ID_SET;
        }
    }
}

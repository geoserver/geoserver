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

import org.apache.directory.shared.i18n.I18n;


/**
 * 
 * This class describes the four types of states part of the syncStateValue as described in rfc4533.
 * 
 *  state ENUMERATED {
 *            present (0),
 *            add (1),
 *            modify (2),
 *            delete (3)
 *   }
 *   
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum SyncStateTypeEnum
{
    PRESENT(0), ADD(1), MODIFY(2), DELETE(3);

    /** the internal value */
    private int value;


    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param value the integer value of the enumeration.
     */
    private SyncStateTypeEnum( int value )
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
     * Get the {@link SyncStateTypeEnum} instance from an integer value.
     * 
     * @param value The value we want the enum element from
     * @return The enum element associated with this integer
     */
    public static SyncStateTypeEnum getSyncStateType( int value )
    {
        if ( value == PRESENT.value )
        {
            return PRESENT;
        }
        else if ( value == ADD.value )
        {
            return ADD;
        }
        else if ( value == MODIFY.value )
        {
            return MODIFY;
        }
        else if ( value == DELETE.value )
        {
            return DELETE;
        }

        throw new IllegalArgumentException( I18n.err( I18n.ERR_04163, value ) );
    }

}

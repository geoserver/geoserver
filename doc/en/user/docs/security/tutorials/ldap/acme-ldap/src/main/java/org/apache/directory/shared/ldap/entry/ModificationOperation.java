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
package org.apache.directory.shared.ldap.entry;

/**
 * An enum storing the different modification operation which can be used
 * in a Modification. There is a one to one mapping with the DirContext.ADD_ATTRIBUTE,
 * DirContext.REMOVE_ATTRIBUTE, DirContext.REPLACE_ATTRIBUTE
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum ModificationOperation
{
    ADD_ATTRIBUTE( 0 ),
    REMOVE_ATTRIBUTE( 1 ),
    REPLACE_ATTRIBUTE( 2 );

    /** Internal value */
    private int value;
    
    
    /**
     * Creates a new instance of ModificationOperation.
     */
    private ModificationOperation( int value )
    {
        this.value = value;
    }
    
    
    /**
     * @return The integer value associated with the element. This value
     * is equivalent to the one found in DirContext.
     */
    public int getValue()
    {
        return value;
    }
    
    
    /**
     * Get the ModificationOperation from an int value
     *
     * @param value the ModificationOperation int value
     * @return the associated ModifciationOperation instance
     */
    public static ModificationOperation getOperation( int value )
    {
        if ( value == ADD_ATTRIBUTE.value )
        {
            return ADD_ATTRIBUTE;
        }
        else if ( value == REMOVE_ATTRIBUTE.value )
        {
            return REMOVE_ATTRIBUTE;
        }
        else if ( value == REPLACE_ATTRIBUTE.value )
        {
            return REPLACE_ATTRIBUTE;
        }
        else
        {
            return null;
        }
    }
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        switch ( this )
        {
            case ADD_ATTRIBUTE :
                return "add";
                
            case REPLACE_ATTRIBUTE :
                return "replace";
                
            case REMOVE_ATTRIBUTE :
                return "remove";
                
            default :
                return "";
        }
    }
}

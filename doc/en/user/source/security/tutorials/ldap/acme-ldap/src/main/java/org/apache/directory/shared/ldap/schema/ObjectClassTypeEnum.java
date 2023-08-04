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
package org.apache.directory.shared.ldap.schema;

import org.apache.directory.shared.i18n.I18n;


/**
 * Type safe enumerations for an objectClass' type. An ObjectClass type can be
 * one of the following types:
 * <ul>
 * <li>ABSTRACT</li>
 * <li>AUXILIARY</li>
 * <li>STRUCTURAL</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum ObjectClassTypeEnum
{
    /** The enumeration constant value for the abstract objectClasses */
    ABSTRACT( 0 ),

    /** The enumeration constant value for the auxillary objectClasses */
    AUXILIARY( 1 ),

    /** The enumeration constant value for the structural objectClasses */
    STRUCTURAL( 2 );

    /** The int constant value for the abstract objectClasses */
    public static final int ABSTRACT_VAL = 0;

    /** The int constant value for the auxillary objectClasses */
    public static final int AUXILIARY_VAL = 1;

    /** The int constant value for the structural objectClasses */
    public static final int STRUCTURAL_VAL=2;
    
    /** Stores the integer value of each element of the enumeration */
    private int value;
    
    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param name
     *            a string name for the enumeration value.
     * @param value
     *            the integer value of the enumeration.
     */
    private ObjectClassTypeEnum( int value )
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
     * Gets the objectClass type enumeration of AUXILIARY, STRUCTURAL, or,
     * ABSTRACT.
     * 
     * @param name options are AUXILIARY, STRUCTURAL, or, ABSTRACT
     * 
     * @return the type safe enumeration for the objectClass type
     */
    public static ObjectClassTypeEnum getClassType( String name )
    {
        String upperCase = name.trim().toUpperCase();

        if ( upperCase.equals( "STRUCTURAL" ) )
        {
            return STRUCTURAL;
        }
        else if ( upperCase.equals( "AUXILIARY" ) )
        {
            return AUXILIARY;
        }
        else if ( upperCase.equals( "ABSTRACT" ) )
        {
            return ABSTRACT;
        }

        throw new IllegalArgumentException( I18n.err( I18n.ERR_04327, name) );
    }
}

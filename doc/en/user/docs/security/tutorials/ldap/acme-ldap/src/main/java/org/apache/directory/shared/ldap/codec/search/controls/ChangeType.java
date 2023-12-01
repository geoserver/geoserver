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
package org.apache.directory.shared.ldap.codec.search.controls;

import org.apache.directory.shared.i18n.I18n;


/**
 * Enumeration type for entry changes associates with the persistent search
 * control and the entry change control. Used for the following ASN1
 * enumeration:
 * 
 * <pre>
 *   changeType ENUMERATED 
 *   {
 *       add             (1),
 *       delete          (2),
 *       modify          (4),
 *       modDN           (8)
 *   }
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $
 */
public class ChangeType
{
    public static final int ADD_VALUE = 1;

    public static final int DELETE_VALUE = 2;

    public static final int MODIFY_VALUE = 4;

    public static final int MODDN_VALUE = 8;

    public static final ChangeType ADD = new ChangeType( "ADD", ADD_VALUE );

    public static final ChangeType DELETE = new ChangeType( "DELETE", DELETE_VALUE );

    public static final ChangeType MODIFY = new ChangeType( "MODIFY", MODIFY_VALUE );

    public static final ChangeType MODDN = new ChangeType( "MODDN", MODDN_VALUE );
    
    private final String label;

    private final int value;


    private ChangeType(String label, int value)
    {
        this.label = label;
        this.value = value;
    }


    public int getValue()
    {
        return value;
    }


    public String toString()
    {
        return label;
    }


    /**
     * Gets the changeType enumeration type for an integer value.
     * 
     * @param value the value to get the enumeration for
     * @return the enueration type for the value if the value is valid
     * @throws IllegalArgumentException if the value is undefined
     */
    public static ChangeType getChangeType( int value )
    {
        switch ( value )
        {
            case ( ADD_VALUE ):
                return ADD;
            case ( DELETE_VALUE ):
                return DELETE;
            case ( MODIFY_VALUE ):
                return MODIFY;
            case ( MODDN_VALUE ):
                return MODDN;
            default:
                throw new IllegalArgumentException( I18n.err( I18n.ERR_04055, value ) );
        }
    }
}

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


/**
 * Type safe enum for an AttributeType definition's usage string. This can be
 * take one of the following four values:
 * <ul>
 * <li>userApplications</li>
 * <li>directoryOperation</li>
 * <li>distributedOperation</li>
 * <li>dSAOperation</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 896579 $
 */
public enum UsageEnum
{
    /** value for attributes with userApplications usage */
    USER_APPLICATIONS( 0 ),

    /** value for attributes with directoryOperation usage */
    DIRECTORY_OPERATION( 1 ),

    /** value for attributes with distributedOperation usage */
    DISTRIBUTED_OPERATION( 2 ),

    /** value for attributes with dSAOperation usage */
    DSA_OPERATION( 3 );

    /** Stores the integer value of each element of the enumeration */
    private int value;

    /**
     * Private construct so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param value the integer value of the enumeration.
     */
    private UsageEnum( int value )
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
     * Gets the enumeration type for the attributeType usage string regardless
     * of case.
     * 
     * @param usage the usage string
     * @return the usage enumeration type
     */
    public static UsageEnum getUsage( String usage )
    {
        try
        {
            UsageEnum result = valueOf( usage );
            
            return result;
        }
        catch( IllegalArgumentException iae )
        {
            if ( "directoryOperation".equals( usage ) )
            {
                return DIRECTORY_OPERATION;
            }
            else if ( "distributedOperation".equals( usage ) )
            {
                return DISTRIBUTED_OPERATION;
            }
            else if ( "dSAOperation".equals( usage ) )
            {
                return DSA_OPERATION;    
            }
            else if ( "userApplications".equals( usage ) ) 
            {
                return USER_APPLICATIONS;
            }
            else 
            {
                return null;
            }
        }
    }
    
    /**
     * Get the string representation for UsageEnum, which will be
     * used by the AttributeType rendering 
     * @param usage The UsageEnum of which we want the rendering string
     * @return The rendering stringe
     */
    public static String render( UsageEnum usage )
    {
        if ( usage == null)
        {
            return "userApplications";
        }
        
        switch ( usage )
        {
            case DIRECTORY_OPERATION    : return "directoryOperation";
            case DISTRIBUTED_OPERATION  : return "distributedOperation";
            case DSA_OPERATION          : return "dSAOperation";
            case USER_APPLICATIONS      : return "userApplications";
            default : return "";
        }
    }

    /**
     * Get the string representation for UsageEnum, which will be
     * used by the AttributeType rendering 
     * @return The rendering stringe
     */
    public String render()
    {
        return render( this );
    }
}

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
 * Type safe enum for a matching rule's comparator and normalizer component
 * usage string. This can be take one of the following three values:
 * <ul>
 * <li>ORDERING</li>
 * <li>EQUALITY</li>
 * <li>SUBSTRING</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum MatchingRuleEnum
{
    /** value for ordering usage */
    ORDERING( 0 ),

    /** value for equality usage */
    EQUALITY( 1 ),

    /** value for substring usage */
    SUBSTRING( 2 );

    /** Stores the integer value of each element of the enumeration */
    private int value;
    
    /**
     * Private constructor so no other instances can be created other than the
     * public static constants in this class.
     * 
     * @param value
     *            the integer value of the enumeration.
     */
    private MatchingRuleEnum( int value )
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
     * Gets the enumeration type for the usage string regardless of case.
     * 
     * @param matchingRule the usage string
     * @return the matchingRule enumeration type
     */
    public static MatchingRuleEnum getUsage( String matchingRule )
    {
        return valueOf( matchingRule );
    }
}

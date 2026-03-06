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
package org.apache.directory.shared.ldap.codec.search.controls.subentries;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;


/**
 * This class store the SubEntryControl's grammar constants. It is also used for
 * debugging purposes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 905297 $, $Date: 2010-02-01 17:04:10 +0200 (Mon, 01 Feb 2010) $, 
 */
public class SubentriesControlStatesEnum implements IStates
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    // =========================================================================
    // Sub entry control grammar states
    // =========================================================================

    /** Starting state */
    public static final int START_STATE = 0;

    /** Visibility Value */
    public static final int SUB_ENTRY_VISIBILITY_STATE = 1;

    /** terminal state */
    public static final int LAST_SUB_ENTRY_STATE = 2;

    // =========================================================================
    // States debug strings
    // =========================================================================
    /** A string representation of all the states */
    private static String[] SubEntryString = new String[]
        { 
        "START_STATE", 
        "SUB_ENTRY_VISIBILITY_STATE" 
        };

    /** The instance */
    private static SubentriesControlStatesEnum instance = new SubentriesControlStatesEnum();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     */
    private SubentriesControlStatesEnum()
    {
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Get an instance of this class
     * 
     * @return An instance on this class
     */
    public static IStates getInstance()
    {
        return instance;
    }


    /**
     * Get the grammar name
     * 
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "SUB_ENTRY_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof SubentriesControlGrammar )
        {
            return "SUB_ENTRY_GRAMMAR";
        }

        return "UNKNOWN GRAMMAR";
    }


    /**
     * Get the string representing the state
     * 
     * @param state The state number
     * @return The String representing the state
     */
    public String getState( int state )
    {
        return ( ( state == GRAMMAR_END ) ? "SUB_ENTRY_END_STATE" : SubEntryString[state] );
    }
}

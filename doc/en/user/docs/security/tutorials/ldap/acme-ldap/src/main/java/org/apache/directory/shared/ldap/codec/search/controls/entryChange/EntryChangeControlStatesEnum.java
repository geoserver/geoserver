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
package org.apache.directory.shared.ldap.codec.search.controls.entryChange;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;


/**
 * This class store the EntryChangeControl's grammar constants. It is also used
 * for debugging purposes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 764131 $, $Date: 2009-04-11 04:03:00 +0300 (Sat, 11 Apr 2009) $, 
 */
public class EntryChangeControlStatesEnum implements IStates
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    // =========================================================================
    // Entry change control grammar states
    // =========================================================================

    /** Sequence Tag */
    public static final int START_STATE = 0;

    /** Sequence */
    public static final int EC_SEQUENCE_STATE = 1;

    /** changeType */
    public static final int CHANGE_TYPE_STATE = 2;

    /** previousDN */
    public static final int PREVIOUS_DN_STATE = 3;

    /** changeNumber */
    public static final int CHANGE_NUMBER_STATE = 4;

    /** terminal state */
    public static final int LAST_EC_STATE = 5;

    // =========================================================================
    // States debug strings
    // =========================================================================
    /** A string representation of all the states */
    private static String[] EcString = new String[]
        { 
        "START_STATE",
        "EC_SEQUENCE_STATE", 
        "CHANGE_TYPE_STATE",
        "PREVIOUS_DN_STATE", 
        "CHANGE_NUMBER_STATE" 
        };

    /** The instance */
    private static EntryChangeControlStatesEnum instance = new EntryChangeControlStatesEnum();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     */
    private EntryChangeControlStatesEnum()
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
        return "EC_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof EntryChangeControlGrammar )
        {
            return "EC_GRAMMAR";
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
        return ( ( state == GRAMMAR_END ) ? "EC_END_STATE" : EcString[state] );
    }
}

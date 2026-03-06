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
package org.apache.directory.shared.ldap.codec.extended.operations.gracefulDisconnect;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;


/**
 * This class store the GracefulDisconnect's grammar constants. It is also used
 * for debugging purposes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 764131 $, $Date: 2009-04-11 04:03:00 +0300 (Sat, 11 Apr 2009) $, 
 */
public class GracefulDisconnectStatesEnum implements IStates
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    // =========================================================================
    // GracefulDisconnect grammar states
    // =========================================================================
    /** Initial state */
    public static final int START_STATE = 0;

    /** Sequence */
    public static final int GRACEFUL_DISCONNECT_SEQUENCE_STATE = 1;

    /** Time offline */
    public static final int TIME_OFFLINE_STATE = 2;

    /** Delay */
    public static final int DELAY_STATE = 3;

    /** Replicated contexts */
    public static final int REPLICATED_CONTEXTS_STATE = 4;

    /** Referral */
    public static final int REFERRAL_STATE = 5;

    /** terminal state */
    public static final int LAST_GRACEFUL_DISCONNECT_STATE = 6;

    // =========================================================================
    // States debug strings
    // =========================================================================
    /** A string representation of all the states */
    private static String[] GracefulDisconnectString = new String[]
        { 
        "START_STATE", 
        "GRACEFUL_DISCONNECT_SEQUENCE_STATE",
        "TIME_OFFLINE_STATE", 
        "DELAY_STATE", 
        "REPLICATED_CONTEXTS_STATE",
        "REFERRAL_STATE"
        };

    /** The instance */
    private static GracefulDisconnectStatesEnum instance = new GracefulDisconnectStatesEnum();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     */
    private GracefulDisconnectStatesEnum()
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
        return "GRACEFUL_DISCONNECT_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof GracefulDisconnectGrammar )
        {
            return "GRACEFUL_DISCONNECT_GRAMMAR";
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
        return ( ( state == GRAMMAR_END ) ? "GRACEFUL_DISCONNECT_END_STATE" : GracefulDisconnectString[state] );
    }
}

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
package org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;


/**
 * This class store the SyncStateValueControl's grammar constants. It is also used for
 * debugging purposes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 741888 $, $Date: 2009-02-07 13:57:03 +0100 (Sat, 07 Feb 2009) $, 
 */
public class SyncStateValueControlStatesEnum implements IStates
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    // =========================================================================
    // SyncStateValue control grammar states
    // =========================================================================
    /** Initial state */
    public static final int START_STATE = 0;

    /** Sequence Value */
    public static final int SYNC_STATE_VALUE_SEQUENCE_STATE = 1;

    /** sync state type Value */
    public static final int SYNC_TYPE_STATE = 2;
    
    /** syncUUID Value */
    public static final int SYNC_UUID_STATE = 3;

    /** cookie Value */
    public static final int COOKIE_STATE = 4;

    /** terminal state */
    public static final int LAST_SYNC_STATE_VALUE_STATE = 5;

    // =========================================================================
    // States debug strings
    // =========================================================================
    /** A string representation of all the states */
    private static String[] syncStatetValueString = new String[]
        { 
        "START_STATE", 
        "SYNC_STATE_VALUE_SEQUENCE_STATE", 
        "SYNC_TYPE_STATE",
        "SYNC_UUID_STATE", 
        "COOKIE_STATE" 
        };

    /** The instance */
    private static SyncStateValueControlStatesEnum instance = new SyncStateValueControlStatesEnum();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     */
    private SyncStateValueControlStatesEnum()
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
        return "SYNC_REQUEST_VALUE_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof SyncStateValueControlGrammar )
        {
            return "SYNC_STATE_VALUE_GRAMMAR";
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
        return ( ( state == GRAMMAR_END ) ? "SYNC_STATE_VALUE_END_STATE" : syncStatetValueString[state] );
    }
}

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
package org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;


/**
 * This class store the SyncInfoValueControl's grammar constants. It is also used for
 * debugging purposes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 741888 $, $Date: 2009-02-07 13:57:03 +0100 (Sat, 07 Feb 2009) $, 
 */
public class SyncInfoValueControlStatesEnum implements IStates
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    // =========================================================================
    // SyncRequestValue control grammar states
    // =========================================================================
    /** Initial state */
    public static final int START_STATE = 0;

    /** NewCookie state */
    public static final int NEW_COOKIE_STATE = 1;

    /** RefreshDelete state */
    public static final int REFRESH_DELETE_STATE = 2;
    
    /** RefreshDelete cookie state */
    public static final int REFRESH_DELETE_COOKIE_STATE = 3;
    
    /** RefreshDelete refreshDone state */
    public static final int REFRESH_DELETE_REFRESH_DONE_STATE = 4;
    
    /** RefreshPresent state */
    public static final int REFRESH_PRESENT_STATE = 5;
    
    /** RefreshPresent cookie state */
    public static final int REFRESH_PRESENT_COOKIE_STATE = 6;
    
    /** RefreshPresent refreshDone state */
    public static final int REFRESH_PRESENT_REFRESH_DONE_STATE = 7;
    
    /** SyncIdSet state */
    public static final int SYNC_ID_SET_STATE = 8;
    
    /** SyncIdSet cookie state */
    public static final int SYNC_ID_SET_COOKIE_STATE = 9;
    
    /** SyncIdSet refreshDone state */
    public static final int SYNC_ID_SET_REFRESH_DELETES_STATE = 10;
    
    /** SyncIdSet SET OF UUIDs state */
    public static final int SYNC_ID_SET_SET_OF_UUIDS_STATE = 11;
    
    /** SyncIdSet UUID state */
    public static final int SYNC_ID_SET_UUID_STATE = 12;

    /** terminal state */
    public static final int LAST_SYNC_INFO_VALUE_STATE = 13;

    // =========================================================================
    // States debug strings
    // =========================================================================
    /** A string representation of all the states */
    private static String[] syncInfoValueString = new String[]
        { 
            "START_STATE",
            "NEW_COOKIE_STATE",
            "REFRESH_DELETE_STATE",
            "REFRESH_DELETE_COOKIE_STATE",
            "REFRESH_DELETE_REFRESH_DONE_STATE",
            "REFRESH_PRESENT_STATE",
            "REFRESH_PRESENT_COOKIE_STATE",
            "REFRESH_PRESENT_REFRESH_DONE_STATE",
            "SYNC_ID_SET_STATE",
            "SYNC_ID_SET_COOKIE_STATE",
            "SYNC_ID_SET_REFRESH_DELETES_STATE",
            "SYNC_ID_SET_SET_OF_UUIDS_STATE",
            "int SYNC_ID_SET_UUID_STATE"
        };

    /** The instance */
    private static SyncInfoValueControlStatesEnum instance = new SyncInfoValueControlStatesEnum();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     */
    private SyncInfoValueControlStatesEnum()
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
        return "SYNC_INFO_VALUE_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof SyncInfoValueControlGrammar )
        {
            return "SYNC_INFO_VALUE_GRAMMAR";
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
        return ( ( state == GRAMMAR_END ) ? "SYNC_INFO_VALUE_END_STATE" : syncInfoValueString[state] );
    }
}

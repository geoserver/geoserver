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
package org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue;

import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;

/**
 * 
 * ASN.1 grammer constants of SyncDoneValueControl.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyncDoneValueControlStatesEnum implements IStates
{
    /***/
    public static final int START_STATE = 0;
    
    /** sequence start state */
    public static final int SYNC_DONE_VALUE_SEQUENCE_STATE = 1;
    
    /** cookie value state */
    public static final int COOKIE_STATE = 2;
    
    /** refreshDeletes value state */
    public static final int REFRESH_DELETES_STATE = 3;
    
    /** terminal state */
    public static final int LAST_SYNC_DONE_VALUE_STATE = 4;
    
    public static String[] syncDoneValueString = new String[]
       {
       "START_STATE",
       "SYNC_DONE_VALUE_SEQUENCE_STATE",
       "COOKIE_STATE",
       "REFRESH_DELETES_STATE",
       };
    
    private static SyncDoneValueControlStatesEnum instance = new SyncDoneValueControlStatesEnum();
    
    private SyncDoneValueControlStatesEnum()
    {
    }
    
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
        return "SYNC_DONE_VALUE_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof SyncDoneValueControlGrammar )
        {
            return "SYNC_DONE_VALUE_GRAMMAR";
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
        return ( ( state == GRAMMAR_END ) ? "SYNC_DONE_VALUE_GRAMMAR" : syncDoneValueString[state] );
    }
    
}

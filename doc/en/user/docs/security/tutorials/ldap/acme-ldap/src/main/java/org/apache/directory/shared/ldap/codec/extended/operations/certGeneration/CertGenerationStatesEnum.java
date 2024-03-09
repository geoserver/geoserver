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
package org.apache.directory.shared.ldap.codec.extended.operations.certGeneration;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;

/**
 * This class store the CertGeneration's grammar constants. It is also used
 * for debugging purposes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664290 $, $Date: 2008-06-07 11:58:06 +0530 (Sat, 07 Jun 2008) $, 
 */
public class CertGenerationStatesEnum implements IStates
{

    /** start state*/
    public static final int START_STATE = 0;

    /** sequence*/
    public static final int CERT_GENERATION_REQUEST_SEQUENCE_STATE = 1;

    /** the target DN*/
    public static final int TARGETDN_STATE = 2;

    /** the issuer DN*/
    public static final int ISSUER_STATE = 3;

    /** the subject DN*/
    public static final int SUBJECT_STATE = 4;

    /** the key algorithm*/
    public static final int KEY_ALGORITHM_STATE = 5;

    /** terminal state */
    public static final int LAST_CERT_GENERATION_STATE = 6;

    private static String[] certGenerationString = new String[]
        {
          "START_STATE",
          "CERT_GENERATION_REQUEST_SEQUENCE_STATE",
          "TARGETDN_STATE", "ISSUER_STATE",
          "SUBJECT_STATE",
          "KEY_ALGORITHM_STATE"
        };

    /** a singleton instance*/
    private static CertGenerationStatesEnum instance = new CertGenerationStatesEnum();

    
    /**
     * Get the grammar name
     * 
     * @param grammar The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof CertGenerationGrammar )
        {
            return "CERT_GENERATION_GRAMMER";
        }
        
        return "UNKNOWN GRAMMAR";
    }

    
    /**
     * Get the grammar name
     * 
     * @param grammar The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "CERT_GENERATION_GRAMMER";
    }

    
    /**
     * Get the string representing the state
     * 
     * @param state The state number
     * @return The String representing the state
     */
    public String getState( int state )
    {
        return ( ( state == GRAMMAR_END ) ? "CERT_GENERATION_END_STATE" : certGenerationString[state] );
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
}

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
package org.apache.directory.shared.ldap.codec;


import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;


/**
 * This class store the Ldap grammar's constants. It is also used for debugging
 * purpose
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 749713 $, $Date: 2009-03-03 21:49:17 +0200 (Tue, 03 Mar 2009) $, 
 */
public class LdapStatesEnum implements IStates
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------
    public static final int START_STATE =                       0;
    public static final int LDAP_MESSAGE_STATE =                1;
    public static final int MESSAGE_ID_STATE =                  2;
    public static final int BIND_REQUEST_STATE =                3;
    public static final int BIND_RESPONSE_STATE =               4;
    public static final int UNBIND_REQUEST_STATE =              5;
    public static final int SEARCH_REQUEST_STATE =              6;
    public static final int SEARCH_RESULT_ENTRY_STATE =         7;
    public static final int SEARCH_RESULT_DONE_STATE =          8;
    public static final int SEARCH_RESULT_REFERENCE_STATE =     9;
    public static final int MODIFY_REQUEST_STATE =              10;
    public static final int MODIFY_RESPONSE_STATE =             11;
    public static final int ADD_REQUEST_STATE =                 12;
    public static final int ADD_RESPONSE_STATE =                13;
    public static final int DEL_REQUEST_STATE =                 14;
    public static final int DEL_RESPONSE_STATE =                15;
    public static final int MODIFY_DN_REQUEST_STATE =           16;
    public static final int MODIFY_DN_RESPONSE_STATE =          17;
    public static final int COMPARE_REQUEST_STATE =             18;
    public static final int COMPARE_RESPONSE_STATE =            19;
    public static final int ABANDON_REQUEST_STATE =             20;
    public static final int EXTENDED_REQUEST_STATE =            21;
    public static final int EXTENDED_RESPONSE_STATE =           22;
    public static final int VERSION_STATE =                     23;
    public static final int NAME_STATE =                        24;
    public static final int SIMPLE_STATE =                      25;
    public static final int SASL_STATE =                        26;
    public static final int MECHANISM_STATE =                   27;
    public static final int CREDENTIALS_STATE =                 28;
    public static final int RESULT_CODE_BR_STATE =              29;
    public static final int MATCHED_DN_BR_STATE =               30;
    public static final int ERROR_MESSAGE_BR_STATE =            31;
    public static final int REFERRALS_BR_STATE =                32;
    public static final int REFERRAL_BR_STATE =                 33;
    public static final int SERVER_SASL_CREDENTIALS_STATE =     34;
    public static final int RESULT_CODE_STATE =                 35;
    public static final int MATCHED_DN_STATE =                  36;
    public static final int ERROR_MESSAGE_STATE =               37;
    public static final int REFERRALS_STATE =                   38;
    public static final int REFERRAL_STATE =                    39;
    public static final int REQUEST_NAME_STATE =                40;
    public static final int REQUEST_VALUE_STATE =               41;
    public static final int RESPONSE_NAME_STATE =               42;
    public static final int RESPONSE_STATE =                    43;
    public static final int RESULT_CODE_ER_STATE =              44;
    public static final int MATCHED_DN_ER_STATE =               45;
    public static final int ERROR_MESSAGE_ER_STATE =            46;
    public static final int REFERRALS_ER_STATE =                47;
    public static final int REFERRAL_ER_STATE =                 48;
    public static final int ENTRY_STATE =                       49;
    public static final int ATTRIBUTES_STATE =                  50;
    public static final int ATTRIBUTE_STATE =                   51;
    public static final int TYPE_STATE =                        52;
    public static final int VALUES_STATE =                      53;
    public static final int VALUE_STATE =                       54;
    public static final int OBJECT_STATE =                      55;
    public static final int MODIFICATIONS_STATE =               56;
    public static final int MODIFICATIONS_SEQ_STATE =           57;
    public static final int OPERATION_STATE =                   58;
    public static final int MODIFICATION_STATE =                59;
    public static final int TYPE_MOD_STATE =                    60;
    public static final int VALS_STATE =                        61;
    public static final int ATTRIBUTE_VALUE_STATE =             62;
    public static final int ENTRY_MOD_DN_STATE =                63;
    public static final int NEW_RDN_STATE =                     64;
    public static final int DELETE_OLD_RDN_STATE =              65;
    public static final int NEW_SUPERIOR_STATE =                66;
    public static final int ENTRY_COMP_STATE =                  67;
    public static final int AVA_STATE =                         68;
    public static final int ATTRIBUTE_DESC_STATE =              69;
    public static final int ASSERTION_VALUE_STATE =             70;
    public static final int BASE_OBJECT_STATE =                 71;
    public static final int SCOPE_STATE =                       72;
    public static final int DEREF_ALIAS_STATE =                 73;
    public static final int SIZE_LIMIT_STATE =                  74;
    public static final int TIME_LIMIT_STATE =                  75;
    public static final int TYPES_ONLY_STATE =                  76;
    public static final int AND_STATE =                         77;
    public static final int OR_STATE =                          78;
    public static final int NOT_STATE =                         79;
    public static final int EQUALITY_MATCH_STATE =              80;
    public static final int SUBSTRING_FILTER_STATE =            81;
    public static final int GREATER_OR_EQUAL_STATE =            82;
    public static final int LESS_OR_EQUAL_STATE =               83;
    public static final int PRESENT_STATE =                     84;
    public static final int APPROX_MATCH_STATE =                85;
    public static final int EXTENSIBLE_MATCH_STATE =            86;
    public static final int ATTRIBUTE_DESC_FILTER_STATE =       87;
    public static final int ASSERTION_VALUE_FILTER_STATE =      88;
    public static final int ATTRIBUTE_DESCRIPTION_LIST_STATE =  89;
    public static final int ATTRIBUTE_DESCRIPTION_STATE =       90;
    public static final int TYPE_SUBSTRING_STATE =              91;
    public static final int SUBSTRINGS_STATE =                  92;
    public static final int INITIAL_STATE =                     93;
    public static final int ANY_STATE =                         94;
    public static final int FINAL_STATE =                       95;
    public static final int MATCHING_RULE_STATE =               96;
    public static final int TYPE_MATCHING_RULE_STATE =          97;
    public static final int MATCH_VALUE_STATE =                 98;
    public static final int DN_ATTRIBUTES_STATE =               99;
    public static final int OBJECT_NAME_STATE =                 100;
    public static final int ATTRIBUTES_SR_STATE =               101;
    public static final int PARTIAL_ATTRIBUTES_LIST_STATE =     102;
    public static final int TYPE_SR_STATE =                     103;
    public static final int VALS_SR_STATE =                     104;
    public static final int ATTRIBUTE_VALUE_SR_STATE =          105;
    public static final int REFERENCE_STATE =                   106;
    public static final int CONTROLS_STATE =                    107;
    public static final int CONTROL_STATE =                     108;
    public static final int CONTROL_TYPE_STATE =                109;
    public static final int CRITICALITY_STATE =                 110;
    public static final int CONTROL_VALUE_STATE =               111;
    public static final int INTERMEDIATE_RESPONSE_STATE =       112;
    public static final int INTERMEDIATE_RESPONSE_NAME_STATE =  113;
    public static final int INTERMEDIATE_RESPONSE_VALUE_STATE = 114;
    
    
    public static final int LAST_LDAP_STATE = 115;

    // =========================================================================
    // States debug strings
    // =========================================================================
    /** A string representation of all the states */
    private static String[] LdapMessageString = new String[]
        { 
        "START_STATE",
        "LDAP_MESSAGE_STATE",
        "MESSAGE_ID_STATE",
        "BIND_REQUEST_STATE",
        "BIND_RESPONSE_STATE",
        "UNBIND_REQUEST_STATE",
        "SEARCH_REQUEST_STATE",
        "SEARCH_RESULT_ENTRY_STATE",
        "SEARCH_RESULT_DONE_STATE",
        "SEARCH_RESULT_REFERENCE_STATE",
        "MODIFY_REQUEST_STATE",
        "MODIFY_RESPONSE_STATE",
        "ADD_REQUEST_STATE",
        "ADD_RESPONSE_STATE",
        "DEL_REQUEST_STATE",
        "DEL_RESPONSE_STATE",
        "MODIFY_DN_REQUEST_STATE",
        "MODIFY_DN_RESPONSE_STATE",
        "COMPARE_REQUEST_STATE",
        "COMPARE_RESPONSE_STATE",
        "ABANDON_REQUEST_STATE",
        "EXTENDED_REQUEST_STATE",
        "EXTENDED_RESPONSE_STATE",
        "VERSION_STATE",
        "NAME_STATE",
        "SIMPLE_STATE",
        "SASL_STATE",
        "MECHANISM_STATE",
        "CREDENTIALS_STATE",
        "RESULT_CODE_BR_STATE",
        "MATCHED_DN_BR_STATE",
        "ERROR_MESSAGE_BR_STATE",
        "REFERRALS_BR_STATE",
        "REFERRAL_BR_STATE",
        "SERVER_SASL_CREDENTIALS_STATE",
        "RESULT_CODE_STATE",
        "MATCHED_DN_STATE",
        "ERROR_MESSAGE_STATE",
        "REFERRALS_STATE",
        "REFERRAL_STATE",
        "REQUEST_NAME_STATE",
        "REQUEST_VALUE_STATE",
        "RESPONSE_NAME_STATE",
        "RESPONSE_STATE",
        "RESULT_CODE_ER_STATE",
        "MATCHED_DN_ER_STATE",
        "ERROR_MESSAGE_ER_STATE",
        "REFERRALS_ER_STATE",
        "REFERRAL_ER_STATE",
        "ENTRY_STATE",
        "ATTRIBUTES_STATE",
        "ATTRIBUTE_STATE",
        "TYPE_STATE",
        "VALUES_STATE",
        "VALUE_STATE",
        "OBJECT_STATE",
        "MODIFICATIONS_STATE",
        "MODIFICATIONS_SEQ_STATE",
        "OPERATION_STATE",
        "MODIFICATION_STATE",
        "TYPE_MOD_STATE",
        "VALS_STATE",
        "ATTRIBUTE_VALUE_STATE",
        "ENTRY_MOD_DN_STATE",
        "NEW_RDN_STATE",
        "DELETE_OLD_RDN_STATE",
        "NEW_SUPERIOR_STATE",
        "ENTRY_COMP_STATE",
        "AVA_STATE",
        "ATTRIBUTE_DESC_STATE",
        "ASSERTION_VALUE_STATE",
        "BASE_OBJECT_STATE",
        "SCOPE_STATE",
        "DEREF_ALIAS_STATE",
        "SIZE_LIMIT_STATE",
        "TIME_LIMIT_STATE",
        "TYPES_ONLY_STATE",
        "AND_STATE",
        "OR_STATE",
        "NOT_STATE",
        "EQUALITY_MATCH_STATE",
        "SUBSTRINGS_FILTER_STATE",
        "GREATER_OR_EQUAL_STATE",
        "LESS_OR_EQUAL_STATE",
        "PRESENT_STATE",
        "APPROX_MATCH_STATE",
        "EXTENSIBLE_MATCH_STATE",
        "SUBSTRING_FILTER_STATE",
        "ATTRIBUTE_DESC_FILTER_STATE",
        "ASSERTION_VALUE_FILTER_STATE",
        "ATTRIBUTE_DESCRIPTION_LIST_STATE",
        "ATTRIBUTE_DESCRIPTION_STATE",
        "TYPE_SUBSTRING_STATE",
        "SUBSTRINGS_STATE",
        "INITIAL_STATE",
        "ANY_STATE",
        "FINAL_STATE",
        "MATCHING_RULE_STATE",
        "TYPE_MATCHING_RULE_STATE",
        "MATCH_VALUE_STATE",
        "DN_ATTRIBUTES_STATE",
        "OBJECT_NAME_STATE",
        "ATTRIBUTES_SR_STATE",
        "PARTIAL_ATTRIBUTES_LIST_STATE",
        "TYPE_SR_STATE",
        "VALS_SR_STATE",
        "ATTRIBUTE_VALUE_SR_STATE",
        "REFERENCE_STATE",
        "CONTROLS_STATE",
        "CONTROL_STATE",
        "CONTROL_TYPE_STATE",
        "CRITICALITY_STATE",
        "CONTROL_VALUE_STATE",
        "INTERMEDIATE_RESPONSE_STATE",
        "INTERMEDIATE_RESPONSE_NAME_STATE",
        "INTERMEDIATE_RESPONSE_VALUE_STATE",
        "LAST_LDAP_STATE",
        };

    /** The instance */
    private static LdapStatesEnum instance = new LdapStatesEnum();


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * This is a private constructor. This class is a singleton
     */
    private LdapStatesEnum()
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
     * @param grammar
     *            The grammar code
     * @return The grammar name
     */
    public String getGrammarName( int grammar )
    {
        return "LDAP_MESSAGE_GRAMMAR";
    }


    /**
     * Get the grammar name
     * 
     * @param grammar
     *            The grammar class
     * @return The grammar name
     */
    public String getGrammarName( IGrammar grammar )
    {
        if ( grammar instanceof LdapMessageGrammar )
        {
            return "LDAP_MESSAGE_GRAMMAR";
        }
        else
        {
            return "UNKNOWN GRAMMAR";
        }
    }


    /**
     * Get the string representing the state
     * 
     * @param state The state number
     * @return The String representing the state
     */
    public String getState( int state )
    {
        return ( ( state == GRAMMAR_END ) ? "LDAP_MESSAGE_END_STATE" : LdapMessageString[state] );
    }
}

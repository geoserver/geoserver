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


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 910150 $, $Date: 2010-02-15 03:37:34 +0200 (Mon, 15 Feb 2010) $, 
 */
public class LdapConstants
{
    /** The scope constants */
    public static final int SCOPE_BASE_OBJECT = 0;

    public static final int SCOPE_SINGLE_LEVEL = 1;

    public static final int SCOPE_WHOLE_SUBTREE = 2;

    /** The DerefAlias constants */
    public static final int NEVER_DEREF_ALIASES = 0;

    public static final int DEREF_IN_SEARCHING = 1;

    public static final int DEREF_FINDING_BASE_OBJ = 2;

    public static final int DEREF_ALWAYS = 3;

    /** The operations */
    public static final int OPERATION_ADD = 0;

    public static final int OPERATION_DELETE = 1;

    public static final int OPERATION_REPLACE = 2;

    /** The filters */
    public static final int EQUALITY_MATCH_FILTER = 0;

    public static final int GREATER_OR_EQUAL_FILTER = 1;

    public static final int LESS_OR_EQUAL_FILTER = 2;

    public static final int APPROX_MATCH_FILTER = 3;

    /** LDAP contextual tags */
    public static final byte UNBIND_REQUEST_TAG = 0x42;

    public static final byte DEL_REQUEST_TAG = 0x4A;

    public static final byte ABANDON_REQUEST_TAG = 0x50;

    public static final byte BIND_REQUEST_TAG = 0x60;

    public static final byte BIND_RESPONSE_TAG = 0x61;

    public static final byte SEARCH_REQUEST_TAG = 0x63;

    public static final byte SEARCH_RESULT_ENTRY_TAG = 0x64;

    public static final byte SEARCH_RESULT_DONE_TAG = 0x65;

    public static final byte MODIFY_REQUEST_TAG = 0x66;

    public static final byte MODIFY_RESPONSE_TAG = 0x67;

    public static final byte ADD_REQUEST_TAG = 0x68;

    public static final byte ADD_RESPONSE_TAG = 0x69;

    public static final byte DEL_RESPONSE_TAG = 0x6B;

    public static final byte MODIFY_DN_REQUEST_TAG = 0x6C;

    public static final byte MODIFY_DN_RESPONSE_TAG = 0x6D;

    public static final byte COMPARE_REQUEST_TAG = 0x6E;

    public static final byte COMPARE_RESPONSE_TAG = 0x6F;

    public static final byte SEARCH_RESULT_REFERENCE_TAG = 0x73;

    public static final byte EXTENDED_REQUEST_TAG = 0x77;

    public static final byte EXTENDED_RESPONSE_TAG = 0x78;

    public static final byte INTERMEDIATE_RESPONSE_TAG = 0x79;

    // The following tags are ints, because bytes above 127 are negative
    // numbers, and we can't use them as array indexes.
    public static final int BIND_REQUEST_SIMPLE_TAG = 0x80;

    public static final int EXTENDED_REQUEST_NAME_TAG = 0x80;

    public static final int MODIFY_DN_REQUEST_NEW_SUPERIOR_TAG = 0x80;

    public static final int SUBSTRINGS_FILTER_INITIAL_TAG = 0x80;

    public static final int EXTENDED_REQUEST_VALUE_TAG = 0x81;

    public static final int MATCHING_RULE_ID_TAG = 0x81;

    public static final int SUBSTRINGS_FILTER_ANY_TAG = 0x81;

    public static final int MATCHING_RULE_TYPE_TAG = 0x82;

    public static final int SUBSTRINGS_FILTER_FINAL_TAG = 0x82;

    public static final int MATCH_VALUE_TAG = 0x83;

    public static final int DN_ATTRIBUTES_FILTER_TAG = 0x84;

    public static final int SERVER_SASL_CREDENTIAL_TAG = 0x87;

    public static final int PRESENT_FILTER_TAG = 0x87;

    public static final int EXTENDED_RESPONSE_RESPONSE_NAME_TAG = 0x8A;

    public static final int EXTENDED_RESPONSE_RESPONSE_TAG = 0x8B;

    public static final int CONTROLS_TAG = 0xA0;

    public static final int AND_FILTER_TAG = 0xA0;

    public static final int INTERMEDIATE_RESPONSE_NAME_TAG = 0x80;

    public static final int INTERMEDIATE_RESPONSE_VALUE_TAG = 0x81;

    public static final int OR_FILTER_TAG = 0xA1;

    public static final int NOT_FILTER_TAG = 0xA2;

    public static final int BIND_REQUEST_SASL_TAG = 0xA3;

    public static final int LDAP_RESULT_REFERRAL_SEQUENCE_TAG = 0xA3;

    public static final int EQUALITY_MATCH_FILTER_TAG = 0xA3;

    public static final int SUBSTRINGS_FILTER_TAG = 0xA4;

    public static final int GREATER_OR_EQUAL_FILTER_TAG = 0xA5;

    public static final int LESS_OR_EQUAL_FILTER_TAG = 0xA6;

    public static final int APPROX_MATCH_FILTER_TAG = 0xA8;

    public static final int EXTENSIBLE_MATCH_FILTER_TAG = 0xA9;

    // The messages types
    /*
    public static final int ABANDON_REQUEST = 0;

    public static final int ADD_REQUEST = 1;

    public static final int ADD_RESPONSE = 2;

    public static final int BIND_REQUEST = 3;

    public static final int BIND_RESPONSE = 4;

    public static final int COMPARE_REQUEST = 5;

    public static final int COMPARE_RESPONSE = 6;

    public static final int DEL_REQUEST = 7;

    public static final int DEL_RESPONSE = 8;

    public static final int EXTENDED_REQUEST = 9;

    public static final int EXTENDED_RESPONSE = 10;

    public static final int MODIFYDN_REQUEST = 11;

    public static final int MODIFYDN_RESPONSE = 12;

    public static final int MODIFY_REQUEST = 13;

    public static final int MODIFY_RESPONSE = 14;

    public static final int SEARCH_REQUEST = 15;

    public static final int SEARCH_RESULT_DONE = 16;

    public static final int SEARCH_RESULT_ENTRY = 17;

    public static final int SEARCH_RESULT_REFERENCE = 18;

    public static final int UNBIND_REQUEST = 19;
    
    public static final int INTERMEDIATE_RESPONSE = 20;

    public static final int UNKNOWN = -1; // Just in case ...
    */
}

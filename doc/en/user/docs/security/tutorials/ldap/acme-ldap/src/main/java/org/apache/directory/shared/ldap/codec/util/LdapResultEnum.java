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
package org.apache.directory.shared.ldap.codec.util;


/**
 * This typesafe-enum represent the different resultCode of a LdapResult.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664290 $, $Date: 2008-06-07 09:28:06 +0300 (Sat, 07 Jun 2008) $, 
 */
public class LdapResultEnum
{
    // ~ Static fields/initializers
    // -----------------------------------------------------------------

    public static final int SUCCESS = 0;

    public static final int OPERATIONS_ERROR = 1;

    public static final int PROTOCOL_ERROR = 2;

    public static final int TIME_LIMIT_EXCEEDED = 3;

    public static final int SIZE_LIMIT_EXCEEDED = 4;

    public static final int COMPARE_FALSE = 5;

    public static final int COMPARE_TRUE = 6;

    public static final int AUTH_METHOD_NOT_SUPPORTED = 7;

    public static final int STRONG_AUTH_REQUIRED = 8;

    // -- 9 reserved --
    public static final int RESERVED_9 = 9;

    public static final int REFERRAL = 10; // -- new

    public static final int ADMIN_LIMIT_EXCEEDED = 11; // -- new

    public static final int UNAVAILABLE_CRITICAL_EXTENSION = 12; // -- new

    public static final int CONFIDENTIALITY_REQUIRED = 13; // -- new

    public static final int SASL_BIND_IN_PROGRESS = 14; // -- new

    public static final int NO_SUCH_ATTRIBUTE = 16;

    public static final int UNDEFINED_ATTRIBUTE_TYPE = 17;

    public static final int INAPPROPRIATE_MATCHING = 18;

    public static final int CONSTRAINT_VIOLATION = 19;

    public static final int ATTRIBUTE_OR_VALUE_EXISTS = 20;

    public static final int INVALID_ATTRIBUTE_SYNTAX = 21;

    // -- 22-31 unused --
    public static final int UNUSED_22 = 22;

    public static final int UNUSED_23 = 23;

    public static final int UNUSED_24 = 24;

    public static final int UNUSED_25 = 25;

    public static final int UNUSED_26 = 26;

    public static final int UNUSED_27 = 27;

    public static final int UNUSED_28 = 28;

    public static final int UNUSED_29 = 29;

    public static final int UNUSED_30 = 30;

    public static final int UNUSED_31 = 31;

    public static final int NO_SUCH_OBJECT = 32;

    public static final int ALIAS_PROBLEM = 33;

    public static final int INVALID_DN_SYNTAX = 34;

    // Reserved
    public static final int RESERVED_FOR_UNDEFINED_IS_LEAF = 35;

    public static final int ALIAS_DEREFERENCING_PROBLEM = 36;

    // -- 37-47 unused --
    public static final int UNUSED_37 = 37;

    public static final int UNUSED_38 = 38;

    public static final int UNUSED_39 = 39;

    public static final int UNUSED_40 = 40;

    public static final int UNUSED_41 = 41;

    public static final int UNUSED_42 = 42;

    public static final int UNUSED_43 = 43;

    public static final int UNUSED_44 = 44;

    public static final int UNUSED_45 = 45;

    public static final int UNUSED_46 = 46;

    public static final int UNUSED_47 = 47;

    public static final int INAPPROPRIATE_AUTHENTICATION = 48;

    public static final int INVALID_CREDENTIALS = 49;

    public static final int INSUFFICIENT_ACCESS_RIGHTS = 50;

    public static final int BUSY = 51;

    public static final int UNAVAILABLE = 52;

    public static final int UNWILLING_TO_PERFORM = 53;

    public static final int LOOP_DETECT = 54;

    // -- 55-63 unused --
    public static final int UNUSED_55 = 55;

    public static final int UNUSED_56 = 56;

    public static final int UNUSED_57 = 57;

    public static final int UNUSED_58 = 58;

    public static final int UNUSED_59 = 59;

    public static final int UNUSED_60 = 60;

    public static final int UNUSED_61 = 61;

    public static final int UNUSED_62 = 62;

    public static final int UNUSED_63 = 63;

    public static final int NAMING_VIOLATION = 64;

    public static final int OBJECT_CLASS_VIOLATION = 65;

    public static final int NOT_ALLOWED_ON_NON_LEAF = 66;

    public static final int NOT_ALLOWED_ON_RDN = 67;

    public static final int ENTRY_ALREADY_EXISTS = 68;

    public static final int OBJECT_CLASS_MODS_PROHIBITED = 69;

    // -- 70 reserved for CLDAP --
    public static final int RESERVED_FOR_CLDAP = 70;

    public static final int AFFECTS_MULTIPLE_DSAS = 71; // -- new

    // -- 72-79 unused --
    public static final int UNUSED_72 = 72;

    public static final int UNUSED_73 = 73;

    public static final int UNUSED_74 = 74;

    public static final int UNUSED_75 = 75;

    public static final int UNUSED_76 = 76;

    public static final int UNUSED_77 = 77;

    public static final int UNUSED_78 = 78;

    public static final int UNUSED_79 = 79;

    public static final int OTHER = 80;

    // -- 81-90 reserved for APIs --
    public static final int RESERVED_FOR_APIS_81 = 81;

    public static final int RESERVED_FOR_APIS_82 = 82;

    public static final int RESERVED_FOR_APIS_83 = 83;

    public static final int RESERVED_FOR_APIS_84 = 84;

    public static final int RESERVED_FOR_APIS_85 = 85;

    public static final int RESERVED_FOR_APIS_86 = 86;

    public static final int RESERVED_FOR_APIS_87 = 87;

    public static final int RESERVED_FOR_APIS_88 = 88;

    public static final int RESERVED_FOR_APIS_89 = 89;

    public static final int RESERVED_FOR_APIS_90 = 90;


    public static final String errorCode( int resultCode )
    {
        switch ( resultCode )
        {
            case RESERVED_9:
                return "Reserved (9)";
            case UNUSED_22:
                return "Unused (22)";
            case UNUSED_23:
                return "Unused (23)";
            case UNUSED_24:
                return "Unused (24)";
            case UNUSED_25:
                return "Unused (25)";
            case UNUSED_26:
                return "Unused (26)";
            case UNUSED_27:
                return "Unused (27)";
            case UNUSED_28:
                return "Unused (28)";
            case UNUSED_29:
                return "Unused (29)";
            case UNUSED_30:
                return "Unused (30)";
            case UNUSED_31:
                return "Unused (31)";
            case RESERVED_FOR_UNDEFINED_IS_LEAF:
                return "Reserved for undefined is leaf (35)";
            case UNUSED_37:
                return "unused (37)";
            case UNUSED_38:
                return "unused (38)";
            case UNUSED_39:
                return "unused (39)";
            case UNUSED_40:
                return "unused (40)";
            case UNUSED_41:
                return "unused (41)";
            case UNUSED_42:
                return "unused (42)";
            case UNUSED_43:
                return "unused (43)";
            case UNUSED_44:
                return "unused (44)";
            case UNUSED_45:
                return "unused (45)";
            case UNUSED_46:
                return "unused (46)";
            case UNUSED_47:
                return "unused (47)";
            case UNUSED_55:
                return "unused (55)";
            case UNUSED_56:
                return "unused (56)";
            case UNUSED_57:
                return "unused (57)";
            case UNUSED_58:
                return "unused (58)";
            case UNUSED_59:
                return "unused (59)";
            case UNUSED_60:
                return "unused (60)";
            case UNUSED_61:
                return "unused (61)";
            case UNUSED_62:
                return "unused (62)";
            case UNUSED_63:
                return "unused (63)";
            case RESERVED_FOR_CLDAP:
                return "RESERVED_FOR_CLDAP (70)";
            case UNUSED_72:
                return "unused (72)";
            case UNUSED_73:
                return "unused (73)";
            case UNUSED_74:
                return "unused (74)";
            case UNUSED_75:
                return "unused (75)";
            case UNUSED_76:
                return "unused (76)";
            case UNUSED_77:
                return "unused (77)";
            case UNUSED_78:
                return "unused (78)";
            case UNUSED_79:
                return "unused (79)";
            case RESERVED_FOR_APIS_81:
                return "RESERVED_FOR_APIS (81)";
            case RESERVED_FOR_APIS_82:
                return "RESERVED_FOR_APIS (82)";
            case RESERVED_FOR_APIS_83:
                return "RESERVED_FOR_APIS (83)";
            case RESERVED_FOR_APIS_84:
                return "RESERVED_FOR_APIS (84)";
            case RESERVED_FOR_APIS_85:
                return "RESERVED_FOR_APIS (85)";
            case RESERVED_FOR_APIS_86:
                return "RESERVED_FOR_APIS (86)";
            case RESERVED_FOR_APIS_87:
                return "RESERVED_FOR_APIS (87)";
            case RESERVED_FOR_APIS_88:
                return "RESERVED_FOR_APIS (88)";
            case RESERVED_FOR_APIS_89:
                return "RESERVED_FOR_APIS (89)";
            case RESERVED_FOR_APIS_90:
                return "RESERVED_FOR_APIS (90)";
            default:
                return "UNKOWN";
        }
    }
}

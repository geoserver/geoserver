// $ANTLR 2.7.4: "schema.g" -> "AntlrSchemaParser.java"$

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
package org.apache.directory.shared.ldap.schema.syntax;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.parsers.ParserMonitor;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.OpenLdapObjectIdentifierMacro;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.UsageEnum;


public interface AntlrSchemaTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int WHSP = 4;
	int LPAR = 5;
	int RPAR = 6;
	int QUOTE = 7;
	int DOLLAR = 8;
	int LBRACKET = 9;
	int RBRACKET = 10;
	int LEN = 11;
	int SINGLE_VALUE = 12;
	int COLLECTIVE = 13;
	int NO_USER_MODIFICATION = 14;
	int OBSOLETE = 15;
	int ABSTRACT = 16;
	int STRUCTURAL = 17;
	int AUXILIARY = 18;
	int OBJECTIDENTIFIER = 19;
	int OBJECTCLASS = 20;
	int ATTRIBUTETYPE = 21;
	int STARTNUMERICOID = 22;
	int NAME = 23;
	int DESC = 24;
	int SUP = 25;
	int MUST = 26;
	int MAY = 27;
	int AUX = 28;
	int NOT = 29;
	int FORM = 30;
	int OC = 31;
	int EQUALITY = 32;
	int ORDERING = 33;
	int SUBSTR = 34;
	int SYNTAX = 35;
	int APPLIES = 36;
	int EXTENSION = 37;
	int FQCN = 38;
	int BYTECODE = 39;
	int AUX_OR_AUXILIARY = 40;
	int VALUES = 41;
	int VALUE = 42;
	int UNQUOTED_STRING = 43;
	int QUOTED_STRING = 44;
	int FQCN_VALUE = 45;
	int FQCN_IDENTIFIER = 46;
	int FQCN_LETTER = 47;
	int FQCN_LETTERORDIGIT = 48;
	int BYTECODE_VALUE = 49;
	int USAGE = 50;
	int USER_APPLICATIONS = 51;
	int DIRECTORY_OPERATION = 52;
	int DISTRIBUTED_OPERATION = 53;
	int DSA_OPERATION = 54;
}

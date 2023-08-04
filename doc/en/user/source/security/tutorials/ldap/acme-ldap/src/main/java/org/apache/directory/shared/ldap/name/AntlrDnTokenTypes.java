// $ANTLR 2.7.4: "distinguishedName.g" -> "AntlrDnParser.java"$

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
package org.apache.directory.shared.ldap.name;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import javax.naming.NameParser;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.schema.parsers.ParserMonitor;
import org.apache.directory.shared.ldap.util.StringTools;


public interface AntlrDnTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int COMMA = 4;
	int EQUALS = 5;
	int PLUS = 6;
	int HYPHEN = 7;
	int DQUOTE = 8;
	int SEMI = 9;
	int LANGLE = 10;
	int RANGLE = 11;
	int SPACE = 12;
	int NUMERICOID_OR_ALPHA_OR_DIGIT = 13;
	int NUMERICOID = 14;
	int DOT = 15;
	int NUMBER = 16;
	int LDIGIT = 17;
	int DIGIT = 18;
	int ALPHA = 19;
	int HEXPAIR_OR_ESCESC_ESCSHARP_OR_ESC = 20;
	int HEXPAIR = 21;
	int ESC = 22;
	int ESCESC = 23;
	int ESCSHARP = 24;
	int HEX = 25;
	int HEXVALUE_OR_SHARP = 26;
	int HEXVALUE = 27;
	int SHARP = 28;
	int UTFMB = 29;
	int LUTF1_REST = 30;
}

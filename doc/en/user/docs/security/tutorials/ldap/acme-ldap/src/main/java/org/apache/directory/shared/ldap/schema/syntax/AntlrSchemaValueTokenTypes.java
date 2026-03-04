// $ANTLR 2.7.4: "schema-value.g" -> "AntlrSchemaValueParser.java"$

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

import java.util.List;
import java.util.ArrayList;

import org.apache.directory.shared.ldap.schema.parsers.ParserMonitor;


public interface AntlrSchemaValueTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int WHSP = 4;
	int LPAR = 5;
	int RPAR = 6;
	int CHAR = 7;
	int LDIGIT = 8;
	int DIGIT = 9;
	int NUMBER = 10;
	int NUMBER2 = 11;
	int NUMERICOID = 12;
	int HYPEN = 13;
	int OTHER = 14;
	int DESCR = 15;
	int QUIRKS_DESCR = 16;
	int QUOTE = 17;
	int DOLLAR = 18;
	int LCURLY = 19;
	int RCURLY = 20;
	int LEN = 21;
	int DESCR_OR_QUIRKS_DESCR = 22;
}

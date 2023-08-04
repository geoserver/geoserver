// $ANTLR 2.7.4: "subtree-specification.g" -> "AntlrSubtreeSpecificationParser.java"$

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


package org.apache.directory.shared.ldap.subtree;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationModifier;
import org.apache.directory.shared.ldap.schema.NormalizerMappingResolver;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.util.ComponentsMonitor;
import org.apache.directory.shared.ldap.util.OptionalComponentsMonitor;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.StringValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface AntlrSubtreeSpecificationParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int LITERAL_end = 4;
	int OPEN_CURLY = 5;
	int SP = 6;
	int SEP = 7;
	int CLOSE_CURLY = 8;
	int ID_base = 9;
	int ID_specificExclusions = 10;
	int ID_chopBefore = 11;
	int COLON = 12;
	int ID_chopAfter = 13;
	int ID_minimum = 14;
	int ID_maximum = 15;
	int ID_specificationFilter = 16;
	int FILTER = 17;
	int SAFEUTF8STRING = 18;
	int INTEGER = 19;
	int DESCR = 20;
	int NUMERICOID = 21;
	int ID_item = 22;
	int ID_and = 23;
	int ID_or = 24;
	int ID_not = 25;
	int INTEGER_OR_NUMERICOID = 26;
	int DOT = 27;
	int DIGIT = 28;
	int LDIGIT = 29;
	int ALPHA = 30;
	int SAFEUTF8CHAR = 31;
	int FILTER_VALUE = 32;
}

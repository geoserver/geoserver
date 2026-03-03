// $ANTLR 2.7.4: "ACIItem.g" -> "AntlrACIItemParser.java"$

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


package org.apache.directory.shared.ldap.aci;


import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Enumeration;

import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationModifier;
import org.apache.directory.shared.ldap.util.ComponentsMonitor;
import org.apache.directory.shared.ldap.util.MandatoryAndOptionalComponentsMonitor;
import org.apache.directory.shared.ldap.util.MandatoryComponentsMonitor;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.apache.directory.shared.ldap.util.NoDuplicateKeysMap;
import org.apache.directory.shared.ldap.util.OptionalComponentsMonitor;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.entry.StringValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public interface AntlrACIItemParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int ATTRIBUTE_VALUE_CANDIDATE = 4;
	int RANGE_OF_VALUES_CANDIDATE = 5;
	int SP = 6;
	int OPEN_CURLY = 7;
	int SEP = 8;
	int CLOSE_CURLY = 9;
	int ID_identificationTag = 10;
	int SAFEUTF8STRING = 11;
	int ID_precedence = 12;
	int INTEGER = 13;
	int ID_authenticationLevel = 14;
	int ID_none = 15;
	int ID_simple = 16;
	int ID_strong = 17;
	int ID_itemOrUserFirst = 18;
	int ID_itemFirst = 19;
	int COLON = 20;
	int ID_userFirst = 21;
	int ID_protectedItems = 22;
	int ID_entry = 23;
	int ID_allUserAttributeTypes = 24;
	int ID_attributeType = 25;
	int ID_allAttributeValues = 26;
	int ID_allUserAttributeTypesAndValues = 27;
	int ID_selfValue = 28;
	int ID_maxValueCount = 29;
	int ID_type = 30;
	int ID_maxCount = 31;
	int ID_maxImmSub = 32;
	int ID_restrictedBy = 33;
	int ID_valuesIn = 34;
	int ID_classes = 35;
	int ID_itemPermissions = 36;
	int ID_grantsAndDenials = 37;
	int ID_grantAdd = 38;
	int ID_denyAdd = 39;
	int ID_grantDiscloseOnError = 40;
	int ID_denyDiscloseOnError = 41;
	int ID_grantRead = 42;
	int ID_denyRead = 43;
	int ID_grantRemove = 44;
	int ID_denyRemove = 45;
	int ID_grantBrowse = 46;
	int ID_denyBrowse = 47;
	int ID_grantExport = 48;
	int ID_denyExport = 49;
	int ID_grantImport = 50;
	int ID_denyImport = 51;
	int ID_grantModify = 52;
	int ID_denyModify = 53;
	int ID_grantRename = 54;
	int ID_denyRename = 55;
	int ID_grantReturnDN = 56;
	int ID_denyReturnDN = 57;
	int ID_grantCompare = 58;
	int ID_denyCompare = 59;
	int ID_grantFilterMatch = 60;
	int ID_denyFilterMatch = 61;
	int ID_grantInvoke = 62;
	int ID_denyInvoke = 63;
	int ID_userClasses = 64;
	int ID_allUsers = 65;
	int ID_thisEntry = 66;
	int ID_parentOfEntry = 67;
	int ID_name = 68;
	int ID_userGroup = 69;
	int ID_subtree = 70;
	int ID_userPermissions = 71;
	int ID_base = 72;
	int ID_specificExclusions = 73;
	int ID_chopBefore = 74;
	int ID_chopAfter = 75;
	int ID_minimum = 76;
	int ID_maximum = 77;
	int DESCR = 78;
	int NUMERICOID = 79;
	int ID_item = 80;
	int ID_and = 81;
	int ID_or = 82;
	int ID_not = 83;
	int ID_FALSE = 84;
	int ID_TRUE = 85;
	int ID_level = 86;
	int ID_basicLevels = 87;
	int ID_localQualifier = 88;
	int ID_signed = 89;
	int ID_rangeOfValues = 90;
	int ID_specificationFilter = 91;
	int SAFEUTF8CHAR = 92;
	int DIGIT = 93;
	int LDIGIT = 94;
	int ALPHA = 95;
	int HYPHEN = 96;
	int DOT = 97;
	int INTEGER_OR_NUMERICOID = 98;
	int FILTER = 99;
	int FILTER_VALUE = 100;
}

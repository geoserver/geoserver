// $ANTLR 2.7.4: "TriggerSpecification.g" -> "AntlrTriggerSpecificationParser.java"$

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


package org.apache.directory.shared.ldap.trigger;

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.NormalizerMappingResolver;
import org.apache.directory.shared.ldap.trigger.StoredProcedureOption;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification.SPSpec;
import org.apache.directory.shared.ldap.filter.SearchScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public interface AntlrTriggerSpecificationParserTokenTypes {
	int EOF = 1;
	int NULL_TREE_LOOKAHEAD = 3;
	int SP = 4;
	int ID_AFTER = 5;
	int ID_modify = 6;
	int OPEN_PARAN = 7;
	int CLOSE_PARAN = 8;
	int SEMI = 9;
	int ID_add = 10;
	int ID_delete = 11;
	int ID_modifyDN = 12;
	int DOT = 13;
	int ID_modifyDNRename = 14;
	int ID_modifyDNExport = 15;
	int ID_modifyDNImport = 16;
	int ID_CALL = 17;
	int SEP = 18;
	int ID_object = 19;
	int ID_modification = 20;
	int ID_oldEntry = 21;
	int ID_newEntry = 22;
	int ID_entry = 23;
	int ID_attributes = 24;
	int ID_name = 25;
	int ID_deletedEntry = 26;
	int ID_newrdn = 27;
	int ID_deleteoldrdn = 28;
	int ID_newSuperior = 29;
	int ID_oldRDN = 30;
	int ID_oldSuperiorDN = 31;
	int ID_newDN = 32;
	int ID_operationPrincipal = 33;
	int ID_ldapContext = 34;
	int OPEN_CURLY = 35;
	int CLOSE_CURLY = 36;
	int ID_languageScheme = 37;
	int UTF8String = 38;
	int ID_searchContext = 39;
	int ID_search_scope = 40;
	int ID_scope_base = 41;
	int ID_scope_one = 42;
	int ID_scope_subtree = 43;
	int SAFEUTF8CHAR = 44;
	int COMMENT = 45;
	int IDENTIFIER = 46;
	int ALPHA = 47;
}

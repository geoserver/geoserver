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
package org.apache.directory.shared.ldap.schema.registries;


import java.util.Iterator;

import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.DITStructureRule;


/**
 * An DITStructureRule registry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923524 $
 */
public interface DITStructureRuleRegistry extends SchemaObjectRegistry<DITStructureRule>,
    Iterable<DITStructureRule>
{
    /**
     * Checks to see if an DITStructureRule exists in the registry, by its
     * ruleId. 
     * 
     * @param oid the object identifier or name of the DITStructureRule
     * @return true if a DITStructureRule definition exists for the ruleId, false
     * otherwise
     */
    boolean contains( int ruleId );

    
    /**
     * Gets an iterator over the registered descriptions in the registry.
     *
     * @return an Iterator of descriptions
     */
    Iterator<DITStructureRule> iterator();
    
    
    /**
     * Gets an iterator over the registered ruleId in the registry.
     *
     * @return an Iterator of ruleId
     */
    Iterator<Integer> ruleIdIterator();
    
    
    /**
     * Gets the name of the schema this schema object is associated with.
     *
     * @param id the object identifier or the name
     * @return the schema name
     * @throws LdapException if the schema object does not exist
     */
    String getSchemaName( int ruleId ) throws LdapException;

    
    /**
     * Registers a new DITStructureRule with this registry.
     *
     * @param ditStructureRule the DITStructureRule to register
     * @throws LdapException if the DITStructureRule is already registered or
     * the registration operation is not supported
     */
    void register( DITStructureRule ditStructureRule ) throws LdapException;

    
    /**
     * Looks up an dITStructureRule by its unique Object IDentifier or by its
     * name.
     * 
     * @param ruleId the rule identifier for the DITStructureRule
     * @return the DITStructureRule instance for rule identifier
     * @throws LdapException if the DITStructureRule does not exist
     */
    DITStructureRule lookup( int ruleId ) throws LdapException;


    /**
     * Unregisters a DITStructureRule using it's rule identifier. 
     * 
     * @param ruleId the rule identifier for the DITStructureRule to unregister
     * @throws LdapException if no such DITStructureRule exists
     */
    void unregister( int ruleId ) throws LdapException;
    
    
    /**
     * Unregisters all DITStructureRules defined for a specific schema from
     * this registry.
     * 
     * @param schemaName the name of the schema whose syntaxCheckers will be removed from
     * @throws LdapException if no such SchemaElement exists
     */
    void unregisterSchemaElements( String schemaName ) throws LdapException;

    
    /**
     * Modify all the DITStructureRule using a schemaName when this name changes.
     *
     * @param originalSchemaName The original Schema name
     * @param newSchemaName The new Schema name
     * @throws LdapException if the schema can't be renamed
     */
    void renameSchema( String originalSchemaName, String newSchemaName ) throws LdapException;
    
    
    /**
     * Copy the DITStructureRuleRegistry
     */
    DITStructureRuleRegistry copy();
}

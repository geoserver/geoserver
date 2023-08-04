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


import java.util.Collection;
import java.util.List;

import org.apache.directory.shared.ldap.entry.Entry;


/**
 * Loads schemas into registres.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface SchemaLoader
{
    /**
     * Sets listener used to notify of newly loaded schemas.
     * 
     * @param listener the listener to notify (only one is enough for us)
     */
    void setListener( SchemaLoaderListener listener );


    /**
     * Gets a schema object based on it's name.
     * 
     * @param schemaName the name of the schema to load
     * @return the Schema object associated with the name
     */
    Schema getSchema( String schemaName );


    /**
     * Loads a set of schemas.  A best effort should be made to load the dependended 
     * schemas that these schemas may rely on even if they are not included in the collection.
     * 
     * @param registries the registries to populate with these schemas
     * @param check tells if the Registries must be checked after having been loaded
     * @param schemas the set of schemas to load
     * @return the list of erros we met during the loading of schemas
     * @throws Exception if any kind of problems are encountered during the load
     *
    List<Throwable> loadWithDependencies( Registries registries, boolean check, Schema... schemas ) throws Exception;
    
    
    /**
     * Loads all available enabled schemas.
     *
     * @param registries the registry to load all enabled schemas into
     * @param check tells if the Registries must be checked after having been loaded
     * @return the list of erros we met during the loading of schemas
     * @throws Exception if there are any failures
     *
    List<Throwable> loadAllEnabled( Registries registries, boolean check ) throws Exception;
    
    
    /**
     * Loads a single schema.  Do not try to resolve dependencies while implementing this method.
     * 
     * @param schema the schema to load
     * @param registries the registries to populate with these schemas
     * @param isDepLoad tells the loader if this load request is to satisfy a dependency
     * @throws Exception if any kind of problems are encountered during the load
     *
    void load( Schema schema, Registries registries, boolean isDepLoad ) throws Exception;
    
    
    /**
     * Build a list of AttributeTypes read from the underlying storage for
     * a list of specified schema
     *
     * @param schemas the schemas from which AttributeTypes are loaded
     * @throws Exception if there are failures accessing AttributeType information
     */
    List<Entry> loadAttributeTypes( Schema... schemas ) throws Exception;


    /**
     * Build a list of AttributeTypes read from the underlying storage for
     * a list of specific schema, using their name
     *
     * @param schemaNames the schema names from which AttributeTypes are loaded
     * @throws Exception if there are failures accessing AttributeType information
     */
    List<Entry> loadAttributeTypes( String... schemaNames ) throws Exception;


    /**
     * Build a list of Comparators read from the underlying storage for
     * a list of specific schema.
     *
     * @param schemas the schemas from which Comparators are loaded
     * @throws Exception if there are failures accessing Comparator information
     */
    List<Entry> loadComparators( Schema... schemas ) throws Exception;


    /**
     * Build a list of Comparators read from the underlying storage for
     * a list of specific schema, using their name
     *
     * @param schemaNames the schema names from which Comparators are loaded
     * @throws Exception if there are failures accessing Comparator information
     */
    List<Entry> loadComparators( String... schemaNames ) throws Exception;


    /**
     * Build a list of DitContentRules read from the underlying storage for
     * a list of specific schema.
     *
     * @param schemas the schemas from which DitContentRules are loaded
     * @throws Exception if there are failures accessing DitContentRule information
     */
    List<Entry> loadDitContentRules( Schema... schemas ) throws Exception;


    /**
     * Build a list of DitContentRules read from the underlying storage for
     * a list of specified schema names
     *
     * @param schemaNames the schema names from which DitContentRules are loaded
     * @throws Exception if there are failures accessing DitContentRule information
     */
    List<Entry> loadDitContentRules( String... schemanames ) throws Exception;


    /**
     * Build a list of DitStructureRules read from the underlying storage for
     * a list of specific schema.
     *
     * @param schemas the schemas from which DitStructureRules are loaded
     * @throws Exception if there are failures accessing DitStructureRule information
     */
    List<Entry> loadDitStructureRules( Schema... schemas ) throws Exception;


    /**
     * Build a list of DitStructureRules read from the underlying storage for
     * a list of specified schema names
     *
     * @param schemaNames the schema names from which DitStructureRules are loaded
     * @throws Exception if there are failures accessing DitStructureRule information
     */
    List<Entry> loadDitStructureRules( String... schemanames ) throws Exception;


    /**
     * Build a list of MatchingRules read from the underlying storage for
     * a list of specific schema
     *
     * @param schemas the schemas from which MatchingRules are loaded
     * @throws Exception if there are failures accessing MatchingRule information
     */
    List<Entry> loadMatchingRules( Schema... schemas ) throws Exception;


    /**
     * Build a list of MatchingRules read from the underlying storage for
     * a list of specific schema, using their name
     *
     * @param schemaNames the schema names from which MatchingRules are loaded
     * @throws Exception if there are failures accessing MatchingRule information
     */
    List<Entry> loadMatchingRules( String... schemaNames ) throws Exception;


    /**
     * Build a list of MatchingRuleUses read from the underlying storage for
     * a list of specific schema.
     *
     * @param schemas the schemas from which MatchingRuleUses are loaded
     * @throws Exception if there are failures accessing MatchingRuleUse information
     */
    List<Entry> loadMatchingRuleUses( Schema... schemas ) throws Exception;


    /**
     * Build a list of MatchingRuleUses read from the underlying storage for
     * a list of specified schema names
     *
     * @param schemaNames the schema names from which MatchingRuleUses are loaded
     * @throws Exception if there are failures accessing MatchingRuleUses information
     */
    List<Entry> loadMatchingRuleUses( String... schemanames ) throws Exception;


    /**
     * Build a list of NameForms read from the underlying storage for
     * a list of specific schema.
     *
     * @param schemas the schemas from which NameForms are loaded
     * @throws Exception if there are failures accessing NameForm information
     */
    List<Entry> loadNameForms( Schema... schemas ) throws Exception;


    /**
     * Build a list of NameForms read from the underlying storage for
     * a list of specified schema names
     *
     * @param schemaNames the schema names from which NameForms are loaded
     * @throws Exception if there are failures accessing NameForms information
     */
    List<Entry> loadNameForms( String... schemanames ) throws Exception;


    /**
     * Build a list of Normalizers read from the underlying storage for
     * a list of specified schema
     *
     * @param schemas the schemas from which Normalizers are loaded
     * @throws Exception if there are failures accessing Normalizer information
     */
    List<Entry> loadNormalizers( Schema... schemas ) throws Exception;


    /**
     * Build a list of Normalizers read from the underlying storage for
     * a list of specified schema names
     *
     * @param schemaNames the schema names from which Normalizers are loaded
     * @throws Exception if there are failures accessing Normalizer information
     */
    List<Entry> loadNormalizers( String... schemaNames ) throws Exception;


    /**
     * Build a list of ObjectClasses read from the underlying storage for
     * a list of specific schema.
     *
     * @param schemas the schemas from which ObjectClasses are loaded
     * @throws Exception if there are failures accessing ObjectClass information
     */
    List<Entry> loadObjectClasses( Schema... schemas ) throws Exception;


    /**
     * Build a list of ObjectClasses read from the underlying storage for
     * a list of specified schema names
     *
     * @param schemaNames the schema names from which ObjectClasses are loaded
     * @throws Exception if there are failures accessing ObjectClasses information
     */
    List<Entry> loadObjectClasses( String... schemaNames ) throws Exception;


    /**
     * Build a list of Syntaxes read from the underlying storage for
     * a list of specified schema
     *
     * @param schemas the schemas from which Syntaxes are loaded
     * @throws Exception if there are failures accessing Syntax information
     */
    List<Entry> loadSyntaxes( Schema... schemas ) throws Exception;


    /**
     * Build a list of Syntaxes read from the underlying storage for
     * a list of specified schema names
     *
     * @param schemaNames the schema names from which Syntaxes are loaded
     * @throws Exception if there are failures accessing Syntax information
     */
    List<Entry> loadSyntaxes( String... schemaNames ) throws Exception;


    /**
     * Build a list of SyntaxCheckers read from the underlying storage for
     * a list of specified schema
     *
     * @param schemas the schemas from which SyntaxCheckers are loaded
     * @throws Exception if there are failures accessing SyntaxChecker information
     */
    List<Entry> loadSyntaxCheckers( Schema... schemas ) throws Exception;


    /**
     * Build a list of SyntaxCheckers read from the underlying storage for
     * a list of specified schema names
     *
     * @param schemaNames the schema names from which SyntaxCheckers are loaded
     * @throws Exception if there are failures accessing SyntaxChecker information
     */
    List<Entry> loadSyntaxCheckers( String... schemanames ) throws Exception;


    /**
     * @return the list of enabled schemas
     * @throws Exception TODO
     */
    Collection<Schema> getAllEnabled() throws Exception;

    
    /**
     * @return the list of all schemas
     * @throws Exception TODO
     */
    Collection<Schema> getAllSchemas() throws Exception;

    
    /**
     * Add a new schema to the schema's list
     */
    void addSchema( Schema schema );

    
    /**
     * Remove a schema from the schema's list
     */
    void removeSchema( Schema schema );
}


package org.apache.directory.shared.ldap.schema;

import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.registries.Schema;

public interface EntityFactory
{
    /**
     * Return an instance of the Schema associated to the entry
     *
     * @param entry The Schema entry
     * @return An instance of a Schema
     * @throws Exception If the instance can't be created
     */
    Schema getSchema( Entry entry ) throws Exception;
    
    
    /**
     * Construct an AttributeType from an entry representing an AttributeType.
     *
     * @param schemaManager The Schema Manager
     * @param entry The entry containing all the informations to build an AttributeType
     * @param targetRegistries The registries containing all the enabled SchemaObjects
     * @param schemaName The schema this SchemaObject will be part of
     * @return An AttributeType SchemaObject
     * @throws LdapException If the AttributeType is invalid
     */
    AttributeType getAttributeType( SchemaManager schemaManager, Entry entry, Registries targetRegistries, String schemaName ) throws LdapException;

    
    /**
     * Construct a LdapComparator from a description of a comparator.
     *
     * @param schemaManager The Schema Manager
     * @param comparatorDescription The LdapComparator description object 
     * @param targetRegistries The registries containing all the enabled SchemaObjects
     * @param schemaName The schema this SchemaObject will be part of
     * @return A new instance of a LdapComparator
     * @throws Exception If the creation has failed
     */
    LdapComparator<?> getLdapComparator( SchemaManager schemaManager, 
        LdapComparatorDescription comparatorDescription, 
        Registries targetRegistries, String schemaName ) throws Exception;
    
    
    /**
     * Retrieve and load a Comparator class from the DIT.
     * 
     * @param schemaManager The Schema Manager
     * @param entry The entry containing all the informations to build a LdapComparator
     * @param targetRegistries The registries containing all the enabled SchemaObjects
     * @param schemaName The schema this SchemaObject will be part of
     * @return the loaded Comparator
     * @throws LdapException if anything fails during loading
     */
    LdapComparator<?> getLdapComparator( SchemaManager schemaManager, Entry entry, 
        Registries targetRegistries, String schemaName ) throws Exception;
    

    /**
     * Construct an MatchingRule from an entry get from the Dit
     *
     * @param schemaManager The Schema Manager
     * @param entry The entry containing all the informations to build a MatchingRule
     * @param targetRegistries The registries containing all the enabled SchemaObjects
     * @param schemaName The schema this SchemaObject will be part of
     * @return A MatchingRule SchemaObject
     * @throws LdapException If the MatchingRule is invalid
     */
    MatchingRule getMatchingRule( SchemaManager schemaManager, Entry entry, Registries targetRegistries, String schemaName ) throws LdapException;


    /**
     * Create a new instance of a Normalizer 
     *
     * @param schemaManager The Schema Manager
     * @param normalizerDescription The Normalizer description object 
     * @param targetRegistries The registries containing all the enabled SchemaObjects
     * @param schemaName The schema this SchemaObject will be part of
     * @return A new instance of a normalizer
     * @throws Exception If the creation has failed
     */
    Normalizer getNormalizer( SchemaManager schemaManager, NormalizerDescription normalizerDescription, 
        Registries targetRegistries, String schemaName ) throws Exception;
    
    
    /**
     * Retrieve and load a Normalizer class from the DIT.
     * 
     * @param schemaManager The Schema Manager
     * @param entry The entry containing all the informations to build a Normalizer
     * @param targetRegistries The registries containing all the enabled SchemaObjects
     * @param schemaName The schema this SchemaObject will be part of
     * @return the loaded Normalizer
     * @throws LdapException if anything fails during loading
     */
    Normalizer getNormalizer( SchemaManager schemaManager, Entry entry, Registries targetRegistries, String schemaName ) 
        throws Exception;
    
    
    /**
     * 
     * @param schemaManager The Schema Manager
     * @param entry The entry containing all the informations to build an ObjectClass
     * @param targetRegistries The registries containing all the enabled SchemaObjects
     * @param schemaName The schema this SchemaObject will be part of
     * @return
     * @throws Exception
     */
    ObjectClass getObjectClass( SchemaManager schemaManager, Entry entry, Registries targetRegistries, String schemaName ) throws Exception;
    
    
    /**
     * 
     * @param schemaManager The Schema Manager
     * @param entry The entry containing all the informations to build a LdapSyntax
     * @param targetRegistries The registries containing all the enabled SchemaObjects
     * @param schemaName The schema this SchemaObject will be part of
     * @return
     * @throws LdapException
     */
    LdapSyntax getSyntax( SchemaManager schemaManager, Entry entry, Registries targetRegistries, String schemaName ) throws LdapException;
    
    
    /**
     * Retrieve and load a syntaxChecker class from the DIT.
     * 
     * @param schemaManager The Schema Manager
     * @param entry The entry containing all the informations to build a SyntaxChecker
     * @param targetRegistries The registries containing all the enabled SchemaObjects
     * @param schemaName The schema this SchemaObject will be part of
     * @return the loaded SyntaxChecker
     * @throws LdapException if anything fails during loading
     */
    SyntaxChecker getSyntaxChecker( SchemaManager schemaManager, Entry entry, Registries targetRegistries, String schemaName ) throws Exception;
    

    /**
     * Create a new instance of a SyntaxChecker 
     *
     * @param schemaManager The Schema Manager
     * @param syntaxCheckerDescription The SyntaxChecker description object 
     * @param targetRegistries The registries containing all the enabled SchemaObjects
     * @param schemaName The schema this SchemaObject will be part of
     * @return A new instance of a syntaxChecker
     * @throws Exception If the creation has failed
     */
    SyntaxChecker getSyntaxChecker( SchemaManager schemaManager, SyntaxCheckerDescription syntaxCheckerDescription, 
        Registries targetRegistries, String schemaName ) throws Exception;
}

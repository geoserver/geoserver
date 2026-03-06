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


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapProtocolErrorException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.exception.LdapSchemaViolationException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.LoadableSchemaObject;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectWrapper;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Document this class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 927122 $
 */
public class Registries implements SchemaLoaderListener, Cloneable
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( Registries.class );

    /**
     * A String name to Schema object map for the schemas loaded into this
     * registry. The loaded schemas may be disabled.
     */
    protected Map<String, Schema> loadedSchemas = new HashMap<String, Schema>();

    /** The AttributeType registry */
    protected AttributeTypeRegistry attributeTypeRegistry;

    /** The ObjectClass registry */
    protected ObjectClassRegistry objectClassRegistry;

    /** The LdapSyntax registry */
    protected ComparatorRegistry comparatorRegistry;

    /** The DitContentRule registry */
    protected DITContentRuleRegistry ditContentRuleRegistry;

    /** The DitStructureRule registry */
    protected DITStructureRuleRegistry ditStructureRuleRegistry;

    /** The MatchingRule registry */
    protected MatchingRuleRegistry matchingRuleRegistry;

    /** The MatchingRuleUse registry */
    protected MatchingRuleUseRegistry matchingRuleUseRegistry;

    /** The NameForm registry */
    protected NameFormRegistry nameFormRegistry;

    /** The Normalizer registry */
    protected NormalizerRegistry normalizerRegistry;

    /** The global OID registry */
    protected OidRegistry globalOidRegistry;

    /** The SyntaxChecker registry */
    protected SyntaxCheckerRegistry syntaxCheckerRegistry;

    /** The LdapSyntax registry */
    protected LdapSyntaxRegistry ldapSyntaxRegistry;

    /** A map storing all the schema objects associated with a schema */
    private Map<String, Set<SchemaObjectWrapper>> schemaObjects;

    /** A flag indicating that the Registries is relaxed or not */
    private boolean isRelaxed;

    /** A flag indicating that disabled SchemaObject are accepted */
    private boolean disabledAccepted;

    /** Two flags for RELAXED and STRUCT */
    public static final boolean STRICT = false;
    public static final boolean RELAXED = true;

    /**
     *  A map storing a relation between a SchemaObject and all the 
     *  referencing SchemaObjects.
     */
    protected Map<SchemaObjectWrapper, Set<SchemaObjectWrapper>> usedBy;

    /**
     *  A map storing a relation between a SchemaObject and all the 
     *  SchemaObjects it uses.
     */
    protected Map<SchemaObjectWrapper, Set<SchemaObjectWrapper>> using;

    /** A reference on the schema Manager */
    private SchemaManager schemaManager;


    /**
     * Creates a new instance of Registries.
     *
     * @param oidRegistry the OID registry
     */
    public Registries( SchemaManager schemaManager )
    {
        this.globalOidRegistry = new OidRegistry();
        attributeTypeRegistry = new DefaultAttributeTypeRegistry();
        comparatorRegistry = new DefaultComparatorRegistry();
        ditContentRuleRegistry = new DefaultDITContentRuleRegistry();
        ditStructureRuleRegistry = new DefaultDITStructureRuleRegistry();
        ldapSyntaxRegistry = new DefaultLdapSyntaxRegistry();
        matchingRuleRegistry = new DefaultMatchingRuleRegistry();
        matchingRuleUseRegistry = new DefaultMatchingRuleUseRegistry();
        nameFormRegistry = new DefaultNameFormRegistry();
        normalizerRegistry = new DefaultNormalizerRegistry();
        objectClassRegistry = new DefaultObjectClassRegistry();
        syntaxCheckerRegistry = new DefaultSyntaxCheckerRegistry();
        schemaObjects = new HashMap<String, Set<SchemaObjectWrapper>>();
        usedBy = new HashMap<SchemaObjectWrapper, Set<SchemaObjectWrapper>>();
        using = new HashMap<SchemaObjectWrapper, Set<SchemaObjectWrapper>>();

        isRelaxed = STRICT;
        disabledAccepted = false;
        this.schemaManager = schemaManager;
    }


    /**
     * @return The AttributeType registry
     */
    public AttributeTypeRegistry getAttributeTypeRegistry()
    {
        return attributeTypeRegistry;
    }


    /**
     * @return The Comparator registry
     */
    public ComparatorRegistry getComparatorRegistry()
    {
        return comparatorRegistry;
    }


    /**
     * @return The DITContentRule registry
     */
    public DITContentRuleRegistry getDitContentRuleRegistry()
    {
        return ditContentRuleRegistry;
    }


    /**
     * @return The DITStructureRule registry
     */
    public DITStructureRuleRegistry getDitStructureRuleRegistry()
    {
        return ditStructureRuleRegistry;
    }


    /**
     * @return The MatchingRule registry
     */
    public MatchingRuleRegistry getMatchingRuleRegistry()
    {
        return matchingRuleRegistry;
    }


    /**
     * @return The MatchingRuleUse registry
     */
    public MatchingRuleUseRegistry getMatchingRuleUseRegistry()
    {
        return matchingRuleUseRegistry;
    }


    /**
     * @return The NameForm registry
     */
    public NameFormRegistry getNameFormRegistry()
    {
        return nameFormRegistry;
    }


    /**
     * @return The Normalizer registry
     */
    public NormalizerRegistry getNormalizerRegistry()
    {
        return normalizerRegistry;
    }


    /**
     * @return The ObjectClass registry
     */
    public ObjectClassRegistry getObjectClassRegistry()
    {
        return objectClassRegistry;
    }


    /**
     * @return The global Oid registry
     */
    public OidRegistry getGlobalOidRegistry()
    {
        return globalOidRegistry;
    }


    /**
     * @return The SyntaxChecker registry
     */
    public SyntaxCheckerRegistry getSyntaxCheckerRegistry()
    {
        return syntaxCheckerRegistry;
    }


    /**
     * @return The LdapSyntax registry
     */
    public LdapSyntaxRegistry getLdapSyntaxRegistry()
    {
        return ldapSyntaxRegistry;
    }


    /**
     * Get an OID from a name. As we have many possible registries, we 
     * have to look in all of them to get the one containing the OID.
     *
     * @param name The name we are looking at
     * @return The associated OID
     */
    public String getOid( String name )
    {
        // we have many possible Registries to look at.
        // AttributeType
        try
        {
            AttributeType attributeType = attributeTypeRegistry.lookup( name );

            if ( attributeType != null )
            {
                return attributeType.getOid();
            }
        }
        catch ( LdapException ne )
        {
            // Fall down to the next registry
        }

        // ObjectClass
        try
        {
            ObjectClass objectClass = objectClassRegistry.lookup( name );

            if ( objectClass != null )
            {
                return objectClass.getOid();
            }
        }
        catch ( LdapException ne )
        {
            // Fall down to the next registry
        }

        // LdapSyntax
        try
        {
            LdapSyntax ldapSyntax = ldapSyntaxRegistry.lookup( name );

            if ( ldapSyntax != null )
            {
                return ldapSyntax.getOid();
            }
        }
        catch ( LdapException ne )
        {
            // Fall down to the next registry
        }

        // MatchingRule
        try
        {
            MatchingRule matchingRule = matchingRuleRegistry.lookup( name );

            if ( matchingRule != null )
            {
                return matchingRule.getOid();
            }
        }
        catch ( LdapException ne )
        {
            // Fall down to the next registry
        }

        // MatchingRuleUse
        try
        {
            MatchingRuleUse matchingRuleUse = matchingRuleUseRegistry.lookup( name );

            if ( matchingRuleUse != null )
            {
                return matchingRuleUse.getOid();
            }
        }
        catch ( LdapException ne )
        {
            // Fall down to the next registry
        }

        // NameForm
        try
        {
            NameForm nameForm = nameFormRegistry.lookup( name );

            if ( nameForm != null )
            {
                return nameForm.getOid();
            }
        }
        catch ( LdapException ne )
        {
            // Fall down to the next registry
        }

        // DITContentRule
        try
        {
            DITContentRule ditContentRule = ditContentRuleRegistry.lookup( name );

            if ( ditContentRule != null )
            {
                return ditContentRule.getOid();
            }
        }
        catch ( LdapException ne )
        {
            // Fall down to the next registry
        }

        // DITStructureRule
        try
        {
            DITStructureRule ditStructureRule = ditStructureRuleRegistry.lookup( name );

            if ( ditStructureRule != null )
            {
                return ditStructureRule.getOid();
            }
        }
        catch ( LdapException ne )
        {
            // No more registries to look at...
        }

        return null;
    }


    /**
     * Gets a schema that has been loaded into these Registries.
     * 
     * @param schemaName the name of the schema to lookup
     * @return the loaded Schema if one corresponding to the name exists
     */
    public Schema getLoadedSchema( String schemaName )
    {
        return loadedSchemas.get( StringTools.toLowerCase( schemaName ) );
    }


    /**
     * Checks to see if a particular Schema is loaded.
     *
     * @param schemaName the name of the Schema to check
     * @return true if the Schema is loaded, false otherwise
     */
    public boolean isSchemaLoaded( String schemaName )
    {
        return loadedSchemas.containsKey( StringTools.toLowerCase( schemaName ) );
    }


    // ------------------------------------------------------------------------
    // Code used to sanity check the resolution of entities in registries
    // ------------------------------------------------------------------------
    /**
     * Attempts to resolve the dependent schema objects of all entities that
     * refer to other objects within the registries.  Null references will be
     * handed appropriately.
     * The order in which the SchemaObjects must be :
     * <li/>1) Normalizers, Comparators and SyntaxCheckers (as they depend on nothing)
     * <li/>2) Syntaxes (depend on SyntaxCheckers)
     * <li/>3) MatchingRules (depend on Syntaxes, Normalizers and Comparators
     * <li/>4) AttributeTypes (depend on MatchingRules, Syntaxes and AttributeTypes : in this case, we first handle the superior)
     * <li/>5) ObjectClasses (depend on AttributeTypes and ObjectClasses)
     * <br/><br/>
     * Later, when we will support them :
     * <li/>6) MatchingRuleUses (depend on matchingRules and AttributeTypes)
     * <li/>7) DitContentRules (depend on ObjectClasses and AttributeTypes)
     * <li/>8) NameForms (depends on ObjectClasses and AttributeTypes)
     * <li/>9) DitStructureRules (depends onNameForms and DitStructureRules)      * 
     *
     * @return a list of exceptions encountered while resolving entities
     */
    public List<Throwable> checkRefInteg()
    {
        ArrayList<Throwable> errors = new ArrayList<Throwable>();

        // Step 1 :
        // We start with Normalizers, Comparators and SyntaxCheckers
        // as they depend on nothing
        // Check the Normalizers
        for ( Normalizer normalizer : normalizerRegistry )
        {
            resolve( normalizer, errors );
        }

        // Check the Comparators
        for ( LdapComparator<?> comparator : comparatorRegistry )
        {
            resolve( comparator, errors );
        }

        // Check the SyntaxCheckers
        for ( SyntaxChecker syntaxChecker : syntaxCheckerRegistry )
        {
            resolve( syntaxChecker, errors );
        }

        // Step 2 :
        // Check the LdapSyntaxes
        for ( LdapSyntax ldapSyntax : ldapSyntaxRegistry )
        {
            resolve( ldapSyntax, errors );
        }

        // Step 3 :
        // Check the matchingRules
        for ( MatchingRule matchingRule : matchingRuleRegistry )
        {
            resolve( matchingRule, errors );
        }

        // Step 4 :
        // Check the AttributeTypes
        for ( AttributeType attributeType : attributeTypeRegistry )
        {
            resolve( attributeType, errors );
        }

        //  Step 5 :
        // Check the ObjectClasses
        for ( ObjectClass objectClass : objectClassRegistry )
        {
            resolve( objectClass, errors );
        }

        // Step 6-9 aren't yet defined
        return errors;
    }


    /**
     * Add the SchemaObjectReferences. This method does nothing, it's just
     * a catch all. The other methods will be called for each specific 
     * schemaObject
     *
    public void addCrossReferences( SchemaObject schemaObject )
    {
        // Do nothing : it's a catch all method.
    }
    
    
    /**
     * Delete the AT references (using and usedBy) : 
     * AT -> MR (for EQUALITY, ORDERING and SUBSTR)
     * AT -> S
     * AT -> AT
     */
    public void delCrossReferences( AttributeType attributeType )
    {
        if ( attributeType.getEquality() != null )
        {
            delReference( attributeType, attributeType.getEquality() );
        }

        if ( attributeType.getOrdering() != null )
        {
            delReference( attributeType, attributeType.getOrdering() );
        }

        if ( attributeType.getSubstring() != null )
        {
            delReference( attributeType, attributeType.getSubstring() );
        }

        if ( attributeType.getSyntax() != null )
        {
            delReference( attributeType, attributeType.getSyntax() );
        }

        if ( attributeType.getSuperior() != null )
        {
            delReference( attributeType, attributeType.getSuperior() );
        }
    }


    /**
     * Some specific controls must be checked : 
     * - an AT must have either a SYNTAX or a SUP. If there is no SYNTAX, then
     * the AT will take it's superior SYNTAX;
     * - if there is no EQUALITY, ORDERING or SUBSTRING MR, and if there is 
     * a SUP, then the AT will use its parent MR, if any;
     * - if an AT has a superior, then its usage must be the same than its
     * superior Usage;
     * - if an AT is COLLECTIVE, then its usage must be userApplications;
     * - if an AT is NO-USER-MODIFICATION, then its usage must be one of
     * directoryOperation, distributedOperation or dSAOperation;
     * - if an AT has a superior, and if its superior is COLLECTIVE, then
     * the AT will be COLLECTIVE too
     * 
     *
    private void buildRecursiveAttributeTypeReferences( List<Throwable> errors, Set<String> done, AttributeType attributeType )
    {
        buildReference( errors, attributeType );
        // An attributeType has references on Syntax, MatchingRule and itself
        try
        {
            attributeType.addToRegistries( this );
        }
        catch ( LdapException ne )
        {
            String msg = "Cannot build the AttributeType references for the object " + attributeType.getName() +
                ", error : " + ne.getMessage();
            
            Throwable error = new LdapSchemaViolationException( 
                msg, ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
            errors.add( error );
            LOG.info( msg );
        }
        
        // First, check if the AT has a superior
        //buildSuperior( errors, done, attributeType );
        
        // The LdapSyntax (cannot be null)
        //buildSyntax( errors, attributeType );
        
        // The equality MR. 
        //buildEquality( errors, attributeType );

        // The ORDERING MR.
        //buildOrdering( errors, attributeType );
        
        // The SUBSTR MR.
        //buildSubstring( errors, attributeType );
        
        // Last, not least, check some of the other constraints
        //checkUsage( errors, attributeType );
        //checkCollective( errors, attributeType );
        
        // Update the dedicated fields
        /*try
        {
            attributeTypeRegistry.addMappingFor( attributeType );
        }
        catch ( LdapException ne )
        {
            errors.add( ne );
            LOG.info( ne.getMessage() );
        }
        
        // Update the cross references
        addCrossReferences( attributeType );
    }
    
    
    /**
     * Build the AttributeType references. This has to be done recursively, as
     * an AttributeType may inherit its parent's MatchingRules. The references
     * to update are :
     * - EQUALITY MR
     * - ORDERING MR
     * - SUBSTRING MR
     * - SUP AT
     * - SYNTAX
     */
    private void buildAttributeTypeReferences( List<Throwable> errors )
    {
        for ( AttributeType attributeType : attributeTypeRegistry )
        {
            if ( ( getUsing( attributeType ) == null ) || getUsing( attributeType ).isEmpty() )
            {
                buildReference( errors, attributeType );
            }
        }
    }


    /**
     * Build the Comparator references
     */
    private void buildComparatorReferences( List<Throwable> errors )
    {
        for ( LdapComparator<?> comparator : comparatorRegistry )
        {
            buildReference( errors, comparator );
        }
    }


    /**
     * Build the DitContentRule references
     */
    private void buildDitContentRuleReferences( List<Throwable> errors )
    {
        for ( DITContentRule ditContentRule : ditContentRuleRegistry )
        {
            // TODO
        }
    }


    /**
     * Build the DitStructureRule references
     */
    private void buildDitStructureRuleReferences( List<Throwable> errors )
    {
        for ( DITStructureRule ditStructureRule : ditStructureRuleRegistry )
        {
            // TODO
        }
    }


    /**
     * Delete the MR references (using and usedBy) : 
     * MR -> C
     * MR -> N
     * MR -> S
     */
    public void delCrossReferences( MatchingRule matchingRule )
    {
        if ( matchingRule.getLdapComparator() != null )
        {
            delReference( matchingRule, matchingRule.getLdapComparator() );
        }

        if ( matchingRule.getNormalizer() != null )
        {
            delReference( matchingRule, matchingRule.getNormalizer() );
        }

        if ( matchingRule.getSyntax() != null )
        {
            delReference( matchingRule, matchingRule.getSyntax() );
        }
    }


    /**
     * Build the SchemaObject references
     */
    public void buildReference( List<Throwable> errors, SchemaObject schemaObject )
    {
        try
        {
            schemaObject.addToRegistries( errors, this );
        }
        catch ( LdapException ne )
        {
            // Not allowed.
            String msg = I18n.err( I18n.ERR_04292, schemaObject.getName(), ne.getLocalizedMessage() );

            Throwable error = new LdapProtocolErrorException( msg );
            errors.add( error );
            LOG.info( msg );
        }
    }


    /**
     * Unlink the SchemaObject references
     */
    public void removeReference( List<Throwable> errors, SchemaObject schemaObject )
    {
        try
        {
            schemaObject.removeFromRegistries( errors, this );
        }
        catch ( LdapException ne )
        {
            // Not allowed.
            String msg = I18n.err( I18n.ERR_04293, schemaObject.getName(), ne.getLocalizedMessage() );

            Throwable error = new LdapSchemaViolationException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, msg );
            errors.add( error );
            LOG.info( msg );
        }
    }


    /**
     * Build the MatchingRule references
     */
    private void buildMatchingRuleReferences( List<Throwable> errors )
    {
        for ( MatchingRule matchingRule : matchingRuleRegistry )
        {
            buildReference( errors, matchingRule );
        }
    }


    /**
     * Build the MatchingRuleUse references
     */
    private void buildMatchingRuleUseReferences( List<Throwable> errors )
    {
        for ( MatchingRuleUse matchingRuleUse : matchingRuleUseRegistry )
        {
            buildReference( errors, matchingRuleUse );
        }
    }


    /**
     * Build the NameForm references
     */
    private void buildNameFormReferences( List<Throwable> errors )
    {
        for ( NameForm nameFormRule : nameFormRegistry )
        {
            // TODO
        }
    }


    /**
     * Build the Normalizer references
     */
    private void buildNormalizerReferences( List<Throwable> errors )
    {
        for ( Normalizer normalizer : normalizerRegistry )
        {
            buildReference( errors, normalizer );
        }
    }


    /**
     * Build the ObjectClasses references
     */
    private void buildObjectClassReferences( List<Throwable> errors )
    {
        // Remember the OC we have already processed
        Set<String> done = new HashSet<String>();

        // The ObjectClass
        for ( ObjectClass objectClass : objectClassRegistry )
        {
            if ( done.contains( objectClass.getOid() ) )
            {
                continue;
            }
            else
            {
                done.add( objectClass.getOid() );
            }

            buildReference( errors, objectClass );
        }
    }


    /**
     * Build the Syntax references
     */
    private void buildLdapSyntaxReferences( List<Throwable> errors )
    {
        for ( LdapSyntax syntax : ldapSyntaxRegistry )
        {
            buildReference( errors, syntax );
        }
    }


    /**
     * Build the SyntaxChecker references
     */
    private void buildSyntaxCheckerReferences( List<Throwable> errors )
    {
        for ( SyntaxChecker syntaxChecker : syntaxCheckerRegistry )
        {
            buildReference( errors, syntaxChecker );
        }
    }


    /**
     * Build the usedBy and using references from the stored elements.
     * 
     * @return A list of all the errors we met during the cross reference update
     */
    public List<Throwable> buildReferences()
    {
        List<Throwable> errors = new ArrayList<Throwable>();

        // The Comparator references
        buildComparatorReferences( errors );

        // The Normalizer references
        buildNormalizerReferences( errors );

        // The SyntaxChecker references
        buildSyntaxCheckerReferences( errors );

        // The Syntax references
        buildLdapSyntaxReferences( errors );

        // The MatchingRules references
        buildMatchingRuleReferences( errors );

        // The AttributeType references
        buildAttributeTypeReferences( errors );

        // The MatchingRuleUse references
        buildMatchingRuleUseReferences( errors );

        // The ObjectClasses references
        buildObjectClassReferences( errors );

        // The DitContentRules references
        buildDitContentRuleReferences( errors );

        // The NameForms references
        buildNameFormReferences( errors );

        // The DitStructureRules references
        buildDitStructureRuleReferences( errors );

        return errors;
    }


    /**
     * Attempts to resolve the SyntaxChecker associated with a Syntax.
     *
     * @param syntax the LdapSyntax to resolve the SyntaxChecker of
     * @param errors the list of errors to add exceptions to
     */
    private void resolve( LdapSyntax syntax, List<Throwable> errors )
    {
        // A LdapSyntax must point to a valid SyntaxChecker
        // or to the OctetString SyntaxChecker
        try
        {
            syntax.addToRegistries( errors, this );
        }
        catch ( LdapException e )
        {
            errors.add( e );
        }
    }


    /**
     * Attempts to resolve the Normalizer
     *
     * @param normalizer the Normalizer
     * @param errors the list of errors to add exceptions to
     */
    private void resolve( Normalizer normalizer, List<Throwable> errors )
    {
        // This is currently doing nothing.
        try
        {
            normalizer.addToRegistries( errors, this );
        }
        catch ( LdapException e )
        {
            errors.add( e );
        }
    }


    /**
     * Attempts to resolve the LdapComparator
     *
     * @param comparator the LdapComparator
     * @param errors the list of errors to add exceptions to
     */
    private void resolve( LdapComparator<?> comparator, List<Throwable> errors )
    {
        // This is currently doing nothing.
        try
        {
            comparator.addToRegistries( errors, this );
        }
        catch ( LdapException e )
        {
            errors.add( e );
        }
    }


    /**
     * Attempts to resolve the SyntaxChecker
     *
     * @param normalizer the SyntaxChecker
     * @param errors the list of errors to add exceptions to
     */
    private void resolve( SyntaxChecker syntaxChecker, List<Throwable> errors )
    {
        // This is currently doing nothing.
        try
        {
            syntaxChecker.addToRegistries( errors, this );
        }
        catch ( LdapException e )
        {
            errors.add( e );
        }
    }


    /**
     * Check if the Comparator, Normalizer and the syntax are 
     * existing for a matchingRule.
     */
    private void resolve( MatchingRule matchingRule, List<Throwable> errors )
    {
        // Process the Syntax. It can't be null
        String syntaxOid = matchingRule.getSyntaxOid();

        if ( syntaxOid != null )
        {
            // Check if the Syntax is present in the registries
            try
            {
                ldapSyntaxRegistry.lookup( syntaxOid );
            }
            catch ( LdapException ne )
            {
                // This MR's syntax has not been loaded into the Registries.
                errors.add( ne );
            }
        }
        else
        {
            // This is an error. 
            Throwable error = new LdapProtocolErrorException( 
            		I18n.err( I18n.ERR_04294, matchingRule.getOid() ) );
            errors.add( error );
        }

        // Process the Normalizer
        Normalizer normalizer = matchingRule.getNormalizer();

        if ( normalizer == null )
        {
            // Ok, no normalizer, this is an error
            Throwable error = new LdapSchemaViolationException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX,
            		I18n.err( I18n.ERR_04295, matchingRule.getOid() ) );
            errors.add( error );
        }

        // Process the Comparator
        LdapComparator<?> comparator = matchingRule.getLdapComparator();

        if ( comparator == null )
        {
            // Ok, no comparator, this is an error
            Throwable error = new LdapSchemaViolationException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX,
            		I18n.err( I18n.ERR_04296, matchingRule.getOid() ) );
            errors.add( error );
        }
    }


    /**
     * Check AttributeType referential integrity
     */
    private void resolveRecursive( AttributeType attributeType, Set<String> processed, List<Throwable> errors )
    {
        // Process the Superior, if any
        String superiorOid = attributeType.getSuperiorOid();

        AttributeType superior = null;

        if ( superiorOid != null )
        {
            // Check if the Superior is present in the registries
            try
            {
                superior = attributeTypeRegistry.lookup( superiorOid );
            }
            catch ( LdapException ne )
            {
                // This AT's superior has not been loaded into the Registries.
                if ( !processed.contains( superiorOid ) )
                {
                    errors.add( ne );
                }
            }

            // We now have to process the superior, if it hasn't been 
            // processed yet.
            if ( superior != null )
            {
                if ( !processed.contains( superiorOid ) )
                {
                    resolveRecursive( superior, processed, errors );
                    processed.add( attributeType.getOid() );
                }
                else
                {
                    // Not allowed : we have a cyle
                    Throwable error = new LdapSchemaViolationException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX,
                        I18n.err( I18n.ERR_04297, attributeType.getOid() ) );
                    errors.add( error );
                    return;
                }
            }
        }

        // Process the Syntax. If it's null, the attributeType must have 
        // a Superior.
        String syntaxOid = attributeType.getSyntaxOid();

        if ( syntaxOid != null )
        {
            // Check if the Syntax is present in the registries
            try
            {
                ldapSyntaxRegistry.lookup( syntaxOid );
            }
            catch ( LdapException ne )
            {
                // This AT's syntax has not been loaded into the Registries.
                errors.add( ne );
            }
        }
        else
        {
            // No Syntax : get it from the AttributeType's superior
            if ( superior == null )
            {
                // This is an error. if the AT does not have a Syntax,
                // then it must have a superior, which syntax is get from.
                Throwable error = new LdapSchemaViolationException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX,
                    I18n.err( I18n.ERR_04298, attributeType.getOid() ) );
                errors.add( error );
            }
        }

        // Process the EQUALITY MatchingRule. It may be null, but if it's not
        // it must have been processed before
        String equalityOid = attributeType.getEqualityOid();

        if ( equalityOid != null )
        {
            // Check if the MatchingRule is present in the registries
            try
            {
                matchingRuleRegistry.lookup( equalityOid );
            }
            catch ( LdapException ne )
            {
                // This AT's EQUALITY matchingRule has not been loaded into the Registries.
                errors.add( ne );
            }
        }

        // Process the ORDERING MatchingRule. It may be null, but if it's not
        // it must have been processed before
        String orderingOid = attributeType.getOrderingOid();

        if ( orderingOid != null )
        {
            // Check if the MatchingRule is present in the registries
            try
            {
                matchingRuleRegistry.lookup( orderingOid );
            }
            catch ( LdapException ne )
            {
                // This AT's ORDERING matchingRule has not been loaded into the Registries.
                errors.add( ne );
            }
        }

        // Process the SUBSTR MatchingRule. It may be null, but if it's not
        // it must have been processed before
        String substringOid = attributeType.getSubstringOid();

        if ( substringOid != null )
        {
            // Check if the MatchingRule is present in the registries
            try
            {
                matchingRuleRegistry.lookup( substringOid );
            }
            catch ( LdapException ne )
            {
                // This AT's SUBSTR matchingRule has not been loaded into the Registries.
                errors.add( ne );
            }
        }
    }


    /**
     * Check the inheritance, and the existence of MatchingRules and LdapSyntax
     * for an attribute 
     */
    private void resolve( AttributeType attributeType, List<Throwable> errors )
    {
        // This set is used to avoid having more than one error
        // for an AttributeType. It's mandatory when processing
        // a Superior, as it may be broken and referenced more than once. 
        Set<String> processed = new HashSet<String>();

        // Store the AttributeType itself in the processed, to avoid cycle
        processed.add( attributeType.getOid() );

        // Call the recursive method, as we may have superiors to deal with
        resolveRecursive( attributeType, processed, errors );
    }


    private List<AttributeType> getMustRecursive( List<AttributeType> musts, Set<ObjectClass> processed,
        ObjectClass objectClass )
    {
        if ( objectClass != null )
        {
            if ( processed.contains( objectClass ) )
            {
                // We have found a cycle. It has already been reported, 
                // don't add a new error, just exit.
                return null;
            }

            processed.add( objectClass );

            for ( AttributeType must : objectClass.getMustAttributeTypes() )
            {
                musts.add( must );
            }

            for ( ObjectClass superior : objectClass.getSuperiors() )
            {
                getMustRecursive( musts, processed, superior );
            }
        }

        return musts;
    }


    private void resolve( ObjectClass objectClass, List<Throwable> errors )
    {
        // This set is used to avoid having more than one error
        // for an ObjectClass. It's mandatory when processing
        // the Superiors, as they may be broken and referenced more than once. 
        Set<String> processed = new HashSet<String>();

        // Store the ObjectClass itself in the processed, to avoid cycle
        processed.add( objectClass.getOid() );

        // Call the recursive method, as we may have superiors to deal with
        resolveRecursive( objectClass, processed, errors );

        // Check that the MAY and MUST AT are consistent (no AT in MAY and in MUST
        // in one of its superior
        List<AttributeType> musts = getMustRecursive( new ArrayList<AttributeType>(), new HashSet<ObjectClass>(),
            objectClass );

        if ( musts != null )
        {
            for ( AttributeType may : objectClass.getMayAttributeTypes() )
            {
                if ( musts.contains( may ) )
                {
                    // This is not allowed.
                    Throwable error = new LdapProtocolErrorException( I18n.err( I18n.ERR_04299, objectClass.getOid() ) );
                    errors.add( error );
                    return;
                }
            }
        }
    }


    private void resolveRecursive( ObjectClass objectClass, Set<String> processed, List<Throwable> errors )
    {
        // Process the Superiors, if any
        List<String> superiorOids = objectClass.getSuperiorOids();
        ObjectClass superior = null;

        for ( String superiorOid : superiorOids )
        {
            // Check if the Superior is present in the registries
            try
            {
                superior = objectClassRegistry.lookup( superiorOid );
            }
            catch ( LdapException ne )
            {
                // This OC's superior has not been loaded into the Registries.
                if ( !processed.contains( superiorOid ) )
                {
                    errors.add( ne );
                }
            }

            // We now have to process the superior, if it hasn't been 
            // processed yet.
            if ( superior != null )
            {
                if ( !processed.contains( superior.getOid() ) )
                {
                    resolveRecursive( superior, processed, errors );
                    processed.add( objectClass.getOid() );
                }
                else
                {
                    // Not allowed : we have a cyle
                    Throwable error = new LdapProtocolErrorException( I18n.err( I18n.ERR_04300, objectClass.getOid(), superior) );
                    errors.add( error );
                    return;
                }
            }
        }

        // Process the MAY attributeTypes.  
        for ( String mayOid : objectClass.getMayAttributeTypeOids() )
        {
            // Check if the MAY AttributeType is present in the registries
            try
            {
                attributeTypeRegistry.lookup( mayOid );
            }
            catch ( LdapException ne )
            {
                // This AT has not been loaded into the Registries.
                errors.add( ne );
            }
        }

        // Process the MUST attributeTypes.  
        for ( String mustOid : objectClass.getMustAttributeTypeOids() )
        {
            // Check if the MUST AttributeType is present in the registries
            try
            {
                attributeTypeRegistry.lookup( mustOid );
            }
            catch ( LdapException ne )
            {
                // This AT has not been loaded into the Registries.
                errors.add( ne );
            }
        }

        // All is done for this ObjectClass, let's apply the registries
        try
        {
            objectClass.addToRegistries( errors, this );
        }
        catch ( LdapException ne )
        {
            // Do nothing. We may have a broken OC, 
            // but at this point, it doesn't matter.
        }
    }


    /**
     * Applies the added SchemaObject to the given register
     */
    public List<Throwable> add( List<Throwable> errors, SchemaObject schemaObject ) throws LdapException
    {
        // Relax the registries
        boolean wasRelaxed = isRelaxed;
        setRelaxed();

        // Register the SchemaObject in the registries
        register( errors, schemaObject );

        // Associate the SchemaObject with its schema
        associateWithSchema( errors, schemaObject );

        // Build the SchemaObject references
        buildReference( errors, schemaObject );
        
        // Lock the SchemaObject
        schemaObject.lock();

        if ( errors.isEmpty() )
        {
            // Check the registries now
            List<Throwable> checkErrors = checkRefInteg();

            errors.addAll( checkErrors );
        }

        // Get back to Strict mode
        if ( !wasRelaxed )
        {
            setStrict();
        }

        // return the errors
        return errors;
    }


    /**
     * Remove the given SchemaObject from the registries
     */
    public List<Throwable> delete( List<Throwable> errors, SchemaObject schemaObject ) throws LdapException
    {
        // Relax the registries
        boolean wasRelaxed = isRelaxed;
        setRelaxed();

        // Remove the SchemaObject from the registries
        SchemaObject removed = unregister( errors, schemaObject );

        // Remove the SchemaObject from its schema
        dissociateFromSchema( errors, removed );

        // Unlink the SchemaObject references
        removeReference( errors, removed );

        if ( errors.isEmpty() )
        {
            // Check the registries now
            List<Throwable> checkErrors = checkRefInteg();

            errors.addAll( checkErrors );
        }

        // Restore the previous registries state
        if ( !wasRelaxed )
        {
            setStrict();
        }

        // return the errors
        return errors;
    }


    /**
     * Merely adds the schema to the set of loaded schemas.  Does not
     * actually do any work to add schema objects to registries.
     * 
     * {@inheritDoc}
     */
    public void schemaLoaded( Schema schema )
    {
        this.loadedSchemas.put( StringTools.toLowerCase( schema.getSchemaName() ), schema );
    }


    /**
     * Merely removes the schema from the set of loaded schemas.  Does not
     * actually do any work to remove schema objects from registries.
     * 
     * {@inheritDoc}
     */
    public void schemaUnloaded( Schema schema )
    {
        this.loadedSchemas.remove( StringTools.toLowerCase( schema.getSchemaName() ) );
    }


    /**
     * Gets an unmodifiable Map of schema names to loaded Schema objects. 
     * 
     * @return the map of loaded Schema objects
     */
    public Map<String, Schema> getLoadedSchemas()
    {
        return Collections.unmodifiableMap( loadedSchemas );
    }


    /**
     * @return Gets a reference to the Map associating a schemaName to
     * its contained SchemaObjects
     */
    public Map<String, Set<SchemaObjectWrapper>> getObjectBySchemaName()
    {
        return schemaObjects;
    }


    /**
     * Retrieve the schema name for a specific SchemaObject, or return "other" if none is found.
     */
    private String getSchemaName( SchemaObject schemaObject )
    {
        String schemaName = StringTools.toLowerCase( schemaObject.getSchemaName() );

        if ( loadedSchemas.containsKey( schemaName ) )
        {
            return schemaName;
        }
        else
        {
            return MetaSchemaConstants.SCHEMA_OTHER;
        }
    }


    /**
     * Tells if the given SchemaObject is present in one schema. The schema
     * may be disabled.
     *
     * @param schemaObject The schemaObject we are looking for
     * @return true if the schemaObject is present in a schema
     */
    public boolean contains( SchemaObject schemaObject )
    {
        String schemaName = schemaObject.getSchemaName();

        Set<SchemaObjectWrapper> setSchemaObjects = schemaObjects.get( schemaName );

        if ( ( setSchemaObjects == null ) || setSchemaObjects.isEmpty() )
        {
            return false;
        }

        SchemaObjectWrapper wrapper = new SchemaObjectWrapper( schemaObject );

        return setSchemaObjects.contains( wrapper );
    }


    /**
     * Create a new schema association with its content
     *
     * @param schemaName The schema name
     */
    public Set<SchemaObjectWrapper> addSchema( String schemaName )
    {
        Set<SchemaObjectWrapper> content = new HashSet<SchemaObjectWrapper>();
        schemaObjects.put( schemaName, content );

        return content;
    }


    /**
     * Register the given SchemaObject into the associated Registry
     */
    private void register( List<Throwable> errors, SchemaObject schemaObject ) throws LdapException
    {
        LOG.debug( "Registering {}:{}", schemaObject.getObjectType(), schemaObject.getOid() );

        // Check that the SchemaObject is not already registered
        if ( schemaObject instanceof LoadableSchemaObject )
        {
            // TODO : Check for existing Loadable SchemaObject
        }
        else
        {
            if ( globalOidRegistry.contains( schemaObject.getOid() ) )
            {
                // TODO : throw an exception here
                String msg = I18n.err( I18n.ERR_04301, schemaObject.getObjectType(), schemaObject.getOid() );
                LOG.error( msg );
                Throwable error = new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
                errors.add( error );
                return;
            }
        }

        try
        {
            // First call the specific registry's register method
            switch ( schemaObject.getObjectType() )
            {
                case ATTRIBUTE_TYPE:
                    attributeTypeRegistry.register( ( AttributeType ) schemaObject );
                    break;

                case COMPARATOR:
                    comparatorRegistry.register( ( LdapComparator<?> ) schemaObject );
                    break;

                case DIT_CONTENT_RULE:
                    ditContentRuleRegistry.register( ( DITContentRule ) schemaObject );
                    break;

                case DIT_STRUCTURE_RULE:
                    ditStructureRuleRegistry.register( ( DITStructureRule ) schemaObject );
                    break;

                case LDAP_SYNTAX:
                    ldapSyntaxRegistry.register( ( LdapSyntax ) schemaObject );
                    break;

                case MATCHING_RULE:
                    matchingRuleRegistry.register( ( MatchingRule ) schemaObject );
                    break;

                case MATCHING_RULE_USE:
                    matchingRuleUseRegistry.register( ( MatchingRuleUse ) schemaObject );
                    break;

                case NAME_FORM:
                    nameFormRegistry.register( ( NameForm ) schemaObject );
                    break;

                case NORMALIZER:
                    normalizerRegistry.register( ( Normalizer ) schemaObject );
                    break;

                case OBJECT_CLASS:
                    objectClassRegistry.register( ( ObjectClass ) schemaObject );
                    break;

                case SYNTAX_CHECKER:
                    syntaxCheckerRegistry.register( ( SyntaxChecker ) schemaObject );
                    break;
            }
        }
        catch ( Exception e )
        {
            errors.add( e );
        }
    }


    /**
     * Store the given SchemaObject in the Map associating SchemaObjetcs to their
     * related Schema.
     *
     * @param schemaObject The schemaObject to register
     * @throws LdapException If there is a problem
     */
    public void associateWithSchema( List<Throwable> errors, SchemaObject schemaObject )
    {
        LOG.debug( "Registering {}:{}", schemaObject.getObjectType(), schemaObject.getOid() );

        // Check that the SchemaObject is not already registered
        if ( !( schemaObject instanceof LoadableSchemaObject ) && globalOidRegistry.contains( schemaObject.getOid() ) )
        {
            // TODO : throw an exception here
            String msg = I18n.err( I18n.ERR_04301, schemaObject.getObjectType(), schemaObject.getOid() );
            LOG.error( msg );
            Throwable error = new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            errors.add( error );
            return;
        }

        // Get a normalized form of schema name
        String schemaName = getSchemaName( schemaObject );

        // And register the schemaObject within its schema
        Set<SchemaObjectWrapper> content = schemaObjects.get( schemaName );

        if ( content == null )
        {
            content = new HashSet<SchemaObjectWrapper>();
            schemaObjects.put( StringTools.toLowerCase( schemaName ), content );
        }

        SchemaObjectWrapper schemaObjectWrapper = new SchemaObjectWrapper( schemaObject );

        if ( content.contains( schemaObjectWrapper ) )
        {
            // Already present !
            // What should we do ?
            LOG.info( "Registering of {}:{} failed, is already present in the Registries",
                schemaObject.getObjectType(), schemaObject.getOid() );
        }
        else
        {
            // Create the association
            content.add( schemaObjectWrapper );

            // Update the global OidRegistry if the SchemaObject is not
            // an instance of LoadableSchemaObject
            if ( !( schemaObject instanceof LoadableSchemaObject ) )
            {
                try
                {
                    globalOidRegistry.register( schemaObject );
                }
                catch ( LdapException ne )
                {
                    errors.add( ne );
                    return;
                }
            }

            LOG.debug( "registered {} for OID {}", schemaObject.getName(), schemaObject.getOid() );
        }
    }


    /**
     * Store the given SchemaObject in the Map associating SchemaObjetcs to their
     * related Schema.
     *
     * @param schemaObject The schemaObject to register
     * @throws LdapException If there is a problem
     */

    public void dissociateFromSchema( List<Throwable> errors, SchemaObject schemaObject ) throws LdapException
    {
        LOG.debug( "Unregistering {}:{}", schemaObject.getObjectType(), schemaObject.getOid() );

        // Check that the SchemaObject is already registered
        if ( !( schemaObject instanceof LoadableSchemaObject ) && !globalOidRegistry.contains( schemaObject.getOid() ) )
        {
            // TODO : throw an exception here
            String msg = I18n.err( I18n.ERR_04302, schemaObject.getObjectType(), schemaObject.getOid() );
            LOG.error( msg );
            Throwable error = new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            errors.add( error );
            return;
        }

        // Get a normalized form of schema name
        String schemaName = getSchemaName( schemaObject );
        String oid = schemaObject.getOid();

        // And unregister the schemaObject from its schema
        Set<SchemaObjectWrapper> content = schemaObjects.get( schemaName );

        SchemaObjectWrapper schemaObjectWrapper = new SchemaObjectWrapper( schemaObject );

        if ( !content.contains( schemaObjectWrapper ) )
        {
            // Not present !
            // What should we do ?
            LOG.info( "Unregistering of {}:{} failed, is not present in the Registries", schemaObject.getObjectType(),
                schemaObject.getOid() );
        }
        else
        {
            // Remove the association
            content.remove( schemaObjectWrapper );

            // Update the global OidRegistry if the SchemaObject is not
            // an instance of LoadableSchemaObject
            if ( !( schemaObject instanceof LoadableSchemaObject ) )
            {
                try
                {
                    globalOidRegistry.unregister( oid );
                }
                catch ( LdapException ne )
                {
                    errors.add( ne );
                    return;
                }
            }

            LOG.debug( "Unregistered {} for OID {}", schemaObject.getName(), schemaObject.getOid() );
        }
    }


    /**
     * Unregister a SchemaObject from the registries
     *
     * @param schemaObject The SchemaObject we want to deregister
     * @throws LdapException If the removal failed
     */
    private SchemaObject unregister( List<Throwable> errors, SchemaObject schemaObject ) throws LdapException
    {
        LOG.debug( "Unregistering {}:{}", schemaObject.getObjectType(), schemaObject.getOid() );

        // Check that the SchemaObject is present in the registries
        if ( schemaObject instanceof LoadableSchemaObject )
        {
            // TODO : check for an existing Loadable SchemaObject
        }
        else
        {
            if ( !globalOidRegistry.contains( schemaObject.getOid() ) )
            {
                // TODO : throw an exception here
                String msg = I18n.err( I18n.ERR_04302, schemaObject.getObjectType(), schemaObject.getOid() );
                LOG.error( msg );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, msg );
            }
        }

        SchemaObject unregistered = null;

        // First call the specific registry's register method
        switch ( schemaObject.getObjectType() )
        {
            case ATTRIBUTE_TYPE:
                unregistered = attributeTypeRegistry.unregister( ( AttributeType ) schemaObject );
                break;

            case COMPARATOR:
                unregistered = comparatorRegistry.unregister( ( LdapComparator<?> ) schemaObject );
                break;

            case DIT_CONTENT_RULE:
                unregistered = ditContentRuleRegistry.unregister( ( DITContentRule ) schemaObject );
                break;

            case DIT_STRUCTURE_RULE:
                unregistered = ditStructureRuleRegistry.unregister( ( DITStructureRule ) schemaObject );
                break;

            case LDAP_SYNTAX:
                unregistered = ldapSyntaxRegistry.unregister( ( LdapSyntax ) schemaObject );
                break;

            case MATCHING_RULE:
                unregistered = matchingRuleRegistry.unregister( ( MatchingRule ) schemaObject );
                break;

            case MATCHING_RULE_USE:
                unregistered = matchingRuleUseRegistry.unregister( ( MatchingRuleUse ) schemaObject );
                break;

            case NAME_FORM:
                unregistered = nameFormRegistry.unregister( ( NameForm ) schemaObject );
                break;

            case NORMALIZER:
                unregistered = normalizerRegistry.unregister( ( Normalizer ) schemaObject );
                break;

            case OBJECT_CLASS:
                unregistered = objectClassRegistry.unregister( ( ObjectClass ) schemaObject );
                break;

            case SYNTAX_CHECKER:
                unregistered = syntaxCheckerRegistry.unregister( ( SyntaxChecker ) schemaObject );
                break;
        }

        return unregistered;
    }


    /**
     * Remove the given SchemaObject from the Map associating SchemaObjetcs to their
     * related Schema.
     *
     * @param schemaObject The schemaObject to remove
     * @throws LdapException If there is a problem
     */
    public void dissociateFromSchema( SchemaObject schemaObject ) throws LdapException
    {
        // And unregister the schemaObject within its schema
        Set<SchemaObjectWrapper> content = schemaObjects.get( StringTools.toLowerCase( schemaObject.getSchemaName() ) );

        if ( content != null )
        {
            SchemaObjectWrapper schemaObjectWrapper = new SchemaObjectWrapper( schemaObject );
    
            if ( content.contains( schemaObjectWrapper ) )
            {
                // remove the schemaObject
                content.remove( schemaObjectWrapper );
    
                // Update the global OidRegistry if the SchemaObject is not
                // an instance of LoadableSchemaObject
                if ( !( schemaObject instanceof LoadableSchemaObject ) )
                {
                    globalOidRegistry.unregister( schemaObject.getOid() );
                }
    
                LOG.debug( "Unregistered {}:{}", schemaObject.getObjectType(), schemaObject.getOid() );
            }
            else
            {
                // Not present !!
                // What should we do ?
                LOG.debug( "Unregistering of {}:{} failed, not found in Registries", schemaObject.getObjectType(),
                    schemaObject.getOid() );
            }
        }
    }


    /**
     * Checks if a specific SchemaObject is referenced by any other SchemaObject.
     *
     * @param schemaObject The SchemaObject we are looking for
     * @return true if there is at least one SchemaObjetc referencing the given one
     */
    public boolean isReferenced( SchemaObject schemaObject )
    {
        SchemaObjectWrapper wrapper = new SchemaObjectWrapper( schemaObject );

        Set<SchemaObjectWrapper> set = usedBy.get( wrapper );

        boolean referenced = ( set != null ) && ( set.size() != 0 );

        if ( LOG.isDebugEnabled() )
        {
            if ( referenced )
            {
                LOG.debug( "The {}:{} is referenced", schemaObject.getObjectType(), schemaObject.getOid() );
            }
            else
            {
                LOG.debug( "The {}:{} is not referenced", schemaObject.getObjectType(), schemaObject.getOid() );
            }
        }

        return referenced;
    }


    /**
     * Gets the Set of SchemaObjects referencing the given SchemaObject
     *
     * @param schemaObject The SchemaObject we are looking for
     * @return The Set of referencing SchemaObject, or null 
     */
    public Set<SchemaObjectWrapper> getUsedBy( SchemaObject schemaObject )
    {
        SchemaObjectWrapper wrapper = new SchemaObjectWrapper( schemaObject );

        return usedBy.get( wrapper );
    }


    /**
     * Dump the UsedBy data structure as a String
     */
    public String dumpUsedBy()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "USED BY :\n" );

        for ( SchemaObjectWrapper wrapper : usedBy.keySet() )
        {
            sb.append( wrapper.get().getObjectType() ).append( '[' ).append( wrapper.get().getOid() ).append( "] : {" );

            boolean isFirst = true;

            for ( SchemaObjectWrapper uses : usedBy.get( wrapper ) )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ", " );
                }

                sb.append( uses.get().getObjectType() ).append( '[' ).append( wrapper.get().getOid() ).append( "]" );
            }

            sb.append( "}\n" );
        }

        return sb.toString();
    }


    /**
     * Dump the Using data structure as a String
     */
    public String dumpUsing()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "USING :\n" );

        for ( SchemaObjectWrapper wrapper : using.keySet() )
        {
            sb.append( wrapper.get().getObjectType() ).append( '[' ).append( wrapper.get().getOid() ).append( "] : {" );

            boolean isFirst = true;

            for ( SchemaObjectWrapper uses : using.get( wrapper ) )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ", " );
                }

                sb.append( uses.get().getObjectType() ).append( '[' ).append( wrapper.get().getOid() ).append( "]" );
            }

            sb.append( "}\n" );
        }

        return sb.toString();
    }


    /**
     * Gets the Set of SchemaObjects referenced by the given SchemaObject
     *
     * @param schemaObject The SchemaObject we are looking for
     * @return The Set of referenced SchemaObject, or null 
     */
    public Set<SchemaObjectWrapper> getUsing( SchemaObject schemaObject )
    {
        SchemaObjectWrapper wrapper = new SchemaObjectWrapper( schemaObject );

        return using.get( wrapper );
    }


    /**
     * Add an association between a SchemaObject an the SchemaObject it refers
     *
     * @param reference The base SchemaObject
     * @param referee The SchemaObject pointing on the reference
     */
    private void addUsing( SchemaObject reference, SchemaObject referee )
    {
        if ( ( reference == null ) || ( referee == null ) )
        {
            return;
        }

        SchemaObjectWrapper wrapper = new SchemaObjectWrapper( reference );

        Set<SchemaObjectWrapper> uses = getUsing( reference );

        if ( uses == null )
        {
            uses = new HashSet<SchemaObjectWrapper>();
        }

        uses.add( new SchemaObjectWrapper( referee ) );

        // Put back the set (this is a concurrentHashMap, it won't be replaced implicitly
        using.put( wrapper, uses );
    }


    /**
     * Add an association between a SchemaObject an the SchemaObject it refers
     *
     * @param base The base SchemaObject
     * @param referenced The referenced SchemaObject
     */
    public void addReference( SchemaObject base, SchemaObject referenced )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( dump( "add", base, referenced ) );
        }

        addUsing( base, referenced );
        addUsedBy( referenced, base );

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( dumpUsedBy() );
            LOG.debug( dumpUsing() );
        }
    }


    /**
     * Add an association between a SchemaObject an the SchemaObject that refers it
     *
     * @param reference The base SchemaObject
     * @param referee The SchemaObject pointing on the reference
     */
    private void addUsedBy( SchemaObject referee, SchemaObject reference )
    {
        if ( ( reference == null ) || ( referee == null ) )
        {
            return;
        }

        SchemaObjectWrapper wrapper = new SchemaObjectWrapper( referee );

        Set<SchemaObjectWrapper> uses = getUsedBy( referee );

        if ( uses == null )
        {
            uses = new HashSet<SchemaObjectWrapper>();
        }

        uses.add( new SchemaObjectWrapper( reference ) );

        // Put back the set (this is a concurrentHashMap, it won't be replaced implicitly
        usedBy.put( wrapper, uses );
    }


    /**
     * Del an association between a SchemaObject an the SchemaObject it refers
     *
     * @param reference The base SchemaObject
     * @param referee The SchemaObject pointing on the reference
     */
    private void delUsing( SchemaObject reference, SchemaObject referee )
    {
        if ( ( reference == null ) || ( referee == null ) )
        {
            return;
        }

        Set<SchemaObjectWrapper> uses = getUsing( reference );

        if ( uses == null )
        {
            return;
        }

        uses.remove( new SchemaObjectWrapper( referee ) );

        SchemaObjectWrapper wrapper = new SchemaObjectWrapper( reference );

        if ( uses.size() == 0 )
        {
            using.remove( wrapper );
        }
        else
        {
            using.put( wrapper, uses );
        }

        return;
    }


    /**
     * Del an association between a SchemaObject an the SchemaObject that refers it
     *
     * @param reference The base SchemaObject
     * @param referee The SchemaObject pointing on the reference
     */
    private void delUsedBy( SchemaObject referee, SchemaObject reference )
    {
        if ( ( reference == null ) || ( referee == null ) )
        {
            return;
        }

        Set<SchemaObjectWrapper> uses = getUsedBy( referee );

        if ( uses == null )
        {
            return;
        }

        uses.remove( new SchemaObjectWrapper( reference ) );

        SchemaObjectWrapper wrapper = new SchemaObjectWrapper( referee );

        if ( uses.size() == 0 )
        {
            usedBy.remove( wrapper );
        }
        else
        {
            usedBy.put( wrapper, uses );
        }

        return;
    }


    /**
     * Delete an association between a SchemaObject an the SchemaObject it refers
     *
     * @param base The base SchemaObject
     * @param referenced The referenced SchemaObject
     */
    public void delReference( SchemaObject base, SchemaObject referenced )
    {
        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( dump( "del", base, referenced ) );
        }

        delUsing( base, referenced );
        delUsedBy( referenced, base );

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( dumpUsedBy() );
            LOG.debug( dumpUsing() );
        }
    }


    /**
     * Dump the reference operation as a String
     */
    private String dump( String op, SchemaObject reference, SchemaObject referee )
    {
        return op + " : " + reference.getObjectType() + "[" + reference.getOid() + "]/[" + referee.getObjectType()
            + "[" + referee.getOid() + "]";
    }


    private boolean checkReferences( SchemaObject reference, SchemaObject referee, String message )
    {
        SchemaObjectWrapper referenceWrapper = new SchemaObjectWrapper( reference );
        SchemaObjectWrapper refereeWrapper = new SchemaObjectWrapper( referee );

        // Check the references : Syntax -> SyntaxChecker
        if ( !using.containsKey( referenceWrapper ) )
        {
            LOG.debug( "The Syntax {}:{} does not reference any " + message, reference.getObjectType(), reference
                .getOid() );

            return false;
        }

        Set<SchemaObjectWrapper> usings = using.get( referenceWrapper );

        if ( !usings.contains( refereeWrapper ) )
        {
            LOG.debug( "The {}:{} does not reference any " + message, reference.getObjectType(), reference.getOid() );

            return false;
        }

        // Check the referees : SyntaxChecker -> Syntax
        if ( !usedBy.containsKey( refereeWrapper ) )
        {
            LOG.debug( "The {}:{} is not referenced by any " + message, referee.getObjectType(), referee.getOid() );

            return false;
        }

        Set<SchemaObjectWrapper> used = usedBy.get( refereeWrapper );

        if ( !used.contains( referenceWrapper ) )
        {
            LOG.debug( "The {}:{} is not referenced by any " + message, referee.getObjectType(), referee.getOid() );

            return false;
        }

        return true;
    }


    /**
     * Check the registries for invalid relations. This check stops at the first error.
     *
     * @return true if the Registries is consistent, false otherwise
     */
    public boolean check()
    {
        // Check the Syntaxes : check for a SyntaxChecker
        LOG.debug( "Checking Syntaxes" );

        for ( LdapSyntax syntax : ldapSyntaxRegistry )
        {
            // Check that each Syntax has a SyntaxChecker
            if ( syntax.getSyntaxChecker() == null )
            {
                LOG.debug( "The Syntax {} has no SyntaxChecker", syntax );

                return false;
            }

            if ( !syntaxCheckerRegistry.contains( syntax.getSyntaxChecker().getOid() ) )
            {
                LOG.debug( "Cannot find the SyntaxChecker {} for the Syntax {}", syntax.getSyntaxChecker().getOid(),
                    syntax );

                return false;
            }

            // Check the references : Syntax -> SyntaxChecker and SyntaxChecker -> Syntax 
            if ( !checkReferences( syntax, syntax.getSyntaxChecker(), "SyntaxChecker" ) )
            {
                return false;
            }
        }

        // Check the MatchingRules : check for a Normalizer, a Comparator and a Syntax
        LOG.debug( "Checking MatchingRules..." );

        for ( MatchingRule matchingRule : matchingRuleRegistry )
        {
            // Check that each MatchingRule has a Normalizer
            if ( matchingRule.getNormalizer() == null )
            {
                LOG.debug( "The MatchingRule {} has no Normalizer", matchingRule );

                return false;
            }

            // Check that each MatchingRule has a Normalizer
            if ( !normalizerRegistry.contains( matchingRule.getNormalizer().getOid() ) )
            {
                LOG.debug( "Cannot find the Normalizer {} for the MatchingRule {}", matchingRule.getNormalizer()
                    .getOid(), matchingRule );

                return false;
            }

            // Check that each MatchingRule has a Comparator
            if ( matchingRule.getLdapComparator() == null )
            {
                LOG.debug( "The MatchingRule {} has no Comparator", matchingRule );

                return false;
            }

            if ( !comparatorRegistry.contains( matchingRule.getLdapComparator().getOid() ) )
            {
                LOG.debug( "Cannot find the Comparator {} for the MatchingRule {}", matchingRule.getLdapComparator()
                    .getOid(), matchingRule );

                return false;
            }

            // Check that each MatchingRule has a Syntax
            if ( matchingRule.getSyntax() == null )
            {
                LOG.debug( "The MatchingRule {} has no Syntax", matchingRule );

                return false;
            }

            if ( !ldapSyntaxRegistry.contains( matchingRule.getSyntax().getOid() ) )
            {
                LOG.debug( "Cannot find the Syntax {} for the MatchingRule {}", matchingRule.getSyntax().getOid(),
                    matchingRule );

                return false;
            }

            // Check the references : MR -> S and S -> MR 
            if ( !checkReferences( matchingRule, matchingRule.getSyntax(), "Syntax" ) )
            {
                return false;
            }

            // Check the references : MR -> N 
            if ( !checkReferences( matchingRule, matchingRule.getNormalizer(), "Normalizer" ) )
            {
                return false;
            }

            // Check the references : MR -> C and C -> MR 
            if ( !checkReferences( matchingRule, matchingRule.getLdapComparator(), "Comparator" ) )
            {
                return false;
            }
        }

        // Check the ObjectClasses : check for MAY, MUST, SUPERIORS
        LOG.debug( "Checking ObjectClasses..." );

        for ( ObjectClass objectClass : objectClassRegistry )
        {
            // Check that each ObjectClass has all the MAY AttributeTypes
            if ( objectClass.getMayAttributeTypes() != null )
            {
                for ( AttributeType may : objectClass.getMayAttributeTypes() )
                {
                    if ( !attributeTypeRegistry.contains( may.getOid() ) )
                    {
                        LOG.debug( "Cannot find the AttributeType {} for the ObjectClass {} MAY", may, objectClass );

                        return false;
                    }

                    // Check the references : OC -> AT  and AT -> OC (MAY) 
                    if ( !checkReferences( objectClass, may, "AttributeType" ) )
                    {
                        return false;
                    }
                }
            }

            // Check that each ObjectClass has all the MUST AttributeTypes
            if ( objectClass.getMustAttributeTypes() != null )
            {
                for ( AttributeType must : objectClass.getMustAttributeTypes() )
                {
                    if ( !attributeTypeRegistry.contains( must.getOid() ) )
                    {
                        LOG.debug( "Cannot find the AttributeType {} for the ObjectClass {} MUST", must, objectClass );

                        return false;
                    }

                    // Check the references : OC -> AT  and AT -> OC (MUST) 
                    if ( !checkReferences( objectClass, must, "AttributeType" ) )
                    {
                        return false;
                    }
                }
            }

            // Check that each ObjectClass has all the SUPERIORS ObjectClasses
            if ( objectClass.getSuperiors() != null )
            {
                for ( ObjectClass superior : objectClass.getSuperiors() )
                {
                    if ( !objectClassRegistry.contains( objectClass.getOid() ) )
                    {
                        LOG.debug( "Cannot find the ObjectClass {} for the ObjectClass {} SUPERIORS", superior,
                            objectClass );

                        return false;
                    }

                    // Check the references : OC -> OC  and OC -> OC (SUPERIORS) 
                    if ( !checkReferences( objectClass, superior, "ObjectClass" ) )
                    {
                        return false;
                    }
                }
            }
        }

        // Check the AttributeTypes : check for MatchingRules, Syntaxes
        LOG.debug( "Checking AttributeTypes..." );

        for ( AttributeType attributeType : attributeTypeRegistry )
        {
            // Check that each AttributeType has a SYNTAX 
            if ( attributeType.getSyntax() == null )
            {
                LOG.debug( "The AttributeType {} has no Syntax", attributeType );

                return false;
            }

            if ( !ldapSyntaxRegistry.contains( attributeType.getSyntax().getOid() ) )
            {
                LOG.debug( "Cannot find the Syntax {} for the AttributeType {}", attributeType.getSyntax().getOid(),
                    attributeType );

                return false;
            }

            // Check the references for AT -> S and S -> AT
            if ( !checkReferences( attributeType, attributeType.getSyntax(), "AttributeType" ) )
            {
                return false;
            }

            // Check the EQUALITY MatchingRule
            if ( attributeType.getEquality() != null )
            {
                if ( !matchingRuleRegistry.contains( attributeType.getEquality().getOid() ) )
                {
                    LOG.debug( "Cannot find the MatchingRule {} for the AttributeType {}", attributeType.getEquality()
                        .getOid(), attributeType );

                    return false;
                }

                // Check the references for AT -> MR and MR -> AT
                if ( !checkReferences( attributeType, attributeType.getEquality(), "AttributeType" ) )
                {
                    return false;
                }
            }

            // Check the ORDERING MatchingRule
            if ( attributeType.getOrdering() != null )
            {
                if ( !matchingRuleRegistry.contains( attributeType.getOrdering().getOid() ) )
                {
                    LOG.debug( "Cannot find the MatchingRule {} for the AttributeType {}", attributeType.getOrdering()
                        .getOid(), attributeType );

                    return false;
                }

                // Check the references for AT -> MR and MR -> AT
                if ( !checkReferences( attributeType, attributeType.getOrdering(), "AttributeType" ) )
                {
                    return false;
                }
            }

            // Check the SUBSTR MatchingRule
            if ( attributeType.getSubstring() != null )
            {
                if ( !matchingRuleRegistry.contains( attributeType.getSubstring().getOid() ) )
                {
                    LOG.debug( "Cannot find the MatchingRule {} for the AttributeType {}", attributeType.getSubstring()
                        .getOid(), attributeType );

                    return false;
                }

                // Check the references for AT -> MR and MR -> AT
                if ( !checkReferences( attributeType, attributeType.getSubstring(), "AttributeType" ) )
                {
                    return false;
                }
            }

            // Check the SUP
            if ( attributeType.getSuperior() != null )
            {
                AttributeType superior = attributeType.getSuperior();

                if ( !attributeTypeRegistry.contains( superior.getOid() ) )
                {
                    LOG.debug( "Cannot find the AttributeType {} for the AttributeType {} SUPERIOR", superior,
                        attributeType );

                    return false;
                }

                // Check the references : AT -> AT  and AT -> AT (SUPERIOR) 
                if ( !checkReferences( attributeType, superior, "AttributeType" ) )
                {
                    return false;
                }
            }
        }

        return true;
    }


    /**
     * Clone the Registries. This is done in two steps :
     * - first clone the SchemaObjetc registries
     * - second restore the relation between them
     */
    public Registries clone() throws CloneNotSupportedException
    {
        // First clone the structure
        Registries clone = ( Registries ) super.clone();

        // Now, clone the oidRegistry
        clone.globalOidRegistry = globalOidRegistry.copy();

        // We have to clone every SchemaObject registries now
        clone.attributeTypeRegistry = attributeTypeRegistry.copy();
        clone.comparatorRegistry = comparatorRegistry.copy();
        clone.ditContentRuleRegistry = ditContentRuleRegistry.copy();
        clone.ditStructureRuleRegistry = ditStructureRuleRegistry.copy();
        clone.ldapSyntaxRegistry = ldapSyntaxRegistry.copy();
        clone.matchingRuleRegistry = matchingRuleRegistry.copy();
        clone.matchingRuleUseRegistry = matchingRuleUseRegistry.copy();
        clone.nameFormRegistry = nameFormRegistry.copy();
        clone.normalizerRegistry = normalizerRegistry.copy();
        clone.objectClassRegistry = objectClassRegistry.copy();
        clone.syntaxCheckerRegistry = syntaxCheckerRegistry.copy();

        // Store all the SchemaObjects into the globalOid registry
        for ( AttributeType attributeType : clone.attributeTypeRegistry )
        {
            clone.globalOidRegistry.put( attributeType );
        }

        for ( DITContentRule ditContentRule : clone.ditContentRuleRegistry )
        {
            clone.globalOidRegistry.put( ditContentRule );
        }

        for ( DITStructureRule ditStructureRule : clone.ditStructureRuleRegistry )
        {
            clone.globalOidRegistry.put( ditStructureRule );
        }

        for ( MatchingRule matchingRule : clone.matchingRuleRegistry )
        {
            clone.globalOidRegistry.put( matchingRule );
        }

        for ( MatchingRuleUse matchingRuleUse : clone.matchingRuleUseRegistry )
        {
            clone.globalOidRegistry.put( matchingRuleUse );
        }

        for ( NameForm nameForm : clone.nameFormRegistry )
        {
            clone.globalOidRegistry.put( nameForm );
        }

        for ( ObjectClass objectClass : clone.objectClassRegistry )
        {
            clone.globalOidRegistry.put( objectClass );
        }

        for ( LdapSyntax syntax : clone.ldapSyntaxRegistry )
        {
            clone.globalOidRegistry.put( syntax );
        }

        // Clone the schema list
        clone.loadedSchemas = new HashMap<String, Schema>();

        for ( String schemaName : loadedSchemas.keySet() )
        {
            // We don't clone the schemas
            clone.loadedSchemas.put( schemaName, loadedSchemas.get( schemaName ) );
        }

        // Clone the Using and usedBy structures
        // They will be empty
        clone.using = new HashMap<SchemaObjectWrapper, Set<SchemaObjectWrapper>>();
        clone.usedBy = new HashMap<SchemaObjectWrapper, Set<SchemaObjectWrapper>>();

        // Last, rebuild the using and usedBy references
        clone.buildReferences();

        // Now, check the registries. We don't care about errors
        clone.checkRefInteg();

        clone.schemaObjects = new HashMap<String, Set<SchemaObjectWrapper>>();

        // Last, not least, clone the SchemaObjects Map, and reference all the copied
        // SchemaObjects
        for ( String schemaName : schemaObjects.keySet() )
        {
            Set<SchemaObjectWrapper> objects = new HashSet<SchemaObjectWrapper>();

            for ( SchemaObjectWrapper schemaObjectWrapper : schemaObjects.get( schemaName ) )
            {
                SchemaObject original = schemaObjectWrapper.get();

                try
                {
                    if ( ! ( original instanceof LoadableSchemaObject ) )
                    {
                        SchemaObject copy = clone.globalOidRegistry.getSchemaObject( original.getOid() );
                        SchemaObjectWrapper newWrapper = new SchemaObjectWrapper( copy );
                        objects.add( newWrapper );
                    }
                    else
                    {
                        SchemaObjectWrapper newWrapper = new SchemaObjectWrapper( original );
                        objects.add(  newWrapper );
                    }
                }
                catch ( LdapException ne )
                {
                    int i = 0;
                    i++;
                    // Nothing to do
                }
            }

            clone.schemaObjects.put( schemaName, objects );
        }

        return clone;
    }


    /**
     * Tells if the Registries is permissive or if it must be checked 
     * against inconsistencies.
     *
     * @return True if SchemaObjects can be added even if they break the consistency 
     */
    public boolean isRelaxed()
    {
        return isRelaxed;
    }


    /**
     * Tells if the Registries is strict.
     *
     * @return True if SchemaObjects cannot be added if they break the consistency 
     */
    public boolean isStrict()
    {
        return !isRelaxed;
    }


    /**
     * Change the Registries to a relaxed mode, where invalid SchemaObjects
     * can be registered.
     */
    public void setRelaxed()
    {
        isRelaxed = RELAXED;
    }


    /**
     * Change the Registries to a strict mode, where invalid SchemaObjects
     * cannot be registered.
     */
    public void setStrict()
    {
        isRelaxed = STRICT;
    }


    /**
     * Tells if the Registries accept disabled elements.
     *
     * @return True if disabled SchemaObjects can be added 
     */
    public boolean isDisabledAccepted()
    {
        return disabledAccepted;
    }


    /**
     * Check that we can remove a given SchemaObject without breaking some of its references.
     * We will return the list of refereing objects.
     *
     * @param schemaObject The SchemaObject to remove
     * @return The list of SchemaObjects referencing the SchemaObjetc we want to remove
     */
    public Set<SchemaObjectWrapper> getReferencing( SchemaObject schemaObject )
    {
        SchemaObjectWrapper schemaObjectWrapper = new SchemaObjectWrapper( schemaObject );

        return usedBy.get( schemaObjectWrapper );
    }


    /**
     * Change the Registries behavior regarding disabled SchemaObject element.
     *
     * @param acceptDisabled If <code>false</code>, then the Registries won't accept
     * disabled SchemaObject or enabled SchemaObject from disabled schema 
     */
    public void setDisabledAccepted( boolean disabledAccepted )
    {
        this.disabledAccepted = disabledAccepted;
    }


    /**
     * Clear the registries from all its elements
     *
     * @throws LdapException If something goes wrong
     */
    public void clear() throws LdapException
    {
        // The AttributeTypeRegistry
        if ( attributeTypeRegistry != null )
        {
            attributeTypeRegistry.clear();
        }

        // The ComparatorRegistry
        if ( comparatorRegistry != null )
        {
            comparatorRegistry.clear();
        }

        // The DitContentRuleRegistry
        if ( ditContentRuleRegistry != null )
        {
            ditContentRuleRegistry.clear();
        }

        // The DitStructureRuleRegistry
        if ( ditStructureRuleRegistry != null )
        {
            ditStructureRuleRegistry.clear();
        }

        // The MatchingRuleRegistry
        if ( matchingRuleRegistry != null )
        {
            matchingRuleRegistry.clear();
        }

        // The MatchingRuleUseRegistry
        if ( matchingRuleUseRegistry != null )
        {
            matchingRuleUseRegistry.clear();
        }

        // The NameFormRegistry
        if ( nameFormRegistry != null )
        {
            nameFormRegistry.clear();
        }

        // The NormalizerRegistry
        if ( normalizerRegistry != null )
        {
            normalizerRegistry.clear();
        }

        // The ObjectClassRegistry
        if ( objectClassRegistry != null )
        {
            objectClassRegistry.clear();
        }

        // The SyntaxRegistry
        if ( ldapSyntaxRegistry != null )
        {
            ldapSyntaxRegistry.clear();
        }

        // The SyntaxCheckerRegistry
        if ( syntaxCheckerRegistry != null )
        {
            syntaxCheckerRegistry.clear();
        }

        // Clear the schemaObjects map
        for ( String schemaName : schemaObjects.keySet() )
        {
            Set<SchemaObjectWrapper> wrapperSet = schemaObjects.get( schemaName );

            wrapperSet.clear();
        }

        schemaObjects.clear();

        // Clear the usedBy map
        for ( SchemaObjectWrapper wrapper : usedBy.keySet() )
        {
            Set<SchemaObjectWrapper> wrapperSet = usedBy.get( wrapper );

            wrapperSet.clear();
        }

        usedBy.clear();

        // Clear the using map
        for ( SchemaObjectWrapper wrapper : using.keySet() )
        {
            Set<SchemaObjectWrapper> wrapperSet = using.get( wrapper );

            wrapperSet.clear();
        }

        using.clear();

        // Clear the global OID registry
        globalOidRegistry.clear();

        // Clear the loadedSchema Map
        loadedSchemas.clear();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Registries [" );

        if ( isRelaxed )
        {
            sb.append( "RELAXED," );
        }
        else
        {
            sb.append( "STRICT," );
        }

        if ( disabledAccepted )
        {
            sb.append( " Disabled accepted] :\n" );
        }
        else
        {
            sb.append( " Disabled forbidden] :\n" );
        }

        sb.append( "loaded schemas [" );
        boolean isFirst = true;

        for ( String schema : loadedSchemas.keySet() )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }

            sb.append( schema );
        }

        sb.append( "]\n" );

        sb.append( "AttributeTypes : " ).append( attributeTypeRegistry.size() ).append( "\n" );
        sb.append( "Comparators : " ).append( comparatorRegistry.size() ).append( "\n" );
        sb.append( "DitContentRules : " ).append( ditContentRuleRegistry.size() ).append( "\n" );
        sb.append( "DitStructureRules : " ).append( ditStructureRuleRegistry.size() ).append( "\n" );
        sb.append( "MatchingRules : " ).append( matchingRuleRegistry.size() ).append( "\n" );
        sb.append( "MatchingRuleUses : " ).append( matchingRuleUseRegistry.size() ).append( "\n" );
        sb.append( "NameForms : " ).append( nameFormRegistry.size() ).append( "\n" );
        sb.append( "Normalizers : " ).append( normalizerRegistry.size() ).append( "\n" );
        sb.append( "ObjectClasses : " ).append( objectClassRegistry.size() ).append( "\n" );
        sb.append( "Syntaxes : " ).append( ldapSyntaxRegistry.size() ).append( "\n" );
        sb.append( "SyntaxCheckers : " ).append( syntaxCheckerRegistry.size() ).append( "\n" );

        sb.append( "GlobalOidRegistry : " ).append( globalOidRegistry.size() ).append( '\n' );

        return sb.toString();
    }
}

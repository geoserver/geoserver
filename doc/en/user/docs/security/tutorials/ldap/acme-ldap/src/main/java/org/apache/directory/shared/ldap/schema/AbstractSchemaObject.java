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
package org.apache.directory.shared.ldap.schema;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Most schema objects have some common attributes. This class
 * contains the minimum set of properties exposed by a SchemaObject.<br> 
 * We have 11 types of SchemaObjects :
 * <li> AttributeType
 * <li> DitCOntentRule
 * <li> DitStructureRule
 * <li> LdapComparator (specific to ADS)
 * <li> LdapSyntaxe
 * <li> MatchingRule
 * <li> MatchingRuleUse
 * <li> NameForm
 * <li> Normalizer (specific to ADS)
 * <li> ObjectClass
 * <li> SyntaxChecker (specific to ADS)
 * <br>
 * <br>
 * This class provides accessors and setters for the following attributes, 
 * which are common to all those SchemaObjects :
 * <li>oid : The numeric OID 
 * <li>description : The SchemaObject description
 * <li>obsolete : Tells if the schema object is obsolete
 * <li>extensions : The extensions, a key/Values map
 * <li>schemaObjectType : The SchemaObject type (see upper)
 * <li>schema : The schema the SchemaObject is associated with (it's an extension).
 * Can be null
 * <li>isEnabled : The SchemaObject status (it's related to the schema status)
 * <li>isReadOnly : Tells if the SchemaObject can be modified or not
 * <br><br>
 * Some of those attributes are not used by some Schema elements, even if they should
 * have been used. Here is the list :
 * <b>name</b> : LdapSyntax, Comparator, Normalizer, SyntaxChecker
 * <b>numericOid</b> : DitStructureRule, 
 * <b>obsolete</b> : LdapSyntax, Comparator, Normalizer, SyntaxChecker
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 885381 $
 */
public abstract class AbstractSchemaObject implements SchemaObject
{
    /** The serialVersionUID */
    public static final long serialVersionUID = 1L;

    /** The SchemaObject numeric OID */
    protected String oid;

    /** The optional names for this SchemaObject */
    protected List<String> names;

    /** Whether or not this SchemaObject is enabled */
    protected boolean isEnabled = true;

    /** Whether or not this SchemaObject can be modified */
    protected boolean isReadOnly = false;

    /** Whether or not this SchemaObject is obsolete */
    protected boolean isObsolete = false;

    /** A short description of this SchemaObject */
    protected String description;

    /** The SchemaObject specification */
    protected String specification;

    /** The name of the schema this object is associated with */
    protected String schemaName;

    /** The SchemaObjectType */
    protected SchemaObjectType objectType;

    /** A map containing the list of supported extensions */
    protected Map<String, List<String>> extensions;
    
    /** A locked to avoid modifications when set to true */
    protected volatile boolean locked;
    
    /** The hashcoe for this schemaObject */
    private int h;


    /**
     * A constructor for a SchemaObject instance. It must be 
     * invoked by the inherited class.
     * 
     * @param objectType The SchemaObjectType to create
     */
    protected AbstractSchemaObject( SchemaObjectType objectType, String oid )
    {
        this.objectType = objectType;
        this.oid = oid;
        extensions = new HashMap<String, List<String>>();
        names = new ArrayList<String>();
    }


    /**
     * Constructor used when a generic reusable SchemaObject is assigned an
     * OID after being instantiated.
     * 
     * @param objectType The SchemaObjectType to create
     */
    protected AbstractSchemaObject( SchemaObjectType objectType )
    {
        this.objectType = objectType;
        extensions = new HashMap<String, List<String>>();
        names = new ArrayList<String>();
    }


    /**
     * Gets usually what is the numeric object identifier assigned to this
     * SchemaObject. All schema objects except for MatchingRuleUses have an OID
     * assigned specifically to then. A MatchingRuleUse's OID really is the OID
     * of it's MatchingRule and not specific to the MatchingRuleUse. This
     * effects how MatchingRuleUse objects are maintained by the system.
     * 
     * @return an OID for this SchemaObject or its MatchingRule if this
     *         SchemaObject is a MatchingRuleUse object
     */
    public String getOid()
    {
        return oid;
    }


    /**
     * A special method used when renaming an SchemaObject: we may have to
     * change it's OID
     * @param oid The new OID
     */
    public void setOid( String oid )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        this.oid = oid;
    }


    /**
     * Gets short names for this SchemaObject if any exists for it, otherwise,
     * returns an empty list.
     * 
     * @return the names for this SchemaObject
     */
    public List<String> getNames()
    {
        if ( names != null )
        {
            return Collections.unmodifiableList( names );
        }
        else
        {
            return Collections.emptyList();
        }
    }


    /**
     * Gets the first name in the set of short names for this SchemaObject if
     * any exists for it.
     * 
     * @return the first of the names for this SchemaObject or the oid
     * if one does not exist
     */
    public String getName()
    {
        if ( ( names != null ) && ( names.size() != 0 ) )
        {
            return names.get( 0 );
        }
        else
        {
            return oid;
        }
    }


    /**
     * Inject this SchemaObject to the given registries, updating the references to
     * other SchemaObject
     *
     * @param errors The errors we got
     * @param registries The Registries
     */
    public void addToRegistries( List<Throwable> errors, Registries registries ) throws LdapException
    {
        // do nothing
    }


    /**
     * Remove this SchemaObject from the given registries, updating the references to
     * other SchemaObject
     *
     * @param errors The errors we got
     * @param registries The Registries
     */
    public void removeFromRegistries( List<Throwable> errors, Registries registries ) throws LdapException
    {
        // do nothing
    }


    /**
     * Inject the Registries into the SchemaObject
     *
     * @param registries The Registries
     */
    public void setRegistries( Registries registries )
    {
        // do nothing
    }


    /**
     * Add a new name to the list of names for this SchemaObject. The name
     * is lowercased and trimmed.
     *  
     * @param names The names to add
     */
    public void addName( String... names )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            // We must avoid duplicated names, as names are case insensitive
            Set<String> lowerNames = new HashSet<String>();

            // Fills a set with all the existing names
            for ( String name : this.names )
            {
                lowerNames.add( StringTools.toLowerCase( name ) );
            }

            for ( String name : names )
            {
                if ( name != null )
                {
                    String lowerName = StringTools.toLowerCase( name );
                    // Check that the lower cased names is not already present
                    if ( !lowerNames.contains( lowerName ) )
                    {
                        this.names.add( name );
                        lowerNames.add( lowerName );
                    }
                }
            }
        }
    }


    /**
     * Sets the list of names for this SchemaObject. The names are
     * lowercased and trimmed.
     *  
     * @param names The list of names. Can be empty
     */
    public void setNames( List<String> names )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( names == null )
        {
            return;
        }

        if ( !isReadOnly )
        {
            this.names = new ArrayList<String>( names.size() );

            for ( String name : names )
            {
                if ( name != null )
                {
                    this.names.add( name );
                }
            }
        }
    }


    /**
     * Sets the list of names for this SchemaObject. The names are
     * lowercased and trimmed.
     *  
     * @param names The list of names.
     */
    public void setNames( String... names )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( names == null )
        {
            return;
        }

        if ( !isReadOnly )
        {
            for ( String name : names )
            {
                if ( name != null )
                {
                    this.names.add( name );
                }
            }
        }
    }


    /**
     * Gets a short description about this SchemaObject.
     * 
     * @return a short description about this SchemaObject
     */
    public String getDescription()
    {
        return description;
    }


    /**
     * Sets the SchemaObject's description
     * 
     * @param description The SchemaObject's description
     */
    public void setDescription( String description )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.description = description;
        }
    }


    /**
     * Gets the SchemaObject specification.
     * 
     * @return the SchemaObject specification
     */
    public String getSpecification()
    {
        return specification;
    }


    /**
     * Sets the SchemaObject's specification
     * 
     * @param specification The SchemaObject's specification
     */
    public void setSpecification( String specification )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.specification = specification;
        }
    }


    /**
     * Tells if this SchemaObject is enabled.
     *  
     * @param schemaEnabled the associated schema status
     * @return true if the SchemaObject is enabled, or if it depends on 
     * an enabled schema
     */
    public boolean isEnabled()
    {
        return isEnabled;
    }


    /**
     * Tells if this SchemaObject is disabled.
     *  
     * @return true if the SchemaObject is disabled
     */
    public boolean isDisabled()
    {
        return !isEnabled;
    }


    /**
     * Sets the SchemaObject state, either enabled or disabled.
     * 
     * @param enabled The current SchemaObject state
     */
    public void setEnabled( boolean enabled )
    {
        if ( !isReadOnly )
        {
            isEnabled = enabled;
        }
    }


    /**
     * Tells if this SchemaObject is ReadOnly.
     *  
     * @return true if the SchemaObject is not modifiable
     */
    public boolean isReadOnly()
    {
        return isReadOnly;
    }


    /**
     * Sets the SchemaObject readOnly flag
     * 
     * @param enabled The current SchemaObject ReadOnly status
     */
    public void setReadOnly( boolean isReadOnly )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        this.isReadOnly = isReadOnly;
    }


    /**
     * Gets whether or not this SchemaObject has been inactivated. All
     * SchemaObjects except Syntaxes allow for this parameter within their
     * definition. For Syntaxes this property should always return false in
     * which case it is never included in the description.
     * 
     * @return true if inactive, false if active
     */
    public boolean isObsolete()
    {
        return isObsolete;
    }


    /**
     * Sets the Obsolete flag.
     * 
     * @param obsolete The Obsolete flag state
     */
    public void setObsolete( boolean obsolete )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.isObsolete = obsolete;
        }
    }


    /**
     * @return The SchemaObject extensions, as a Map of [extension, values]
     */
    public Map<String, List<String>> getExtensions()
    {
        return extensions;
    }


    /**
     * Add an extension with its values
     * @param key The extension key
     * @param values The associated values
     */
    public void addExtension( String key, List<String> values )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            extensions.put( key, values );
        }
    }


    /**
     * Add an extensions with their values. (Actually do a copy)
     * 
     * @param key The extension key
     * @param values The associated values
     */
    public void setExtensions( Map<String, List<String>> extensions )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly && ( extensions != null ) )
        {
            this.extensions = new HashMap<String, List<String>>();

            for ( String key : extensions.keySet() )
            {
                List<String> values = new ArrayList<String>();

                for ( String value : extensions.get( key ) )
                {
                    values.add( value );
                }

                this.extensions.put( key, values );
            }

        }
    }


    /**
     * The SchemaObject type :
     * <li> AttributeType
     * <li> DitCOntentRule
     * <li> DitStructureRule
     * <li> LdapComparator (specific to ADS)
     * <li> LdapSyntaxe
     * <li> MatchingRule
     * <li> MatchingRuleUse
     * <li> NameForm
     * <li> Normalizer (specific to ADS)
     * <li> ObjectClass
     * <li> SyntaxChecker (specific to ADS)
     * 
     * @return the SchemaObject type
     */
    public SchemaObjectType getObjectType()
    {
        return objectType;
    }


    /**
     * Gets the name of the schema this SchemaObject is associated with.
     *
     * @return the name of the schema associated with this schemaObject
     */
    public String getSchemaName()
    {
        return schemaName;
    }


    /**
     * Sets the name of the schema this SchemaObject is associated with.
     * 
     * @param schemaName the new schema name
     */
    public void setSchemaName( String schemaName )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.schemaName = schemaName;
        }
    }


    /**
     * @see Object#hashCode()
     */
    public int hashCode()
    {
        return h;
    }


    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object o1 )
    {
        if ( this == o1 )
        {
            return true;
        }

        if ( !( o1 instanceof AbstractSchemaObject ) )
        {
            return false;
        }

        AbstractSchemaObject that = ( AbstractSchemaObject ) o1;

        // Two schemaObject are equals if their oid is equal,
        // their ObjectType is equal, their names are equals
        // their schema name is the same, all their flags are equals,
        // the description is the same and their extensions are equals
        if ( !compareOid( oid, that.oid ) )
        {
            return false;
        }

        // Compare the names
        if ( names == null )
        {
            if ( that.names != null )
            {
                return false;
            }
        }
        else if ( that.names == null )
        {
            return false;
        }
        else
        {
            int nbNames = 0;

            for ( String name : names )
            {
                if ( !that.names.contains( name ) )
                {
                    return false;
                }

                nbNames++;
            }

            if ( nbNames != names.size() )
            {
                return false;
            }
        }

        if ( schemaName == null )
        {
            if ( that.schemaName != null )
            {
                return false;
            }
        }
        else
        {
            if ( !schemaName.equalsIgnoreCase( that.schemaName ) )
            {
                return false;
            }
        }

        if ( objectType != that.objectType )
        {
            return false;
        }

        if ( extensions != null )
        {
            if ( that.extensions == null )
            {
                return false;
            }
            else
            {
                for ( String key : extensions.keySet() )
                {
                    if ( !that.extensions.containsKey( key ) )
                    {
                        return false;
                    }

                    List<String> thisValues = extensions.get( key );
                    List<String> thatValues = that.extensions.get( key );

                    if ( thisValues != null )
                    {
                        if ( thatValues == null )
                        {
                            return false;
                        }
                        else
                        {
                            if ( thisValues.size() != thatValues.size() )
                            {
                                return false;
                            }

                            // TODO compare the values
                        }
                    }
                    else if ( thatValues != null )
                    {
                        return false;
                    }
                }
            }
        }
        else if ( that.extensions != null )
        {
            return false;
        }

        if ( this.isEnabled != that.isEnabled )
        {
            return false;
        }

        if ( this.isObsolete != that.isObsolete )
        {
            return false;
        }

        if ( this.isReadOnly != that.isReadOnly )
        {
            return false;
        }

        if ( this.description == null )
        {
            return that.description == null;
        }
        else
        {
            return this.description.equalsIgnoreCase( that.description );
        }
    }


    /**
     * Register the given SchemaObject into the given registries' globalOidRegistry
     *
     * @param schemaObject the SchemaObject we want to register
     * @param registries The registries in which we want it to be stored
     * @throws LdapException If the OID is invalid
     */
    public void registerOid( SchemaObject schemaObject, Registries registries ) throws LdapException
    {
        // Add the SchemaObject into the globalOidRegistry
        registries.getGlobalOidRegistry().register( schemaObject );
    }


    /**
     * Copy the current SchemaObject on place
     *
     * @return The copied SchemaObject
     */
    public abstract SchemaObject copy();


    /**
     * Compare two oids, and return true if they are both null or
     * equals
     */
    protected boolean compareOid( String oid1, String oid2 )
    {
        if ( oid1 == null )
        {
            return oid2 == null;
        }
        else
        {
            return oid1.equals( oid2 );
        }
    }


    /**
     * Copy a SchemaObject.
     * 
     * @return A copy of the current SchemaObject
     */
    public SchemaObject copy( SchemaObject original )
    {
        // copy the description
        description = original.getDescription();

        // copy the flags
        isEnabled = original.isEnabled();
        isObsolete = original.isObsolete();
        isReadOnly = original.isReadOnly();

        // copy the names
        names = new ArrayList<String>();

        for ( String name : original.getNames() )
        {
            names.add( name );
        }

        // copy the extensions
        extensions = new HashMap<String, List<String>>();

        for ( String key : original.getExtensions().keySet() )
        {
            List<String> extensionValues = original.getExtensions().get( key );

            List<String> cloneExtension = new ArrayList<String>();

            for ( String value : extensionValues )
            {
                cloneExtension.add( value );
            }

            extensions.put( key, cloneExtension );
        }

        // The SchemaName
        schemaName = original.getSchemaName();

        // The specification
        specification = original.getSpecification();

        return this;
    }


    /**
     * Clear the current SchemaObject : remove all the references to other objects, 
     * and all the Maps. 
     */
    public void clear()
    {
        // Clear the extensions
        for ( String extension : extensions.keySet() )
        {
            List<String> extensionList = extensions.get( extension );

            extensionList.clear();
        }

        extensions.clear();

        // Clear the names
        names.clear();
    }
    

    /**
     * {@inheritDoc}
     */
    public final void lock()
    {
        if ( locked )
        {
            return;
        }
        
        h = 37;

        // The OID
        h += h * 17 + oid.hashCode();

        // The SchemaObject type
        h += h * 17 + objectType.getValue();

        // The Names, if any
        if ( ( names != null ) && ( names.size() != 0 ) )
        {
            for ( String name : names )
            {
                h += h * 17 + name.hashCode();
            }
        }

        // The schemaName if any
        if ( schemaName != null )
        {
            h += h * 17 + schemaName.hashCode();
        }

        h += h * 17 + ( isEnabled ? 1 : 0 );
        h += h * 17 + ( isReadOnly ? 1 : 0 );

        // The description, if any
        if ( description != null )
        {
            h += h * 17 + description.hashCode();
        }

        // The extensions, if any
        for ( String key : extensions.keySet() )
        {
            h += h * 17 + key.hashCode();

            List<String> values = extensions.get( key );

            if ( values != null )
            {
                for ( String value : values )
                {
                    h += h * 17 + value.hashCode();
                }
            }
        }
        
        locked = true;
    }
}

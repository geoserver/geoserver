/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.schema.registries;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.exception.LdapAttributeInUseException;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.LoadableSchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Common schema object registry interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class DefaultSchemaObjectRegistry<T extends SchemaObject> implements SchemaObjectRegistry<T>,
    Iterable<T>
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultSchemaObjectRegistry.class );

    /** A speedup for debug */
    private static final boolean DEBUG = LOG.isDebugEnabled();

    /** a map of SchemaObject looked up by name */
    protected Map<String, T> byName;

    /** The SchemaObject type, used by the toString() method  */
    protected SchemaObjectType schemaObjectType;

    /** the global OID Registry */
    protected OidRegistry oidRegistry;


    /**
     * Creates a new DefaultSchemaObjectRegistry instance.
     */
    protected DefaultSchemaObjectRegistry( SchemaObjectType schemaObjectType, OidRegistry oidRegistry )
    {
        byName = new HashMap<String, T>();
        this.schemaObjectType = schemaObjectType;
        this.oidRegistry = oidRegistry;
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String oid )
    {
        if ( !byName.containsKey( oid ) )
        {
            return byName.containsKey( StringTools.toLowerCase( oid ) );
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    public String getSchemaName( String oid ) throws LdapException
    {
        if ( !OID.isOID( oid ) )
        {
            String msg = I18n.err( I18n.ERR_04267 );
            LOG.warn( msg );
            throw new LdapException( msg );
        }

        SchemaObject schemaObject = byName.get( oid );

        if ( schemaObject != null )
        {
            return schemaObject.getSchemaName();
        }

        String msg = I18n.err( I18n.ERR_04268, oid );
        LOG.warn( msg );
        throw new LdapException( msg );
    }


    /**
     * {@inheritDoc}
     */
    public void renameSchema( String originalSchemaName, String newSchemaName )
    {
        // Loop on all the SchemaObjects stored and remove those associated
        // with the give schemaName
        for ( T schemaObject : this )
        {
            if ( originalSchemaName.equalsIgnoreCase( schemaObject.getSchemaName() ) )
            {
                schemaObject.setSchemaName( newSchemaName );

                if ( DEBUG )
                {
                    LOG.debug( "Renamed {} schemaName to {}", schemaObject, newSchemaName );
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<T> iterator()
    {
        return ( Iterator<T> ) oidRegistry.iterator();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<String> oidsIterator()
    {
        return byName.keySet().iterator();
    }


    /**
     * {@inheritDoc}
     */
    public T lookup( String oid ) throws LdapException
    {
        if ( oid == null )
        {
            return null;
        }

        T schemaObject = byName.get( oid );

        if ( schemaObject == null )
        {
            // let's try with trimming and lowercasing now
            schemaObject = byName.get( StringTools.trim( StringTools.toLowerCase( oid ) ) );
        }

        if ( schemaObject == null )
        {
            String msg = I18n.err( I18n.ERR_04269, schemaObjectType.name(), oid );
            LOG.debug( msg );
            throw new LdapException( msg );
        }

        if ( DEBUG )
        {
            LOG.debug( "Found {} with oid: {}", schemaObject, oid );
        }

        return schemaObject;
    }


    /**
     * {@inheritDoc}
     */
    public void register( T schemaObject ) throws LdapException
    {
        String oid = schemaObject.getOid();

        if ( byName.containsKey( oid ) )
        {
            String msg = I18n.err( I18n.ERR_04270, schemaObjectType.name(), oid );
            LOG.warn( msg );
            throw new LdapAttributeInUseException( msg );
        }

        byName.put( oid, schemaObject );

        /*
         * add the aliases/names to the name map along with their toLowerCase
         * versions of the name: this is used to make sure name lookups work
         */
        for ( String name : schemaObject.getNames() )
        {
            String lowerName = StringTools.trim( StringTools.toLowerCase( name ) );

            if ( byName.containsKey( lowerName ) )
            {
                String msg = I18n.err( I18n.ERR_04271, schemaObjectType.name(), name );
                LOG.warn( msg );
                throw new LdapAttributeInUseException( msg );
            }
            else
            {
                byName.put( lowerName, schemaObject );
            }
        }

        // And register the oid -> schemaObject relation
        oidRegistry.register( schemaObject );

        if ( LOG.isDebugEnabled() )
        {
            LOG.debug( "registered " + schemaObject.getName() + " for OID {}", oid );
        }
    }


    /**
     * {@inheritDoc}
     */
    public T unregister( String numericOid ) throws LdapException
    {
        if ( !OID.isOID( numericOid ) )
        {
            String msg = I18n.err( I18n.ERR_04272, numericOid );
            LOG.error( msg );
            throw new LdapException( msg );
        }

        T schemaObject = byName.remove( numericOid );

        for ( String name : schemaObject.getNames() )
        {
            byName.remove( name );
        }

        // And remove the SchemaObject from the oidRegistry
        oidRegistry.unregister( numericOid );

        if ( DEBUG )
        {
            LOG.debug( "Removed {} with oid {} from the registry", schemaObject, numericOid );
        }

        return schemaObject;
    }


    /**
     * {@inheritDoc}
     */
    public T unregister( T schemaObject ) throws LdapException
    {
        String oid = schemaObject.getOid();

        if ( !byName.containsKey( oid ) )
        {
            String msg = I18n.err( I18n.ERR_04273, schemaObjectType.name(), oid );
            LOG.warn( msg );
            throw new LdapException( msg );
        }

        // Remove the oid
        T removed = byName.remove( oid );

        /*
         * Remove the aliases/names from the name map along with their toLowerCase
         * versions of the name.
         */
        for ( String name : schemaObject.getNames() )
        {
            byName.remove( StringTools.trim( StringTools.toLowerCase( name ) ) );
        }

        // And unregister the oid -> schemaObject relation
        oidRegistry.unregister( oid );

        return removed;
    }


    /**
     * {@inheritDoc}
     */
    public void unregisterSchemaElements( String schemaName ) throws LdapException
    {
        if ( schemaName == null )
        {
            return;
        }

        // Loop on all the SchemaObjects stored and remove those associated
        // with the give schemaName
        for ( T schemaObject : this )
        {
            if ( schemaName.equalsIgnoreCase( schemaObject.getSchemaName() ) )
            {
                String oid = schemaObject.getOid();
                SchemaObject removed = unregister( oid );

                if ( DEBUG )
                {
                    LOG.debug( "Removed {} with oid {} from the registry", removed, oid );
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public String getOidByName( String name ) throws LdapException
    {
        T schemaObject = byName.get( name );

        if ( schemaObject == null )
        {
            // last resort before giving up check with lower cased version
            String lowerCased = name.toLowerCase();

            schemaObject = byName.get( lowerCased );

            // ok this name is not for a schema object in the registry
            if ( schemaObject == null )
            {
                throw new LdapException( I18n.err( I18n.ERR_04274, name ) );
            }
        }

        // we found the schema object by key on the first lookup attempt
        return schemaObject.getOid();
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObjectRegistry<T> copy( SchemaObjectRegistry<T> original )
    {
        // Fill the byName and OidRegistry maps, the type has already be copied
        for ( String key : ( ( DefaultSchemaObjectRegistry<T> ) original ).byName.keySet() )
        {
            // Clone each SchemaObject
            T value = ( ( DefaultSchemaObjectRegistry<T> ) original ).byName.get( key );

            if ( value instanceof LoadableSchemaObject )
            {
                // Update the data structure. 
                // Comparators, Normalizers and SyntaxCheckers aren't copied, 
                // they are immutable
                byName.put( key, value );

                // Update the OidRegistry
                oidRegistry.put( value );
            }
            else
            {
                T copiedValue = null;

                // Copy the value if it's not already in the oidRegistry
                if ( oidRegistry.contains( value.getOid() ) )
                {
                    try
                    {
                        copiedValue = ( T ) oidRegistry.getSchemaObject( value.getOid() );
                    }
                    catch ( LdapException ne )
                    {
                        // Can't happen
                    }
                }
                else
                {
                    copiedValue = ( T ) value.copy();
                }

                // Update the data structure. 
                byName.put( key, copiedValue );

                // Update the OidRegistry
                oidRegistry.put( copiedValue );
            }
        }

        return this;
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObject get( String oid )
    {
        try
        {
            return oidRegistry.getSchemaObject( oid );
        }
        catch ( LdapException ne )
        {
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObjectType getType()
    {
        return schemaObjectType;
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return oidRegistry.size();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( schemaObjectType ).append( ": " );
        boolean isFirst = true;

        for ( String name : byName.keySet() )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ", " );
            }

            T schemaObject = byName.get( name );

            sb.append( '<' ).append( name ).append( ", " ).append( schemaObject.getOid() ).append( '>' );
        }

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        // Clear all the schemaObjects
        for ( SchemaObject schemaObject : oidRegistry )
        {
            // Don't clear LoadableSchemaObject
            if ( !( schemaObject instanceof LoadableSchemaObject ) )
            {
                schemaObject.clear();
            }
        }

        // Remove the byName elements
        byName.clear();

        // Clear the OidRegistry
        oidRegistry.clear();
    }
}

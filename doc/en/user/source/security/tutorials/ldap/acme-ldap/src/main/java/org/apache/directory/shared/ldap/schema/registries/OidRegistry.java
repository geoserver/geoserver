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


import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Object identifier registry. It stores the OIDs for AT, OC, MR, LS, MRU, DSR, DCR and NF.
 * An OID is unique, and associated with a SO.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923524 $
 */
public class OidRegistry implements Iterable<SchemaObject>
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( OidRegistry.class );

    /** Speedup for DEBUG mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** Maps OID to a SchemaObject */
    private Map<String, SchemaObject> byOid = new HashMap<String, SchemaObject>();


    /**
     * Tells if the given OID is present on this registry
     * 
     * @param oid The OID to lookup
     * @return true if the OID alreadyexists
     */
    public boolean contains( String oid )
    {
        return byOid.containsKey( oid );
    }


    /**
     * Gets the primary name associated with an OID.  The primary name is the
     * first name specified for the OID.
     * 
     * @param oid the object identifier
     * @return the primary name
     * @throws LdapException if oid does not exist
     */
    public String getPrimaryName( String oid ) throws LdapException
    {
        SchemaObject schemaObject = byOid.get( oid );

        if ( schemaObject != null )
        {
            return schemaObject.getName();
        }
        else
        {
            String msg = I18n.err( I18n.ERR_04286, oid );
            LOG.error( msg );
            throw new LdapException( msg );
        }
    }


    /**
     * Gets the SchemaObject associated with an OID. 
     * 
     * @param oid the object identifier
     * @return the associated SchemaObject
     * @throws LdapException if oid does not exist
     */
    public SchemaObject getSchemaObject( String oid ) throws LdapException
    {
        SchemaObject schemaObject = byOid.get( oid );

        if ( schemaObject != null )
        {
            return schemaObject;
        }
        else
        {
            String msg = I18n.err( I18n.ERR_04287, oid );
            LOG.error( msg );
            throw new LdapException( msg );
        }
    }


    /**
     * Gets the names associated with an OID.  An OID is unique however it may 
     * have many names used to refer to it.  A good example is the cn and
     * commonName attribute names for OID 2.5.4.3.  Within a server one name 
     * within the set must be chosen as the primary name.  This is used to
     * name certain things within the server internally.  If there is more than
     * one name then the first name is taken to be the primary.
     * 
     * @param oid the OID for which we return the set of common names
     * @return a sorted set of names
     * @throws LdapException if oid does not exist
     */
    public List<String> getNameSet( String oid ) throws LdapException
    {
        SchemaObject schemaObject = byOid.get( oid );

        if ( null == schemaObject )
        {
            String msg = I18n.err( I18n.ERR_04288, oid );
            LOG.error( msg );
            throw new LdapException( msg );
        }

        List<String> names = schemaObject.getNames();

        if ( IS_DEBUG )
        {
            LOG.debug( "looked up names '{}' for OID '{}'", ArrayUtils.toString( names ), oid );
        }

        return names;
    }


    /**
     * Lists all the OIDs within the registry.  This may be a really big list.
     * 
     * @return all the OIDs registered
     */
    public Iterator<String> iteratorOids()
    {
        return Collections.unmodifiableSet( byOid.keySet() ).iterator();
    }


    /**
     * Lists all the SchemaObjects within the registry.  This may be a really big list.
     * 
     * @return all the SchemaObject registered
     */
    public Iterator<SchemaObject> iterator()
    {
        return byOid.values().iterator();
    }


    /**
     * Adds an OID name pair to the registry.
     * 
     * @param type The SchemaObjectType the oid belongs to
     * @param oid the OID to add or associate a new name with
     */
    public void register( SchemaObject schemaObject ) throws LdapException
    {
        if ( schemaObject == null )
        {
            String message = I18n.err( I18n.ERR_04289 );

            LOG.debug( message );
            throw new LdapException( message );
        }

        String oid = schemaObject.getOid();

        if ( !OID.isOID( oid ) )
        {
            String message = I18n.err( I18n.ERR_04290 );

            LOG.debug( message );
            throw new LdapException( message );
        }

        /*
         * Update OID Map if it does not already exist
         */
        if ( byOid.containsKey( oid ) )
        {
            String message = I18n.err( I18n.ERR_04291, oid );
            LOG.info( message );
            return;
        }
        else
        {
            byOid.put( oid, schemaObject );

            if ( IS_DEBUG )
            {
                LOG.debug( "registed SchemaObject '" + schemaObject + "' with OID: " + oid );
            }
        }
    }


    /**
     * Store the given SchemaObject into the OidRegistry. Available only to 
     * the current package. A weak form (no check is done) of the register 
     * method, define for clone methods.
     *
     * @param schemaObject The SchemaObject to inject into the OidRegistry
     */
    /* No qualifier */void put( SchemaObject schemaObject )
    {
        byOid.put( schemaObject.getOid(), schemaObject );
    }


    /**
     * Removes an oid from this registry.
     *
     * @param oid the numeric identifier for the object
     * @throws LdapException if the identifier is not numeric
     */
    public void unregister( String oid ) throws LdapException
    {
        // Removes the <OID, names> from the byOID map
        SchemaObject removed = byOid.remove( oid );

        if ( IS_DEBUG )
        {
            LOG.debug( "Unregisted SchemaObject '{}' with OID: {}", removed, oid );
        }
    }


    /**
     * Copy the OidRegistry, without the contained values
     * 
     * @return A new OidRegistry instance
     */
    public OidRegistry copy()
    {
        OidRegistry copy = new OidRegistry();

        // Clone the map
        copy.byOid = new HashMap<String, SchemaObject>();

        return copy;
    }


    /**
     * @return The number of stored OIDs
     */
    public int size()
    {
        return byOid.size();
    }


    public void clear()
    {
        // remove all the OID
        byOid.clear();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if ( byOid != null )
        {
            boolean isFirst = true;

            for ( String oid : byOid.keySet() )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ", " );
                }

                sb.append( "<" );

                SchemaObject schemaObject = byOid.get( oid );

                if ( schemaObject != null )
                {
                    sb.append( schemaObject.getObjectType() );
                    sb.append( ", " );
                    sb.append( schemaObject.getOid() );
                    sb.append( ", " );
                    sb.append( schemaObject.getName() );
                }

                sb.append( ">" );
            }
        }

        return sb.toString();
    }
}

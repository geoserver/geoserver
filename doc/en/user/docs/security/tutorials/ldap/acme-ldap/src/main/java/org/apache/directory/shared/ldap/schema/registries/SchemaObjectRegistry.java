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


import java.util.Iterator;

import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;


/**
 * Common schema object registry interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface SchemaObjectRegistry<T extends SchemaObject>
{
    /**
     * Checks to see if an SchemaObject exists in the registry, by its
     * OID or name. 
     * 
     * @param oid the object identifier or name of the SchemaObject
     * @return true if a SchemaObject definition exists for the oid, false
     * otherwise
     */
    boolean contains( String oid );


    /**
     * Gets the name of the schema this schema object is associated with.
     *
     * @param id the object identifier or the name
     * @return the schema name
     * @throws LdapException if the schema object does not exist
     */
    String getSchemaName( String oid ) throws LdapException;


    /**
     * Gets the SchemaObject associated with a given OID.
     *
     * @param oid The SchemaObject's OID we are looking for
     * @return The SchemaObject, if any. Null otherwise
     */
    SchemaObject get( String oid );


    /**
     * Modify all the SchemaObject using a schemaName when this name changes.
     *
     * @param originalSchemaName The original Schema name
     * @param newSchemaName The new Schema name
     * @throws LdapException if the schema object does not exist
     */
    void renameSchema( String originalSchemaName, String newSchemaName ) throws LdapException;


    /**
     * Gets an iterator over the registered schema objects in the registry.
     *
     * @return an Iterator of homogeneous schema objects
     */
    Iterator<T> iterator();


    /**
     * Gets an iterator over the registered schema objects'OID in the registry.
     *
     * @return an Iterator of OIDs
     */
    Iterator<String> oidsIterator();


    /**
     * Looks up a SchemaObject by its unique Object Identifier or by name.
     *
     * @param oid the object identifier or name
     * @return the SchemaObject instance for the id
     * @throws LdapException if the SchemaObject does not exist
     */
    T lookup( String oid ) throws LdapException;


    /**
     * Registers a new SchemaObject with this registry.
     *
     * @param schemaObject the SchemaObject to register
     * @throws LdapException if the SchemaObject is already registered or
     * the registration operation is not supported
     */
    void register( T schemaObject ) throws LdapException;


    /**
     * Removes the SchemaObject registered with this registry, using its
     * numeric OID.
     * 
     * @param numericOid the numeric identifier
     * @throws LdapException if the numeric identifier is invalid
     */
    T unregister( String numericOid ) throws LdapException;


    /**
     * Removes the SchemaObject registered with this registry.
     * 
     * @param T the schemaObject to unregister
     * @throws LdapException if the schemaObject can't be unregistered is invalid
     */
    T unregister( T schemaObject ) throws LdapException;


    /**
     * Unregisters all SchemaObjects defined for a specific schema from
     * this registry.
     * 
     * @param schemaName the name of the schema whose SchemaObjects will be removed from
     */
    void unregisterSchemaElements( String schemaName ) throws LdapException;


    /**
     * Gets the numericOid for a name/alias if one is associated.  To prevent
     * lookup failures due to case variance in the name, a failure to lookup the
     * OID, will trigger a lookup using a lower cased version of the name and 
     * the name that failed to match will automatically be associated with the
     * OID.
     * 
     * @param name The name we are looking the oid for
     * @return The numericOID associated with this name
     * @throws LdapException If the OID can't be found
     */
    String getOidByName( String name ) throws LdapException;


    /**
     * Copy a DefaultSchemaObjectRegistry. All the stored SchemaObject will also
     * be copied, by the cross references will be lost.
     * 
     * @return SchemaObjectRegistry<T> The copied registry
     */
    SchemaObjectRegistry<T> copy();


    /**
     * @return the type
     */
    SchemaObjectType getType();


    /**
     *  @return The number of AttributeType stored
     */
    int size();


    /**
     * Clear the registry from all its content
     */
    void clear() throws LdapException;
}

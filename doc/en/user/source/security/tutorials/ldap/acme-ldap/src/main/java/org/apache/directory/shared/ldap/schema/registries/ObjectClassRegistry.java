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
import java.util.List;

import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.ObjectClass;


/**
 * ObjectClass registry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923524 $
 */
public interface ObjectClassRegistry extends SchemaObjectRegistry<ObjectClass>,
    Iterable<ObjectClass>
{
    /**
     * Quick lookup to see if an objectClass has descendants.
     * 
     * @param ancestorId the name alias or OID for an ObjectClass
     * @return an Iterator over the ObjectClasses which have the ancestor
     * within their superior chain to the top
     * @throws LdapException if the ancestor ObjectClass cannot be 
     * discerned from the ancestorId supplied
     */
    boolean hasDescendants( String ancestorId ) throws LdapException;
    
    
    /**
     * Get's an iterator over the set of descendant ObjectClasses for
     * some ancestor's name alias or their OID.
     * 
     * @param ancestorId the name alias or OID for an ObjectClass
     * @return an Iterator over the ObjectClasses which have the ancestor
     * within their superior chain to the top
     * @throws LdapException if the ancestor ObjectClass cannot be 
     * discerned from the ancestorId supplied
     */
    Iterator<ObjectClass> descendants( String ancestorId ) throws LdapException;

    
    /**
     * Store the ObjectClass into a map associating an ObjectClass to its
     * descendants.
     * 
     * @param attributeType The ObjectClass to register
     * @throws LdapException If something went wrong
     */
    void registerDescendants( ObjectClass objectClass, List<ObjectClass> ancestors ) 
        throws LdapException;
    
    
    /**
     * Remove the ObjectClass from the map associating an ObjectClass to its
     * descendants.
     * 
     * @param attributeType The ObjectClass to unregister
     * @param ancestor its ancestor 
     * @throws LdapException If something went wrong
     */
    void unregisterDescendants( ObjectClass attributeType, List<ObjectClass> ancestors ) 
        throws LdapException;
    
    
    /**
     * Registers a new ObjectClass with this registry.
     *
     * @param objectClass the ObjectClass to register
     * @throws LdapException if the ObjectClass is already registered or
     * the registration operation is not supported
     */
    void register( ObjectClass objectClass ) throws LdapException;
    
    
    /**
     * Removes the ObjectClass registered with this registry.
     * 
     * @param numericOid the numeric identifier
     * @throws LdapException if the numeric identifier is invalid
     */
    ObjectClass unregister( String numericOid ) throws LdapException;
    
    
    /**
     * Copy the ObjectClassRegistry
     */
    ObjectClassRegistry copy();
}

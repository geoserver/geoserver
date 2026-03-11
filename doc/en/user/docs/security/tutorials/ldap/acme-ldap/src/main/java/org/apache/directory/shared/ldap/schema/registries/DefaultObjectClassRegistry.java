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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An ObjectClass registry's service default implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 828111 $
 */
public class DefaultObjectClassRegistry extends DefaultSchemaObjectRegistry<ObjectClass> 
    implements ObjectClassRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultObjectClassRegistry.class );

    /** Speedup for DEBUG mode */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** maps OIDs to a Set of descendants for that OID */
    private Map<String,Set<ObjectClass>> oidToDescendants;

    /**
     * Creates a new default ObjectClassRegistry instance.
     */
    public DefaultObjectClassRegistry()
    {
        super( SchemaObjectType.OBJECT_CLASS, new OidRegistry() );
        oidToDescendants = new HashMap<String,Set<ObjectClass>>();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean hasDescendants( String ancestorId ) throws LdapException
    {
        try
        {
            String oid = getOidByName( ancestorId );
            Set<ObjectClass> descendants = oidToDescendants.get( oid );
            return (descendants != null) && !descendants.isEmpty();
        }
        catch ( LdapException ne )
        {
            throw new LdapNoSuchAttributeException( ne.getMessage() );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Iterator<ObjectClass> descendants( String ancestorId ) throws LdapException
    {
        try
        {
            String oid = getOidByName( ancestorId );
            Set<ObjectClass> descendants = oidToDescendants.get( oid );
            
            if ( descendants == null )
            {
                return Collections.EMPTY_SET.iterator();
            }
            
            return descendants.iterator();
        }
        catch ( LdapException ne )
        {
            throw new LdapNoSuchAttributeException( ne.getMessage() );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public void registerDescendants( ObjectClass objectClass, List<ObjectClass> ancestors ) 
        throws LdapException
    {
        // add this attribute to descendant list of other attributes in superior chain
        if ( ( ancestors == null ) || ( ancestors.size() == 0 ) ) 
        {
            return;
        }
        
        for ( ObjectClass ancestor : ancestors )
        {
            // Get the ancestor's descendant, if any
            Set<ObjectClass> descendants = oidToDescendants.get( ancestor.getOid() );
    
            // Initialize the descendant Set to store the descendants for the attributeType
            if ( descendants == null )
            {
                descendants = new HashSet<ObjectClass>( 1 );
                oidToDescendants.put( ancestor.getOid(), descendants );
            }
            
            // Add the current ObjectClass as a descendant
            descendants.add( objectClass );
            
            try
            {
                // And recurse until we reach the top of the hierarchy
                registerDescendants( objectClass, ancestor.getSuperiors() );
            }
            catch ( LdapException ne )
            {
                throw new LdapNoSuchAttributeException( ne.getMessage() );
            }
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void unregisterDescendants( ObjectClass attributeType, List<ObjectClass> ancestors ) 
        throws LdapException
    {
        // add this attribute to descendant list of other attributes in superior chain
        if ( ( ancestors == null ) || ( ancestors.size() == 0 ) ) 
        {
            return;
        }
        
        for ( ObjectClass ancestor : ancestors )
        {
            // Get the ancestor's descendant, if any
            Set<ObjectClass> descendants = oidToDescendants.get( ancestor.getOid() );
    
            if ( descendants != null )
            {
                descendants.remove( attributeType );
                
                if ( descendants.size() == 0 )
                {
                    oidToDescendants.remove( descendants );
                }
            }
            
            try
            {
                // And recurse until we reach the top of the hierarchy
                unregisterDescendants( attributeType, ancestor.getSuperiors() );
            }
            catch ( LdapException ne )
            {
                throw new LdapNoSuchAttributeException( ne.getMessage() );
            }
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public ObjectClass unregister( String numericOid ) throws LdapException
    {
        try
        {
            ObjectClass removed = super.unregister( numericOid );
    
            // Deleting an ObjectClass which might be used as a superior means we have
            // to recursively update the descendant map. We also have to remove
            // the at.oid -> descendant relation
            oidToDescendants.remove( numericOid );
            
            // Now recurse if needed
            unregisterDescendants( removed, removed.getSuperiors() );
            
            return removed;
        }
        catch ( LdapException ne )
        {
            throw new LdapNoSuchAttributeException( ne.getMessage() );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public DefaultObjectClassRegistry copy()
    {
        DefaultObjectClassRegistry copy = new DefaultObjectClassRegistry();
        
        // Copy the base data
        copy.copy( this );
        
        return copy;
    }


    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        // Clear the contained SchemaObjects
        for ( SchemaObject objectClass : oidRegistry )
        {
            objectClass.clear();
        }
        
        // First clear the shared elements
        super.clear();
        
        // and clear the descendant
        for ( String oid : oidToDescendants.keySet() )
        {
            Set<ObjectClass> descendants = oidToDescendants.get( oid );
            
            if ( descendants != null )
            {
                descendants.clear();
            }
        }
        
        oidToDescendants.clear();
    }
}

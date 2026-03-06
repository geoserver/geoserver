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


import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Normalizer registry's service default implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 831344 $
 */
public class DefaultNormalizerRegistry extends DefaultSchemaObjectRegistry<Normalizer>
    implements NormalizerRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultNormalizerRegistry.class );

    /** A speedup for debug */
    private static final boolean DEBUG = LOG.isDebugEnabled();
    
    /**
     * Creates a new default NormalizerRegistry instance.
     */
    public DefaultNormalizerRegistry()
    {
        super( SchemaObjectType.NORMALIZER, new OidRegistry() );
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
        for ( Normalizer normalizer : this )
        {
            if ( schemaName.equalsIgnoreCase( normalizer.getSchemaName() ) )
            {
                String oid = normalizer.getOid();
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
    public DefaultNormalizerRegistry copy()
    {
        DefaultNormalizerRegistry copy = new DefaultNormalizerRegistry();
        
        // Copy the base data
        copy.copy( this );
        
        return copy;
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
            
            Normalizer normalizer = byName.get( name );
            
            String fqcn = normalizer.getFqcn();
            int lastDotPos = fqcn.lastIndexOf( '.' );
            
            sb.append( '<' ).append( normalizer.getOid() ).append( ", " );
            
            
            if ( lastDotPos > 0 )
            {
                sb.append( fqcn.substring( lastDotPos + 1 ) );
            }
            else
            {
                sb.append( fqcn );
            }
            
            sb.append( '>' );
        }
        
        return sb.toString();
    }
}

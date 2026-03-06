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
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SyntaxChecker registry component's service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 831344 $
 */
public class DefaultSyntaxCheckerRegistry extends  DefaultSchemaObjectRegistry<SyntaxChecker>
    implements SyntaxCheckerRegistry
{
    /** static class logger */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultSyntaxCheckerRegistry.class );

    /** A speedup for debug */
    private static final boolean DEBUG = LOG.isDebugEnabled();
    
    /**
     * Creates a new default SyntaxCheckerRegistry instance.
     */
    public DefaultSyntaxCheckerRegistry()
    {
        super( SchemaObjectType.SYNTAX_CHECKER, new OidRegistry() );
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
        for ( SyntaxChecker syntaxChecker : this )
        {
            if ( schemaName.equalsIgnoreCase( syntaxChecker.getSchemaName() ) )
            {
                String oid = syntaxChecker.getOid();
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
    public DefaultSyntaxCheckerRegistry copy()
    {
        DefaultSyntaxCheckerRegistry copy = new DefaultSyntaxCheckerRegistry();
        
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
            
            SyntaxChecker syntaxChecker = byName.get( name );
            
            String fqcn = syntaxChecker.getFqcn();
            int lastDotPos = fqcn.lastIndexOf( '.' );
            
            sb.append( '<' ).append( syntaxChecker.getOid() ).append( ", " );
            
            
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

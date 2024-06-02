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


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.schema.SchemaObjectWrapper;
import org.apache.directory.shared.ldap.util.StringTools;



/**
 * The default Schema interface implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultSchema implements Schema
{
    /** The default schema's owner */
    private static final String DEFAULT_OWNER = "uid=admin,ou=system";
    
    /** Tells if this schema is disabled */
    private boolean disabled;
    
    /** Contains the list of schema it depends on */
    private String[] dependencies;
    
    /** The schema owner */
    private String owner;
    
    /** The schema name */
    private String name;
    
    /** The set of SchemaObjects declared in this schema */
    private Set<SchemaObjectWrapper> content;
    
    
    /**
     * Creates a new instance of DefaultSchema.
     *
     * @param name The schema's name
     */
    public DefaultSchema( String name )
    {
        this( name, null, null, false );
    }
    
        
    /**
     * Creates a new instance of DefaultSchema.
     *
     * @param name The schema's name
     * @param owner the schema's owner
     */
    public DefaultSchema( String name, String owner )
    {
        this( name, owner, null, false );
    }
    
        
    /**
     * Creates a new instance of DefaultSchema.
     *
     * @param name The schema's name
     * @param owner the schema's owner
     * @param dependencies The list of schemas it depends on 
     */
    public DefaultSchema( String name, String owner, String[] dependencies )
    {
        this( name, owner, dependencies, false );
    }
    
        
    /**
     * Creates a new instance of DefaultSchema.
     *
     * @param name The schema's name
     * @param owner the schema's owner
     * @param dependencies The list of schemas it depends on
     * @param disabled Set the status for this schema 
     */
    public DefaultSchema( String name, String owner, String[] dependencies, boolean disabled )
    {
        if ( name == null )
        {
            throw new NullPointerException( I18n.err( I18n.ERR_04266 ) );
        }
        
        this.name = name;
        
        if ( owner != null )
        {
            this.owner = owner;
        }
        else
        {
            this.owner = DEFAULT_OWNER;
        }
        
        if ( dependencies != null )
        {
            this.dependencies = dependencies;
        }
        else
        {
            this.dependencies = StringTools.EMPTY_STRINGS;
        }
        
        this.disabled = disabled;
        
        content = new HashSet<SchemaObjectWrapper>(); 
    }


    /**
     * {@inheritDoc}
     */
    public String[] getDependencies()
    {
        String[] copy = new String[dependencies.length];
        System.arraycopy( dependencies, 0, copy, 0, dependencies.length );
        return copy;
    }

    
    /**
     * {@inheritDoc}
     */
    public void addDependencies( String... dependencies )
    {
        if ( dependencies != null )
        {
            this.dependencies = new String[dependencies.length];
            System.arraycopy( this.dependencies, 0, dependencies, 0, dependencies.length );
        }
    }

    
    /**
     * {@inheritDoc}
     */
    public String getOwner()
    {
        return owner;
    }

    
    /**
     * {@inheritDoc}
     */
    public String getSchemaName()
    {
        return name;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean isDisabled()
    {
        return disabled;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean isEnabled()
    {
        return !disabled;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void disable()
    {
        this.disabled = true;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void enable()
    {
        this.disabled = false;
    }


    /**
     * {@inheritDoc}
     */
    public Set<SchemaObjectWrapper> getContent()
    {
        return content;
    }
    

    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder( "\tSchema Name: " );
        sb.append( name );
        sb.append( "\n\t\tDisabled: " );
        sb.append( disabled );
        sb.append( "\n\t\tOwner: " );
        sb.append( owner );
        sb.append( "\n\t\tDependencies: " );
        sb.append( Arrays.toString( dependencies ) );
        
        // TODO : print the associated ShcemaObjects
        return sb.toString();
    }
}

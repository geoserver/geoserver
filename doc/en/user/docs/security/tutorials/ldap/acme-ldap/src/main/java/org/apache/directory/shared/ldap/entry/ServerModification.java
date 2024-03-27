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
package org.apache.directory.shared.ldap.entry;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An internal implementation for a ModificationItem. The name has been
 * chosen so that it does not conflict with @see ModificationItem
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerModification implements Modification
{
    public static final long serialVersionUID = 1L;
    
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( ServerModification.class );

    /** The modification operation */
    private ModificationOperation operation;
    
    /** The attribute which contains the modification */
    private EntryAttribute attribute;
 
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------
    /**
     * Create a new instance of a ServerModification.
     */
    public ServerModification()
    {
    }
    
    
    /**
     * Create a new instance of a ServerModification.
     * 
     * @param operation the Modification operation (one of add, replace or remove)
     * @param attribute the modified attribute
     */
    public ServerModification( ModificationOperation operation, EntryAttribute attribute )
    {
        this.operation = operation;
        this.attribute = attribute;
    }
    
    
    public ServerModification( SchemaManager schemaManager, Modification modification )
    {
        operation = modification.getOperation();
        
        EntryAttribute modAttribute = modification.getAttribute();
        
        try
        {
            AttributeType at = null;
            
            if ( modAttribute instanceof DefaultServerAttribute )
            {
                at = ((EntryAttribute)modAttribute).getAttributeType();
            }
            else
            {
                at = schemaManager.lookupAttributeTypeRegistry( modAttribute.getId() );
            }
            
            attribute = new DefaultServerAttribute( at, modAttribute );
        }
        catch ( LdapException ne )
        {
            // The attributeType is incorrect. Log, but do nothing otherwise.
            LOG.error( I18n.err( I18n.ERR_04472, modAttribute.getId() ) );
        }
    }
    
    
    //-------------------------------------------------------------------------
    // API
    //-------------------------------------------------------------------------
    /**
     *  @return the operation
     */
    public ModificationOperation getOperation()
    {
        return operation;
    }
    
    
    /**
     * Store the modification operation
     *
     * @param operation The DirContext value to assign
     */
    public void setOperation( int operation )
    {
        this.operation = ModificationOperation.getOperation( operation );
    }

    
    /**
     * Store the modification operation
     *
     * @param operation The DirContext value to assign
     */
    public void setOperation( ModificationOperation operation )
    {
        this.operation = operation;
    }
        
    
    /**
     * @return the attribute containing the modifications
     */
    public EntryAttribute getAttribute()
    {
        return attribute;
    }
    
    
    /**
     * Set the attribute's modification
     *
     * @param attribute The modified attribute 
     */
    public void setAttribute( EntryAttribute attribute )
    {
        this.attribute = (EntryAttribute)attribute;
    }
    

    /**
     * Convert the current ServerModification to a ClientModification instance 
     *
     * @return a new ClientModification instance
     */
    public Modification toClientModification()
    {
        ModificationOperation newOperation = operation;
        EntryAttribute newAttribute = attribute.toClientAttribute();
        Modification newModification = new ClientModification( newOperation, newAttribute );
        
        return newModification;
    }
    
    //-------------------------------------------------------------------------
    // Overloaded Object class methods
    //-------------------------------------------------------------------------
    /**
     * Compute the modification @see Object#hashCode
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int h = 37;
        
        h += h*17 + operation.getValue();
        h += h*17 + attribute.hashCode();
        
        return h;
    }
    
    
    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object that )
    {
        // Shortcut
        if ( this == that )
        {
            return true;
        }
        
        if ( ! ( that instanceof ServerModification ) )
        {
            return false;
        }
        
        ServerModification modification = (ServerModification)that;
        
        if ( operation != modification.getOperation() )
        {
            return false;
        }
        
        if ( attribute == null )
        {
            return modification.getAttribute() == null;
        }
        
        return attribute.equals( modification.getAttribute() );
    }
    
    
    /**
     * Create a clone instance
     */
    public ServerModification clone()
    {
        try
        {
            ServerModification clone = (ServerModification)super.clone();
            
            clone.attribute = (EntryAttribute)this.attribute.clone();
            return clone;
        }
        catch ( CloneNotSupportedException cnse )
        {
            return null;
        }
    }
    
    
    /**
     * @see java.io.Externalizable#writeExternal(ObjectOutput)
     * 
     * We can't use this method for a ServerModification.
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        throw new IllegalStateException( I18n.err( I18n.ERR_04469 ) );
    }
    
    
    /**
     * @see java.io.Externalizable#readExternal(ObjectInput)
     * 
     * We can't use this method for a ServerModification.
     */
    public void readExternal( ObjectInput in ) throws IOException
    {
        throw new IllegalStateException( I18n.err( I18n.ERR_04469 ) );
    }
    
    
    /**
     * Deserialize a ServerModification
     * 
     * @param in The buffer containing the serialized value
     * @param atRegistry The AttributeType registry
     * @throws IOException If we weren't able to deserialize the data
     * @throws ClassNotFoundException if we weren't able to construct a Modification instance
     * @throws LdapException If we didn't found the AttributeType in the registries
     */
    public void deserialize( ObjectInput in, SchemaManager schemaManager ) throws IOException, ClassNotFoundException, LdapException
    {
        // Read the operation
        int op = in.readInt();
        
        operation = ModificationOperation.getOperation( op );
        
        // Read the attribute OID
        String oid = in.readUTF();
        
        // Lookup for tha associated AttributeType
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( oid );
        
        attribute = new DefaultServerAttribute( attributeType );
        
        // Read the attribute
        ((DefaultServerAttribute)attribute).deserialize( in );
    }
    
    
    /**
     * Serialize a ServerModification.
     */
    public void serialize( ObjectOutput out ) throws IOException
    {
        if ( attribute == null )
        {
            throw new IOException( I18n.err( I18n.ERR_04471 ) );
        }
        
        // Write the operation
        out.writeInt( operation.getValue() );
        
        AttributeType at = ((DefaultServerAttribute)attribute).getAttributeType();
        
        // Write the attribute's oid
        out.writeUTF( at.getOid() );
        
        // Write the attribute
        ((DefaultServerAttribute)attribute).serialize( out );
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "Modification: " ).
            append( operation ).
            append( "\n" ).
            append( ", attribute : " ).
            append( attribute );
        
        return sb.toString();
    }
}

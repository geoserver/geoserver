/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.ldap.entry.client;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.directory.shared.ldap.exception.LdapException;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.entry.AbstractEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A default implementation of a ServerEntry which should suite most
 * use cases.
 * 
 * This class is final, it should not be extended.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class DefaultClientEntry extends AbstractEntry<String> //implements ClientEntry
{
    /** Used for serialization */
    private static final long serialVersionUID = 2L;
    
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultClientEntry.class );

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------
    /**
     * Creates a new instance of DefaultClientEntry. 
     * <p>
     * This entry <b>must</b> be initialized before being used !
     */
    public DefaultClientEntry()
    {
        dn = DN.EMPTY_DN;
    }


    /**
     * Creates a new instance of DefaultServerEntry, with a 
     * DN. 
     * 
     * @param dn The DN for this serverEntry. Can be null.
     */
    public DefaultClientEntry( DN dn )
    {
        this.dn = dn;
    }


    /**
     * Creates a new instance of DefaultServerEntry, with a 
     * DN and a list of IDs. 
     * 
     * @param dn The DN for this serverEntry. Can be null.
     * @param upIds The list of attributes to create.
     */
    public DefaultClientEntry( DN dn, String... upIds )
    {
        this.dn = dn;

        for ( String upId:upIds )
        {
            // Add a new AttributeType without value
            set( upId );
        }
    }

    
    /**
     * <p>
     * Creates a new instance of DefaultClientEntry, with a 
     * DN and a list of EntryAttributes.
     * </p> 
     * 
     * @param dn The DN for this serverEntry. Can be null
     * @param attributes The list of attributes to create
     */
    public DefaultClientEntry( DN dn, EntryAttribute... attributes )
    {
        this.dn = dn;

        for ( EntryAttribute attribute:attributes )
        {
            if ( attribute == null )
            {
                continue;
            }
            
            // Store a new ClientAttribute
            this.attributes.put( attribute.getId(), attribute );
        }
    }

    
    //-------------------------------------------------------------------------
    // Helper methods
    //-------------------------------------------------------------------------
    private String getId( String upId ) throws IllegalArgumentException
    {
        String id = StringTools.trim( StringTools.toLowerCase( upId ) );
        
        // If empty, throw an error
        if ( ( id == null ) || ( id.length() == 0 ) ) 
        {
            String message = I18n.err( I18n.ERR_04133 );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        return id;
    }

    
    //-------------------------------------------------------------------------
    // Entry methods
    //-------------------------------------------------------------------------
    /**
     * Add some Attributes to the current Entry.
     *
     * @param attributes The attributes to add
     * @throws LdapException If we can't add any of the attributes
     */
    public void add( EntryAttribute... attributes ) throws LdapException
    {
        // Loop on all the added attributes
        for ( EntryAttribute attribute:attributes )
        {
            // If the attribute already exist, we will add the new values.
            if ( contains( attribute ) )
            {
                EntryAttribute existingAttr = get( attribute.getId() );
                
                // Loop on all the values, and add them to the existing attribute
                for ( Value<?> value:attribute )
                {
                    existingAttr.add( value );
                }
            }
            else
            {
                // Stores the attribute into the entry
                this.attributes.put( attribute.getId(), attribute );
            }
        }
    }

    
    /**
     * Add an attribute (represented by its ID and binary values) into an entry. 
     *
     * @param upId The attribute ID
     * @param values The list of binary values to inject. It can be empty
     * @throws LdapException If the attribute does not exist
     */
    public void add( String upId, byte[]... values ) throws LdapException
    {
        // First, transform the upID to a valid ID
        String id = getId( upId );
        
        // Now, check to see if we already have such an attribute
        EntryAttribute attribute = attributes.get( id );
        
        if ( attribute != null )
        {
            // This Attribute already exist, we add the values 
            // into it. (If the values already exists, they will
            // not be added, but this is done in the add() method)
            attribute.add( values );
            attribute.setUpId( upId );
        }
        else
        {
            // We have to create a new Attribute and set the values
            // and the upId
            attributes.put( id, new DefaultClientAttribute( upId, values ) );
        }
    }


    /**
     * Add some String values to the current Entry.
     *
     * @param upId The user provided ID of the attribute we want to add 
     * some values to
     * @param values The list of String values to add
     * @throws LdapException If we can't add any of the values
     */
    public void add( String upId, String... values ) throws LdapException
    {
        // First, transform the upID to a valid ID
        String id = getId( upId );

        // Now, check to see if we already have such an attribute
        EntryAttribute attribute = attributes.get( id );
        
        if ( attribute != null )
        {
            // This Attribute already exist, we add the values 
            // into it. (If the values already exists, they will
            // not be added, but this is done in the add() method)
            attribute.add( values );
            attribute.setUpId( upId );
        }
        else
        {
            // We have to create a new Attribute and set the values
            // and the upId
            attributes.put( id, new DefaultClientAttribute( upId, values ) );
        }
    }


    /**
     * Add an attribute (represented by its ID and Value values) into an entry. 
     *
     * @param upId The attribute ID
     * @param values The list of Value values to inject. It can be empty
     * @throws LdapException If the attribute does not exist
     */
    public void add( String upId, Value<?>... values ) throws LdapException
    {
        // First, transform the upID to a valid ID
        String id = getId( upId );

        // Now, check to see if we already have such an attribute
        EntryAttribute attribute = attributes.get( id );
        
        if ( attribute != null )
        {
            // This Attribute already exist, we add the values 
            // into it. (If the values already exists, they will
            // not be added, but this is done in the add() method)
            attribute.add( values );
            attribute.setUpId( upId );
        }
        else
        {
            // We have to create a new Attribute and set the values
            // and the upId
            attributes.put( id, new DefaultClientAttribute( upId, values ) );
        }
    }


    /**
     * Clone an entry. All the element are duplicated, so a modification on
     * the original object won't affect the cloned object, as a modification
     * on the cloned object has no impact on the original object
     */
    public Entry clone()
    {
        // First, clone the structure
        DefaultClientEntry clone = (DefaultClientEntry)super.clone();
        
        // Just in case ... Should *never* happen
        if ( clone == null )
        {
            return null;
        }
        
        // An Entry has a DN and many attributes.
        // First, clone the DN, if not null.
        if ( dn != null )
        {
            clone.setDn( (DN)dn.clone() );
        }
        
        // then clone the ClientAttribute Map.
        clone.attributes = (Map<String, EntryAttribute>)(((HashMap<String, EntryAttribute>)attributes).clone());
        
        // now clone all the attributes
        clone.attributes.clear();
        
        for ( EntryAttribute attribute:attributes.values() )
        {
            clone.attributes.put( attribute.getId(), attribute.clone() );
        }
        
        // We are done !
        return clone;
    }
    

    /**
     * <p>
     * Checks if an entry contains a list of attributes.
     * </p>
     * <p>
     * If the list is null or empty, this method will return <code>true</code>
     * if the entry has no attribute, <code>false</code> otherwise.
     * </p>
     *
     * @param attributes The Attributes to look for
     * @return <code>true</code> if all the attributes are found within 
     * the entry, <code>false</code> if at least one of them is not present.
     * @throws LdapException If the attribute does not exist
     */
    public boolean contains( EntryAttribute... attributes ) throws LdapException
    {
        for ( EntryAttribute attribute:attributes )
        {
            if ( attribute == null )
            {
                return this.attributes.size() == 0;
            }
            
            if ( !this.attributes.containsKey( attribute.getId() ) )
            {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * Checks if an entry contains a specific attribute
     *
     * @param attributes The Attributes to look for
     * @return <code>true</code> if the attributes are found within the entry
     * @throws LdapException If the attribute does not exist
     */
    public boolean contains( String upId ) throws LdapException
    {
        String id = getId( upId );
        
        return attributes.containsKey( id );
    }

    
    /**
     * Checks if an entry contains an attribute with some binary values.
     *
     * @param id The Attribute we are looking for.
     * @param values The searched values.
     * @return <code>true</code> if all the values are found within the attribute,
     * false if at least one value is not present or if the ID is not valid. 
     */
    public boolean contains( String upId, byte[]... values )
    {
        String id = getId( upId );
        
        EntryAttribute attribute = attributes.get( id );
        
        if ( attribute == null )
        {
            return false;
        }
        
        return attribute.contains( values );
    }
    
    
    /**
     * Checks if an entry contains an attribute with some String values.
     *
     * @param id The Attribute we are looking for.
     * @param values The searched values.
     * @return <code>true</code> if all the values are found within the attribute,
     * false if at least one value is not present or if the ID is not valid. 
     */
    public boolean contains( String upId, String... values )
    {
        String id = getId( upId );
        
        EntryAttribute attribute = attributes.get( id );
        
        if ( attribute == null )
        {
            return false;
        }
        
        return attribute.contains( values );
    }
    
    
    /**
     * Checks if an entry contains an attribute with some values.
     *
     * @param id The Attribute we are looking for.
     * @param values The searched values.
     * @return <code>true</code> if all the values are found within the attribute,
     * false if at least one value is not present or if the ID is not valid. 
     */
    public boolean contains( String upId, Value<?>... values )
    {
        String id = getId( upId );
        
        EntryAttribute attribute = attributes.get( id );
        
        if ( attribute == null )
        {
            return false;
        }
        
        return attribute.contains( values );
    }
    
    
    /**
     * Checks if an entry contains some specific attributes.
     *
     * @param attributes The Attributes to look for.
     * @return <code>true</code> if the attributes are all found within the entry.
     */
    public boolean containsAttribute( String... attributes )
    {
        for ( String attribute:attributes )
        {
            String id = getId( attribute );
    
            if ( !this.attributes.containsKey( id ) )
            {
                return false;
            }
        }
        
        return true;
    }

    
    /**
     * <p>
     * Returns the attribute with the specified alias. The return value
     * is <code>null</code> if no match is found.  
     * </p>
     * <p>An Attribute with an id different from the supplied alias may 
     * be returned: for example a call with 'cn' may in some implementations 
     * return an Attribute whose getId() field returns 'commonName'.
     * </p>
     *
     * @param alias an aliased name of the attribute identifier
     * @return the attribute associated with the alias
     */
    public EntryAttribute get( String alias )
    {
        try
        {
            String id = getId( alias );
            
            return attributes.get( id );
        }
        catch( IllegalArgumentException iea )
        {
            LOG.error( I18n.err( I18n.ERR_04134, alias ) );
            return null;
        }
    }


    /**
     * <p>
     * Put an attribute (represented by its ID and some binary values) into an entry. 
     * </p>
     * <p> 
     * If the attribute already exists, the previous attribute will be 
     * replaced and returned.
     * </p>
     *
     * @param upId The attribute ID
     * @param values The list of binary values to put. It can be empty.
     * @return The replaced attribute
     */
    public EntryAttribute put( String upId, byte[]... values )
    {
        // Get the normalized form of the ID
        String id = getId( upId );
        
        // Create a new attribute
        EntryAttribute clientAttribute = new DefaultClientAttribute( upId, values );

        // Replace the previous one, and return it back
        return attributes.put( id, clientAttribute );
    }


    /**
     * <p>
     * Put an attribute (represented by its ID and some String values) into an entry. 
     * </p>
     * <p> 
     * If the attribute already exists, the previous attribute will be 
     * replaced and returned.
     * </p>
     *
     * @param upId The attribute ID
     * @param values The list of String values to put. It can be empty.
     * @return The replaced attribute
     */
    public EntryAttribute put( String upId, String... values )
    {
        // Get the normalized form of the ID
        String id = getId( upId );
        
        // Create a new attribute
        EntryAttribute clientAttribute = new DefaultClientAttribute( upId, values );

        // Replace the previous one, and return it back
        return attributes.put( id, clientAttribute );
    }


    /**
     * <p>
     * Put an attribute (represented by its ID and some values) into an entry. 
     * </p>
     * <p> 
     * If the attribute already exists, the previous attribute will be 
     * replaced and returned.
     * </p>
     *
     * @param upId The attribute ID
     * @param values The list of values to put. It can be empty.
     * @return The replaced attribute
     */
    public EntryAttribute put( String upId, Value<?>... values )
    {
        // Get the normalized form of the ID
        String id = getId( upId );
        
        // Create a new attribute
        EntryAttribute clientAttribute = new DefaultClientAttribute( upId, values );

        // Replace the previous one, and return it back
        return attributes.put( id, clientAttribute );
    }


    /**
     * <p>
     * Put some new ClientAttribute using the User Provided ID. 
     * No value is inserted. 
     * </p>
     * <p>
     * If an existing Attribute is found, it will be replaced by an
     * empty attribute, and returned to the caller.
     * </p>
     * 
     * @param upIds The user provided IDs of the AttributeTypes to add.
     * @return A list of replaced Attributes.
     */
    public List<EntryAttribute> set( String... upIds )
    {
        if ( upIds == null )
        {
            String message = I18n.err( I18n.ERR_04135 );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }
        
        List<EntryAttribute> returnedClientAttributes = new ArrayList<EntryAttribute>();
        
        // Now, loop on all the attributeType to add
        for ( String upId:upIds )
        {
            String id = StringTools.trim( StringTools.toLowerCase( upId ) );
            
            if ( id == null )
            {
                String message = I18n.err( I18n.ERR_04136 );
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
            
            if ( attributes.containsKey( id ) )
            {
                // Add the removed serverAttribute to the list
                returnedClientAttributes.add( attributes.remove( id ) );
            }

            EntryAttribute newAttribute = new DefaultClientAttribute( upId );
            attributes.put( id, newAttribute );
        }
        
        return returnedClientAttributes;
    }

    
    /**
     * <p>
     * Places attributes in the attribute collection. 
     * </p>
     * <p>If there is already an attribute with the same ID as any of the 
     * new attributes, the old ones are removed from the collection and 
     * are returned by this method. If there was no attribute with the 
     * same ID the return value is <code>null</code>.
     *</p>
     *
     * @param attributes the attributes to be put
     * @return the old attributes with the same OID, if exist; otherwise
     *         <code>null</code>
     * @exception LdapException if the operation fails
     */
    public List<EntryAttribute> put( EntryAttribute... attributes ) throws LdapException
    {
        // First, get the existing attributes
        List<EntryAttribute> previous = new ArrayList<EntryAttribute>();
        
        for ( EntryAttribute attribute:attributes )
        {
            String id = attribute.getId();
            
            if ( contains( id ) )
            {
                // Store the attribute and remove it from the list
                previous.add( get( id ) );
                this.attributes.remove( id );
            }
            
            // add the new one
            this.attributes.put( id, (EntryAttribute)attribute );            
        }
        
        // return the previous attributes
        return previous;
    }


    public List<EntryAttribute> remove( EntryAttribute... attributes ) throws LdapException
    {
        List<EntryAttribute> removedAttributes = new ArrayList<EntryAttribute>();
        
        for ( EntryAttribute attribute:attributes )
        {
            if ( contains( attribute.getId() ) )
            {
                this.attributes.remove( attribute.getId() );
                removedAttributes.add( attribute );
            }
        }
        
        return removedAttributes;
    }


    /**
     * <p>
     * Removes the attribute with the specified alias. 
     * </p>
     * <p>
     * The removed attribute are returned by this method. 
     * </p>
     * <p>
     * If there is no attribute with the specified alias,
     * the return value is <code>null</code>.
     * </p>
     *
     * @param attributes an aliased name of the attribute to be removed
     * @return the removed attributes, if any, as a list; otherwise <code>null</code>
     */
    public List<EntryAttribute> removeAttributes( String... attributes )
    {
        if ( attributes.length == 0 )
        {
            return null;
        }
        
        List<EntryAttribute> removed = new ArrayList<EntryAttribute>( attributes.length );
        
        for ( String attribute:attributes )
        {
            EntryAttribute attr = get( attribute );
            
            if ( attr != null )
            {
                removed.add( this.attributes.remove( attr.getId() ) );
            }
            else
            {
                String message = I18n.err( I18n.ERR_04137, attribute );
                LOG.warn( message );
                continue;
            }
        }
        
        if ( removed.size() == 0 )
        {
            return null;
        }
        else
        {
            return removed;
        }
    }


    /**
     * <p>
     * Removes the specified binary values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns 
     * <code>false</code>
     * </p> 
     *
     * @param upId The attribute ID  
     * @param values the values to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist. 
     */
    public boolean remove( String upId, byte[]... values ) throws LdapException
    {
        try
        {
            String id = getId( upId );
            
            EntryAttribute attribute = get( id );
            
            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }
            
            int nbOldValues = attribute.size();
            
            // Remove the values
            attribute.remove( values );
            
            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( id );
                
                return true;
            }
            
            if ( nbOldValues != attribute.size() )
            {
                // At least one value have been removed, return true.
                return true;
            }
            else
            {
                // No values have been removed, return false.
                return false;
            }
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( I18n.err( I18n.ERR_04138, upId ) );
            return false;
        }
    }


    /**
     * <p>
     * Removes the specified String values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns 
     * <code>false</code>
     * </p> 
     *
     * @param upId The attribute ID  
     * @param attributes the attributes to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist. 
     */
    public boolean remove( String upId, String... values ) throws LdapException
    {
        try
        {
            String id = getId( upId );
            
            EntryAttribute attribute = get( id );
            
            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }
            
            int nbOldValues = attribute.size();
            
            // Remove the values
            attribute.remove( values );
            
            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( id );
                
                return true;
            }
            
            if ( nbOldValues != attribute.size() )
            {
                // At least one value have been removed, return true.
                return true;
            }
            else
            {
                // No values have been removed, return false.
                return false;
            }
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( I18n.err( I18n.ERR_04138, upId ) );
            return false;
        }
    }


    /**
     * <p>
     * Removes the specified values from an attribute.
     * </p>
     * <p>
     * If at least one value is removed, this method returns <code>true</code>.
     * </p>
     * <p>
     * If there is no more value after having removed the values, the attribute
     * will be removed too.
     * </p>
     * <p>
     * If the attribute does not exist, nothing is done and the method returns 
     * <code>false</code>
     * </p> 
     *
     * @param upId The attribute ID  
     * @param attributes the attributes to be removed
     * @return <code>true</code> if at least a value is removed, <code>false</code>
     * if not all the values have been removed or if the attribute does not exist. 
     */
    public boolean remove( String upId, Value<?>... values ) throws LdapException
    {
        try
        {
            String id = getId( upId );
            
            EntryAttribute attribute = get( id );
            
            if ( attribute == null )
            {
                // Can't remove values from a not existing attribute !
                return false;
            }
            
            int nbOldValues = attribute.size();
            
            // Remove the values
            attribute.remove( values );
            
            if ( attribute.size() == 0 )
            {
                // No mare values, remove the attribute
                attributes.remove( id );
                
                return true;
            }
            
            if ( nbOldValues != attribute.size() )
            {
                // At least one value have been removed, return true.
                return true;
            }
            else
            {
                // No values have been removed, return false.
                return false;
            }
        }
        catch ( IllegalArgumentException iae )
        {
            LOG.error( I18n.err( I18n.ERR_04138, upId ) );
            return false;
        }
    }


    public Iterator<EntryAttribute> iterator()
    {
        return Collections.unmodifiableMap( attributes ).values().iterator();
    }


    /**
     * @see Externalizable#writeExternal(ObjectOutput)<p>
     * 
     * This is the place where we serialize entries, and all theirs
     * elements.
     * <p>
     * The structure used to store the entry is the following :
     * <li>
     * <b>[DN]</b> : If it's null, stores an empty DN
     * </li>
     * <li>
     * <b>[attributes number]</b> : the number of attributes.
     * </li>
     * <li>
     * <b>[attribute]*</b> : each attribute, if we have some
     * </li>
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        // First, the DN
        if ( dn == null )
        {
            // Write an empty DN
            out.writeObject( DN.EMPTY_DN );
        }
        else
        {
            // Write the DN
            out.writeObject( dn );
        }
        
        // Then the attributes. 
        // Store the attributes' nulber first
        out.writeInt( attributes.size() );
        
        // Iterate through the keys.
        for ( EntryAttribute attribute:attributes.values() )
        {
            // Store the attribute
            out.writeObject( attribute );
        }
        
        out.flush();
    }

    
    /**
     * @see Externalizable#readExternal(ObjectInput)
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // Read the DN
        dn = (DN)in.readObject();
        
        // Read the number of attributes
        int nbAttributes = in.readInt();
        
        // Read the attributes
        for ( int i = 0; i < nbAttributes; i++ )
        {
            // Read each attribute
            EntryAttribute attribute = (DefaultClientAttribute)in.readObject();
            
            attributes.put( attribute.getId(), attribute );
        }
    }
    
    
    /**
     * Get the hash code of this ClientEntry.
     *
     * @see java.lang.Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int result = 37;
        
        result = result*17 + dn.hashCode();
        
        SortedMap<String, EntryAttribute> sortedMap = new TreeMap<String, EntryAttribute>();
        
        for ( String id:attributes.keySet() )
        {
            sortedMap.put( id, attributes.get( id ) );
        }
        
        for ( String id:sortedMap.keySet() )
        {
            result = result*17 + sortedMap.get( id ).hashCode();
        }
        
        return result;
    }

    
    /**
     * Tells if an entry has a specific ObjectClass value
     * 
     * @param objectClass The ObjectClass we want to check
     * @return <code>true</code> if the ObjectClass value is present 
     * in the ObjectClass attribute
     */
    public boolean hasObjectClass( String objectClass )
    {
        return contains( "objectclass", objectClass );
    }


    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object o )
    {
        // Short circuit

        if ( this == o )
        {
            return true;
        }
        
        if ( ! ( o instanceof DefaultClientEntry ) )
        {
            return false;
        }
        
        DefaultClientEntry other = (DefaultClientEntry)o;
        
        // Both DN must be equal
        if ( dn == null )
        {
            if ( other.getDn() != null )
            {
                return false;
            }
        }
        else
        {
            if ( !dn.equals( other.getDn() ) )
            {
                return false;
            }
        }
        
        // They must have the same number of attributes
        if ( size() != other.size() )
        {
            return false;
        }
        
        // Each attribute must be equal
        for ( EntryAttribute attribute:other )
        {
            if ( !attribute.equals( this.get( attribute.getId() ) ) )
            {
                return false;
            }
        }
        
        return true;
    }
        

    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "ClientEntry\n" );
        sb.append( "    dn: " ).append( dn.getName() ).append( '\n' );
        
        // First dump the ObjectClass attribute
        if ( containsAttribute( "objectClass" ) )
        {
            EntryAttribute objectClass = get( "objectclass" );
            
            sb.append( objectClass );
        }
        
        if ( attributes.size() != 0 )
        {
            for ( EntryAttribute attribute:attributes.values() )
            {
                if ( !attribute.getId().equals( "objectclass" ) )
                {
                    sb.append( attribute );
                }
            }
        }
        
        return sb.toString();
    }
}

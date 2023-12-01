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
package org.apache.directory.shared.ldap.entry;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.naming.NamingException;

import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A server side entry attribute aware of schema.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class DefaultServerAttribute extends DefaultClientAttribute implements EntryAttribute
{
    public static final long serialVersionUID = 1L;
    
    /** logger for reporting errors that might not be handled properly upstream */
    private static final Logger LOG = LoggerFactory.getLogger( DefaultServerAttribute.class );
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------
    /**
     * 
     * Creates a new instance of DefaultServerAttribute, by copying
     * another attribute, which can be a ClientAttribute. If the other
     * attribute is a ServerAttribute, it will be copied.
     *
     * @param attributeType The attribute's type 
     * @param attribute The attribute to be copied
     */
    public DefaultServerAttribute( AttributeType attributeType, EntryAttribute attribute )
    {
        // Copy the common values. isHR is only available on a ServerAttribute 
        this.attributeType = attributeType;
        this.id = attribute.getId();
        this.upId = attribute.getUpId();

        if ( attributeType == null )
        {
            isHR = attribute.isHR();

            // Copy all the values
            for ( Value<?> value:attribute )
            {
                add( value.clone() );
            }
        }
        else
        {
            
            isHR = attributeType.getSyntax().isHumanReadable();

            // Copy all the values
            for ( Value<?> clientValue:attribute )
            {
                Value<?> serverValue = null; 

                // We have to convert the value first
                if ( clientValue instanceof StringValue )
                {
                    if ( isHR )
                    {
                        serverValue = new StringValue( attributeType, clientValue.getString() );
                    }
                    else
                    {
                        // We have to convert the value to a binary value first
                        serverValue = new BinaryValue( attributeType, 
                            clientValue.getBytes() );
                    }
                }
                else if ( clientValue instanceof BinaryValue )
                {
                    if ( isHR )
                    {
                        // We have to convert the value to a String value first
                        serverValue = new StringValue( attributeType, 
                            clientValue.getString() );
                    }
                    else
                    {
                        serverValue = new BinaryValue( attributeType, clientValue.getBytes() );
                    }
                }

                add( serverValue );
            }
        }
    }
    
    
    // maybe have some additional convenience constructors which take
    // an initial value as a string or a byte[]
    /**
     * Create a new instance of a EntryAttribute, without ID nor value.
     * 
     * @param attributeType the attributeType for the empty attribute added into the entry
     */
    public DefaultServerAttribute( AttributeType attributeType )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04442 ) );
        }
        
        setAttributeType( attributeType );
    }


    /**
     * Create a new instance of a EntryAttribute, without value.
     * 
     * @param upId the ID for the added attributeType
     * @param attributeType the added AttributeType
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType )
    {
        if ( attributeType == null ) 
        {
            String message = I18n.err( I18n.ERR_04442 );
            LOG.error( message );
            throw new IllegalArgumentException( message );
        }

        setAttributeType( attributeType );
        setUpId( upId, attributeType );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new Value which uses the specified
     * attributeType.
     *
     * @param attributeType the attribute type according to the schema
     * @param vals an initial set of values for this attribute
     */
    public DefaultServerAttribute( AttributeType attributeType, Value<?>... vals )
    {
        this( null, attributeType, vals );
    }


    /**
     * Doc me more!
     *
     * If the value does not correspond to the same attributeType, then it's
     * wrapped value is copied into a new Value which uses the specified
     * attributeType.
     * 
     * Otherwise, the value is stored, but as a reference. It's not a copy.
     *
     * @param upId the ID of the added attribute
     * @param attributeType the attribute type according to the schema
     * @param vals an initial set of values for this attribute
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType, Value<?>... vals )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04442 ) );
        }
        
        setAttributeType( attributeType );
        setUpId( upId, attributeType );
        add( vals );
    }


    /**
     * Create a new instance of a EntryAttribute, without ID but with some values.
     * 
     * @param attributeType The attributeType added on creation
     * @param vals The added value for this attribute
     */
    public DefaultServerAttribute( AttributeType attributeType, String... vals )
    {
        this( null, attributeType, vals );
    }


    /**
     * Create a new instance of a EntryAttribute.
     * 
     * @param upId the ID for the added attribute
     * @param attributeType The attributeType added on creation
     * @param vals the added values for this attribute
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType, String... vals )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04442 ) );
        }

        setAttributeType( attributeType );
        add( vals );
        setUpId( upId, attributeType );
    }


    /**
     * Create a new instance of a EntryAttribute, with some byte[] values.
     * 
     * @param attributeType The attributeType added on creation
     * @param vals The value for the added attribute
     */
    public DefaultServerAttribute( AttributeType attributeType, byte[]... vals )
    {
        this( null, attributeType, vals );
    }


    /**
     * Create a new instance of a EntryAttribute, with some byte[] values.
     * 
     * @param upId the ID for the added attribute
     * @param attributeType the AttributeType to be added
     * @param vals the values for the added attribute
     */
    public DefaultServerAttribute( String upId, AttributeType attributeType, byte[]... vals )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04442 ) );
        }

        setAttributeType( attributeType );
        add( vals );
        setUpId( upId, attributeType );
    }
    
    
    //-------------------------------------------------------------------------
    // API
    //-------------------------------------------------------------------------
    /**
     * <p>
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * </p>
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     * <p>
     * If the value's type is different from the attribute's type,
     * the value is not added.
     * </p>
     * It's the responsibility of the caller to check if the stored
     * values are consistent with the attribute's type.
     * <p>
     *
     * @param vals some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    public int add( byte[]... vals )
    {
        if ( !isHR )
        {
            int nbAdded = 0;
            
            for ( byte[] val:vals )
            {
                Value<?> value = new BinaryValue( attributeType, val );
                
                try
                {
                    value.normalize();
                }
                catch( LdapException ne )
                {
                    // The value can't be normalized : we don't add it.
                    LOG.error( I18n.err( I18n.ERR_04449, StringTools.dumpBytes( val ) ) );
                    return 0;
                }
                
                if ( add( value ) != 0 )
                {
                    nbAdded++;
                }
                else
                {
                    LOG.error( I18n.err( I18n.ERR_04450, StringTools.dumpBytes( val ) ) );
                }
            }
            
            return nbAdded;
        }
        else
        {
            // We can't add Binary values into a String serverAttribute
            return 0;
        }
    }    


    /**
     * <p>
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * </p>
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     * If the value's type is different from the attribute's type,
     * the value is not added.
     *
     * @param vals some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    public int add( String... vals )
    {
        if ( isHR )
        {
            int nbAdded = 0;
            
            for ( String val:vals )
            {
                Value<String> newValue = new StringValue( attributeType, val );
                
                if ( add( newValue ) != 0 )
                {
                    nbAdded++;
                }
                else
                {
                    LOG.error( I18n.err( I18n.ERR_04450, val ) );
                }
            }
            
            return nbAdded;
        }
        else
        {
            // We can't add String values into a Binary serverAttribute
            return 0;
        }
    }    


    /**
     * @see EntryAttribute#add(org.apache.directory.shared.ldap.entry.Value...)
     * 
     * @return the number of added values into this attribute
     */
    public int add( Value<?>... vals )
    {
        int nbAdded = 0;
        
        for ( Value<?> val:vals )
        {
            if ( attributeType.getSyntax().isHumanReadable() )
            {
                if ( ( val == null ) || val.isNull() )
                {
                    Value<String> nullSV = new StringValue( attributeType, (String)null );
                    
                    if ( values.add( nullSV ) )
                    {
                        nbAdded++;
                    }
                }
                else if ( val instanceof StringValue )
                {
                    StringValue stringValue = (StringValue)val;
                    
                    if ( stringValue.getAttributeType() == null )
                    {
                        stringValue.apply( attributeType );
                    }
                    
                    if ( values.add( val ) )
                    {
                        nbAdded++;
                    }
                }
                else
                {
                    String message = I18n.err( I18n.ERR_04451 );
                    LOG.error( message );
                }
            }
            else
            {
                if ( val == null )
                {
                    Value<byte[]> nullSV = new BinaryValue( attributeType, (byte[])null );
                    
                    if ( values.add( nullSV ) )
                    {
                        nbAdded++;
                    }
                }
                else
                {
                    if ( val instanceof BinaryValue )
                    {
                        BinaryValue binaryValue = (BinaryValue)val;
                        
                        if ( binaryValue.getAttributeType() == null )
                        {
                            binaryValue = new BinaryValue( attributeType, val.getBytes() ); 
                        }
    
                        if ( values.add( binaryValue ) )
                        {
                            nbAdded++;
                        }
                    }
                    else
                    {
                        String message = I18n.err( I18n.ERR_04452 );
                        LOG.error( message );
                    }
                }
            }
        }
        
        return nbAdded;
    }


    /**
     * Remove all the values from this attribute type, including a 
     * null value. 
     */
    public void clear()
    {
        values.clear();
    }


    /**
     * <p>
     * Indicates whether all the specified values are attribute's values. If
     * at least one value is not an attribute's value, this method will return 
     * <code>false</code>
     * </p>
     * <p>
     * If the Attribute is HR, this method will returns <code>false</code>
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    public boolean contains( byte[]... vals )
    {
        if ( !isHR )
        {
            // Iterate through all the values, and quit if we 
            // don't find one in the values
            for ( byte[] val:vals )
            {
                BinaryValue value = new BinaryValue( attributeType, val );
                
                try
                {
                    value.normalize();
                }
                catch ( LdapException ne )
                {
                    return false;
                }
                
                if ( !values.contains( value ) )
                {
                    return false;
                }
            }
            
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * <p>
     * Indicates whether all the specified values are attribute's values. If
     * at least one value is not an attribute's value, this method will return 
     * <code>false</code>
     * </p>
     * <p>
     * If the Attribute is not HR, this method will returns <code>false</code>
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    public boolean contains( String... vals )
    {
        if ( isHR )
        {
            // Iterate through all the values, and quit if we 
            // don't find one in the values
            for ( String val:vals )
            {
                StringValue value = new StringValue( attributeType, val );
                
                if ( !values.contains( value ) )
                {
                    return false;
                }
            }
            
            return true;
        }
        else
        {
            return false;
        }
    }
    
    
    /**
     * <p>
     * Indicates whether the specified values are some of the attribute's values.
     * </p>
     * <p>
     * If the Attribute is HR, te metho will only accept String Values. Otherwise, 
     * it will only accept Binary values.
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    public boolean contains( Value<?>... vals )
    {
        // Iterate through all the values, and quit if we 
        // don't find one in the values. We have to separate the check
        // depending on the isHR flag value.
        if ( isHR )
        {
            for ( Value<?> val:vals )
            {
                if ( val instanceof StringValue )
                {
                    StringValue stringValue = (StringValue)val;
                    
                    if ( stringValue.getAttributeType() == null )
                    {
                        stringValue.apply( attributeType );
                    }
                    
                    if ( !values.contains( val ) )
                    {
                        return false;
                    }
                }
                else
                {
                    // Not a String value
                    return false;
                }
            }
        }
        else
        {
            for ( Value<?> val:vals )
            {
                if ( val instanceof BinaryValue )
                {
                    if ( !values.contains( val ) )
                    {
                        return false;
                    }
                }
                else
                {
                    // Not a Binary value
                    return false;
                }
            }
        }
        
        return true;
    }
    
    
    /**
     * <p>
     * Checks to see if this attribute is valid along with the values it contains.
     * </p>
     * <p>
     * An attribute is valid if :
     * <li>All of its values are valid with respect to the attributeType's syntax checker</li>
     * <li>If the attributeType is SINGLE-VALUE, then no more than a value should be present</li>
     *</p>
     * @return true if the attribute and it's values are valid, false otherwise
     * @throws NamingException if there is a failure to check syntaxes of values
     */
    public boolean isValid() throws LdapException
    {
        // First check if the attribute has more than one value
        // if the attribute is supposed to be SINGLE_VALUE
        if ( attributeType.isSingleValued() && ( values.size() > 1 ) )
        {
            return false;
        }

        // Check that we can have no value for this attributeType
        if ( values.size() == 0 )
        {
            return attributeType.getSyntax().getSyntaxChecker().isValidSyntax( null );
        }

        for ( Value<?> value : values )
        {
            if ( ! value.isValid() )
            {
                return false;
            }
        }
        
        return true;
    }


    /**
     * @see EntryAttribute#remove(byte[]...)
     * 
     * @return <code>true</code> if all the values shave been removed from this attribute
     */
    public boolean remove( byte[]... vals )
    {
        if ( isHR ) 
        {
            return false;
        }
        
        boolean removed = true;
        
        for ( byte[] val:vals )
        {
            BinaryValue value = new BinaryValue( attributeType, val );
            removed &= values.remove( value );
        }
        
        return removed;
    }


    /**
     * @see EntryAttribute#remove(String...)
     * 
     * @return <code>true</code> if all the values shave been removed from this attribute
     */
    public boolean remove( String... vals )
    {
        if ( !isHR )
        {
            return false;
        }
        
        boolean removed = true;
        
        for ( String val:vals )
        {
            StringValue value = new StringValue( attributeType, val );
            removed &= values.remove( value );
        }
        
        return removed;
    }


    /**
     * @see EntryAttribute#remove(org.apache.directory.shared.ldap.entry.Value...)
     * 
     * @return <code>true</code> if all the values shave been removed from this attribute
     */
    public boolean remove( Value<?>... vals )
    {
        boolean removed = true;
        
        // Loop through all the values to remove. If one of
        // them is not present, the method will return false.
        // As the attribute may be HR or not, we have two separated treatments
        if ( isHR )
        {
            for ( Value<?> val:vals )
            {
                if ( val instanceof StringValue )
                {
                    StringValue stringValue = (StringValue)val;
                    
                    if ( stringValue.getAttributeType() == null )
                    {
                        stringValue.apply( attributeType );
                    }
                    
                    removed &= values.remove( stringValue );
                }
                else
                {
                    removed = false;
                }
            }
        }
        else
        {
            for ( Value<?> val:vals )
            {
                if ( val instanceof BinaryValue )
                {
                    BinaryValue binaryValue = (BinaryValue)val;
                    
                    if ( binaryValue.getAttributeType() == null )
                    {
                        binaryValue = new BinaryValue( attributeType, (byte[])val.get() );
                    }
                    
                    removed &= values.remove( binaryValue );
                }
                else
                {
                    removed = false;
                }
            }
        }
        
        return removed;
    }


    
    /**
     * <p>
     * Overload the ClientAttribte isHR method : we can't change this flag
     * for a ServerAttribute, as the HR is already set using the AttributeType.
     * Set the attribute to Human Readable or to Binary. 
     * </p>
     * 
     * @param isHR <code>true</code> for a Human Readable attribute, 
     * <code>false</code> for a Binary attribute.
     */
    public void setHR( boolean isHR )
    {
        // Do nothing...
    }

    
    /**
     * <p>
     * Overload the {@link DefaultClientAttribute#setId(String)} method.
     * </p>
     * <p>
     * As the attributeType has already been set, we have to be sure that the 
     * argument is compatible with the attributeType's name. 
     * </p>
     * <p>
     * If the given ID is not compatible with the attributeType's possible
     * names, the previously loaded ID will be kept.
     * </p>
     *
     * @param id The attribute ID
     */
    public void setId( String id )
    {
        if ( !StringTools.isEmpty( StringTools.trim( id  ) ) )
        {
            if ( attributeType.getName() == null )
            {
                // If the name is null, then we may have to store an OID
                if ( OID.isOID( id )  && attributeType.getOid().equals( id ) )
                {
                    // Everything is fine, store the upId.
                    // This should not happen...
                    super.setId( id );
                }
            }
            else
            {
                // We have at least one name. Check that the normalized upId
                // is one of those names. Otherwise, the upId may be an OID too.
                // In this case, it must be equals to the attributeType OID.
                String normId = StringTools.lowerCaseAscii( StringTools.trim( id ) );
                
                for ( String atName:attributeType.getNames() )
                {
                    if ( atName.equalsIgnoreCase( normId ) )
                    {
                        // Found ! We can store the upId and get out
                        super.setId( normId );
                        return;
                    }
                }
                
                // Last case, the UpId is an OID
                if ( OID.isOID( normId ) && attributeType.getOid().equals( normId ) )
                {
                    // We have an OID : stores it
                    super.setUpId( normId );
                }
                else
                {
                    // The id is incorrect : this is not allowed 
                    throw new IllegalArgumentException( I18n.err( I18n.ERR_04455, id, attributeType.getName() ) );
                }
            }
        }
        else
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04456 ) );
        }
    }
    
    
    /**
     * <p>
     * Overload the {@link DefaultClientAttribute#setUpId(String)} method.
     * </p>
     * <p>
     * As the attributeType has already been set, we have to be sure that the 
     * argument is compatible with the attributeType's name. 
     * </p>
     * <p>
     * If the given ID is not compatible with the attributeType's possible
     * names, the previously loaded ID will be kept.
     * </p>
     *
     * @param upId The attribute ID
     *
    public void setUpId( String upId )
    {
        if ( !StringTools.isEmpty( StringTools.trim( upId  ) ) )
        {
            if ( attributeType.getName() == null )
            {
                // If the name is null, then we may have to store an OID
                if ( OID.isOID( upId )  && attributeType.getOid().equals( upId ) )
                {
                    // Everything is fine, store the upId.
                    // This should not happen...
                    super.setUpId( upId );
                    return;
                }
            }
            else
            {
                // We have at least one name. Check that the normalized upId
                // is one of those names. Otherwise, the upId may be an OID too.
                // In this case, it must be equals to the attributeType OID.
                String normUpId = StringTools.lowerCaseAscii( StringTools.trim( upId ) );
                
                for ( String atId:attributeType.getNames() )
                {
                    if ( atId.equalsIgnoreCase( normUpId ) )
                    {
                        // Found ! We can store the upId and get out
                        super.setUpId( upId );
                        return;
                    }
                }
                
                // Last case, the UpId is an OID
                if ( OID.isOID( normUpId ) && attributeType.getOid().equals( normUpId ) )
                {
                    // We have an OID : stores it
                    super.setUpId( upId );
                    return;
                }
                
                return;
            }
        }
        
        return;
    }
    
    
    //-------------------------------------------------------------------------
    // Serialization methods
    //-------------------------------------------------------------------------
    
    /**
     * @see java.io.Externalizable#writeExternal(ObjectOutput)
     * 
     * We can't use this method for a ServerAttribute, as we have to feed the value
     * with an AttributeType object
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        throw new IllegalStateException( I18n.err( I18n.ERR_04454 ) );
    }
    
    
    /**
     * @see Externalizable#writeExternal(ObjectOutput)
     * <p>
     * 
     * This is the place where we serialize attributes, and all theirs
     * elements. 
     * 
     * The inner structure is the same as the client attribute, but we can't call
     * it as we won't be able to serialize the serverValues
     * 
     */
    public void serialize( ObjectOutput out ) throws IOException
    {
        // Write the UPId (the id will be deduced from the upID)
        out.writeUTF( upId );
        
        // Write the HR flag, if not null
        if ( isHR != null )
        {
            out.writeBoolean( true );
            out.writeBoolean( isHR );
        }
        else
        {
            out.writeBoolean( false );
        }
        
        // Write the number of values
        out.writeInt( size() );
        
        if ( size() > 0 ) 
        {
            // Write each value
            for ( Value<?> value:values )
            {
                // Write the value, using the correct method
                if ( value instanceof StringValue )
                {
                    ((StringValue)value).serialize( out );
                }
                else
                {
                    ((BinaryValue)value).serialize( out );
                }
            }
        }
    }

    
    /**
     * @see java.io.Externalizable#readExternal(ObjectInput)
     * 
     * We can't use this method for a ServerAttribute, as we have to feed the value
     * with an AttributeType object
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        throw new IllegalStateException( I18n.err( I18n.ERR_04454 ) );
    }
    
    
    /**
     * @see Externalizable#readExternal(ObjectInput)
     */
    public void deserialize( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // Read the ID and the UPId
        upId = in.readUTF();
        
        // Compute the id
        setUpId( upId );
        
        // Read the HR flag, if not null
        if ( in.readBoolean() )
        {
            isHR = in.readBoolean();
        }

        // Read the number of values
        int nbValues = in.readInt();

        if ( nbValues > 0 )
        {
            for ( int i = 0; i < nbValues; i++ )
            {
                Value<?> value = null;
                
                if ( isHR )
                {
                    value  = new StringValue( attributeType );
                    ((StringValue)value).deserialize( in );
                }
                else
                {
                    value  = new BinaryValue( attributeType );
                    ((BinaryValue)value).deserialize( in );
                }
                
                try
                {
                    value.normalize();
                }
                catch ( LdapException ne )
                {
                    // Do nothing...
                }
                    
                values.add( value );
            }
        }
    }
    
    
    //-------------------------------------------------------------------------
    // Overloaded Object class methods
    //-------------------------------------------------------------------------
    /**
     * Clone an attribute. All the element are duplicated, so a modification on
     * the original object won't affect the cloned object, as a modification
     * on the cloned object has no impact on the original object
     * 
     * @return a clone of the current attribute
     */
    public EntryAttribute clone()
    {
        // clone the structure by cloner the inherited class
        EntryAttribute clone = (EntryAttribute)super.clone();
        
        // We are done !
        return clone;
    }


    /**
     * @see Object#equals(Object)
     * 
     * @return <code>true</code> if the two objects are equal
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }
        
        if ( ! (obj instanceof EntryAttribute ) )
        {
            return false;
        }
        
        EntryAttribute other = (EntryAttribute)obj;
        
        if ( !attributeType.equals( other.getAttributeType() ) )
        {
            return false;
        }
        
        if ( values.size() != other.size() )
        {
            return false;
        }
        
        for ( Value<?> val:values )
        {
            if ( ! other.contains( val ) )
            {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * The hashCode is based on the id, the isHR flag and 
     * on the internal values.
     *  
     * @see Object#hashCode()
     * 
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int h = super.hashCode();
        
        if ( attributeType != null )
        {
            h = h*17 + attributeType.hashCode();
        }
        
        return h;
    }
    
    
    /**
     * @see Object#toString()
     * 
     * @return A String representation of this instance
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        if ( ( values != null ) && ( values.size() != 0 ) )
        {
            for ( Value<?> value:values )
            {
                sb.append( "    " ).append( upId ).append( ": " );
                
                if ( value.isNull() )
                {
                    sb.append( "''" );
                }
                else
                {
                    sb.append( value );
                }
                
                sb.append( '\n' );
            }
        }
        else
        {
            sb.append( "    " ).append( upId ).append( ": (null)\n" );
        }
        
        return sb.toString();
    }
}

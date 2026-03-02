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
import java.util.Iterator;
import java.util.List;

import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;

/**
 * A generic interface mocking the Attribute JNDI interface. This interface
 * will be the base interface for the ServerAttribute and ClientAttribute.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface EntryAttribute extends Iterable<Value<?>>, Cloneable, Externalizable
{
    /**
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     * <p>
     * If the value's type is different from the attribute's type,
     * a conversion is done. For instance, if we try to set some String
     * into a Binary attribute, we just store the UTF-8 byte array 
     * encoding for this String.
     * </p>
     * <p>
     * If we try to store some byte[] in a HR attribute, we try to 
     * convert those byte[] assuming they represent an UTF-8 encoded
     * String. Of course, if it's not the case, the stored value will
     * be incorrect.
     * </p>
     * <p>
     * It's the responsibility of the caller to check if the stored
     * values are consistent with the attribute's type.
     * </p>
     * <p>
     * The caller can set the HR flag in order to enforce a type for 
     * the current attribute, otherwise this type will be set while
     * adding the first value, using the value's type to set the flag.
     * </p>
     *
     * @param vals some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    int add( String... vals );


    /**
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     * If the value's type is different from the attribute's type,
     * a conversion is done. For instance, if we try to set some String
     * into a Binary attribute, we just store the UTF-8 byte array 
     * encoding for this String.
     * If we try to store some byte[] in a HR attribute, we try to 
     * convert those byte[] assuming they represent an UTF-8 encoded
     * String. Of course, if it's not the case, the stored value will
     * be incorrect.
     * <br>
     * It's the responsibility of the caller to check if the stored
     * values are consistent with the attribute's type.
     * <br>
     * The caller can set the HR flag in order to enforce a type for 
     * the current attribute, otherwise this type will be set while
     * adding the first value, using the value's type to set the flag.
     *
     * @param vals some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    int add( byte[]... vals );


    /**
     * Adds some values to this attribute. If the new values are already present in
     * the attribute values, the method has no effect.
     * <p>
     * The new values are added at the end of list of values.
     * </p>
     * <p>
     * This method returns the number of values that were added.
     * </p>
     * <p>
     * If the value's type is different from the attribute's type,
     * a conversion is done. For instance, if we try to set some 
     * StringValue into a Binary attribute, we just store the UTF-8 
     * byte array encoding for this StringValue.
     * </p>
     * <p>
     * If we try to store some BinaryValue in a HR attribute, we try to 
     * convert those BinaryValue assuming they represent an UTF-8 encoded
     * String. Of course, if it's not the case, the stored value will
     * be incorrect.
     * </p>
     * <p>
     * It's the responsibility of the caller to check if the stored
     * values are consistent with the attribute's type.
     * </p>
     * <p>
     * The caller can set the HR flag in order to enforce a type for 
     * the current attribute, otherwise this type will be set while
     * adding the first value, using the value's type to set the flag.
     * </p>
     * <p>
     * <b>Note : </b>If the entry contains no value, and the unique added value
     * is a null length value, then this value will be considered as
     * a binary value.
     * </p>
     * @param vals some new values to be added which may be null
     * @return the number of added values, or 0 if none has been added
     */
    int add( Value<?>... val );
    
    
    /**
     * Remove all the values from this attribute.
     */
    void clear();
    
    
    /**
     * @return A clone of the current object
     */
    EntryAttribute clone();


    /**
     * <p>
     * Indicates whether the specified values are some of the attribute's values.
     * </p>
     * <p>
     * If the Attribute is not HR, the values will be converted to byte[]
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    boolean contains( String... vals );


    /**
     * <p>
     * Indicates whether the specified values are some of the attribute's values.
     * </p>
     * <p>
     * If the Attribute is HR, the values will be converted to String
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    boolean contains( byte[]... vals );


    /**
     * <p>
     * Indicates whether the specified values are some of the attribute's values.
     * </p>
     * <p>
     * If the Attribute is HR, the binary values will be converted to String before
     * being checked.
     * </p>
     *
     * @param vals the values
     * @return true if this attribute contains all the values, otherwise false
     */
    boolean contains( Value<?>... vals );


    /**
     * Get the attribute type associated with this ServerAttribute.
     *
     * @return the attributeType associated with this entry attribute
     */
    AttributeType getAttributeType();

    
    /**
     * <p>
     * Set the attribute type associated with this ServerAttribute.
     * </p>
     * <p>
     * The current attributeType will be replaced. It is the responsibility of
     * the caller to insure that the existing values are compatible with the new
     * AttributeType
     * </p>
     *
     * @param attributeType the attributeType associated with this entry attribute
     */
    void setAttributeType( AttributeType attributeType );

    
    /**
     * <p>
     * Check if the current attribute type is of the expected attributeType
     * </p>
     * <p>
     * This method won't tell if the current attribute is a descendant of 
     * the attributeType. For instance, the "CN" serverAttribute will return
     * false if we ask if it's an instance of "Name". 
     * </p> 
     *
     * @param attributeId The AttributeType ID to check
     * @return True if the current attribute is of the expected attributeType
     * @throws LdapInvalidAttributeValueException If there is no AttributeType
     */
    boolean instanceOf( String attributeId ) throws LdapInvalidAttributeValueException;

    
    /**
     * <p>
     * Get the first value of this attribute. If there is none, 
     * null is returned.
     * </p>
     * <p>
     * Note : even if we are storing values into a Set, one can assume
     * the values are ordered following the insertion order.
     * </p>
     * <p> 
     * This method is meant to be used if the attribute hold only one value.
     * </p>
     * 
     *  @return The first value for this attribute.
     */
    Value<?> get();


    /**
     * Returns an iterator over all the attribute's values.
     * <p>
     * The effect on the returned enumeration of adding or removing values of
     * the attribute is not specified.
     * </p>
     * <p>
     * This method will throw any <code>NamingException</code> that occurs.
     * </p>
     *
     * @return an enumeration of all values of the attribute
     */
    Iterator<Value<?>> getAll();


    /**
     * <p>
     * Get the nth value of this attribute. If there is none, 
     * null is returned.
     * </p>
     * <p>
     * Note : even if we are storing values into a Set, one can assume
     * the values are ordered following the insertion order.
     * </p>
     * <p> 
     * 
     * @param i the index  of the value to get
     *  @return The nth value for this attribute.
     */
    Value<?> get( int i );
    
    
    /**
     * <p>
     * Get the byte[] value, if and only if the value is known to be Binary,
     * otherwise a InvalidAttributeValueException will be thrown
     * </p>
     * <p>
     * Note that this method returns the first value only.
     * </p>
     *
     * @return The value as a byte[]
     * @throws LdapInvalidAttributeValueException If the value is a String
     */
    byte[] getBytes() throws LdapInvalidAttributeValueException;
    
    
    /**
     * Get's the attribute identifier for this entry.  This is the value
     * that will be used as the identifier for the attribute within the
     * entry.  
     *
     * @return the identifier for this attribute
     */
    String getId();

    
    /**
     * Get's the user provided identifier for this entry.  This is the value
     * that will be used as the identifier for the attribute within the
     * entry.  If this is a commonName attribute for example and the user
     * provides "COMMONname" instead when adding the entry then this is
     * the format the user will have that entry returned by the directory
     * server.  To do so we store this value as it was given and track it
     * in the attribute using this property.
     *
     * @return the user provided identifier for this attribute
     */
    String getUpId();
    
    
    /**
     * <p>
     * Tells if the attribute is Human Readable. 
     * </p>
     * <p>This flag is set by the caller, or implicitly when adding String 
     * values into an attribute which is not yet declared as Binary.
     * </p> 
     * @return
     */
    boolean isHR();

    
    /**
     * <p>
     * Get the String value, if and only if the value is known to be a String,
     * otherwise a InvalidAttributeValueException will be thrown
     * </p>
     * <p>
     * Note that this method returns the first value only.
     * </p>
     *
     * @return The value as a String
     * @throws LdapInvalidAttributeValueException If the value is a byte[]
     */
    String getString() throws LdapInvalidAttributeValueException;

    
    /**
     * Puts some values to this attribute.
     * <p>
     * The new values will replace the previous values.
     * </p>
     * <p>
     * This method returns the number of values that were put.
     * </p>
     *
     * @param val some values to be put which may be null
     * @return the number of added values, or 0 if none has been added
     */
    int put( String... vals );


    /**
     * Puts some values to this attribute.
     * <p>
     * The new values will replace the previous values.
     * </p>
     * <p>
     * This method returns the number of values that were put.
     * </p>
     *
     * @param val some values to be put which may be null
     * @return the number of added values, or 0 if none has been added
     */
    int put( byte[]... vals );

    
    /**
     * Puts some values to this attribute.
     * <p>
     * The new values are replace the previous values.
     * </p>
     * <p>
     * This method returns the number of values that were put.
     * </p>
     *
     * @param val some values to be put which may be null
     * @return the number of added values, or 0 if none has been added
     */
    int put( Value<?>... vals );


    /**
     * <p>
     * Puts a list of values into this attribute.
     * </p>
     * <p>
     * The new values will replace the previous values.
     * </p>
     * <p>
     * This method returns the number of values that were put.
     * </p>
     *
     * @param vals the values to be put
     * @return the number of added values, or 0 if none has been added
     */
    int put( List<Value<?>> vals );


    /**
     * <p>
     * Removes all the  values that are equal to the given values.
     * </p>
     * <p>
     * Returns true if all the values are removed.
     * </p>
     * <p>
     * If the attribute type is not HR, then the values will be first converted
     * to byte[]
     * </p>
     *
     * @param vals the values to be removed
     * @return true if all the values are removed, otherwise false
     */
    boolean remove( String... vals );
    
    
    /**
     * <p>
     * Removes all the  values that are equal to the given values.
     * </p>
     * <p>
     * Returns true if all the values are removed. 
     * </p>
     * <p>
     * If the attribute type is HR, then the values will be first converted
     * to String
     * </p>
     *
     * @param vals the values to be removed
     * @return true if all the values are removed, otherwise false
     */
    boolean remove( byte[]... val );


    /**
     * <p>
     * Removes all the  values that are equal to the given values.
     * </p>
     * <p>
     * Returns true if all the values are removed.
     * </p>
     * <p>
     * If the attribute type is HR and some value which are not String, we
     * will convert the values first (same thing for a non-HR attribute).
     * </p>
     *
     * @param vals the values to be removed
     * @return true if all the values are removed, otherwise false
     */
    boolean remove( Value<?>... vals );

    
    /**
     * <p>
     * Set the attribute to Human Readable or to Binary. 
     * </p>
     * @param isHR <code>true</code> for a Human Readable attribute, 
     * <code>false</code> for a Binary attribute.
     */
    void setHR( boolean isHR );

    
    /**
     * Set the normalized ID. The ID will be lowercased, and spaces
     * will be trimmed. 
     *
     * @param id The attribute ID
     * @throws IllegalArgumentException If the ID is empty or null or
     * resolve to an empty value after being trimmed
     */
    public void setId( String id );

    
    /**
     * Set the user provided ID. It will also set the ID, normalizing
     * the upId (removing spaces before and after, and lowercasing it)
     *
     * @param upId The attribute ID
     * @throws IllegalArgumentException If the ID is empty or null or
     * resolve to an empty value after being trimmed
     */
    public void setUpId( String upId );


    /**
     * <p>
     * Set the user provided ID. If we have none, the upId is assigned
     * the attributetype's name. If it does not have any name, we will
     * use the OID.
     * </p>
     * <p>
     * If we have an upId and an AttributeType, they must be compatible. :
     *  - if the upId is an OID, it must be the AttributeType's OID
     *  - otherwise, its normalized form must be equals to ones of
     *  the attributeType's names.
     * </p>
     * <p>
     * In any case, the ATtributeType will be changed. The caller is responsible for
     * the present values to be compatoble with the new AttributeType.
     * </p>
     * 
     * @param upId The attribute ID
     * @param attributeType The associated attributeType
     */
    void setUpId( String upId, AttributeType attributeType );

    
    /**
      * Retrieves the number of values in this attribute.
      *
      * @return the number of values in this attribute, including any values
      * wrapping a null value if there is one
      */
    int size();
    
    
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
     * @throws LdapException  if there is a failure to check syntaxes of values
     */
    boolean isValid() throws LdapException;


    /**
     * Checks to see if this attribute is valid along with the values it contains.
     *
     * @param checker The syntax checker
     * @return true if the attribute and it's values are valid, false otherwise
     * @throws LdapException if there is a failure to check syntaxes of values
     */
    boolean isValid( SyntaxChecker checker) throws LdapException;
    
    
    /**
     * Convert the ServerAttribute to a ClientAttribute
     *
     * @return An instance of ClientAttribute
     */
    EntryAttribute toClientAttribute();
}

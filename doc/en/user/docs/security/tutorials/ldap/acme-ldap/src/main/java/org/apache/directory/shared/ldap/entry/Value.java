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


import java.io.Externalizable;

import org.apache.directory.shared.ldap.exception.LdapException;

import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;


/**
 * A interface for wrapping attribute values stored into an EntryAttribute. These
 * values can be a String or a byte[].
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface Value<T> extends Cloneable, Externalizable, Comparable<Value<T>>
{
    
    Value<T> clone();
    
    
    /**
     * Check if the contained value is null or not
     * 
     * @return <code>true</code> if the inner value is null.
     */
    boolean isNull();
    
    
    /**
     * Get the wrapped value. It will return a copy, not a reference.
     *
     * @return a copy of the wrapped value
     */
    T get();
    
    
    /**
     * Get the wrapped value as a byte[]. If the original value
     * is binary, this method will return a copy of the wrapped byte[]
     *
     * @return the wrapped value as a byte[]
     */
    byte[] getBytes();
    
    
    /**
     * Get the wrapped value as a String. If the original value
     * is binary, this method will return the value as if it was 
     * an UTF-8 encoded String.
     *
     * @return the wrapped value as a String
     */
    String getString();
    
    
    /**
     * Get a reference on the stored value.
     *
     * @return a reference on the wrapped value.
     */
    T getReference();
    
    
    /**
     * Tells if the value has already be normalized or not.
     *
     * @return <code>true</code> if the value has already been normalized.
     */
    boolean isNormalized();
    
    
    /**
     * Tells if the value is valid. The value must have already been
     * validated at least once through a call to isValid( SyntaxChecker ).  
     * 
     * @return <code>true</code> if the value is valid
     */
    boolean isValid();

    
    /**
     * Tells if the value is valid wrt a Syntax checker
     * 
     * @param checker the SyntaxChecker to use to validate the value
     * @return <code>true</code> if the value is valid
     * @exception LdapException if the value cannot be validated
     */
    boolean isValid( SyntaxChecker checker ) throws LdapException;

    
    /**
     * Set the normalized flag.
     * 
     * @param normalized the value : true or false
     */
    void setNormalized( boolean normalized );

    
    /**
     * Gets the normalized (canonical) representation for the wrapped string.
     * If the wrapped String is null, null is returned, otherwise the normalized
     * form is returned.  If the normalizedValue is null, then this method
     * will attempt to generate it from the wrapped value: repeated calls to
     * this method do not unnecessarily normalize the wrapped value.  Only changes
     * to the wrapped value result in attempts to normalize the wrapped value.
     *
     * @return gets the normalized value
     */
    T getNormalizedValue();
    
    
    /**
     * Gets a reference to the the normalized (canonical) representation 
     * for the wrapped value.
     *
     * @return gets a reference to the normalized value
     */
    T getNormalizedValueReference();

    
    /**
     * Gets a copy of the the normalized (canonical) representation 
     * for the wrapped value.
     *
     * @return gets a copy of the normalized value
     */
    T getNormalizedValueCopy();

    
    /**
     * Normalize the value. In order to use this method, the Value
     * must be schema aware.
     * 
     * @exception LdapException if the value cannot be normalized
     */
    void normalize() throws LdapException;

    
    /**
     * Normalize the value. For a client String value, applies the given normalizer.
     * 
     * It supposes that the client has access to the schema in order to select the
     * appropriate normalizer.
     * 
     * @param normalizer the normalizer to apply to the value
     * @exception LdapException if the value cannot be normalized
     */
    void normalize( Normalizer normalizer ) throws LdapException;
    
    
    /**
     * Tells if the current value is Binary or String
     * 
     * @return <code>true</code> if the value is Binary, <code>false</code> otherwise
     */
    boolean isBinary();
    
    
    /**
     * @return The length of the interned value
     */
    int length();
}

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

import org.apache.directory.shared.ldap.exception.LdapException;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A server side schema aware wrapper around a String attribute value.
 * This value wrapper uses schema information to syntax check values,
 * and to compare them for equality and ordering.  It caches results
 * and invalidates them when the wrapped value changes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StringValue extends AbstractValue<String>
{
    /** Used for serialization */
    private static final long serialVersionUID = 2L;
    
    /** logger for reporting errors that might not be handled properly upstream */
    protected static final Logger LOG = LoggerFactory.getLogger( StringValue.class );


    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------
    /**
     * Creates a StringValue without an initial wrapped value.
     */
    public StringValue()
    {
        normalized = false;
        valid = null;
    }


    /**
     * Creates a StringValue without an initial wrapped value.
     *
     * @param attributeType the schema type associated with this StringValue
     */
    public StringValue( AttributeType attributeType )
    {
        if ( attributeType == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04442 ) );
        }

        if ( attributeType.getSyntax() == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04445 ) );
        }

        if ( ! attributeType.getSyntax().isHumanReadable() )
        {
            LOG.warn( "Treating a value of a binary attribute {} as a String: " +
                    "\nthis could cause data corruption!", attributeType.getName() );
        }

        this.attributeType = attributeType;
    }


    /**
     * Creates a StringValue with an initial wrapped String value.
     *
     * @param value the value to wrap which can be null
     */
    public StringValue( String value )
    {
        this.wrappedValue = value;
        normalized = false;
        valid = null;
    }


    /**
     * Creates a StringValue with an initial wrapped String value.
     *
     * @param attributeType the schema type associated with this StringValue
     * @param wrapped the value to wrap which can be null
     */
    public StringValue( AttributeType attributeType, String value )
    {
        this( attributeType );
        this.wrappedValue = value;
    }


    // -----------------------------------------------------------------------
    // Value<String> Methods
    // -----------------------------------------------------------------------
    /**
     * Get a copy of the stored value.
     *
     * @return A copy of the stored value.
     */
    public String get()
    {
        // The String is immutable, we can safely return the internal
        // object without copying it.
        return wrappedValue;
    }
    
    
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
    public String getNormalizedValue()
    {
        if ( isNull() )
        {
            normalized = true;
            return null;
        }

        if ( !normalized )
        {
            try
            {
                normalize();
            }
            catch ( LdapException ne )
            {
                String message = "Cannot normalize the value :" + ne.getLocalizedMessage();
                LOG.info( message );
                normalized = false;
            }
        }
        
        if ( normalizedValue == null )
        {
            return wrappedValue;
        }

        return normalizedValue;
    }
    
    
    /**
     * Gets a copy of the the normalized (canonical) representation 
     * for the wrapped value.
     *
     * @return gets a copy of the normalized value
     */
    public String getNormalizedValueCopy()
    {
        return getNormalizedValue();
    }


    /**
     * Compute the normalized (canonical) representation for the wrapped string.
     * If the wrapped String is null, the normalized form will be null too.  
     *
     * @throws LdapException if the value cannot be properly normalized
     */
    public void normalize() throws LdapException
    {
        // If the value is already normalized, get out.
        if ( normalized )
        {
            return;
        }
        
        if ( attributeType != null )
        {
            Normalizer normalizer = getNormalizer();
    
            if ( normalizer == null )
            {
                normalizedValue = wrappedValue;
            }
            else
            {
                normalizedValue = ( String ) normalizer.normalize( wrappedValue );
            }
    
            normalized = true;
        }
    }

    
    /**
     * Normalize the value. For a client String value, applies the given normalizer.
     * 
     * It supposes that the client has access to the schema in order to select the
     * appropriate normalizer.
     * 
     * @param Normalizer The normalizer to apply to the value
     * @exception LdapException If the value cannot be normalized
     */
    public final void normalize( Normalizer normalizer ) throws LdapException
    {
        if ( normalizer != null )
        {
            normalizedValue = (String)normalizer.normalize( wrappedValue );
            normalized = true;
        }
    }

    
    // -----------------------------------------------------------------------
    // Comparable<String> Methods
    // -----------------------------------------------------------------------
    /**
     * @see ServerValue#compareTo(ServerValue)
     * @throws IllegalStateException on failures to extract the comparator, or the
     * normalizers needed to perform the required comparisons based on the schema
     */
    public int compareTo( Value<String> value )
    {
        if ( isNull() )
        {
            if ( ( value == null ) || value.isNull() )
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
        else if ( ( value == null ) || value.isNull() )
        {
            return 1;
        }

        if ( !( value instanceof StringValue ) )
        {
            String message = I18n.err( I18n.ERR_04128, toString(), value.getClass() );
            LOG.error( message );
            throw new NotImplementedException( message );
        }
        
        StringValue stringValue = ( StringValue ) value;
        
        if ( attributeType != null )
        {
            if ( stringValue.getAttributeType() == null )
            {
                return getNormalizedValue().compareTo( stringValue.getNormalizedValue() );
            }
            else
            {
                if ( !attributeType.equals( stringValue.getAttributeType() ) )
                {
                    String message = I18n.err( I18n.ERR_04128, toString(), value.getClass() );
                    LOG.error( message );
                    throw new NotImplementedException( message );
                }
            }
        }
        else 
        {
            return getNormalizedValue().compareTo( stringValue.getNormalizedValue() );
        }
            
        try
        {
            return getLdapComparator().compare( getNormalizedValue(), stringValue.getNormalizedValue() );
        }
        catch ( LdapException e )
        {
            String msg = I18n.err( I18n.ERR_04443, this, value );
            LOG.error( msg, e );
            throw new IllegalStateException( msg, e );
        }
    }


    // -----------------------------------------------------------------------
    // Cloneable methods
    // -----------------------------------------------------------------------
    /**
     * Get a clone of the Client Value
     * 
     * @return a copy of the current value
     */
    public StringValue clone()
    {
        return (StringValue)super.clone();
    }


    // -----------------------------------------------------------------------
    // Object Methods
    // -----------------------------------------------------------------------
    /**
     * @see Object#hashCode()
     * @return the instance's hashcode 
     */
    public int hashCode()
    {
        // return zero if the value is null so only one null value can be
        // stored in an attribute - the binary version does the same 
        if ( isNull() )
        {
            if ( attributeType != null )
            {
                // return the OID hashcode if the value is null. 
                return attributeType.getOid().hashCode();
            }
            
            return 0;
        }

        // If the normalized value is null, will default to wrapped
        // which cannot be null at this point.
        // If the normalized value is null, will default to wrapped
        // which cannot be null at this point.
        int h = 0;

        String normalized = getNormalizedValue();
        
        if ( normalized != null )
        {
            h = normalized.hashCode();
        }
        else
        {
            h = 17;
        }
        
        // Add the OID hashcode if we have an AttributeType
        if ( attributeType != null )
        {
            h = h*37 + attributeType.getOid().hashCode();
        }
        
        return h;
    }


    /**
     * @see Object#equals(Object)
     * 
     * Two StringValue are equals if their normalized values are equal
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( ! ( obj instanceof StringValue ) )
        {
            return false;
        }

        StringValue other = ( StringValue ) obj;
        
        if ( this.isNull() )
        {
            return other.isNull();
        }
       
        // If we have an attributeType, it must be equal
        // We should also use the comparator if we have an AT
        if ( attributeType != null )
        {
            if ( other.attributeType != null )
            {
                if ( !attributeType.equals( other.attributeType ) )
                {
                    return false;
                }
            }
            else
            {
                return this.getNormalizedValue().equals( other.getNormalizedValue() );
            }
        }
        else if ( other.attributeType != null )
        {
            return this.getNormalizedValue().equals( other.getNormalizedValue() );
        }

        // Shortcut : compare the values without normalization
        // If they are equal, we may avoid a normalization.
        // Note : if two values are equal, then their normalized
        // value are equal too if their attributeType are equal. 
        if ( getReference().equals( other.getReference() ) )
        {
            return true;
        }

        if ( attributeType != null )
        {
            try
            {
                LdapComparator<String> comparator = getLdapComparator();

                // Compare normalized values
                if ( comparator == null )
                {
                    return getNormalizedValue().equals( other.getNormalizedValue() );
                }
                else
                {
                    if ( isNormalized() )
                    {
                        return comparator.compare( getNormalizedValue(), other.getNormalizedValue() ) == 0;
                    }
                    else
                    {
                        Normalizer normalizer = attributeType.getEquality().getNormalizer();
                        return comparator.compare( normalizer.normalize( get() ), normalizer.normalize( other.get() ) ) == 0;
                    }
                }
            }
            catch ( LdapException ne )
            {
                return false;
            }
        }
        else
        {
            return this.getNormalizedValue().equals( other.getNormalizedValue() );
        }
    }
    
    
    /**
     * Tells if the current value is Binary or String
     * 
     * @return <code>true</code> if the value is Binary, <code>false</code> otherwise
     */
    public boolean isBinary()
    {
        return false;
    }

    
    /**
     * @return The length of the interned value
     */
    public int length()
    {
        return wrappedValue != null ? wrappedValue.length() : 0;
    }
    
    
    /**
     * Get the wrapped value as a byte[].
     * @return the wrapped value as a byte[]
     */
    public byte[] getBytes()
    {
        return StringTools.getBytesUtf8( wrappedValue );
    }
    
    
    /**
     * Get the wrapped value as a String.
     *
     * @return the wrapped value as a String
     */
    public String getString()
    {
        return wrappedValue != null ? wrappedValue : "";
    }
    
    
    /**
     * @see Externalizable#readExternal(ObjectInput)
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // Read the wrapped value, if it's not null
        if ( in.readBoolean() )
        {
            wrappedValue = in.readUTF();
        }
        
        // Read the isNormalized flag
        normalized = in.readBoolean();
        
        if ( normalized )
        {
            // Read the normalized value, if not null
            if ( in.readBoolean() )
            {
                normalizedValue = in.readUTF();
            }
        }
    }

    
    /**
     * @see Externalizable#writeExternal(ObjectOutput)
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        // Write the wrapped value, if it's not null
        if ( wrappedValue != null )
        {
            out.writeBoolean( true );
            out.writeUTF( wrappedValue );
        }
        else
        {
            out.writeBoolean( false );
        }
        
        // Write the isNormalized flag
        if ( normalized )
        {
            out.writeBoolean( true );
            
            // Write the normalized value, if not null
            if ( normalizedValue != null )
            {
                out.writeBoolean( true );
                out.writeUTF( normalizedValue );
            }
            else
            {
                out.writeBoolean( false );
            }
        }
        else
        {
            out.writeBoolean( false );
        }
        
        // and flush the data
        out.flush();
    }

    
    /**
     * We will write the value and the normalized value, only
     * if the normalized value is different.
     * 
     * If the value is empty, a flag is written at the beginning with 
     * the value true, otherwise, a false is written.
     * 
     * The data will be stored following this structure :
     *  [empty value flag]
     *  [UP value]
     *  [normalized] (will be false if the value can't be normalized)
     *  [same] (a flag set to true if the normalized value equals the UP value)
     *  [Norm value] (the normalized value if different from the UP value)
     *  
     *  @param out the buffer in which we will stored the serialized form of the value
     *  @throws IOException if we can't write into the buffer
     */
    public void serialize( ObjectOutput out ) throws IOException
    {
        if ( wrappedValue != null )
        {
            // write a flag indicating that the value is not null
            out.writeBoolean( true );
            
            // Write the data
            out.writeUTF( wrappedValue );
            
            // Normalize the data
            try
            {
                normalize();
                out.writeBoolean( true );
                
                if ( wrappedValue.equals( normalizedValue ) )
                {
                    out.writeBoolean( true );
                }
                else
                {
                    out.writeBoolean( false );
                    out.writeUTF( normalizedValue );
                }
            }
            catch ( LdapException ne )
            {
                // The value can't be normalized, we don't write the 
                // normalized value.
                normalizedValue = null;
                out.writeBoolean( false );
            }
        }
        else
        {
            // Write a flag indicating that the value is null
            out.writeBoolean( false );
        }
        
        out.flush();
    }

    
    /**
     * Deserialize a StringValue. 
     *
     * @param in the buffer containing the bytes with the serialized value
     * @throws IOException 
     * @throws ClassNotFoundException
     */
    public void deserialize( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        // If the value is null, the flag will be set to false
        if ( !in.readBoolean() )
        {
            wrappedValue = null;
            normalizedValue = null;
            return;
        }
        
        // Read the value
        String wrapped = in.readUTF();
        
        wrappedValue = wrapped;
        
        // Read the normalized flag
        normalized = in.readBoolean();
        
        if ( normalized )
        {
            normalized = true;

            // Read the 'same' flag
            if ( in.readBoolean() )
            {
                normalizedValue = wrapped;
            }
            else
            {
                // The normalized value is different. Read it
                normalizedValue = in.readUTF();
            }
        }
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return wrappedValue == null ? "null": wrappedValue;
    }
}

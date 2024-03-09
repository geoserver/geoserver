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
package org.apache.directory.shared.ldap.name;


import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Attribute Type And Value, which is the basis of all RDN. It contains a
 * type, and a value. The type must not be case sensitive. Superfluous leading
 * and trailing spaces MUST have been trimmed before. The value MUST be in UTF8
 * format, according to RFC 2253. If the type is in OID form, then the value
 * must be a hexadecimal string prefixed by a '#' character. Otherwise, the
 * string must respect the RC 2253 grammar. No further normalization will be
 * done, because we don't have any knowledge of the Schema definition in the
 * parser.
 *
 * We will also keep a User Provided form of the atav (Attribute Type And Value),
 * called upName.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 928945 $, $Date: 2010-03-30 02:59:49 +0300 (Tue, 30 Mar 2010) $
 */
public class AVA implements Cloneable, Comparable, Externalizable
{
    /**
     * Declares the Serial Version Uid.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;

    /** The LoggerFactory used by this class */
    private static Logger LOG = LoggerFactory.getLogger( AVA.class );

    /** The normalized Name type */
    private String normType;

    /** The user provided Name type */
    private String upType;

    /** The name value. It can be a String or a byte array */
    private Value<?> normValue;

    /** The name user provided value. It can be a String or a byte array */
    private Value<?> upValue;

    /** The user provided AVA */
    private String upName;

    /** The starting position of this atav in the given string from which
     * we have extracted the upName */
    private int start;

    /** The length of this atav upName */
    private int length;

    /** Two values used for comparizon */
    private static final boolean CASE_SENSITIVE = true;

    private static final boolean CASE_INSENSITIVE = false;


    /**
     * Construct an empty AVA
     */
    public AVA()
    {
        normType = null;
        upType = null;
        normValue = null;
        upValue = null;
        upName = "";
        start = -1;
        length = 0;
    }

    
    /**
     * Construct an AVA. The type and value are normalized :
     * <li> the type is trimmed and lowercased </li>
     * <li> the value is trimmed </li>
     * <p>
     * Note that the upValue should <b>not</b> be null or empty, or resolved
     * to an empty string after having trimmed it. 
     *
     * @param upType The Usrr Provided type
     * @param normType The normalized type
     * @param upValue The User Provided value
     * @param normValue The normalized value
     */
    public AVA( String upType, String normType, String upValue, String normValue ) throws LdapInvalidDnException
    {
        this( upType, normType, new StringValue( upValue ), new StringValue( normValue ) );
    }



    
    /**
     * Construct an AVA. The type and value are normalized :
     * <li> the type is trimmed and lowercased </li>
     * <li> the value is trimmed </li>
     * <p>
     * Note that the upValue should <b>not</b> be null or empty, or resolved
     * to an empty string after having trimmed it. 
     *
     * @param upType The Usrr Provided type
     * @param normType The normalized type
     * @param upValue The User Provided value
     * @param normValue The normalized value
     */
    public AVA( String upType, String normType, byte[] upValue, byte[] normValue ) throws LdapInvalidDnException
    {
        this( upType, normType, new BinaryValue( upValue ), new BinaryValue( normValue ) );
    }


    /**
     * Construct an AVA. The type and value are normalized :
     * <li> the type is trimmed and lowercased </li>
     * <li> the value is trimmed </li>
     * <p>
     * Note that the upValue should <b>not</b> be null or empty, or resolved
     * to an empty string after having trimmed it. 
     *
     * @param upType The Usrr Provided type
     * @param normType The normalized type
     * @param upValue The User Provided value
     * @param normValue The normalized value
     */
    public AVA( String upType, String normType, Value<?> upValue, Value<?> normValue ) throws LdapInvalidDnException
    {
        String upTypeTrimmed = StringTools.trim( upType );
        String normTypeTrimmed = StringTools.trim( normType );
        
        if ( StringTools.isEmpty( upTypeTrimmed ) )
        {
            if ( StringTools.isEmpty( normTypeTrimmed ) )
            {
                String message =  I18n.err( I18n.ERR_04188 );
                LOG.error( message );
                throw new LdapInvalidDnException( ResultCodeEnum.INVALID_DN_SYNTAX, message );
            }
            else
            {
                // In this case, we will use the normType instead
                this.normType = StringTools.lowerCaseAscii( normTypeTrimmed );
                this.upType = normType;
            }
        }
        else if ( StringTools.isEmpty( normTypeTrimmed ) )
        {
            // In this case, we will use the upType instead
            this.normType = StringTools.lowerCaseAscii( upTypeTrimmed );
            this.upType = upType;
        }
        else
        {
            this.normType = StringTools.lowerCaseAscii( normTypeTrimmed );
            this.upType = upType;
            
        }
            
        this.normValue = normValue;
        this.upValue = upValue;
        
        upName = this.upType + '=' + ( this.upValue == null ? "" : this.upValue.getString() );
        start = 0;
        length = upName.length();
    }


    /**
     * Construct an AVA. The type and value are normalized :
     * <li> the type is trimmed and lowercased </li>
     * <li> the value is trimmed </li>
     * <p>
     * Note that the upValue should <b>not</b> be null or empty, or resolved
     * to an empty string after having trimmed it. 
     *
     * @param upType The User Provided type
     * @param normType The normalized type
     * @param upValue The User Provided value
     * @param normValue The normalized value
     * @param upName The User Provided name (may be escaped)
     */
    public AVA( String upType, String normType, Value<?> upValue, Value<?> normValue, String upName )
        throws LdapInvalidDnException
    {
        String upTypeTrimmed = StringTools.trim( upType );
        String normTypeTrimmed = StringTools.trim( normType );

        if ( StringTools.isEmpty( upTypeTrimmed ) )
        {
            if ( StringTools.isEmpty( normTypeTrimmed ) )
            {
                String message = I18n.err( I18n.ERR_04188 );
                LOG.error( message );
                throw new LdapInvalidDnException( ResultCodeEnum.INVALID_DN_SYNTAX, message );
            }
            else
            {
                // In this case, we will use the normType instead
                this.normType = StringTools.lowerCaseAscii( normTypeTrimmed );
                this.upType = normType;
            }
        }
        else if ( StringTools.isEmpty( normTypeTrimmed ) )
        {
            // In this case, we will use the upType instead
            this.normType = StringTools.lowerCaseAscii( upTypeTrimmed );
            this.upType = upType;
        }
        else
        {
            this.normType = StringTools.lowerCaseAscii( normTypeTrimmed );
            this.upType = upType;

        }

        this.normValue = normValue;
        this.upValue = upValue;

        this.upName = upName;
        start = 0;
        length = upName.length();
    }


    /**
     * Get the normalized type of a AVA
     *
     * @return The normalized type
     */
    public String getNormType()
    {
        return normType;
    }

    /**
     * Get the user provided type of a AVA
     *
     * @return The user provided type
     */
    public String getUpType()
    {
        return upType;
    }


    /**
     * Store a new type
     *
     * @param upType The AVA User Provided type
     * @param type The AVA type
     * 
     * @throws LdapInvalidDnException if the type or upType are empty or null.
     * If the upName is invalid.
     */
    public void setType( String upType, String type ) throws LdapInvalidDnException
    {
        if ( StringTools.isEmpty( type ) || StringTools.isEmpty( type.trim() ) )
        {
            String message = I18n.err( I18n.ERR_04188 );
            LOG.error( message );
            throw new LdapInvalidDnException( ResultCodeEnum.INVALID_DN_SYNTAX, message );
        }
        
        if ( StringTools.isEmpty( upType ) || StringTools.isEmpty( upType.trim() ) )
        {
            String message = I18n.err( I18n.ERR_04189 );
            LOG.error( message );
            throw new LdapInvalidDnException( ResultCodeEnum.INVALID_DN_SYNTAX, message );
        }
        
        int equalPosition = upName.indexOf( '=' );
        
        if ( equalPosition <= 1 )
        {
            String message = I18n.err( I18n.ERR_04190 ); 
            LOG.error( message );
            throw new LdapInvalidDnException( ResultCodeEnum.INVALID_DN_SYNTAX, message );
        }

        normType = type.trim().toLowerCase();
        this.upType = upType;
        upName = upType + upName.substring( equalPosition );
        start = -1;
        length = upName.length();
    }


    /**
     * Store the type, after having trimmed and lowercased it.
     *
     * @param type The AVA type
     */
    public void setTypeNormalized( String type ) throws LdapInvalidDnException
    {
        if ( StringTools.isEmpty( type ) || StringTools.isEmpty( type.trim() ) )
        {
            LOG.error( I18n.err( I18n.ERR_04191 ) );
            throw new LdapInvalidDnException( ResultCodeEnum.INVALID_DN_SYNTAX, I18n.err( I18n.ERR_04191 ) );
        }

        normType = type.trim().toLowerCase();
        upType = type;
        upName = type + upName.substring( upName.indexOf( '=' ) );
        start = -1;
        length = upName.length();
    }


    /**
     * Get the Value of a AVA
     *
     * @return The value
     */
    public Value<?> getNormValue()
    {
        return normValue;
    }

    /**
     * Get the User Provided Value of a AVA
     *
     * @return The value
     */
    public Value<?> getUpValue()
    {
        return upValue;
    }

    /**
     * Get the normalized Name of a AVA
     *
     * @return The name
     */
    public String getNormName()
    {
        return normalize();
    }


    /**
     * Store the value of a AVA.
     *
     * @param value The user provided value of the AVA
     * @param normValue The normalized value
     */
    public void setValue( Value<?> upValue, Value<?> normValue )
    {
        this.normValue = normValue;
        this.upValue = upValue;
        upName = upName.substring( 0, upName.indexOf( '=' ) + 1 ) + upValue;
        start = -1;
        length = upName.length();
    }


    /**
     * Get the upName length
     *
     * @return the upName length
     */
    public int getLength()
    {
        return length;
    }


    /**
     * get the position in the original upName where this atav starts.
     *
     * @return The starting position of this atav
     */
    public int getStart()
    {
        return start;
    }


    /**
     * Get the user provided form of this attribute type and value
     *
     * @return The user provided form of this atav
     */
    public String getUpName()
    {
        return upName;
    }


    /**
     * Store the value of a AVA, after having trimmed it.
     *
     * @param value The value of the AVA
     */
    public void setValueNormalized( String value )
    {
        String newValue = StringTools.trim( value );

        if ( StringTools.isEmpty( newValue ) )
        {
            this.normValue = new StringValue( "" );
        }
        else
        {
            this.normValue = new StringValue( newValue );
        }

        upName = upName.substring( 0, upName.indexOf( '=' ) + 1 ) + value;
        start = -1;
        length = upName.length();
    }


    /**
     * Implements the cloning.
     *
     * @return a clone of this object
     */
    public Object clone()
    {
        try
        {
            return super.clone();
        }
        catch ( CloneNotSupportedException cnse )
        {
            throw new Error( "Assertion failure" );
        }
    }


    /**
     * Compares two NameComponents. They are equals if : 
     * - types are equals, case insensitive, 
     * - values are equals, case sensitive
     *
     * @param object
     * @return 0 if both NC are equals, otherwise a positive value if the
     *         original NC is superior to the second one, a negative value if
     *         the second NC is superior.
     */
    public int compareTo( Object object )
    {
        if ( object instanceof AVA )
        {
            AVA nc = ( AVA ) object;

            int res = compareType( normType, nc.normType );

            if ( res != 0 )
            {
                return res;
            }
            else
            {
                return compareValue( normValue, nc.normValue, CASE_SENSITIVE );
            }
        }
        else
        {
            return 1;
        }
    }


    /**
     * Compares two NameComponents. They are equals if : 
     * - types are equals, case insensitive, 
     * - values are equals, case insensitive
     *
     * @param object
     * @return 0 if both NC are equals, otherwise a positive value if the
     *         original NC is superior to the second one, a negative value if
     *         the second NC is superior.
     */
    public int compareToIgnoreCase( Object object )
    {
        if ( object instanceof AVA )
        {
            AVA nc = ( AVA ) object;

            int res = compareType( normType, nc.normType );

            if ( res != 0 )
            {
                return res;
            }
            else
            {
                return compareValue( normValue, nc.normValue, CASE_INSENSITIVE );
            }
        }
        else
        {
            return 1;
        }
    }


    /**
     * Compare two types, trimed and case insensitive
     *
     * @param val1 First String
     * @param val2 Second String
     * @return true if both strings are equals or null.
     */
    private int compareType( String val1, String val2 )
    {
        if ( StringTools.isEmpty( val1 ) )
        {
            return StringTools.isEmpty( val2 ) ? 0 : -1;
        }
        else if ( StringTools.isEmpty( val2 ) )
        {
            return 1;
        }
        else
        {
            return ( StringTools.trim( val1 ) ).compareToIgnoreCase( StringTools.trim( val2 ) );
        }
    }


    /**
     * Compare two values
     *
     * @param val1 First value
     * @param val2 Second value
     * @param sensitivity A flag to define the case sensitivity
     * @return -1 if the first value is inferior to the second one, +1 if
     * its superior, 0 if both values are equal
     */
    private int compareValue( Value<?> val1, Value<?> val2, boolean sensitivity )
    {
        if ( !val1.isBinary() )
        {
            if ( !val2.isBinary() )
            {
                int val = ( sensitivity == CASE_SENSITIVE ) ? 
                    ( val1.getString() ).compareTo( val2.getString() )
                    : ( val1.getString() ).compareToIgnoreCase( val2.getString() );

                return ( val < 0 ? -1 : ( val > 0 ? 1 : val ) );
            }
            else
            {
                return 1;
            }
        }
        else
        {
            if ( val2.isBinary() )
            {
                if ( Arrays.equals( val1.getBytes(), val2.getBytes() ) )
                {
                    return 0;
                }
                else
                {
                    return 1;
                }
            }
            else
            {
                return 1;
            }
        }
    }

    private static final boolean[] DN_ESCAPED_CHARS = new boolean[]
        {
        true,  true,  true,  true,  true,  true,  true,  true,  // 0x00 -> 0x07
        true,  true,  true,  true,  true,  true,  true,  true,  // 0x08 -> 0x0F
        true,  true,  true,  true,  true,  true,  true,  true,  // 0x10 -> 0x17
        true,  true,  true,  true,  true,  true,  true,  true,  // 0x18 -> 0x1F
        true,  false, true,  true,  false, false, false, false, // 0x20 -> 0x27 ' ', '"', '#'
        false, false, false, true,  true,  false, false, false, // 0x28 -> 0x2F '+', ','
        false, false, false, false, false, false, false, false, // 0x30 -> 0x37 
        false, false, false, true,  true,  false, true,  false, // 0x38 -> 0x3F ';', '<', '>'
        false, false, false, false, false, false, false, false, // 0x40 -> 0x47
        false, false, false, false, false, false, false, false, // 0x48 -> 0x4F
        false, false, false, false, false, false, false, false, // 0x50 -> 0x57
        false, false, false, false, true,  false, false, false, // 0x58 -> 0x5F
        false, false, false, false, false, false, false, false, // 0x60 -> 0x67
        false, false, false, false, false, false, false, false, // 0x68 -> 0x6F
        false, false, false, false, false, false, false, false, // 0x70 -> 0x77
        false, false, false, false, false, false, false, false, // 0x78 -> 0x7F
        };
    
    
    public String normalizeValue()
    {
        // The result will be gathered in a stringBuilder
        StringBuilder sb = new StringBuilder();
        
        String normalizedValue =  normValue.getString();
        int valueLength = normalizedValue.length();

        if ( normalizedValue.length() > 0 )
        {
            char[] chars = normalizedValue.toCharArray();

            // Here, we have a char to escape. Start again the loop...
            for ( int i = 0; i < valueLength; i++ )
            {
                char c = chars[i];

                if ( ( c >= 0 ) && ( c < DN_ESCAPED_CHARS.length ) && DN_ESCAPED_CHARS[ c ] ) 
                {
                    // Some chars need to be escaped even if they are US ASCII
                    // Just prefix them with a '\'
                    // Special cases are ' ' (space), '#') which need a special
                    // treatment.
                    switch ( c )
                    {
                        case ' ' :
                            if ( ( i == 0 ) || ( i == valueLength - 1 ) )
                            {
                                sb.append( "\\ " );
                            }
                            else
                            {
                                sb.append( ' ' );
                            }
    
                            break;
                            
                        case '#' :
                            if ( i == 0 )
                            {
                                sb.append( "\\#" );
                                continue;
                            }
                            else
                            {
                                sb.append( '#' );
                            }
                        
                            break;

                        default :
                            sb.append( '\\' ).append( c );
                    }
                }
                else
                {
                    // Standard ASCII chars are just appended
                    sb.append( c );
                }
            }
        }
        
        return sb.toString();
    }

    /**
     * A Normalized String representation of a AVA : 
     * - type is trimed and lowercased 
     * - value is trimed and lowercased, and special characters
     * are escaped if needed.
     *
     * @return A normalized string representing a AVA
     */
    public String normalize()
    {
        if ( !normValue.isBinary() )
        {
            // The result will be gathered in a stringBuilder
            StringBuilder sb = new StringBuilder();
            
            // First, store the type and the '=' char
            sb.append( normType ).append( '=' );
            
            String normalizedValue = normValue.getString();
            
            if ( normalizedValue.length() > 0 )
            {
                sb.append( normalizeValue() );
            }
            
            return sb.toString();
        }
        else
        {
            return normType + "=#"
                + StringTools.dumpHexPairs( normValue .getBytes() );
        }
    }


    /**
     * Gets the hashcode of this object.
     *
     * @see java.lang.Object#hashCode()
     * @return The instance hash code
     */
    public int hashCode()
    {
        int result = 37;

        result = result*17 + ( normType != null ? normType.hashCode() : 0 );
        result = result*17 + ( normValue != null ? normValue.hashCode() : 0 );

        return result;
    }

    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        
        if ( !( obj instanceof AVA ) )
        {
            return false;
        }
        
        AVA instance = (AVA)obj;
     
        // Compare the type
        if ( normType == null )
        {
            if ( instance.normType != null )
            {
                return false;
            }
        }
        else 
        {
            if ( !normType.equals( instance.normType ) )
            {
                return false;
            }
        }
            
        // Compare the values
        if ( normValue.isNull() )
        {
            return instance.normValue.isNull();
        }
        else
        {
            return normValue.equals( instance.normValue );
        }
    }

    
    /**
     * @see Externalizable#readExternal(ObjectInput)<p>
     * 
     * An AVA is composed of  a type and a value.
     * The data are stored following the structure :
     * 
     * <li>upName</li> The User provided ATAV
     * <li>start</li> The position of this ATAV in the DN
     * <li>length</li> The ATAV length
     * <li>upType</li> The user Provided Type
     * <li>normType</li> The normalized AttributeType
     * <li>isHR<li> Tells if the value is a String or not
     * <p>
     * if the value is a String :
     * <li>upValue</li> The User Provided value.
     * <li>value</li> The normalized value.
     * <p>
     * if the value is binary :
     * <li>upValueLength</li>
     * <li>upValue</li> The User Provided value.
     * <li>valueLength</li>
     * <li>value</li> The normalized value.
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        if ( StringTools.isEmpty( upName ) || 
             StringTools.isEmpty( upType ) ||
             StringTools.isEmpty( normType ) ||
             ( start < 0 ) ||
             ( length < 2 ) ||             // At least a type and '='
             ( upValue.isNull() ) ||
             ( normValue.isNull() ) )
        {
            String message = "Cannot serialize an wrong ATAV, ";
            
            if ( StringTools.isEmpty( upName ) )
            {
                message += "the upName should not be null or empty";
            }
            else if ( StringTools.isEmpty( upType ) )
            {
                message += "the upType should not be null or empty";
            }
            else if ( StringTools.isEmpty( normType ) )
            {
                message += "the normType should not be null or empty";
            }
            else if ( start < 0 )
            {
                message += "the start should not be < 0";
            }
            else if ( length < 2 )
            {
                message += "the length should not be < 2";
            }
            else if ( upValue.isNull() )
            {
                message += "the upValue should not be null";
            }
            else if ( normValue.isNull() )
            {
                message += "the value should not be null";
            }
                
            LOG.error( message );
            throw new IOException( message );
        }
        
        out.writeUTF( upName );
        out.writeInt( start );
        out.writeInt( length );
        out.writeUTF( upType );
        out.writeUTF( normType );
        
        boolean isHR = !normValue.isBinary();
        
        out.writeBoolean( isHR );
        
        if ( isHR )
        {
            out.writeUTF( upValue.getString() );
            out.writeUTF( normValue.getString() );
        }
        else
        {
            out.writeInt( upValue.length() );
            out.write( upValue.getBytes() );
            out.writeInt( normValue.length() );
            out.write( normValue.getBytes() );
        }
    }
    
    
    /**
     * @see Externalizable#readExternal(ObjectInput)
     * 
     * We read back the data to create a new ATAV. The structure 
     * read is exposed in the {@link AVA#writeExternal(ObjectOutput)} 
     * method<p>
     */
    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
    {
        upName = in.readUTF();
        start = in.readInt();
        length = in.readInt();
        upType = in.readUTF();
        normType = in.readUTF();
        
        boolean isHR = in.readBoolean();
        
        if ( isHR )
        {
            upValue = new StringValue( in.readUTF() );
            normValue = new StringValue( in.readUTF() );
        }
        else
        {
            int upValueLength = in.readInt();
            byte[] upValueBytes = new byte[upValueLength];
            in.readFully( upValueBytes );
            upValue = new BinaryValue( upValueBytes );

            int valueLength = in.readInt();
            byte[] normValueBytes = new byte[valueLength];
            in.readFully( normValueBytes );
            normValue = new BinaryValue( normValueBytes );
        }
    }
    
    
    /**
     * A String representation of a AVA.
     *
     * @return A string representing a AVA
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        if ( StringTools.isEmpty( normType ) || StringTools.isEmpty( normType.trim() ) )
        {
            return "";
        }

        sb.append( normType ).append( "=" );

        if ( normValue != null )
        {
            sb.append( normValue.getString() );
        }

        return sb.toString();
    }
}

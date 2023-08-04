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
package org.apache.directory.shared.ldap.util;


import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.InvalidAttributeIdentifierException;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeTypeException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;


/**
 * A set of utility fuctions for working with Attributes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923893 $
 */
public class AttributeUtils
{
    /**
     * Correctly removes an attribute from an entry using it's attributeType information.
     * 
     * @param type the attributeType of the attribute to remove
     * @param entry the entry to remove the attribute from 
     * @return the Attribute that is removed
     */
    public static Attribute removeAttribute( AttributeType type, Attributes entry )
    {
        Attribute attr = entry.get( type.getOid() );

        if ( attr == null )
        {
            List<String> aliases = type.getNames();

            for ( String alias : aliases )
            {
                attr = entry.get( alias );

                if ( attr != null )
                {
                    return entry.remove( attr.getID() );
                }
            }
        }

        if ( attr == null )
        {
            return null;
        }

        return entry.remove( attr.getID() );
    }


    /**
     * Compare two values and return true if they are equal.
     * 
     * @param value1 The first value
     * @param value2 The second value
     * @return true if both value are null or if they are equal.
     */
    public static final boolean equals( Object value1, Object value2 )
    {
        if ( value1 == value2 )
        {
            return true;
        }

        if ( value1 == null )
        {
            return ( value2 == null );
        }

        if ( value1 instanceof byte[] )
        {
            if ( value2 instanceof byte[] )
            {
                return Arrays.equals( ( byte[] ) value1, ( byte[] ) value2 );
            }
            else
            {
                return false;
            }
        }
        else
        {
            return value1.equals( value2 );
        }
    }


    /**
     * Clone the value. An attribute value is supposed to be either a String
     * or a byte array. If it's a String, then we just return it ( as String
     * is immutable, we don't need to copy it). If it's a bu=yte array, we
     * create a new byte array and copy the bytes into it.
     * 
     * @param value The value to clone
     * @return The cloned value
     */
    public static Object cloneValue( Object value )
    {
        // First copy the value
        Object newValue = null;

        if ( value instanceof byte[] )
        {
            newValue = ( ( byte[] ) value ).clone();
        }
        else
        {
            newValue = value;
        }

        return newValue;
    }


    /**
     * Switch from a BasicAttribute to a AttributeImpl. This is
     * necessary to allow cloning to be correctly handled.
     * 
     * @param attribute The attribute to transform
     * @return A instance of AttributeImpl
     */
    public static final Attribute toBasicAttribute( Attribute attribute )
    {
        if ( attribute instanceof BasicAttribute )
        {
            // Just return the attribute
            return attribute;
        }
        else
        {
            // Create a new AttributeImpl from the original attribute
            Attribute newAttribute = new BasicAttribute( attribute.getID() );

            try
            {
                NamingEnumeration<?> values = attribute.getAll();

                while ( values.hasMoreElements() )
                {
                    newAttribute.add( cloneValue( values.next() ) );
                }

                return newAttribute;
            }
            catch ( NamingException ne )
            {
                return newAttribute;
            }
        }
    }


    /**
     * Utility method to extract an attribute from Attributes object using
     * all combinationos of the name including aliases.
     * 
     * @param attrs the Attributes to get the Attribute object from
     * @param type the attribute type specification
     * @return an Attribute with matching the attributeType spec or null
     */
    public static final Attribute getAttribute( Attributes attrs, AttributeType type )
    {
        // check if the attribute's OID is used
        Attribute attr = attrs.get( type.getOid() );

        if ( attr != null )
        {
            return attr;
        }

        // optimization bypass to avoid cost of the loop below
        if ( type.getNames().size() == 1 )
        {
            attr = attrs.get( type.getNames().get( 0 ) );

            if ( attr != null )
            {
                return attr;
            }
        }

        // iterate through aliases
        for ( String alias : type.getNames() )
        {
            attr = attrs.get( alias );

            if ( attr != null )
            {
                return attr;
            }
        }

        return null;
    }


    /**
     * Check if an attribute contains a specific value, using the associated matchingRule for that
     *
     * @param attr The attribute we are searching in
     * @param compared The object we are looking for
     * @param type The attribute type
     * @return <code>true</code> if the value exists in the attribute</code>
     * @throws LdapException If something went wrong while accessing the data
     */
    public static boolean containsValue( Attribute attr, Value<?> compared, AttributeType type ) throws LdapException
    {
        // quick bypass test
        if ( attr.contains( compared ) )
        {
            return true;
        }

        MatchingRule matchingRule = type.getEquality();

        Normalizer normalizer = null;

        if ( matchingRule != null )
        {
            normalizer = matchingRule.getNormalizer();
        }
        else
        {
            normalizer = new NoOpNormalizer( type.getOid() );
        }

        if ( type.getSyntax().isHumanReadable() )
        {
            try
            {
                String comparedStr = normalizer.normalize( compared.getString() );
                
                for ( NamingEnumeration<?> values = attr.getAll(); values.hasMoreElements(); /**/)
                {
                    String value = ( String ) values.nextElement();
                    if ( comparedStr.equals( normalizer.normalize( value ) ) )
                    {
                        return true;
                    }
                }
            }
            catch( NamingException e )
            {
                throw new LdapException( e.getMessage() );
            }
        }
        else
        {
            byte[] comparedBytes = null;

            if ( !compared.isBinary() )
            {
                if ( compared.getString().length() < 3 )
                {
                    return false;
                }

                // Transform the String to a byte array
                int state = 1;
                comparedBytes = new byte[compared.getString().length() / 3];
                int pos = 0;

                for ( char c : compared.getString().toCharArray() )
                {
                    switch ( state )
                    {
                        case 1:
                            if ( c != '\\' )
                            {
                                return false;
                            }

                            state++;
                            break;

                        case 2:
                            int high = StringTools.getHexValue( c );

                            if ( high == -1 )
                            {
                                return false;
                            }

                            comparedBytes[pos] = ( byte ) ( high << 4 );

                            state++;
                            break;

                        case 3:
                            int low = StringTools.getHexValue( c );

                            if ( low == -1 )
                            {
                                return false;
                            }

                            comparedBytes[pos] += ( byte ) low;
                            pos++;

                            state = 1;
                            break;
                    }
                }
            }
            else
            {
                comparedBytes = compared.getBytes();
            }

            try
            {
                for ( NamingEnumeration<?> values = attr.getAll(); values.hasMoreElements(); /**/)
                {
                    Object value = values.nextElement();
    
                    if ( value instanceof byte[] )
                    {
                        if ( ArrayUtils.isEquals( comparedBytes, value ) )
                        {
                            return true;
                        }
                    }
                }
            }
            catch ( NamingException ne )
            {
                throw new LdapException( ne.getMessage() );
            }
        }

        return false;
    }


    /**
     * Check if an attribute contains a value. The test is case insensitive,
     * and the value is supposed to be a String. If the value is a byte[],
     * then the case sensitivity is useless.
     *
     * @param attr The attribute to check
     * @param value The value to look for
     * @return true if the value is present in the attribute
     */
    public static boolean containsValueCaseIgnore( Attribute attr, Object value )
    {
        // quick bypass test
        if ( attr.contains( value ) )
        {
            return true;
        }

        try
        {
            if ( value instanceof String )
            {
                String strVal = ( String ) value;

                NamingEnumeration<?> attrVals = attr.getAll();

                while ( attrVals.hasMoreElements() )
                {
                    Object attrVal = attrVals.nextElement();

                    if ( attrVal instanceof String )
                    {
                        if ( strVal.equalsIgnoreCase( ( String ) attrVal ) )
                        {
                            return true;
                        }
                    }
                }
            }
            else
            {
                byte[] valueBytes = ( byte[] ) value;

                NamingEnumeration<?> attrVals = attr.getAll();

                while ( attrVals.hasMoreElements() )
                {
                    Object attrVal = attrVals.nextElement();

                    if ( attrVal instanceof byte[] )
                    {
                        if ( Arrays.equals( ( byte[] ) attrVal, valueBytes ) )
                        {
                            return true;
                        }

                    }
                }
            }
        }
        catch ( NamingException ne )
        {
            return false;
        }

        return false;
    }


    /*
    public static boolean containsAnyValues( Attribute attr, Object[] compared, AttributeType type )
        throws NamingException
    {
        // quick bypass test
        for ( Object object : compared )
        {
            if ( attr.contains( object ) )
            {
                return true;
            }
        }

        Normalizer normalizer = type.getEquality().getNormalizer();

        if ( type.getSyntax().isHumanReadable() )
        {
            for ( Object object : compared )
            {
                String comparedStr = ( String ) normalizer.normalize( object );

                for ( int ii = attr.size(); ii >= 0; ii-- )
                {
                    String value = ( String ) attr.get( ii );

                    if ( comparedStr.equals( normalizer.normalize( value ) ) )
                    {
                        return true;
                    }
                }
            }
        }
        else
        {
            for ( Object object : compared )
            {
                byte[] comparedBytes = ( byte[] ) object;

                for ( int ii = attr.size(); ii >= 0; ii-- )
                {
                    if ( ArrayUtils.isEquals( comparedBytes, attr.get( ii ) ) )
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }
    */


    /**
     * Creates a new attribute which contains the values representing the
     * difference of two attributes. If both attributes are null then we cannot
     * determine the attribute ID and an {@link IllegalArgumentException} is
     * raised. Note that the order of arguments makes a difference.
     * 
     * @param attr0
     *            the first attribute
     * @param attr1
     *            the second attribute
     * @return a new attribute with the difference of values from both attribute
     *         arguments
     * @throws NamingException
     *             if there are problems accessing attribute values
     */
    public static Attribute getDifference( Attribute attr0, Attribute attr1 ) throws NamingException
    {
        String id;

        if ( ( attr0 == null ) && ( attr1 == null ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04339 ) );
        }
        else if ( attr0 == null )
        {
            return new BasicAttribute( attr1.getID() );
        }
        else if ( attr1 == null )
        {
            return ( Attribute ) attr0.clone();
        }
        else if ( !attr0.getID().equalsIgnoreCase( attr1.getID() ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04340 ) );
        }
        else
        {
            id = attr0.getID();
        }

        Attribute attr = new BasicAttribute( id );

        for ( int ii = 0; ii < attr0.size(); ii++ )
        {
            attr.add( attr0.get( ii ) );
        }

        for ( int ii = 0; ii < attr1.size(); ii++ )
        {
            attr.remove( attr1.get( ii ) );
        }

        return attr;
    }


    /**
     * Creates a new attribute which contains the values representing the union
     * of two attributes. If one attribute is null then the resultant attribute
     * returned is a copy of the non-null attribute. If both are null then we
     * cannot determine the attribute ID and an {@link IllegalArgumentException}
     * is raised.
     * 
     * @param attr0
     *            the first attribute
     * @param attr1
     *            the second attribute
     * @return a new attribute with the union of values from both attribute
     *         arguments
     * @throws NamingException
     *             if there are problems accessing attribute values
     */
    public static Attribute getUnion( Attribute attr0, Attribute attr1 ) throws NamingException
    {
        String id;

        if ( attr0 == null && attr1 == null )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04341 ) );
        }
        else if ( attr0 == null )
        {
            id = attr1.getID();
        }
        else if ( attr1 == null )
        {
            id = attr0.getID();
        }
        else if ( !attr0.getID().equalsIgnoreCase( attr1.getID() ) )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04342 ) );
        }
        else
        {
            id = attr0.getID();
        }

        Attribute attr = new BasicAttribute( id );

        if ( attr0 != null )
        {
            for ( int ii = 0; ii < attr0.size(); ii++ )
            {
                attr.add( attr0.get( ii ) );
            }
        }

        if ( attr1 != null )
        {
            for ( int ii = 0; ii < attr1.size(); ii++ )
            {
                attr.add( attr1.get( ii ) );
            }
        }

        return attr;
    }


    /**
     * Check if the attributes is a BasicAttributes, and if so, switch
     * the case sensitivity to false to avoid tricky problems in the server.
     * (Ldap attributeTypes are *always* case insensitive)
     * 
     * @param attributes The Attributes to check
     */
    public static Attributes toCaseInsensitive( Attributes attributes )
    {
        if ( attributes == null )
        {
            return attributes;
        }

        if ( attributes instanceof BasicAttributes )
        {
            if ( attributes.isCaseIgnored() )
            {
                // Just do nothing if the Attributes is already case insensitive
                return attributes;
            }
            else
            {
                // Ok, bad news : we have to create a new BasicAttributes
                // which will be case insensitive
                Attributes newAttrs = new BasicAttributes( true );

                NamingEnumeration<?> attrs = attributes.getAll();

                if ( attrs != null )
                {
                    // Iterate through the attributes now
                    while ( attrs.hasMoreElements() )
                    {
                        newAttrs.put( ( Attribute ) attrs.nextElement() );
                    }
                }

                return newAttrs;
            }
        }
        else
        {
            // we can safely return the attributes if it's not a BasicAttributes
            return attributes;
        }
    }


    /**
     * Return a string representing the attributes with tabs in front of the
     * string
     * 
     * @param tabs
     *            Spaces to be added before the string
     * @param attribute
     *            The attribute to print
     * @return A string
     */
    public static String toString( String tabs, Attribute attribute )
    {
        StringBuffer sb = new StringBuffer();

        sb.append( tabs ).append( "Attribute\n" );

        if ( attribute != null )
        {
            sb.append( tabs ).append( "    Type : '" ).append( attribute.getID() ).append( "'\n" );

            for ( int j = 0; j < attribute.size(); j++ )
            {

                try
                {
                    Object attr = attribute.get( j );

                    if ( attr != null )
                    {
                        if ( attr instanceof String )
                        {
                            sb.append( tabs ).append( "        Val[" ).append( j ).append( "] : " ).append( attr )
                                .append( " \n" );
                        }
                        else if ( attr instanceof byte[] )
                        {
                            String string = StringTools.utf8ToString( ( byte[] ) attr );

                            sb.append( tabs ).append( "        Val[" ).append( j ).append( "] : " );
                            sb.append( string ).append( '/' );
                            sb.append( StringTools.dumpBytes( ( byte[] ) attr ) );
                            sb.append( " \n" );
                        }
                        else
                        {
                            sb.append( tabs ).append( "        Val[" ).append( j ).append( "] : " ).append( attr )
                                .append( " \n" );
                        }
                    }
                }
                catch ( NamingException ne )
                {
                    sb.append( "Bad attribute : " ).append( ne.getMessage() );
                }
            }
        }

        return sb.toString();
    }


    /**
     * Return a string representing the attribute
     * 
     * @param attribute
     *            The attribute to print
     * @return A string
     */
    public static String toString( Attribute attribute )
    {
        return toString( "", attribute );
    }


    /**
     * Return a string representing the attributes with tabs in front of the
     * string
     * 
     * @param tabs
     *            Spaces to be added before the string
     * @param attributes
     *            The attributes to print
     * @return A string
     */
    public static String toString( String tabs, Attributes attributes )
    {
        StringBuffer sb = new StringBuffer();
        sb.append( tabs ).append( "Attributes\n" );

        if ( attributes != null )
        {
            NamingEnumeration<?> attributesIterator = attributes.getAll();

            while ( attributesIterator.hasMoreElements() )
            {
                Attribute attribute = ( Attribute ) attributesIterator.nextElement();
                sb.append( tabs ).append( attribute.toString() );
            }
        }

        return sb.toString();
    }


    /**
     * Parse attribute's options :
     * 
     * options = *( ';' option )
     * option = 1*keychar
     * keychar = 'a'-z' | 'A'-'Z' / '0'-'9' / '-'
     */
    private static void parseOptions( String str, Position pos ) throws ParseException
    {
        while ( StringTools.isCharASCII( str, pos.start, ';' ) )
        {
            pos.start++;

            // We have an option
            if ( !StringTools.isAlphaDigitMinus( str, pos.start ) )
            {
                // We must have at least one keychar
                throw new ParseException( I18n.err( I18n.ERR_04343 ), pos.start );
            }

            pos.start++;

            while ( StringTools.isAlphaDigitMinus( str, pos.start ) )
            {
                pos.start++;
            }
        }
    }


    /**
     * Parse a number :
     * 
     * number = '0' | '1'..'9' digits
     * digits = '0'..'9'*
     * 
     * @return true if a number has been found
     */
    private static boolean parseNumber( String filter, Position pos )
    {
        char c = StringTools.charAt( filter, pos.start );

        switch ( c )
        {
            case '0':
                // If we get a starting '0', we should get out
                pos.start++;
                return true;

            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                pos.start++;
                break;

            default:
                // Not a number.
                return false;
        }

        while ( StringTools.isDigit( filter, pos.start ) )
        {
            pos.start++;
        }

        return true;
    }


    /**
     * 
     * Parse an OID.
     *
     * numericoid = number 1*( '.' number )
     * number = '0'-'9' / ( '1'-'9' 1*'0'-'9' )
     *
     * @param str The OID to parse
     * @param pos The current position in the string
     * @return A valid OID
     * @throws ParseException If we don't have a valid OID
     */
    public static void parseOID( String str, Position pos ) throws ParseException
    {
        // We have an OID
        parseNumber( str, pos );

        // We must have at least one '.' number
        if ( !StringTools.isCharASCII( str, pos.start, '.' ) )
        {
            throw new ParseException( I18n.err( I18n.ERR_04344 ), pos.start );
        }

        pos.start++;

        if ( !parseNumber( str, pos ) )
        {
            throw new ParseException( I18n.err( I18n.ERR_04345 ), pos.start );
        }

        while ( true )
        {
            // Break if we get something which is not a '.'
            if ( !StringTools.isCharASCII( str, pos.start, '.' ) )
            {
                break;
            }

            pos.start++;

            if ( !parseNumber( str, pos ) )
            {
                throw new ParseException(I18n.err( I18n.ERR_04345 ), pos.start );
            }
        }
    }


    /**
     * Parse an attribute. The grammar is :
     * attributedescription = attributetype options
     * attributetype = oid
     * oid = descr / numericoid
     * descr = keystring
     * numericoid = number 1*( '.' number )
     * options = *( ';' option )
     * option = 1*keychar
     * keystring = leadkeychar *keychar
     * leadkeychar = 'a'-z' | 'A'-'Z'
     * keychar = 'a'-z' | 'A'-'Z' / '0'-'9' / '-'
     * number = '0'-'9' / ( '1'-'9' 1*'0'-'9' )
     *
     * @param str The parsed attribute,
     * @param pos The position of the attribute in the current string
     * @return The parsed attribute if valid
     */
    public static String parseAttribute( String str, Position pos, boolean withOption ) throws ParseException
    {
        // We must have an OID or an DESCR first
        char c = StringTools.charAt( str, pos.start );

        if ( c == '\0' )
        {
            throw new ParseException( I18n.err( I18n.ERR_04346 ), pos.start );
        }

        int start = pos.start;

        if ( StringTools.isAlpha( c ) )
        {
            // A DESCR
            pos.start++;

            while ( StringTools.isAlphaDigitMinus( str, pos.start ) )
            {
                pos.start++;
            }

            // Parse the options if needed
            if ( withOption )
            {
                parseOptions( str, pos );
            }

            return str.substring( start, pos.start );
        }
        else if ( StringTools.isDigit( c ) )
        {
            // An OID
            pos.start++;

            // Parse the OID
            parseOID( str, pos );

            // Parse the options
            if ( withOption )
            {
                parseOptions( str, pos );
            }

            return str.substring( start, pos.start );
        }
        else
        {
            throw new ParseException( I18n.err( I18n.ERR_04347 ), pos.start );
        }
    }


    /**
     * Return a string representing the attributes
     * 
     * @param attributes
     *            The attributes to print
     * @return A string
     */
    public static String toString( Attributes attributes )
    {
        return toString( "", attributes );
    }


    /**
     * A method to apply a modification to an existing entry.
     * 
     * @param entry The entry on which we want to apply a modification
     * @param modification the Modification to be applied
     * @throws LdapException if some operation fails.
     */
    public static void applyModification( Entry entry, Modification modification ) throws LdapException
    {
        EntryAttribute modAttr = modification.getAttribute();
        String modificationId = modAttr.getId();

        switch ( modification.getOperation() )
        {
            case ADD_ATTRIBUTE:
                EntryAttribute modifiedAttr = entry.get( modificationId );

                if ( modifiedAttr == null )
                {
                    // The attribute should be added.
                    entry.put( modAttr );
                }
                else
                {
                    // The attribute exists : the values can be different,
                    // so we will just add the new values to the existing ones.
                    for ( Value<?> value : modAttr )
                    {
                        // If the value already exist, nothing is done.
                        // Note that the attribute *must* have been
                        // normalized before.
                        modifiedAttr.add( value );
                    }
                }

                break;

            case REMOVE_ATTRIBUTE:
                if ( modAttr.get() == null )
                {
                    // We have no value in the ModificationItem attribute :
                    // we have to remove the whole attribute from the initial
                    // entry
                    entry.removeAttributes( modificationId );
                }
                else
                {
                    // We just have to remove the values from the original
                    // entry, if they exist.
                    modifiedAttr = entry.get( modificationId );

                    if ( modifiedAttr == null )
                    {
                        break;
                    }

                    for ( Value<?> value : modAttr )
                    {
                        // If the value does not exist, nothing is done.
                        // Note that the attribute *must* have been
                        // normalized before.
                        modifiedAttr.remove( value );
                    }

                    if ( modifiedAttr.size() == 0 )
                    {
                        // If this was the last value, remove the attribute
                        entry.removeAttributes( modifiedAttr.getId() );
                    }
                }

                break;

            case REPLACE_ATTRIBUTE:
                if ( modAttr.get() == null )
                {
                    // If the modification does not have any value, we have
                    // to delete the attribute from the entry.
                    entry.removeAttributes( modificationId );
                }
                else
                {
                    // otherwise, just substitute the existing attribute.
                    entry.put( modAttr );
                }

                break;
        }
    }


    /**
     * Check if an attribute contains a specific value and remove it using the associated
     * matchingRule for the attribute type supplied.
     *
     * @param attr the attribute we are searching in
     * @param compared the object we are looking for
     * @param type the attribute type
     * @return the value removed from the attribute, otherwise null
     * @throws NamingException if something went wrong while removing the value
     *
    public static Object removeValue( Attribute attr, Object compared, AttributeType type ) throws NamingException
    {
        // quick bypass test
        if ( attr.contains( compared ) )
        {
            return attr.remove( compared );
        }

        MatchingRule matchingRule = type.getEquality();
        Normalizer normalizer;

        if ( matchingRule != null )
        {
            normalizer = type.getEquality().getNormalizer();
        }
        else
        {
            normalizer = new NoOpNormalizer();
        }

        if ( type.getSyntax().isHumanReadable() )
        {
            String comparedStr = ( String ) normalizer.normalize( compared );

            for ( NamingEnumeration<?> values = attr.getAll(); values.hasMoreElements(); )
            {
                String value = ( String ) values.nextElement();
                if ( comparedStr.equals( normalizer.normalize( value ) ) )
                {
                    return attr.remove( value );
                }
            }
        }
        else
        {
            byte[] comparedBytes = null;

            if ( compared instanceof String )
            {
                if ( ( ( String ) compared ).length() < 3 )
                {
                    return null;
                }

                // Tansform the String to a byte array
                int state = 1;
                comparedBytes = new byte[( ( String ) compared ).length() / 3];
                int pos = 0;

                for ( char c : ( ( String ) compared ).toCharArray() )
                {
                    switch ( state )
                    {
                        case 1:
                            if ( c != '\\' )
                            {
                                return null;
                            }

                            state++;
                            break;

                        case 2:
                            int high = StringTools.getHexValue( c );

                            if ( high == -1 )
                            {
                                return null;
                            }

                            comparedBytes[pos] = ( byte ) ( high << 4 );

                            state++;
                            break;

                        case 3:
                            int low = StringTools.getHexValue( c );

                            if ( low == -1 )
                            {
                                return null;
                            }

                            comparedBytes[pos] += ( byte ) low;
                            pos++;

                            state = 1;
                            break;
                    }
                }
            }
            else
            {
                comparedBytes = ( byte[] ) compared;
            }

            for ( NamingEnumeration<?> values = attr.getAll(); values.hasMoreElements(); )
            {
                Object value = values.nextElement();

                if ( value instanceof byte[] )
                {
                    if ( ArrayUtils.isEquals( comparedBytes, value ) )
                    {
                        return attr.remove( value );
                    }
                }
            }
        }

        return null;
    }


    /**
     * Convert a BasicAttributes or a AttributesImpl to a ServerEntry
     *
     * @param attributes the BasicAttributes or AttributesImpl instance to convert
     * @param registries The registries, needed ro build a ServerEntry
     * @param dn The DN which is needed by the ServerEntry 
     * @return An instance of a ServerEntry object
     * 
     * @throws InvalidAttributeIdentifierException If we get an invalid attribute
     */
    public static Entry toClientEntry( Attributes attributes, DN dn ) throws LdapException
    {
        if ( attributes instanceof BasicAttributes )
        {
            try
            {
                Entry entry = new DefaultClientEntry( dn );

                for ( NamingEnumeration<? extends Attribute> attrs = attributes.getAll(); attrs.hasMoreElements(); )
                {
                    Attribute attr = attrs.nextElement();

                    EntryAttribute entryAttribute = toClientAttribute( attr );

                    if ( entryAttribute != null )
                    {
                        entry.put( entryAttribute );
                    }
                }

                return entry;
            }
            catch ( LdapException ne )
            {
                throw new LdapInvalidAttributeTypeException( ne.getMessage() );
            }
        }
        else
        {
            return null;
        }
    }


    /**
     * Converts an {@link Entry} to an {@link Attributes}.
     *
     * @param entry
     *      the {@link Entry} to convert
     * @return
     *      the equivalent {@link Attributes}
     */
    public static Attributes toAttributes( Entry entry )
    {
        if ( entry != null )
        {
            Attributes attributes = new BasicAttributes( true );

            // Looping on attributes
            for ( Iterator<EntryAttribute> attributeIterator = entry.iterator(); attributeIterator.hasNext(); )
            {
                EntryAttribute entryAttribute = ( EntryAttribute ) attributeIterator.next();

                attributes.put( toAttribute( entryAttribute ) );
            }

            return attributes;
        }

        return null;
    }


    /**
     * Converts an {@link EntryAttribute} to an {@link Attribute}.
     *
     * @param entryAttribute
     *      the {@link EntryAttribute} to convert
     * @return
     *      the equivalent {@link Attribute}
     */
    public static Attribute toAttribute( EntryAttribute entryAttribute )
    {
        if ( entryAttribute != null )
        {
            Attribute attribute = new BasicAttribute( entryAttribute.getId() );

            // Looping on values
            for ( Iterator<Value<?>> valueIterator = entryAttribute.iterator(); valueIterator.hasNext(); )
            {
                Value<?> value = valueIterator.next();
                attribute.add( value.get() );
            }

            return attribute;
        }

        return null;
    }


    /**
     * Convert a BasicAttribute or a AttributeImpl to a EntryAttribute
     *
     * @param attribute the BasicAttributes or AttributesImpl instance to convert
     * @param attributeType
     * @return An instance of a ClientEntry object
     * 
     * @throws InvalidAttributeIdentifierException If we had an incorrect attribute
     */
    public static EntryAttribute toClientAttribute( Attribute attribute )
    {
        if ( attribute == null )
        {
            return null;
        }

        try
        {
            EntryAttribute clientAttribute = new DefaultClientAttribute( attribute.getID() );

            for ( NamingEnumeration<?> values = attribute.getAll(); values.hasMoreElements(); )
            {
                Object value = values.nextElement();

                if ( value instanceof String )
                {
                    clientAttribute.add( ( String ) value );
                }
                else if ( value instanceof byte[] )
                {
                    clientAttribute.add( ( byte[] ) value );
                }
                else
                {
                    clientAttribute.add( ( String ) null );
                }
            }

            return clientAttribute;
        }
        catch ( NamingException ne )
        {
            return null;
        }
    }
}

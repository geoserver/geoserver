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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The DN class contains a DN (Distinguished Name).
 *
 * Its specification can be found in RFC 2253,
 * "UTF-8 String Representation of Distinguished Names".
 *
 * We will store two representation of a DN :
 * - a user Provider representation, which is the parsed String given by a user
 * - an internal representation.
 *
 * A DN is formed of RDNs, in a specific order :
 *  RDN[n], RDN[n-1], ... RDN[1], RDN[0]
 *
 * It represents a tree, in which the root is the last RDN (RDN[0]) and the leaf
 * is the first RDN (RDN[n]).
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 930282 $, $Date: 2010-04-02 17:29:58 +0300 (Fri, 02 Apr 2010) $
 */
public class DN implements Cloneable, Serializable, Comparable<DN>, Iterable<RDN>
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( DN.class );

    /**
     * Declares the Serial Version Uid.
     *
     * @see <a
     *      href="http://c2.com/cgi/wiki?AlwaysDeclareSerialVersionUid">Always
     *      Declare Serial Version Uid</a>
     */
    private static final long serialVersionUID = 1L;

    /** Value returned by the compareTo method if values are not equals */
    public static final int NOT_EQUAL = -1;

    /** Value returned by the compareTo method if values are equals */
    public static final int EQUAL = 0;

    /** A flag used to tell if the DN has been normalized */
    private boolean normalized;

    // ~ Static fields/initializers
    // -----------------------------------------------------------------
    /**
     *  The RDNs that are elements of the DN
     * NOTE THAT THESE ARE IN THE OPPOSITE ORDER FROM THAT IMPLIED BY THE JAVADOC!
     * Rdn[0] is rdns.get(n) and Rdn[n] is rdns.get(0)
     */
    protected List<RDN> rdns = new ArrayList<RDN>( 5 );

    /** The user provided name */
    private String upName;

    /** The normalized name */
    private String normName;

    /** The bytes representation of the normName */
    private byte[] bytes;

    /** A null DN */
    public static final DN EMPTY_DN = new DN();


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Construct an empty DN object
     */
    public DN()
    {
        upName = "";
        normName = null;
        normalized = true;
    }


    /**
     * Copies a DN to an DN.
     *
     * @param name composed of String name components.
     * @throws LdapInvalidDnException If the Name is invalid.
     */
    public DN( DN dn ) throws LdapInvalidDnException
    {
        if ( ( dn != null ) && ( dn.size() != 0 ) )
        {
            for ( int ii = 0; ii < dn.size(); ii++ )
            {
                String nameComponent = dn.get( ii );
                add( nameComponent );
            }
        }

        normalized = false;
    }


    /**
     * Parse a String and checks that it is a valid DN <br>
     * <p>
     * &lt;distinguishedName&gt; ::= &lt;name&gt; | e <br>
     * &lt;name&gt; ::= &lt;name-component&gt; &lt;name-components&gt; <br>
     * &lt;name-components&gt; ::= &lt;spaces&gt; &lt;separator&gt;
     * &lt;spaces&gt; &lt;name-component&gt; &lt;name-components&gt; | e <br>
     * </p>
     *
     * @param upName The String that contains the DN.
     * @throws LdapInvalidNameException if the String does not contain a valid DN.
     */
    public DN( String upName ) throws LdapInvalidDnException
    {
        if ( upName != null )
        {
            DnParser.parseInternal( upName, rdns );
        }

        // Stores the representations of a DN : internal (as a string and as a
        // byte[]) and external.
        normalizeInternal();
        normalized = false;

        this.upName = upName;
    }


    /**
     * Creates a new instance of DN, using varargs to declare the RDNs. Each
     * String is either a full RDN, or a couple of AttributeType DI and a value.
     * If the String contains a '=' symbol, the the constructor will assume that
     * the String arg contains afull RDN, otherwise, it will consider that the 
     * following arg is the value.
     * An example of usage would be :
     * <pre>
     * String exampleName = "example";
     * String baseDn = "dc=apache,dc=org";
     * 
     * DN dn = new DN(
     *     "cn=Test",
     *     "ou", exampleName,
     *     baseDn);
     * </pre>
     *
     * @param upNames
     * @throws LdapInvalidDnException
     */
    public DN( String... upRdns ) throws LdapInvalidDnException
    {
        StringBuilder sb = new StringBuilder();
        boolean valueExpected = false;
        boolean isFirst = true;
        
        for ( String upRdn : upRdns )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else if ( !valueExpected )
            {
                sb.append( ',' );
            }
            
            if ( !valueExpected )
            {
                sb.append( upRdn );
                
                if ( upRdn.indexOf( '=' ) == -1 )
                {
                    valueExpected = true;
                }
            }
            else
            {
                sb.append( "=" ).append( upRdn );
                
                valueExpected = false;
            }
        }
        
        if ( valueExpected )
        {
            throw new LdapInvalidDnException( ResultCodeEnum.INVALID_DN_SYNTAX, I18n.err( I18n.ERR_04202 ) );
        }

        // Stores the representations of a DN : internal (as a string and as a
        // byte[]) and external.
        upName = sb.toString();
        DnParser.parseInternal( upName, rdns );
        normalizeInternal();
        normalized = false;
    }
    
    /**
     * Create a DN when deserializing it.
     * 
     * Note : this constructor is used only by the deserialization method.
     * @param upName The user provided name
     * @param normName the normalized name
     * @param bytes the name as a byte[]
     */
    DN( String upName, String normName, byte[] bytes )
    {
        normalized = true;
        this.upName = upName;
        this.normName = normName;
        this.bytes = bytes;
    }


    /**
     * Static factory which creates a normalized DN from a String and a Map of OIDs.
     *
     * @param name The DN as a String
     * @param oidsMap The OID mapping
     * @return A valid DN
     * @throws LdapInvalidNameException If the DN is invalid.
     * @throws LdapInvalidDnException If something went wrong.
     */
    public static DN normalize( String name, Map<String, OidNormalizer> oidsMap ) throws LdapInvalidDnException
    {
        if ( ( name == null ) || ( name.length() == 0 ) || ( oidsMap == null ) || ( oidsMap.size() == 0 ) )
        {
            return DN.EMPTY_DN;
        }

        DN newDn = new DN( name );
        
        Enumeration<RDN> rdns = newDn.getAllRdn();
        
        // Loop on all RDNs
        while ( rdns.hasMoreElements() )
        {
            RDN rdn = rdns.nextElement();
            String upName = rdn.getName();
            rdnOidToName( rdn, oidsMap );
            rdn.normalize();
            rdn.setUpName( upName );
        }
        
        newDn.normalizeInternal();
        newDn.normalized = true;
        
        return newDn;
    }


    /**
     * Normalize the DN by triming useless spaces and lowercasing names.
     */
    void normalizeInternal()
    {
        normName = toNormName();
    }


    /**
     * Build the normalized DN as a String,
     *
     * @return A String representing the normalized DN
     */
    private String toNormName()
    {
        if ( rdns.size() == 0 )
        {
            bytes = null;
            return "";
        }
        else
        {
            StringBuffer sb = new StringBuffer();
            boolean isFirst = true;

            for ( RDN rdn : rdns )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ',' );
                }

                sb.append( rdn.getNormName() );
            }

            String newNormName = sb.toString();

            if ( ( normName == null ) || !normName.equals( newNormName ) )
            {
                bytes = StringTools.getBytesUtf8( newNormName );
                normName = newNormName;
            }

            return normName;
        }
    }


    /**
     * Return the normalized DN as a String. It returns the same value as the
     * getNormName method
     *
     * @return A String representing the normalized DN
     */
    public String toString()
    {
        return getName();
    }


    /**
     * Return the User Provided DN as a String,
     *
     * @return A String representing the User Provided DN
     */
    private String toUpName()
    {
        if ( rdns.size() == 0 )
        {
            upName = "";
        }
        else
        {
            StringBuffer sb = new StringBuffer();
            boolean isFirst = true;

            for ( RDN rdn : rdns )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ',' );
                }

                sb.append( rdn.getName() );
            }

            upName = sb.toString();
        }

        return upName;
    }


    /**
     * Return the User Provided prefix representation of the DN starting at the
     * posn position.
     *
     * If posn = 0, return an empty string.
     *
     * for DN : sn=smith, dc=apache, dc=org
     * getUpname(0) -> ""
     * getUpName(1) -> "dc=org"
     * getUpname(3) -> "sn=smith, dc=apache, dc=org"
     * getUpName(4) -> ArrayOutOfBoundException
     *
     * Warning ! The returned String is not exactly the
     * user provided DN, as spaces before and after each RDNs have been trimmed.
     *
     * @param posn
     *            The starting position
     * @return The truncated DN
     */
    private String getUpNamePrefix( int posn )
    {
        if ( posn == 0 )
        {
            return "";
        }

        if ( posn > rdns.size() )
        {
            String message = I18n.err( I18n.ERR_04203, posn, rdns.size() );
            LOG.error( message );
            throw new ArrayIndexOutOfBoundsException( message );
        }

        int start = rdns.size() - posn;
        StringBuffer sb = new StringBuffer();
        boolean isFirst = true;

        for ( int i = start; i < rdns.size(); i++ )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ',' );
            }

            sb.append( rdns.get( i ).getName() );
        }

        return sb.toString();
    }


    /**
     * Return the User Provided suffix representation of the DN starting at the
     * posn position.
     * If posn = 0, return an empty string.
     *
     * for DN : sn=smith, dc=apache, dc=org
     * getUpname(0) -> "sn=smith, dc=apache, dc=org"
     * getUpName(1) -> "sn=smith, dc=apache"
     * getUpname(3) -> "sn=smith"
     * getUpName(4) -> ""
     *
     * Warning ! The returned String is not exactly the user
     * provided DN, as spaces before and after each RDNs have been trimmed.
     *
     * @param posn The starting position
     * @return The truncated DN
     */
    private String getUpNameSuffix( int posn )
    {
        if ( posn > rdns.size() )
        {
            return "";
        }

        int end = rdns.size() - posn;
        StringBuffer sb = new StringBuffer();
        boolean isFirst = true;

        for ( int i = 0; i < end; i++ )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( ',' );
            }

            sb.append( rdns.get( i ).getName() );
        }

        return sb.toString();
    }


    /**
     * Gets the hash code of this name.
     *
     * @see java.lang.Object#hashCode()
     * @return the instance hash code
     */
    public int hashCode()
    {
        int result = 37;

        for ( RDN rdn : rdns )
        {
            result = result * 17 + rdn.hashCode();
        }

        return result;
    }


    /**
     * Get the initial DN
     *
     * @return The DN as a String
     */
    public String getName()
    {
        return ( upName == null ? "" : upName );
    }


    /**
     * Sets the up name.
     * 
     * @param upName the new up name
     */
    void setUpName( String upName )
    {
        this.upName = upName;
    }


    /**
     * Get the normalized DN
     *
     * @return The DN as a String
     */
    public String getNormName()
    {
        if ( normName == null )
        {
            normName = toNormName();
        }
        
        return normName;
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return rdns.size();
    }


    /**
     * Get the number of bytes necessary to store this DN

     * @param dn The DN.
     * @return A integer, which is the size of the UTF-8 byte array
     */
    public static int getNbBytes( DN dn )
    {
        return dn.bytes == null ? 0 : dn.bytes.length;
    }


    /**
     * Get an UTF-8 representation of the normalized form of the DN
     * 
     * @param dn The DN.
     * @return A byte[] representation of the DN
     */
    public static byte[] getBytes( DN dn )
    {
        return dn == null ? null : dn.bytes;
    }


    /**
     * Tells if the current DN is a parent of another DN.<br>
     * For instance, <b>dc=com</b> is a parent
     * of <b>dc=example, dc=com</b>
     * 
     * @param dn The child
     * @return true if the current DN is a parent of the given DN
     */
    public boolean isParentOf( String dn )
    {
        try
        {
            return isParentOf( new DN( dn ) );
        }
        catch( LdapInvalidDnException lide )
        {
            return false;
        }
    }
    

    /**
     * Tells if the current DN is a parent of another DN.<br>
     * For instance, <b>dc=com</b> is a parent
     * of <b>dc=example, dc=com</b>
     * 
     * @param dn The child
     * @return true if the current DN is a parent of the given DN
     */
    public boolean isParentOf( DN dn )
    {
        if ( dn == null )
        {
            return false;
        }
        
        return dn.isChildOf( this );
    }


    /**
     * Tells if a DN is a child of another DN.<br>
     * For instance, <b>dc=example, dc=com</b> is a child
     * of <b>dc=com</b>
     * 
     * @param dn The parent
     * @return true if the current DN is a child of the given DN
     */
    public boolean isChildOf( String dn )
    {
        try
        {
            return isChildOf( new DN( dn ) );
        }
        catch( LdapInvalidDnException lide )
        {
            return false;
        }
    }
    

    /**
     * Tells if a DN is a child of another DN.<br>
     * For instance, <b>dc=example, dc=com</b> is a child
     * of <b>dc=com</b>
     * 
     * @param dn The parent
     * @return true if the current DN is a child of the given DN
     */
    public boolean isChildOf( DN dn )
    {
        if ( dn == null )
        {
            return true;
        }

        if ( dn.size() == 0 )
        {
            return true;
        }

        if ( dn.size() > size() )
        {
            // The name is longer than the current DN.
            return false;
        }

        // Ok, iterate through all the RDN of the name,
        // starting a the end of the current list.

        for ( int i = dn.size() - 1; i >= 0; i-- )
        {
            RDN nameRdn = dn.rdns.get( dn.rdns.size() - i - 1 );
            RDN ldapRdn = rdns.get( rdns.size() - i - 1 );

            if ( nameRdn.compareTo( ldapRdn ) != 0 )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Determines whether this name has a specific suffix. A name
     * <tt>name</tt> has a DN as a suffix if its right part contains the given DN
     *
     * Be aware that for a specific
     * DN like : <b>cn=xxx, ou=yyy</b> the hasSuffix method will return false with
     * <b>ou=yyy</b>, and true with <b>cn=xxx</b>
     *
     * @param dn the name to check
     * @return true if <tt>dn</tt> is a suffix of this name, false otherwise
     */
    public boolean hasSuffix( DN dn )
    {

        if ( dn == null )
        {
            return true;
        }

        if ( dn.size() == 0 )
        {
            return true;
        }

        if ( dn.size() > size() )
        {
            // The name is longer than the current DN.
            return false;
        }

        // Ok, iterate through all the RDN of the name,
        // starting a the end of the current list.

        for ( int i = 0; i < dn.size(); i++ )
        {
            RDN nameRdn = dn.rdns.get( i );
            RDN ldapRdn = rdns.get( i );

            if ( nameRdn.compareTo( ldapRdn ) != 0 )
            {
                return false;
            }
        }

        return true;
    }
    
    
    /**
     * return true if this DN contains no RDNs
     */
    public boolean isEmpty()
    {
        return ( rdns.size() == 0 );
    }


    /**
     * Get the given RDN as a String. The position is used in the 
     * reverse order. Assuming that we have a DN like 
     * <pre>dc=example,dc=apache,dc=org</pre>
     * then :
     * <li><code>get(0)</code> will return dc=org</li>
     * <li><code>get(1)</code> will return dc=apache</li>
     * <li><code>get(2)</code> will return dc=example</li>
     * 
     * @param posn The position of the wanted RDN in the DN.
     */
    public String get( int posn )
    {
        if ( rdns.size() == 0 )
        {
            return "";
        }
        else
        {
            RDN rdn = rdns.get( rdns.size() - posn - 1 );

            return rdn.getNormName();
        }
    }


    /**
     * Retrieves a component of this name.
     *
     * @param posn
     *            the 0-based index of the component to retrieve. Must be in the
     *            range [0,size()).
     * @return the component at index posn
     * @throws ArrayIndexOutOfBoundsException
     *             if posn is outside the specified range
     */
    public RDN getRdn( int posn )
    {
        if ( rdns.size() == 0 )
        {
            return null;
        }
        else
        {
            RDN rdn = rdns.get( rdns.size() - posn - 1 );

            return rdn;
        }
    }


    /**
     * Retrieves the last (leaf) component of this name.
     *
     * @return the last component of this DN
     */
    public RDN getRdn()
    {
        if ( rdns.size() == 0 )
        {
            return null;
        }
        else
        {
            return rdns.get( 0 );
        }
    }


    /**
     * Retrieves all the components of this name.
     *
     * @return All the components
     */
    public List<RDN> getRdns()
    {
        List<RDN> newRdns = new ArrayList<RDN>();

        // We will clone the list, to avoid user modifications
        for ( RDN rdn : rdns )
        {
            newRdns.add( ( RDN ) rdn.clone() );
        }

        return newRdns;
    }


    /**
     * Retrieves the components of this name as an enumeration of strings. The
     * effect on the enumeration of updates to this name is undefined. If the
     * name has zero components, an empty (non-null) enumeration is returned.
     * This starts at the root (rightmost) rdn.
     *
     * @return an enumeration of the components of this name, as Rdn
     */
    public Enumeration<RDN> getAllRdn()
    {
        /*
         * Note that by accessing the name component using the get() method on
         * the name rather than get() on the list we are reading components from
         * right to left with increasing index values. LdapName.get() does the
         * index translation on m_list for us.
         */
        return new Enumeration<RDN>()
        {
            private int pos;


            public boolean hasMoreElements()
            {
                return pos < rdns.size();
            }


            public RDN nextElement()
            {
                if ( pos >= rdns.size() )
                {
                    LOG.error( I18n.err( I18n.ERR_04205 ) );
                    throw new NoSuchElementException();
                }

                RDN rdn = rdns.get( rdns.size() - pos - 1 );
                pos++;
                return rdn;
            }
        };
    }


    /**
     * {@inheritDoc}
     */
    public DN getPrefix( int posn )
    {
        if ( rdns.size() == 0 )
        {
            return EMPTY_DN;
        }

        if ( ( posn < 0 ) || ( posn > rdns.size() ) )
        {
            String message = I18n.err( I18n.ERR_04206, posn, rdns.size() );
            LOG.error( message );
            throw new ArrayIndexOutOfBoundsException( message );
        }

        DN newDN = new DN();

        for ( int i = rdns.size() - posn; i < rdns.size(); i++ )
        {
            // Don't forget to clone the rdns !
            newDN.rdns.add( ( RDN ) rdns.get( i ).clone() );
        }

        newDN.normName = newDN.toNormName();
        newDN.upName = getUpNamePrefix( posn );

        return newDN;
    }


    /**
     * {@inheritDoc}
     */
    public DN getSuffix( int posn )
    {
        if ( rdns.size() == 0 )
        {
            return EMPTY_DN;
        }

        if ( ( posn < 0 ) || ( posn > rdns.size() ) )
        {
            String message = I18n.err( I18n.ERR_04206, posn, rdns.size() );
            LOG.error( message );
            throw new ArrayIndexOutOfBoundsException( message );
        }

        DN newDN = new DN();

        for ( int i = 0; i < size() - posn; i++ )
        {
            // Don't forget to clone the rdns !
            newDN.rdns.add( ( RDN ) rdns.get( i ).clone() );
        }

        newDN.normName = newDN.toNormName();
        newDN.upName = getUpNameSuffix( posn );

        return newDN;
    }


    /**
     * Adds the components of a name -- in order -- at a specified position
     * within this name. Components of this name at or after the index of the
     * first new component are shifted up (away from 0) to accommodate the new
     * components. Compoenents are supposed to be normalized.
     *
     * @param posn the index in this name at which to add the new components.
     *            Must be in the range [0,size()]. Note this is from the opposite end as rnds.get(posn)
     * @param name the components to add
     * @return the updated name (not a new one)
     * @throws ArrayIndexOutOfBoundsException
     *             if posn is outside the specified range
     * @throws LdapInvalidDnException
     *             if <tt>n</tt> is not a valid name, or if the addition of
     *             the components would violate the syntax rules of this name
     */
    public DN addAllNormalized( int posn, DN name ) throws LdapInvalidDnException
    {
        if ( name instanceof DN )
        {
            DN dn = (DN)name;
            
            if ( ( dn == null ) || ( dn.size() == 0 ) )
            {
                return this;
            }

            // Concatenate the rdns
            rdns.addAll( size() - posn, dn.rdns );

            if ( StringTools.isEmpty( normName ) )
            {
                normName = dn.normName;
                bytes = dn.bytes;
                upName = dn.upName;
            }
            else
            {
                normName = dn.normName + "," + normName;
                bytes = StringTools.getBytesUtf8( normName );
                upName = dn.upName + "," + upName;
            }
        }
        else
        {
            if ( ( name == null ) || ( name.size() == 0 ) )
            {
                return this;
            }

            for ( int i = name.size() - 1; i >= 0; i-- )
            {
                RDN rdn = new RDN( name.get( i ) );
                rdns.add( size() - posn, rdn );
            }

            normalizeInternal();
            toUpName();
        }

        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DN addAll( DN suffix ) throws LdapInvalidDnException
    {
        addAll( rdns.size(), suffix );
        normalizeInternal();
        toUpName();

        return this;
    }


    /**
     * {@inheritDoc}
     */
    public DN addAll( int posn, Name name ) throws InvalidNameException, LdapInvalidDnException
    {
        if ( ( name == null ) || ( name.size() == 0 ) )
        {
            return this;
        }

        for ( int i = name.size() - 1; i >= 0; i-- )
        {
            RDN rdn = new RDN( name.get( i ) );
            rdns.add( size() - posn, rdn );
        }

        normalizeInternal();
        toUpName();

        return this;
    }

    
    /**
     * {@inheritDoc}
     */
    public DN addAll( int posn, DN dn ) throws LdapInvalidDnException
    {
        if ( ( dn == null ) || ( dn.size() == 0 ) )
        {
            return this;
        }

        // Concatenate the rdns
        rdns.addAll( size() - posn, dn.rdns );

        // Regenerate the normalized name and the original string
        if ( this.isNormalized() && dn.isNormalized() )
        {
            if ( this.size() != 0 )
            {
                normName = dn.getNormName() + "," + normName;
                bytes = StringTools.getBytesUtf8( normName );
                upName = dn.getName() + "," + upName;
            }
        }
        else
        {
            normalizeInternal();
            toUpName();
        }

        return this;
    }


    /**
     * {@inheritDoc}
     */
    public DN add( String comp ) throws LdapInvalidDnException
    {
        if ( comp.length() == 0 )
        {
            return this;
        }

        //FIXME this try-catch block is for the time being, during removal of
        // java.naming.Name we have to remove this
        try
        {
            // We have to parse the nameComponent which is given as an argument
            RDN newRdn = new RDN( comp );
            
            rdns.add( 0, newRdn );
        }
        catch( LdapInvalidDnException le )
        {
            throw new LdapInvalidDnException( le.getMessage() );
        }
        
        normalizeInternal();
        toUpName();

        return this;
    }


    /**
     * Adds a single RDN to the (leaf) end of this name.
     *
     * @param newRdn the RDN to add
     * @return the updated name (not a new one)
     */
    public DN add( RDN newRdn )
    {
        rdns.add( 0, newRdn );
        
        normalizeInternal();
        toUpName();

        return this;
    }


    /**
     * Adds a single RDN to a specific position.
     *
     * @param newRdn the RDN to add
     * @param pos The position where we want to add the Rdn
     * @return the updated name (not a new one)
     */
    public DN add( int pos, RDN newRdn )
    {
        rdns.add( newRdn );
        
        normalizeInternal();
        toUpName();

        return this;
    }


    /**
     * Adds a single normalized RDN to the (leaf) end of this name.
     *
     * @param newRdn the RDN to add
     * @return the updated name (not a new one)
     */
    public DN addNormalized( RDN newRdn )
    {
        rdns.add( 0, newRdn );
        
        // Avoid a call to the toNormName() method which
        // will iterate through all the rdns, when we only
        // have to build a new normName by using the current
        // RDN normalized name. The very same for upName.
        if (rdns.size() == 1 )
        {
            normName = newRdn.getNormName();
            upName = newRdn.getName();
        }
        else
        {
            normName = newRdn + "," + normName;
            upName = newRdn.getName() + "," + upName;
        }
        
        bytes = StringTools.getBytesUtf8( normName );

        return this;
    }


    /**
     * {@inheritDoc}
     */
    public DN add( int posn, String comp ) throws LdapInvalidDnException
    {
        if ( ( posn < 0 ) || ( posn > size() ) )
        {
            String message = I18n.err( I18n.ERR_04206, posn, rdns.size() );
            LOG.error( message );
            throw new ArrayIndexOutOfBoundsException( message );
        }

        //FIXME this try-catch block is for the time being, during removal of
        // java.naming.Name we have to remove this
        try
        {
            // We have to parse the nameComponent which is given as an argument
            RDN newRdn = new RDN( comp );
            
            int realPos = size() - posn;
            rdns.add( realPos, newRdn );
        }
        catch( LdapInvalidDnException le )
        {
            throw new LdapInvalidDnException( le.getMessage() );
        }

        normalizeInternal();
        toUpName();

        return this;
    }


    /**
     * {@inheritDoc}
     */
    public RDN remove( int posn ) throws LdapInvalidDnException
    {
        if ( rdns.size() == 0 )
        {
            return RDN.EMPTY_RDN;
        }

        if ( ( posn < 0 ) || ( posn >= rdns.size() ) )
        {
            String message = I18n.err( I18n.ERR_04206, posn, rdns.size() );
            LOG.error( message );
            throw new ArrayIndexOutOfBoundsException( message );
        }

        int realPos = size() - posn - 1;
        RDN rdn = rdns.remove( realPos );

        normalizeInternal();
        toUpName();

        return rdn;
    }


    /**
     * {@inheritDoc}
     */
    public Object clone()
    {
        try
        {
            DN dn = ( DN ) super.clone();
            dn.rdns = new ArrayList<RDN>();

            for ( RDN rdn : rdns )
            {
                dn.rdns.add( ( RDN ) rdn.clone() );
            }

            return dn;
        }
        catch ( CloneNotSupportedException cnse )
        {
            LOG.error( I18n.err( I18n.ERR_04207 ) );
            throw new Error( I18n.err( I18n.ERR_04208 ) );
        }
    }


    /**
     * @see java.lang.Object#equals(java.lang.Object)
     * @return <code>true</code> if the two instances are equals
     */
    public boolean equals( Object obj )
    {
        if ( obj instanceof String )
        {
            return normName.equals( obj );
        }
        else if ( obj instanceof DN )
        {
            DN name = ( DN ) obj;

            if ( name.size() != this.size() )
            {
                return false;
            }

            for ( int i = 0; i < this.size(); i++ )
            {
                if ( name.rdns.get( i ).compareTo( rdns.get( i ) ) != 0 )
                {
                    return false;
                }
            }

            // All components matched so we return true
            return true;
        }
        else
        {
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public int compareTo( DN dn )
    {
        if ( dn.size() != size() )
        {
            return size() - dn.size();
        }

        for ( int i = rdns.size(); i > 0; i-- )
        {
            RDN rdn1 = rdns.get( i - 1 );
            RDN rdn2 = dn.rdns.get( i - 1 );
            int res = rdn1.compareTo( rdn2 );

            if ( res != 0 )
            {
                return res;
            }
        }

        return EQUAL;
    }


    private static AVA atavOidToName( AVA atav, Map<String, OidNormalizer> oidsMap )
        throws LdapInvalidDnException
    {
        String type = StringTools.trim( atav.getNormType() );

        if ( ( type.startsWith( "oid." ) ) || ( type.startsWith( "OID." ) ) )
        {
            type = type.substring( 4 );
        }

        if ( StringTools.isNotEmpty( type ) )
        {
            if ( oidsMap == null )
            {
                return atav;
            }
            else
            {
                OidNormalizer oidNormalizer = oidsMap.get( type.toLowerCase() );

                if ( oidNormalizer != null )
                {
                    try
                    {
                    return new AVA( 
                        atav.getUpType(), 
                        oidNormalizer.getAttributeTypeOid(), 
                        atav.getUpValue(),
                            oidNormalizer.getNormalizer().normalize( atav.getNormValue() ),
                        atav.getUpName() );
                    }
                    catch ( LdapException le )
                    {
                        throw new LdapInvalidDnException( le.getMessage() );
                    }
                }
                else
                {
                    // We don't have a normalizer for this OID : just do nothing.
                    return atav;
                }
            }
        }
        else
        {
            // The type is empty : this is not possible...
            LOG.error( I18n.err( I18n.ERR_04209 ) );
            throw new LdapInvalidDnException( ResultCodeEnum.INVALID_DN_SYNTAX, I18n.err( I18n.ERR_04209 ) );
        }
    }


    /**
     * Transform a RDN by changing the value to its OID counterpart and
     * normalizing the value accordingly to its type.
     *
     * @param rdn The RDN to modify.
     * @param oidsMap The map of all existing oids and normalizer.
     * @throws LdapInvalidDnException If the RDN is invalid.
     */
    /** No qualifier */ static void rdnOidToName( RDN rdn, Map<String, OidNormalizer> oidsMap ) throws LdapInvalidDnException
    {
        if ( rdn.getNbAtavs() > 1 )
        {
            // We have more than one ATAV for this RDN. We will loop on all
            // ATAVs
            RDN rdnCopy = ( RDN ) rdn.clone();
            rdn.clear();

            for ( AVA val:rdnCopy )
            {
                AVA newAtav = atavOidToName( val, oidsMap );
                rdn.addAttributeTypeAndValue( newAtav );
            }
        }
        else
        {
            AVA val = rdn.getAtav();
            rdn.clear();
            AVA newAtav = atavOidToName( val, oidsMap );
            rdn.addAttributeTypeAndValue( newAtav );
        }
    }


    /**
     * Change the internal DN, using the OID instead of the first name or other
     * aliases. As we still have the UP name of each RDN, we will be able to
     * provide both representation of the DN. example : dn: 2.5.4.3=People,
     * dc=example, domainComponent=com will be transformed to : 2.5.4.3=People,
     * 0.9.2342.19200300.100.1.25=example, 0.9.2342.19200300.100.1.25=com 
     * because 2.5.4.3 is the OID for cn and dc is the first
     * alias of the couple of aliases (dc, domaincomponent), which OID is 
     * 0.9.2342.19200300.100.1.25. 
     * This is really important do have such a representation, as 'cn' and 
     * 'commonname' share the same OID.
     * 
     * @param dn The DN to transform.
     * @param oidsMap The mapping between names and oids.
     * @return A normalized form of the DN.
     * @throws LdapInvalidDnException If something went wrong.
     */
    public static DN normalize( DN dn, Map<String, OidNormalizer> oidsMap ) throws LdapInvalidDnException
    {
        if ( ( dn == null ) || ( dn.size() == 0 ) || ( oidsMap == null ) || ( oidsMap.size() == 0 ) )
        {
            return dn;
        }

        Enumeration<RDN> rdns = dn.getAllRdn();

        // Loop on all RDNs
        while ( rdns.hasMoreElements() )
        {
            RDN rdn = rdns.nextElement();
            String upName = rdn.getName();
            rdnOidToName( rdn, oidsMap );
            rdn.normalize();
            rdn.setUpName( upName );
        }

        dn.normalizeInternal();

        dn.normalized = true;
        return dn;
    }


    /**
     * Change the internal DN, using the OID instead of the first name or other
     * aliases. As we still have the UP name of each RDN, we will be able to
     * provide both representation of the DN. example : dn: 2.5.4.3=People,
     * dc=example, domainComponent=com will be transformed to : 2.5.4.3=People,
     * 0.9.2342.19200300.100.1.25=example, 0.9.2342.19200300.100.1.25=com 
     * because 2.5.4.3 is the OID for cn and dc is the first
     * alias of the couple of aliases (dc, domaincomponent), which OID is 
     * 0.9.2342.19200300.100.1.25. 
     * This is really important do have such a representation, as 'cn' and 
     * 'commonname' share the same OID.
     *
     * @param oidsMap The mapping between names and oids.
     * @throws LdapInvalidDnException If something went wrong.
     * @return The normalized DN
     */
    public DN normalize( Map<String, OidNormalizer> oidsMap ) throws LdapInvalidDnException
    {
        if ( ( oidsMap == null ) || ( oidsMap.size() == 0 ) )
        {
            return this;
        }

        if ( size() == 0 )
        {
            normalized = true;
            return this;
        }

        Enumeration<RDN> localRdns = getAllRdn();

        // Loop on all RDNs
        while ( localRdns.hasMoreElements() )
        {
            RDN rdn = localRdns.nextElement();
            String localUpName = rdn.getName();
            rdnOidToName( rdn, oidsMap );
            rdn.normalize();
            rdn.setUpName( localUpName );
        }

        normalizeInternal();
        normalized = true;
        return this;
    }


    /**
     * Check if a DistinguishedName is syntactically valid.
     *
     * @param dn The DN to validate
     * @return <code>true></code> if the DN is valid, <code>false</code>
     * otherwise
     */
    public static boolean isValid( String dn )
    {
        return DnParser.validateInternal( dn );
    }
    

    /**
     * Tells if the DN has already been normalized or not
     *
     * @return <code>true</code> if the DN is already normalized.
     */
    public boolean isNormalized()
    {
        return normalized;
    }


    /**
     * @see Externalizable#readExternal(ObjectInput)<p>
     * 
     * We have to store a DN data efficiently. Here is the structure :
     * 
     * <li>upName</li> The User provided DN<p>
     * <li>normName</li> May be null if the normName is equaivalent to 
     * the upName<p>
     * <li>rdns</li> The rdn's List.<p>
     * 
     * for each rdn :
     * <li>call the RDN write method</li>
     *
     *@param out The stream in which the DN will be serialized
     *@throws IOException If the serialization fail
     */
    public void writeExternal( ObjectOutput out ) throws IOException
    {
        if ( upName == null )
        {
            String message = I18n.err( I18n.ERR_04210 );
            LOG.error( message );
            throw new IOException( message );
        }
        
        // Write the UPName
        out.writeUTF( upName );
        
        // Write the NormName if different
        if ( isNormalized() )
        {
            if ( upName.equals( normName ) )
            {
                out.writeUTF( "" );
            }
            else
            {
                out.writeUTF( normName );
            }
        }
        else
        {
            String message = I18n.err( I18n.ERR_04211 );
            LOG.error( message );
            throw new IOException( message );
        }
        
        // Should we store the byte[] ???
        
        // Write the RDNs. Is it's null, the number will be -1. 
        out.writeInt( rdns.size() );

        // Loop on the RDNs
        for ( RDN rdn:rdns )
        {
            out.writeObject( rdn );
        }
    }


    /**
     * @see Externalizable#readExternal(ObjectInput)
     * 
     * We read back the data to create a new DN. The structure 
     * read is exposed in the {@link DN#writeExternal(ObjectOutput)} 
     * method<p>
     * 
     * @param in The stream from which the DN is read
     * @throws IOException If the stream can't be read
     * @throws ClassNotFoundException If the RDN can't be created 
     */
    public void readExternal( ObjectInput in ) throws IOException , ClassNotFoundException
    {
        // Read the UPName
        upName = in.readUTF();
        
        // Read the NormName
        normName = in.readUTF();
        
        if ( normName.length() == 0 )
        {
            // As the normName is equal to the upName,
            // we didn't saved the nbnormName on disk.
            // restore it by copying the upName.
            normName = upName;
        }
        
        // A serialized DN is always normalized.
        normalized = true;
            
        // Should we read the byte[] ???
        bytes = StringTools.getBytesUtf8( upName );
        
        // Read the RDNs. Is it's null, the number will be -1.
        int nbRdns = in.readInt();
        rdns = new ArrayList<RDN>( nbRdns );
        
        for ( int i = 0; i < nbRdns; i++ )
        {
            RDN rdn = (RDN)in.readObject();
            rdns.add( rdn );
        }
    }
    
    
    /**
     * Convert a {@link javax.naming.Name} to a DN
     *
     * @param name The Name to convert
     * @return A DN
     */
    public static DN fromName( Name name )
    {
        try
        {
            DN dn = new DN( name.toString() );
        
            return dn;
        }
        catch ( LdapInvalidDnException lide )
        {
            // TODO : check if we must throw an exception.
            // Logically, the Name must be valid.
            return null;
        }
    }
    
    
    /**
     * Convert a DN to a {@link javax.naming.Name}
     *
     * @param name The DN to convert
     * @return A Name
     */
    public static Name toName( DN dn )
    {
        try
        {
            Name name = new LdapName( dn.toString() );
        
            return name;
        }
        catch ( InvalidNameException ine )
        {
            // TODO : check if we must throw an exception.
            // Logically, the DN must be valid.
            return null;
        }
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<RDN> iterator()
    {
        return rdns.iterator();
    }
}

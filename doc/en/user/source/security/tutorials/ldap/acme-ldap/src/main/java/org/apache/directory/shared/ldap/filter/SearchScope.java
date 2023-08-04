/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.filter;

import org.apache.directory.shared.i18n.I18n;

/**
 * A search scope enumerated type.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum SearchScope
{
    OBJECT( 0, "base" ), 
    ONELEVEL( 1, "one" ), 
    SUBTREE( 2, "sub" );
    
    /** 
     * The corresponding LDAP scope constant value as defined in 
     * RFC 4511
     */ 
    private final int scope;
    
    /**
     * The LDAP URL string value of either base, one or sub as defined in RFC
     * 2255.
     * 
     * @see <a href="http://www.faqs.org/rfcs/rfc2255.html">RFC 2255</a>
     */
    private final String ldapUrlValue;
    

    /**
     * Creates a new instance of SearchScope based on the respective 
     * scope constant.
     *
     * @param scope the scope constant
     * @param ldapUrlValue LDAP URL scope string value: base, one, or sub
     */
    private SearchScope( int scope, String ldapUrlValue )
    {
        this.scope = scope;
        this.ldapUrlValue = ldapUrlValue;
    }

    
    /**
     * Gets the LDAP URL value for the scope: according to RFC 2255 this is 
     * either base, one, or sub.
     * 
     * @see <a href="http://www.faqs.org/rfcs/rfc2255.html">RFC 2255</a>
     */
    public String getLdapUrlValue()
    {
        return ldapUrlValue;
    }
    

    /**
     * Gets the corresponding scope constant value as defined in 
     * RFC 4511.
     * 
     * @return the scope
     */
    public int getScope()
    {
        return scope;
    }
    
    
    /**
     * Gets the SearchScope enumerated type for the corresponding 
     * scope numeric value.
     *
     * @param scope the numeric value to get SearchScope for
     * @return the SearchScope enumerated type for the scope numeric value
     */
    public static SearchScope getSearchScope( int scope )
    {
        switch( scope )
        {
            case 0 : 
                return OBJECT;
            
            case 1 :
                return ONELEVEL;
                
            case 2 :
                return SUBTREE;
                
            default:
                throw new IllegalArgumentException( I18n.err( I18n.ERR_04160, scope ) );
        }
    }


    /**
     * Gets the SearchScope enumerated type for the corresponding 
     * LDAP URL scope value of either base, one or sub.
     *
     * @param ldapUrlValue the LDAP URL scope value to get SearchScope for
     * @return the SearchScope enumerated type for the LDAP URL scope value
     */
    public static int getSearchScope( String ldapUrlValue )
    {
        if ( "base".equalsIgnoreCase( ldapUrlValue ) )
        {
            return OBJECT.getScope();
        }
        else if ( "one".equalsIgnoreCase( ldapUrlValue ) )
        {
            return ONELEVEL.getScope();
        }
        else if ( "sub".equalsIgnoreCase( ldapUrlValue ) )
        {
            return SUBTREE.getScope();
        }
        else
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04161, ldapUrlValue ) );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return ldapUrlValue;
    }
}

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
package org.apache.directory.shared.ldap.aci;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;


/**
 * A flatten entity which is converted from an {@link ACIItem}. The tuples are
 * accepted by ACDF (Access Control Decision Function, 18.8, X.501)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $
 */
public class ACITuple implements Serializable
{
    private static final long serialVersionUID = 4353150626941232371L;

    private final Collection<UserClass> userClasses;

    private final AuthenticationLevel authenticationLevel;

    private final Collection<ProtectedItem> protectedItems;

    private final Set<MicroOperation> microOperations;

    private final boolean grant;

    private final int precedence;


    /**
     * Creates a new instance.
     * 
     * @param userClasses
     *            the collection of {@link UserClass}es this tuple relates to
     * @param authenticationLevel
     *            the level of authentication required
     * @param protectedItems
     *            the collection of {@link ProtectedItem}s this tuple relates
     * @param microOperations
     *            the set of {@link MicroOperation}s this tuple relates
     * @param grant
     *            <tt>true</tt> if and only if this tuple grants an access
     * @param precedence
     *            the precedence of this tuple (<tt>0</tt>-<tt>255</tt>)
     */
    public ACITuple( 
            Collection<UserClass> userClasses, 
            AuthenticationLevel authenticationLevel, 
            Collection<ProtectedItem> protectedItems,
            Set<MicroOperation> microOperations, 
            boolean grant, 
            int precedence )
    {
        if ( authenticationLevel == null )
        {
            throw new NullPointerException( I18n.err( I18n.ERR_04003) );
        }

        if ( precedence < 0 || precedence > 255 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04002, precedence ) );
        }

        this.userClasses = Collections.unmodifiableCollection( new ArrayList<UserClass>( userClasses ) );
        this.authenticationLevel = authenticationLevel;
        this.protectedItems = Collections.unmodifiableCollection( new ArrayList<ProtectedItem>( protectedItems ) );
        this.microOperations = Collections.unmodifiableSet( new HashSet<MicroOperation>( microOperations ) );
        this.grant = grant;
        this.precedence = precedence;
    }


    /**
     * Returns the collection of {@link UserClass}es this tuple relates to.
     */
    public Collection<UserClass> getUserClasses()
    {
        return userClasses;
    }


    /**
     * Returns the level of authentication required.
     */
    public AuthenticationLevel getAuthenticationLevel()
    {
        return authenticationLevel;
    }


    /**
     * Returns the collection of {@link ProtectedItem}s this tuple relates.
     */
    public Collection<ProtectedItem> getProtectedItems()
    {
        return protectedItems;
    }


    /**
     * Returns the set of {@link MicroOperation}s this tuple relates.
     */
    public Set<MicroOperation> getMicroOperations()
    {
        return microOperations;
    }


    /**
     * Returns <tt>true</tt> if and only if this tuple grants an access.
     */
    public boolean isGrant()
    {
        return grant;
    }


    /**
     * Returns the precedence of this tuple (<tt>0</tt>-<tt>255</tt>).
     */
    public int getPrecedence()
    {
        return precedence;
    }


    public String toString()
    {
        return "ACITuple: userClasses=" + userClasses + ", " + "authenticationLevel=" + authenticationLevel + ", "
            + "protectedItems=" + protectedItems + ", " + ( grant ? "grants=" : "denials=" ) + microOperations + ", "
            + "precedence=" + precedence;
    }
}

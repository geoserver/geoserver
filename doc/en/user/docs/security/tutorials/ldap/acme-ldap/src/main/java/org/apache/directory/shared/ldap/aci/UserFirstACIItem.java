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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.directory.shared.ldap.constants.AuthenticationLevel;


/**
 * An {@link ACIItem} which specifies {@link UserClass}es first and then
 * {@link ProtectedItem}s each {@link UserClass} will have. (18.4.2.4. X.501)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664290 $, $Date: 2008-06-07 09:28:06 +0300 (Sat, 07 Jun 2008) $
 */
public class UserFirstACIItem extends ACIItem
{
    private static final long serialVersionUID = 5587483838404246148L;

    private final Collection<UserClass> userClasses;

    private final Collection<UserPermission> userPermissions;


    /**
     * Creates a new instance.
     * 
     * @param identificationTag
     *            the id string of this item
     * @param precedence
     *            the precedence of this item
     * @param authenticationLevel
     *            the level of authentication required to this item
     * @param userClasses
     *            the collection of {@link UserClass}es this item protects
     * @param userPermissions
     *            the collection of {@link UserPermission}s each
     *            <tt>protectedItems</tt> will have
     */
    public UserFirstACIItem(String identificationTag, int precedence, AuthenticationLevel authenticationLevel,
        Collection<UserClass> userClasses, Collection<UserPermission> userPermissions)
    {
        super( identificationTag, precedence, authenticationLevel );

        this.userClasses = Collections.unmodifiableCollection( new ArrayList<UserClass>( userClasses ) );
        this.userPermissions = Collections.unmodifiableCollection( new ArrayList<UserPermission>( userPermissions ) );
    }


    /**
     * Returns the set of {@link UserClass}es.
     */
    public Collection<UserClass> getUserClasses()
    {
        return userClasses;
    }


    /**
     * Returns the set of {@link UserPermission}s.
     */
    public Collection<UserPermission> getUserPermission()
    {
        return userPermissions;
    }


    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        
        // identificationTag
        buf.append( "{ identificationTag \"" );
        buf.append( getIdentificationTag() );
        buf.append( "\", " );
        
        // precedence
        buf.append( "precedence " );
        buf.append( getPrecedence() );
        buf.append( ", " );
        
        // authenticationLevel
        buf.append( "authenticationLevel " );
        buf.append( getAuthenticationLevel().getName() );
        buf.append( ", " );
        
        // itemOrUserFirst
        buf.append( "itemOrUserFirst userFirst: { " );
        
        // protectedItems
        buf.append( "userClasses { " );

        boolean isFirst = true;
        
        for ( UserClass userClass:userClasses )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                buf.append( ", " );
            }
            
            buf.append( userClass.toString() );
        }

        buf.append( " }, " );
        
        // itemPermissions
        buf.append( "userPermissions { " );

        isFirst = true;
        
        for ( UserPermission permission:userPermissions )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                buf.append( ", " );
            }
            
            buf.append( permission.toString() );
        }
        
        buf.append( " } } }" );

        return buf.toString();
    }


    public Collection<ACITuple> toTuples()
    {
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();

        for ( UserPermission userPermission:userPermissions )
        {
            Set<GrantAndDenial> grants = userPermission.getGrants();
            Set<GrantAndDenial> denials = userPermission.getDenials();
            int precedence = userPermission.getPrecedence() >= 0 ? userPermission.getPrecedence() : this
                .getPrecedence();

            if ( grants.size() > 0 )
            {
                tuples.add( new ACITuple( getUserClasses(), getAuthenticationLevel(), userPermission
                    .getProtectedItems(), toMicroOperations( grants ), true, precedence ) );
            }
            if ( denials.size() > 0 )
            {
                tuples.add( new ACITuple( getUserClasses(), getAuthenticationLevel(), userPermission
                    .getProtectedItems(), toMicroOperations( denials ), false, precedence ) );
            }
        }
        return tuples;
    }
}

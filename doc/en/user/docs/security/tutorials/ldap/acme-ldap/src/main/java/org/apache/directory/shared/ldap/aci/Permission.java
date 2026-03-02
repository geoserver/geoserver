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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/**
 * An abstract base class for {@link ItemPermission} and {@link UserPermission}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 572194 $, $Date: 2007-09-03 02:58:18 +0300 (Mon, 03 Sep 2007) $
 */
public abstract class Permission implements Serializable
{
    private final int precedence;

    private final Set<GrantAndDenial> grantsAndDenials;

    private final Set<GrantAndDenial> grants;

    private final Set<GrantAndDenial> denials;


    /**
     * Creates a new instance
     * 
     * @param precedence
     *            the precedence of this permission (<tt>-1</tt> to use the
     *            default)
     * @param grantsAndDenials
     *            the set of {@link GrantAndDenial}s
     */
    protected Permission(int precedence, Collection<GrantAndDenial> grantsAndDenials)
    {
        if ( precedence < 0 || precedence > 255 )
        {
            precedence = -1;
        }

        this.precedence = precedence;

        Set<GrantAndDenial> tmpGrantsAndDenials = new HashSet<GrantAndDenial>();
        Set<GrantAndDenial> tmpGrants = new HashSet<GrantAndDenial>();
        Set<GrantAndDenial> tmpDenials = new HashSet<GrantAndDenial>();
        
        for ( GrantAndDenial gad:grantsAndDenials )
        {
            if ( gad.isGrant() )
            {
                tmpGrants.add( gad );
            }
            else
            {
                tmpDenials.add( gad );
            }

            tmpGrantsAndDenials.add( gad );
        }

        this.grants = Collections.unmodifiableSet( tmpGrants );
        this.denials = Collections.unmodifiableSet( tmpDenials );
        this.grantsAndDenials = Collections.unmodifiableSet( tmpGrantsAndDenials );
    }


    /**
     * Returns the precedence of this permission.
     */
    public int getPrecedence()
    {
        return precedence;
    }


    /**
     * Returns the set of {@link GrantAndDenial}s.
     */
    public Set<GrantAndDenial> getGrantsAndDenials()
    {
        return grantsAndDenials;
    }


    /**
     * Returns the set of grants only.
     */
    public Set<GrantAndDenial> getGrants()
    {
        return grants;
    }


    /**
     * Returns the set of denials only.
     */
    public Set<GrantAndDenial> getDenials()
    {
        return denials;
    }
}

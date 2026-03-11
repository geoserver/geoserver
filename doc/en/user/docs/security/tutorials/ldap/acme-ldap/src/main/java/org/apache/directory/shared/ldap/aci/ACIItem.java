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
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;


/**
 * An abstract class that provides common properties and operations for
 * {@link ItemFirstACIItem} and {@link UserFirstACIItem} as specified X.501
 * specification.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $
 */
public abstract class ACIItem implements Serializable
{
    private String identificationTag;

    /* 0 ~ 255 */
    private int precedence = 0;

    private AuthenticationLevel authenticationLevel;


    /**
     * Creates a new instance
     * 
     * @param identificationTag
     *            the id string of this item
     * @param precedence
     *            the precedence of this item
     * @param authenticationLevel
     *            the level of authentication required to this item
     */
    protected ACIItem(String identificationTag, int precedence, AuthenticationLevel authenticationLevel)
    {
        if ( identificationTag == null )
        {
            throw new NullPointerException( I18n.err( I18n.ERR_04001 ) );
        }
        if ( precedence < 0 || precedence > 255 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04002, precedence ) );
        }
        if ( authenticationLevel == null )
        {
            throw new NullPointerException( I18n.err( I18n.ERR_04003 ) );
        }

        this.identificationTag = identificationTag;
        this.precedence = precedence;
        this.authenticationLevel = authenticationLevel;
    }


    /**
     * Returns the id string of this item.
     */
    public String getIdentificationTag()
    {
        return identificationTag;
    }


    /**
     * Returns the precedence of this item.
     */
    public int getPrecedence()
    {
        return precedence;
    }


    /**
     * Returns the level of authentication required to this item.
     */
    public AuthenticationLevel getAuthenticationLevel()
    {
        return authenticationLevel;
    }


    /**
     * Converts this item into a collection of {@link ACITuple}s and returns
     * it.
     */
    public abstract Collection<ACITuple> toTuples();


    /**
     * Converts a set of {@link GrantAndDenial}s into a set of
     * {@link MicroOperation}s and returns it.
     */
    protected static Set<MicroOperation> toMicroOperations( Set<GrantAndDenial> grantsAndDenials )
    {
        Set<MicroOperation> microOps = new HashSet<MicroOperation>();
        
        for ( GrantAndDenial grantAndDenial:grantsAndDenials )
        {
            microOps.add( grantAndDenial.getMicroOperation() );
        }
        
        return microOps;
    }
}

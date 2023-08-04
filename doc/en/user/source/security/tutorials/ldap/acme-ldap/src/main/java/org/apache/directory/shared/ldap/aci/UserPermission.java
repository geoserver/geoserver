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


import java.util.Collection;
import java.util.Collections;


/**
 * Represents permissions to be applied to all {@link UserClass}es in
 * {@link UserFirstACIItem}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664290 $, $Date: 2008-06-07 09:28:06 +0300 (Sat, 07 Jun 2008) $
 */
public class UserPermission extends Permission
{
    private static final long serialVersionUID = 3940100745409337694L;

    private final Collection<ProtectedItem> protectedItems;


    /**
     * Creates a new instance
     * 
     * @param precedence
     *            the precedence of this permission (<tt>-1</tt> to use the
     *            default)
     * @param grantsAndDenials
     *            the set of {@link GrantAndDenial}s
     * @param protectedItems
     *            the collection of {@link ProtectedItem}s
     */
    public UserPermission( int precedence, Collection<GrantAndDenial> grantsAndDenials, Collection<ProtectedItem> protectedItems )
    {
        super( precedence, grantsAndDenials );

        this.protectedItems = Collections.unmodifiableCollection( protectedItems );
    }


    /**
     * Returns the collection of {@link ProtectedItem}s.
     */
    public Collection<ProtectedItem> getProtectedItems()
    {
        return protectedItems;
    }


    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        
        buf.append( "{ " );

        if ( getPrecedence() >= 0 && getPrecedence() <= 255 )
        {
            buf.append( "precedence " );
            buf.append( getPrecedence() );
            buf.append( ", " );
        }
        
        buf.append( "protectedItems { " );
        
        boolean isFirst = true;
        
        for ( ProtectedItem item:protectedItems )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                buf.append( ", " );
            }
            
            buf.append( item.toString() );
        }
        
        buf.append( " }, grantsAndDenials { " );

        isFirst = true;
        
        for ( GrantAndDenial grantAndDenial:getGrantsAndDenials() )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                buf.append( ", " );
            }

            buf.append( grantAndDenial.toString() );
        }
        
        buf.append( " } }" );
        
        return buf.toString();
    }
}

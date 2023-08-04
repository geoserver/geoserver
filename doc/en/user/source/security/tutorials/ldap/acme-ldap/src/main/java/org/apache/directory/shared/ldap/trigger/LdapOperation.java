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

package org.apache.directory.shared.ldap.trigger;


/**
 * An enumeration that represents change inducing LDAP operations.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public class LdapOperation
{
    public static final LdapOperation MODIFY = new LdapOperation( "Modify" );

    public static final LdapOperation ADD = new LdapOperation( "Add" );

    public static final LdapOperation DELETE = new LdapOperation( "Delete" );

    public static final LdapOperation MODIFYDN = new LdapOperation( "ModifyDN" );
    
    public static final LdapOperation MODIFYDN_RENAME = new LdapOperation( "ModifyDN.Rename" );
    
    public static final LdapOperation MODIFYDN_EXPORT = new LdapOperation( "ModifyDN.Export" );
    
    public static final LdapOperation MODIFYDN_IMPORT = new LdapOperation( "ModifyDN.Import" );

    
    private final String name;


    private LdapOperation( String name )
    {
        this.name = name;
    }


    /**
     * Returns the name of this LDAP operation.
     */
    public String getName()
    {
        return name;
    }


    public String toString()
    {
        return name;
    }


    /**
     * @see java.lang.Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int h = 37;

        h = h*17 + ( ( name == null ) ? 0 : name.hashCode() );
        
        return h;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        
        if ( ! ( obj  instanceof LdapOperation ) )
        {
            return false;
        }

        final LdapOperation other = ( LdapOperation ) obj;

        if ( name == null )
        {
            if ( other.name != null )
            {
                return false;
            }
            else
            { 
                return true;
            }
        }
        else 
        {
            return name.equals( other.name );
        }
    }
}

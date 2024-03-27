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

package org.apache.directory.shared.ldap.schema.syntaxCheckers;


/**
 * An OpenLDAP object identifier macro. 
 * See http://www.openldap.org/doc/admin24/schema.html#OID%20Macros
 * <br/>
 * <code>objectIdentifier &lt;name&gt; { &lt;oid&gt; | &lt;name&gt;[:&lt;suffix&gt;] }</code>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class OpenLdapObjectIdentifierMacro
{
    private String name;

    private String rawOidOrNameSuffix;

    private String resolvedOid;


    /**
     * Instantiates a new OpenLDAP object identifier macro.
     */
    public OpenLdapObjectIdentifierMacro()
    {
        name = null;
        rawOidOrNameSuffix = null;
        resolvedOid = null;
    }


    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName()
    {
        return name;
    }


    /**
     * Sets the name.
     * 
     * @param name the new name
     */
    public void setName( String name )
    {
        this.name = name;
    }


    /**
     * Gets the raw OID or name plus suffix.
     * 
     * @return the raw OID or name plus suffix
     */
    public String getRawOidOrNameSuffix()
    {
        return rawOidOrNameSuffix;
    }


    /**
     * Sets the raw OID or name plus suffix.
     * 
     * @param rawOidOrNameSuffix the new raw OID or name plus suffix
     */
    public void setRawOidOrNameSuffix( String rawOidOrNameSuffix )
    {
        this.rawOidOrNameSuffix = rawOidOrNameSuffix;
    }


    /**
     * Gets the resolved OID, null if not yet resolved.
     * 
     * @return the resolved OID
     */
    public String getResolvedOid()
    {
        return resolvedOid;
    }


    /**
     * Checks if is resolved.
     * 
     * @return true, if is resolved
     */
    public boolean isResolved()
    {
        return getResolvedOid() != null;
    }


    /**
     * Sets the resolved OID.
     * 
     * @param resolvedOid the new resolved OID
     */
    public void setResolvedOid( String resolvedOid )
    {
        this.resolvedOid = resolvedOid;
    }


    public String toString()
    {
        if ( isResolved() )
        {
            return "resolved: " + name + " " + resolvedOid;
        }
        else
        {
            return "unresolved: " + name + " " + rawOidOrNameSuffix;
        }
    }

}
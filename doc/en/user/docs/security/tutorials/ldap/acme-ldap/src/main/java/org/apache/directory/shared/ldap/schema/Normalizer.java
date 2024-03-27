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
package org.apache.directory.shared.ldap.schema;


import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;


/**
 * Converts attribute values to a canonical form.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 929091 $
 */
public abstract class Normalizer extends LoadableSchemaObject
{
    /** The serialversionUID */
    private static final long serialVersionUID = 1L;


    /**
     * The Normalizer base constructor. We use it's MR OID to
     * initialize the SchemaObject instance
     * 
     * @param oid The associated OID. It's the element's MR OID
     */
    protected Normalizer( String oid )
    {
        super( SchemaObjectType.NORMALIZER, oid );
    }


    /**
     * Use this default constructor when the Normalizer must be instantiated
     * before setting the OID.
     */
    protected Normalizer()
    {
        super( SchemaObjectType.NORMALIZER );
    }


    /**
     * Gets the normalized value.
     * 
     * @param value the value to normalize. It must *not* be null !
     * @return the normalized form for a value
     * @throws LdapException if an error results during normalization
     */
    public abstract Value<?> normalize( Value<?> value ) throws LdapException;


    /**
     * Gets the normalized value.
     * 
     * @param value the value to normalize. It must *not* be null !
     * @return the normalized form for a value
     * @throws LdapException if an error results during normalization
     */
    public abstract String normalize( String value ) throws LdapException;


    /**
     * Store the SchemaManager in this instance. It may be necessary for some
     * normalizer which needs to have access to the oidNormalizer Map.
     *
     * @param schemaManager the schemaManager to store
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        // Do nothing (general case).
    }


    /**
     * @see Object#equals()
     */
    public boolean equals( Object o )
    {
        if ( !super.equals( o ) )
        {
            return false;
        }

        return o instanceof Normalizer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return objectType + " " + DescriptionUtils.getDescription( this );
    }
}

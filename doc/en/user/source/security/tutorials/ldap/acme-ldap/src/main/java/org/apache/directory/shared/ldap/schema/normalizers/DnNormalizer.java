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
package org.apache.directory.shared.ldap.schema.normalizers;


import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * Normalizer a DN
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DnNormalizer extends Normalizer
{
    // The serial UID
    private static final long serialVersionUID = 1L;
    
    /** A reference to the schema manager used to normalize the DN */
    private SchemaManager schemaManager;
    
    /**
     * Empty constructor
     */
    public DnNormalizer()
    {
        super( SchemaConstants.DISTINGUISHED_NAME_MATCH_MR_OID );
    }


    /**
     * {@inheritDoc}
     */
    public Value<?> normalize( Value<?> value ) throws LdapException
    {
        DN dn = null;
        
        String dnStr = value.getString();
        
        dn = new DN( dnStr );
        
        dn.normalize( schemaManager.getNormalizerMapping() );
        return new StringValue( dn.getNormName() );
    }


    /**
     * {@inheritDoc}
     */
    public String normalize( String value ) throws LdapException
    {
        DN dn = null;
        
        dn = new DN( value );
        
        dn.normalize( schemaManager.getNormalizerMapping() );
        return dn.getNormName();
    }


    /**
     * Normalize a DN
     * @param value The DN to normalize
     * @return A normalized DN
     * @throws LdapException
     */
    public String normalize( DN value ) throws LdapException
    {
        DN dn = null;
        
        dn = new DN( value );
        
        dn.normalize( schemaManager.getNormalizerMapping() );
        return dn.getNormName();
    }


    /**
     * {@inheritDoc}
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }
}

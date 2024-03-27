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
package org.apache.directory.shared.ldap.schema.registries;

import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;


/**
 * A LdapSyntax registry's service default implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 828111 $
 */
public class DefaultLdapSyntaxRegistry extends DefaultSchemaObjectRegistry<LdapSyntax>
    implements LdapSyntaxRegistry
{
    /**
     * Creates a new default LdapSyntaxRegistry instance.
     */
    public DefaultLdapSyntaxRegistry()
    {
        super( SchemaObjectType.LDAP_SYNTAX, new OidRegistry() );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public DefaultLdapSyntaxRegistry copy()
    {
        DefaultLdapSyntaxRegistry copy = new DefaultLdapSyntaxRegistry();
        
        // Copy the base data
        copy.copy( this );
        
        return copy;
    }
}

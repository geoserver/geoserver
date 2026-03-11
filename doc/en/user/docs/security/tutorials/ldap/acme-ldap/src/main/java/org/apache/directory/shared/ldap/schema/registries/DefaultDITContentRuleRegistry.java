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


import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;


/**
 * An DITContentRule registry's service default implementation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 828111 $
 */
public class DefaultDITContentRuleRegistry extends DefaultSchemaObjectRegistry<DITContentRule>
    implements DITContentRuleRegistry
{
    /**
     * Creates a new default DITContentRuleRegistry instance.
     */
    public DefaultDITContentRuleRegistry()
    {
        super( SchemaObjectType.DIT_CONTENT_RULE, new OidRegistry() );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public DefaultDITContentRuleRegistry copy()
    {
        DefaultDITContentRuleRegistry copy = new DefaultDITContentRuleRegistry();
        
        // Copy the base data
        copy.copy( this );

        return copy;
    }
}

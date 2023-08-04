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


import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;


/**
 * A MatchingRuleUse registry service default implementation. 
 * MatchingRuleUse objects are special in that they do not have unique OID's 
 * specifically assigned to them. Their OID is really the OID of the MatchingRule 
 * they refer to.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 828111 $
 */
public class DefaultMatchingRuleUseRegistry extends DefaultSchemaObjectRegistry<MatchingRuleUse>
    implements MatchingRuleUseRegistry
{
    /**
     * Creates a new default MatchingRuleUseRegistry instance.
     */
    public DefaultMatchingRuleUseRegistry()
    {
        super( SchemaObjectType.MATCHING_RULE_USE, new OidRegistry() );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public DefaultMatchingRuleUseRegistry copy()
    {
        DefaultMatchingRuleUseRegistry copy = new DefaultMatchingRuleUseRegistry();
        
        // Copy the base data
        copy.copy( this );
        
        return copy;
    }
}

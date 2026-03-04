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


import java.util.Iterator;

import org.apache.directory.shared.ldap.exception.LdapException;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;


/**
 * An immutable wrapper of the DITContentRule registry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 828111 $
 */
public class ImmutableDITContentRuleRegistry implements DITContentRuleRegistry
{
    /** The wrapped DITContentRule registry */
    DITContentRuleRegistry immutableDITContentRuleRegistry;


    /**
     * Creates a new instance of ImmutableDITContentRuleRegistry.
     *
     * @param ditContentRuleRegistry The wrapped DITContentRule registry
     */
    public ImmutableDITContentRuleRegistry( DITContentRuleRegistry ditContentRuleRegistry )
    {
        immutableDITContentRuleRegistry = ditContentRuleRegistry;
    }


    /**
     * {@inheritDoc}
     */
    public ImmutableDITContentRuleRegistry copy()
    {
        return ( ImmutableDITContentRuleRegistry ) immutableDITContentRuleRegistry.copy();
    }


    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return immutableDITContentRuleRegistry.size();
    }


    /**
     * {@inheritDoc}
     */
    public boolean contains( String oid )
    {
        return immutableDITContentRuleRegistry.contains( oid );
    }


    /**
     * {@inheritDoc}
     */
    public String getOidByName( String name ) throws LdapException
    {
        return immutableDITContentRuleRegistry.getOidByName( name );
    }


    /**
     * {@inheritDoc}
     */
    public String getSchemaName( String oid ) throws LdapException
    {
        return immutableDITContentRuleRegistry.getSchemaName( oid );
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObjectType getType()
    {
        return immutableDITContentRuleRegistry.getType();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<DITContentRule> iterator()
    {
        return immutableDITContentRuleRegistry.iterator();
    }


    /**
     * {@inheritDoc}
     */
    public DITContentRule lookup( String oid ) throws LdapException
    {
        return immutableDITContentRuleRegistry.lookup( oid );
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<String> oidsIterator()
    {
        return immutableDITContentRuleRegistry.oidsIterator();
    }


    /**
     * {@inheritDoc}
     */
    public void register( DITContentRule schemaObject ) throws LdapException
    {
        throw new LdapUnwillingToPerformException( ResultCodeEnum.NO_SUCH_OPERATION, I18n.err( I18n.ERR_04277 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void renameSchema( String originalSchemaName, String newSchemaName ) throws LdapException
    {
        throw new LdapUnwillingToPerformException( ResultCodeEnum.NO_SUCH_OPERATION, I18n.err( I18n.ERR_04277 ) );
    }


    /**
     * {@inheritDoc}
     */
    public DITContentRule unregister( String numericOid ) throws LdapException
    {
        throw new LdapUnwillingToPerformException( ResultCodeEnum.NO_SUCH_OPERATION, I18n.err( I18n.ERR_04277 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void unregisterSchemaElements( String schemaName ) throws LdapException
    {
        throw new LdapUnwillingToPerformException( ResultCodeEnum.NO_SUCH_OPERATION, I18n.err( I18n.ERR_04277 ) );
    }


    /**
     * {@inheritDoc}
     */
    public SchemaObject get( String oid )
    {
        return immutableDITContentRuleRegistry.get( oid );
    }


    /**
     * {@inheritDoc}
     */
    public void clear() throws LdapException
    {
        throw new LdapUnwillingToPerformException( ResultCodeEnum.NO_SUCH_OPERATION, I18n.err( I18n.ERR_04277 ) );
    }


    /**
     * {@inheritDoc}
     */
    public DITContentRule unregister( DITContentRule schemaObject ) throws LdapException
    {
        throw new LdapUnwillingToPerformException( ResultCodeEnum.NO_SUCH_OPERATION, I18n.err( I18n.ERR_04277 ) );
    }
}

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


import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapOtherException;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.NumericOidSyntaxChecker;


/**
 * A name or numeric id normalizer.  Needs an OID registry to operate properly.
 * The OID registry is injected into this class after instantiation if a 
 * setRegistries(Registries) method is exposed.
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameOrNumericIdNormalizer extends Normalizer
{
    /** The serial UID */
    public static final long serialVersionUID = 1L;

    private NumericOidSyntaxChecker checker = new NumericOidSyntaxChecker();

    /** A reference to the schema manager used to normalize the Name */
    private SchemaManager schemaManager;

    /** A static instance of this normalizer */
    public static final NameOrNumericIdNormalizer INSTANCE = new NameOrNumericIdNormalizer();


    /**
     * Creates a new instance of GeneralizedTimeNormalizer.
     */
    public NameOrNumericIdNormalizer()
    {
        super( SchemaConstants.NAME_OR_NUMERIC_ID_MATCH_OID );
    }


    /**
     * {@inheritDoc} 
     */
    public Value<?> normalize( Value<?> value ) throws LdapException
    {
        if ( value == null )
        {
            return null;
        }

        String strValue = value.getString();

        if ( strValue.length() == 0 )
        {
            return new StringValue( "" );
        }

        // if value is a numeric id then return it as is
        if ( checker.isValidSyntax( strValue ) )
        {
            return value;
        }

        // if it is a name we need to do a lookup
        String oid = schemaManager.getRegistries().getOid( strValue );

        if ( oid != null )
        {
            return new StringValue( oid );
        }

        // if all else fails
        throw new LdapOtherException( I18n.err( I18n.ERR_04225, value ) );
    }


    /**
     * {@inheritDoc} 
     */
    public String normalize( String value ) throws LdapException
    {
        if ( value == null )
        {
            return null;
        }

        if ( value.length() == 0 )
        {
            return value;
        }

        // if value is a numeric id then return it as is
        if ( checker.isValidSyntax( value ) )
        {
            return value;
        }

        // if it is a name we need to do a lookup
        String oid = schemaManager.getRegistries().getOid( value );

        if ( oid != null )
        {
            return oid;
        }

        // if all else fails
        throw new LdapOtherException( I18n.err( I18n.ERR_04226, value ) );
    }


    /**
     * {@inheritDoc}
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }
}

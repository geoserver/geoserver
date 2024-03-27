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


import java.io.UnsupportedEncodingException;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A DN Name component Normalizer which uses the bootstrap registries to find
 * the appropriate normalizer for the attribute of the name component with which
 * to normalize the name component value.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 928296 $
 */
public class ConcreteNameComponentNormalizer implements NameComponentNormalizer
{
    /** The LoggerFactory used by this Interceptor */
    private static Logger LOG = LoggerFactory.getLogger( ConcreteNameComponentNormalizer.class );

    /** the schemaManager used to dynamically resolve Normalizers */
    private final SchemaManager schemaManager;
    

    /**
     * Creates a DN Name component Normalizer which uses the bootstrap
     * registries to find the appropriate normalizer for the attribute of the
     * name component with which to normalize the name component value.
     *
     * @param schemaManager the schemaManager used to dynamically resolve Normalizers
     */
    public ConcreteNameComponentNormalizer( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }

    
    private String unescape( String value )
    {
        char[] newVal = new char[value.length()];
        int escaped = 0;
        char high = 0;
        char low = 0;
        int pos = 0;
        
        for ( char c:value.toCharArray() )
        {
            switch ( escaped )
            {
                case 0 :
                    if ( c == '\\' )
                    {
                        escaped = 1;
                    }
                    else
                    {
                        newVal[pos++] = c;
                    }
                    
                    break;

                case 1 :
                    escaped++;
                    high = c;
                    break;
                    
                case 2 :
                    escaped=0;
                    low = c;
                    newVal[pos++] = (char)StringTools.getHexValue( high, low );
                    
            }
        }
        
        return new String( newVal, 0, pos );
    }

    /**
     * @see NameComponentNormalizer#normalizeByName(String, String)
     */
    public Object normalizeByName( String name, String value ) throws LdapException
    {
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( name );
        
        if ( attributeType.getSyntax().isHumanReadable() )
        {
            return lookup( name ).normalize( value );
        }
        else
        {
            try
            {
                String unescaped = unescape( value );
                byte[] valBytes = unescaped.getBytes( "UTF-8" );
                
                return lookup( name ).normalize( new BinaryValue( valBytes ) ); 
            }
            catch ( UnsupportedEncodingException uee )
            {
                String message = I18n.err( I18n.ERR_04222 );
                LOG.error( message );
                throw new LdapException( message );
            }
        }
        
    }


    /**
     * @see NameComponentNormalizer#normalizeByName(String, String)
     */
    public Object normalizeByName( String name, byte[] value ) throws LdapException
    {
        AttributeType attributeType = schemaManager.lookupAttributeTypeRegistry( name );
        
        if ( !attributeType.getSyntax().isHumanReadable() )
        {
            return lookup( name ).normalize( new BinaryValue( value ) );
        }
        else
        {
            try
            {
                String valStr = new String( value, "UTF-8" );
                return lookup( name ).normalize( valStr ); 
            }
            catch ( UnsupportedEncodingException uee )
            {
                String message = I18n.err( I18n.ERR_04223 );
                LOG.error( message );
                throw new LdapException( message );
            }
        }
    }


    /**
     * @see NameComponentNormalizer#normalizeByOid(String, String)
     */
    public Object normalizeByOid( String oid, String value ) throws LdapException
    {
        return lookup( oid ).normalize( value );
    }


    /**
     * @see NameComponentNormalizer#normalizeByOid(String, String)
     */
    public Object normalizeByOid( String oid, byte[] value ) throws LdapException
    {
        return lookup( oid ).normalize( new BinaryValue( value ) );
    }


    /**
     * Looks up the Normalizer to use for a name component using the attributeId
     * for the name component.  First the attribute is resolved, then its
     * equality matching rule is looked up.  The normalizer of that matching
     * rule is returned.
     *
     * @param id the name or oid of the attribute in the name component to
     * normalize the value of
     * @return the Normalizer to use for normalizing the value of the attribute
     * @throws LdapException if there are failures resolving the Normalizer
     */
    private Normalizer lookup( String id ) throws LdapException
    {
        AttributeType type = schemaManager.lookupAttributeTypeRegistry( id );
        MatchingRule mrule = type.getEquality();
        
        if ( mrule == null )
        {
            return new NoOpNormalizer( id );
        }
        
        return mrule.getNormalizer();
    }


    /**
     * @see NameComponentNormalizer#isDefined(String)
     */
    public boolean isDefined( String id )
    {
        return schemaManager.getAttributeTypeRegistry().contains( id );
    }


    public String normalizeName( String attributeName ) throws LdapException
    {
        return schemaManager.getAttributeTypeRegistry().getOidByName( attributeName );
    }
}

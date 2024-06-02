/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.schema.parsers;


import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaObject;



/**
 * Utilities for dealing with various schema descriptions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ParserDescriptionUtils
{
    /**
     * Checks two schema objectClasses for an exact match.
     *
     * @param ocd0 the first objectClass to compare
     * @param ocd1 the second objectClass to compare
     * @return true if both objectClasses match exactly, false otherwise
     */
    public static boolean objectClassesMatch( ObjectClass oc0, ObjectClass oc1 ) throws NamingException
    {
        // compare all common description parameters
        if ( ! descriptionsMatch( oc0, oc1 ) )
        {
            return false;
        }

        // compare the objectClass type (AUXILIARY, STRUCTURAL, ABSTRACT)
        if ( oc0.getType() != oc1.getType() )
        {
            return false;
        }
        
        // compare the superior objectClasses (sizes must match)
        if ( oc0.getSuperiorOids().size() != oc1.getSuperiorOids().size() )
        {
            return false;
        }

        // compare the superior objectClasses (sizes must match)
        for ( int i = 0; i < oc0.getSuperiorOids().size(); i++ )
        {
            if ( ! oc0.getSuperiorOids().get( i ).equals( oc1.getSuperiorOids().get( i ) ) )
            {
                return false;
            }
        }
        
        // compare the must attributes (sizes must match)
        for ( int i = 0; i < oc0.getMustAttributeTypeOids().size(); i++ )
        {
            if ( ! oc0.getMustAttributeTypeOids().get( i ).equals( oc1.getMustAttributeTypeOids().get( i ) ) )
            {
                return false;
            }
        }
        
        // compare the may attributes (sizes must match)
        for ( int i = 0; i < oc0.getMayAttributeTypeOids().size(); i++ )
        {
            if ( ! oc0.getMayAttributeTypeOids().get( i ).equals( oc1.getMayAttributeTypeOids().get( i ) ) )
            {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * Checks two schema attributeTypes for an exact match.
     *
     * @param atd0 the first attributeType to compare
     * @param atd1 the second attributeType to compare
     * @return true if both attributeTypes match exactly, false otherwise
     */
    public static boolean attributeTypesMatch( AttributeType at0, AttributeType at1 )
    {
        // compare all common description parameters
        if ( ! descriptionsMatch( at0, at1 ) )
        {
            return false;
        }

        // check that the same super type is being used for both attributes
        if ( ! at0.getSuperiorOid().equals( at1.getSuperiorOid() ) )
        {
            return false;
        }
        
        // check that the same matchingRule is used by both ATs for EQUALITY
        if ( ! at0.getEqualityOid().equals( at1.getEqualityOid() ) )
        {
            return false;
        }
        
        // check that the same matchingRule is used by both ATs for SUBSTRING
        if ( ! at0.getSubstringOid().equals( at1.getSubstringOid() ) )
        {
            return false;
        }
        
        // check that the same matchingRule is used by both ATs for ORDERING
        if ( ! at0.getOrderingOid().equals( at1.getOrderingOid() ) )
        {
            return false;
        }
        
        // check that the same syntax is used by both ATs
        if ( ! at0.getSyntaxOid().equals( at1.getSyntaxOid() ) )
        {
            return false;
        }
        
        // check that the syntax length constraint is the same for both
        if ( at0.getSyntaxLength() != at1.getSyntaxLength() )
        {
            return false;
        }
        
        // check that the ATs have the same single valued flag value
        if ( at0.isSingleValued() != at1.isSingleValued() )
        {
            return false;
        }
        
        // check that the ATs have the same collective flag value
        if ( at0.isCollective() != at1.isCollective() )
        {
            return false;
        }
        
        // check that the ATs have the same user modifiable flag value
        if ( at0.isUserModifiable() != at1.isUserModifiable() )
        {
            return false;
        }
        
        // check that the ATs have the same USAGE
        if ( at0.getUsage() != at1.getUsage() )
        {
            return false;
        }
        
        return true;
    }
    
    
    /**
     * Checks to see if two matchingRule match exactly.
     *
     * @param mrd0 the first matchingRule to compare
     * @param mrd1 the second matchingRule to compare
     * @return true if the matchingRules match exactly, false otherwise
     */
    public static boolean matchingRulesMatch( MatchingRule matchingRule0, MatchingRule matchingRule1 )
    {
        // compare all common description parameters
        if ( ! descriptionsMatch( matchingRule0, matchingRule1 ) )
        {
            return false;
        }

        // check that the syntaxes of the matchingRules match
        if ( ! matchingRule0.getSyntaxOid().equals( matchingRule1.getSyntaxOid() ) )
        {
            return false;
        }
        
        return true;
    }
    
    
    /**
     * Checks to see if two syntax match exactly.
     *
     * @param ldapSyntax0 the first ldapSyntax to compare
     * @param ldapSyntax1 the second ldapSyntax to compare
     * @return true if the syntaxes match exactly, false otherwise
     */
    public static boolean syntaxesMatch( LdapSyntax ldapSyntax0, LdapSyntax ldapSyntax1 )
    {
        return descriptionsMatch( ldapSyntax0, ldapSyntax1 );
    }
    
    
    /**
     * Checks if two base schema descriptions match for the common components 
     * in every schema description.  NOTE: for syntaxes the obsolete flag is 
     * not compared because doing so would raise an exception since syntax 
     * descriptions do not support the OBSOLETE flag.
     * 
     * @param lsd0 the first schema description to compare 
     * @param lsd1 the second schema description to compare 
     * @return true if the descriptions match exactly, false otherwise
     */
    public static boolean descriptionsMatch( SchemaObject so0, SchemaObject so1 )
    {
        // check that the OID matches
        if ( ! so0.getOid().equals( so1.getOid() ) )
        {
            return false;
        }
        
        // check that the obsolete flag is equal but not for syntaxes
        if ( ( so0 instanceof LdapSyntax ) || ( so1 instanceof LdapSyntax ) )
        {
            if ( so0.isObsolete() != so1.isObsolete() )
            {
                return false;
            }
        }
        
        // check that the description matches
        if ( ! so0.getDescription().equals( so1.getDescription() ) )
        {
            return false;
        }
        
        // check alias names for exact match
        if ( ! aliasNamesMatch( so0, so1 ) )
        {
            return false;
        }
        
        // check extensions for exact match
        if ( ! extensionsMatch( so0, so1 ) )
        {
            return false;
        }

        return true;
    }


    /**
     * Checks to see if the extensions of a schema description match another
     * description.  The order of the extension values must match for a true
     * return.
     *
     * @param lsd0 the first schema description to compare the extensions of
     * @param lsd1 the second schema description to compare the extensions of
     * @return true if the extensions match exactly, false otherwise
     */
    public static boolean extensionsMatch( SchemaObject lsd0, SchemaObject lsd1 )
    {
        // check sizes first
        if ( lsd0.getExtensions().size() != lsd1.getExtensions().size() )
        {
            return false;
        }
        
        // check contents and order of extension values must match
        for ( String key : lsd0.getExtensions().keySet() )
        {
            List<String> values0 = lsd0.getExtensions().get( key );
            List<String> values1 = lsd1.getExtensions().get( key );
            
            // if the key is not present in asd1
            if ( values1 == null )
            {
                return false;
            }
            
            for ( int i = 0; i < values0.size(); i++ )
            {
                if ( ! values0.get( i ).equals( values1.get( i ) ) )
                {
                    return false;
                }
            }
        }
        
        return true;
    }
    

    /**
     * Checks to see if the alias names of a schema description match another 
     * description.  The order of the alias names do matter.
     *
     * @param asd0 the schema description to compare
     * @param asd1 the schema description to compare
     * @return true if alias names match exactly, false otherwise
     */
    public static boolean aliasNamesMatch( SchemaObject so0, SchemaObject so1 )
    {
        // check sizes first
        if ( so0.getNames().size() != so1.getNames().size() )
        {
            return false;
        }
        
        // check contents and order must match too
        for ( int i = 0; i < so0.getNames().size(); i++ )
        {
            if ( ! so0.getNames().get( i ).equals( so1.getNames().get( i ) ) )
            {
                return false;
            }
        }
        
        return true;
    }
}

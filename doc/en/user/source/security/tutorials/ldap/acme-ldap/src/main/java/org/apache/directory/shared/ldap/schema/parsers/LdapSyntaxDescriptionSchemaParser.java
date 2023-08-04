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
package org.apache.directory.shared.ldap.schema.parsers;


import java.text.ParseException;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A parser for RFC 4512 LDAP syntx descriptions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdapSyntaxDescriptionSchemaParser extends AbstractSchemaParser
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( LdapSyntaxDescriptionSchemaParser.class );

    /**
     * Creates a schema parser instance.
     */
    public LdapSyntaxDescriptionSchemaParser()
    {
    }


    /**
     * Parses a LDAP syntax description according to RFC 4512:
     * 
     * <pre>
     * SyntaxDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "DESC" SP qdstring ]  ; description
     *    extensions WSP RPAREN      ; extensions
     * </pre>
     * 
     * @param ldapSyntaxDescription the LDAP syntax description to be parsed
     * @return the parsed LdapSyntax bean
     * @throws ParseException if there are any recognition errors (bad syntax)
     */
    public synchronized LdapSyntax parseLdapSyntaxDescription( String ldapSyntaxDescription )
        throws ParseException
    {
        LOG.debug( "Parsing a LdapSyntax : {}", ldapSyntaxDescription );

        if ( ldapSyntaxDescription == null )
        {
            LOG.error( I18n.err( I18n.ERR_04239 ) );
            throw new ParseException( "Null", 0 );
        }

        reset( ldapSyntaxDescription ); // reset and initialize the parser / lexer pair

        try
        {
            LdapSyntax ldapSyntax = parser.ldapSyntaxDescription();
            ldapSyntax.setSpecification( ldapSyntaxDescription );

            // Update the schemaName
            setSchemaName( ldapSyntax );
                
            return ldapSyntax;
        }
        catch ( RecognitionException re )
        {
            String msg = I18n.err( I18n.ERR_04240, ldapSyntaxDescription, re.getMessage(), re.getColumn() );
            LOG.error( msg );
            throw new ParseException( msg, re.getColumn() );
        }
        catch ( TokenStreamException tse )
        {
            String msg = I18n.err( I18n.ERR_04241, ldapSyntaxDescription, tse.getMessage() );
            LOG.error(  msg  );
            throw new ParseException( msg, 0 );
        }
    }


    /**
     * Parses a LdapSyntax description
     * 
     * @param The LdapSyntax description to parse
     * @return An instance of LdapSyntax
     */
    public LdapSyntax parse( String schemaDescription ) throws ParseException
    {
        return parseLdapSyntaxDescription( schemaDescription );
    }
}

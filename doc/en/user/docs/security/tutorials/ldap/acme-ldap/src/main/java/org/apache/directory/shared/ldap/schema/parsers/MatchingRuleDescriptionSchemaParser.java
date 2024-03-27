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
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A parser for RFC 4512 matching rule descriptions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MatchingRuleDescriptionSchemaParser extends AbstractSchemaParser
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( MatchingRuleDescriptionSchemaParser.class );

    /**
     * Creates a schema parser instance.
     */
    public MatchingRuleDescriptionSchemaParser()
    {
    }


    /**
     * Parses a matching rule description according to RFC 4512:
     * 
     * <pre>
     * MatchingRuleDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    SP "SYNTAX" SP numericoid  ; assertion syntax
     *    extensions WSP RPAREN      ; extensions
     * 
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE ) 
     * </pre>
     * 
     * @param matchingRuleDescription the matching rule description to be parsed
     * @return the parsed MatchingRuleDescription bean
     * @throws ParseException if there are any recognition errors (bad syntax)
     */
    public synchronized MatchingRule parseMatchingRuleDescription( String matchingRuleDescription )
        throws ParseException
    {
        LOG.debug( "Parsing a MatchingRule : {}", matchingRuleDescription );

        if ( matchingRuleDescription == null )
        {
            LOG.error( I18n.err( I18n.ERR_04242 ) );
            throw new ParseException( "Null", 0 );
        }

        reset( matchingRuleDescription ); // reset and initialize the parser / lexer pair

        try
        {
            MatchingRule matchingRule = parser.matchingRuleDescription();
            
            // Update the schemaName
            setSchemaName( matchingRule );

            return matchingRule;
        }
        catch ( RecognitionException re )
        {
            String msg = I18n.err( I18n.ERR_04243, matchingRuleDescription, re.getMessage(), re.getColumn() );
            LOG.error( msg );
            throw new ParseException( msg, re.getColumn() );
        }
        catch ( TokenStreamException tse )
        {
            String msg = I18n.err( I18n.ERR_04244, matchingRuleDescription, tse.getMessage() );
            LOG.error( msg );
            throw new ParseException( msg, 0 );
        }
    }


    /**
     * Parses a MatchingRule description
     * 
     * @param The MatchingRule description to parse
     * @return An instance of MatchingRule
     */
    public MatchingRule parse( String schemaDescription ) throws ParseException
    {
        return parseMatchingRuleDescription( schemaDescription );
    }
}

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
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A parser for RFC 4512 DIT content rule descriptons
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DITContentRuleDescriptionSchemaParser extends AbstractSchemaParser
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( DITContentRuleDescriptionSchemaParser.class );

    /**
     * Creates a schema parser instance.
     */
    public DITContentRuleDescriptionSchemaParser()
    {
    }


    /**
     * Parses a DIT content rule description according to RFC 4512:
     * 
     * <pre>
     * DITContentRuleDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    [ SP "AUX" SP oids ]       ; auxiliary object classes
     *    [ SP "MUST" SP oids ]      ; attribute types
     *    [ SP "MAY" SP oids ]       ; attribute types
     *    [ SP "NOT" SP oids ]       ; attribute types
     *    extensions WSP RPAREN      ; extensions
     * </pre>
     * 
     * @param ditContentRuleDescription the DIT content rule description to be parsed
     * @return the parsed DITContentRuleDescription bean
     * @throws ParseException if there are any recognition errors (bad syntax)
     */
    public synchronized DITContentRule parseDITContentRuleDescription( String ditContentRuleDescription )
        throws ParseException
    {
        LOG.debug( "Parsing a DITContentRule : {}", ditContentRuleDescription );

        if ( ditContentRuleDescription == null )
        {
            LOG.error( I18n.err( I18n.ERR_04230 ) );
            throw new ParseException( "Null", 0 );
        }

        reset( ditContentRuleDescription ); // reset and initialize the parser / lexer pair

        try
        {
            DITContentRule ditContentRule = parser.ditContentRuleDescription();
            
            // Update the schemaName
            setSchemaName( ditContentRule );

            return ditContentRule;
        }
        catch ( RecognitionException re )
        {
            String msg = I18n.err( I18n.ERR_04231, ditContentRuleDescription, re.getMessage(), re.getColumn() );
            LOG.error( msg );
            throw new ParseException( msg, re.getColumn() );
        }
        catch ( TokenStreamException tse )
        {
            String msg = I18n.err( I18n.ERR_04232, ditContentRuleDescription, tse.getMessage() );
            LOG.error( msg );
            throw new ParseException( msg, 0 );
        }

    }


    /**
     * Parses a DITContentRule description
     * 
     * @param The DITContentRule description to parse
     * @return An instance of DITContentRule
     */
    public DITContentRule parse( String schemaDescription ) throws ParseException
    {
        return parseDITContentRuleDescription( schemaDescription );
    }
}

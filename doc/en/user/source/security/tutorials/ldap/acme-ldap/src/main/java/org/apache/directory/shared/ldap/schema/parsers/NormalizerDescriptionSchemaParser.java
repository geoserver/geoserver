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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A parser for ApacheDS normalizer descriptions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NormalizerDescriptionSchemaParser extends AbstractSchemaParser
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( NormalizerDescriptionSchemaParser.class );


    /**
     * Creates a schema parser instance.
     */
    public NormalizerDescriptionSchemaParser()
    {
        super();
    }


    /**
     * Parses a normalizer description:
     * 
     * <pre>
     * NormalizerDescription = LPAREN WSP
     *     numericoid                           ; object identifier
     *     [ SP "DESC" SP qdstring ]            ; description
     *     SP "FQCN" SP fqcn                    ; fully qualified class name
     *     [ SP "BYTECODE" SP base64 ]          ; optional base64 encoded bytecode
     *     extensions WSP RPAREN                ; extensions
     * 
     * base64          = *(4base64-char)
     * base64-char     = ALPHA / DIGIT / "+" / "/"
     * fqcn = fqcnComponent 1*( DOT fqcnComponent )
     * fqcnComponent = ???
     * 
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE ) 
     * </pre>
     * 
     * @param normalizerDescription the normalizer description to be parsed
     * @return the parsed NormalizerDescription bean
     * @throws ParseException if there are any recognition errors (bad syntax)
     */
    public synchronized NormalizerDescription parseNormalizerDescription( String normalizerDescription )
        throws ParseException
    {
        LOG.debug( "Parsing a Normalizer : {}", normalizerDescription );

        if ( normalizerDescription == null )
        {
            LOG.error( I18n.err( I18n.ERR_04251 ) );
            throw new ParseException( "Null", 0 );
        }

        reset( normalizerDescription ); // reset and initialize the parser / lexer pair

        try
        {
            NormalizerDescription normalizer = parser.normalizerDescription();

            // Update the schemaName
            setSchemaName( normalizer );

            return normalizer;
        }
        catch ( RecognitionException re )
        {
            String msg = I18n.err( I18n.ERR_04252, normalizerDescription, re.getMessage(), re.getColumn() );
            LOG.error( msg );
            throw new ParseException( msg, re.getColumn() );
        }
        catch ( TokenStreamException tse )
        {
            String msg = I18n.err( I18n.ERR_04253, normalizerDescription, tse.getMessage() );
            LOG.error( msg );
            throw new ParseException( msg, 0 );
        }

    }


    /**
     * Parses a Normalizer description
     * 
     * @param The Normalizer description to parse
     * @return An instance of NormalizerDescription
     */
    public NormalizerDescription parse( String schemaDescription ) throws ParseException
    {
        return parseNormalizerDescription( schemaDescription );
    }
}

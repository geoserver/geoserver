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
import org.apache.directory.shared.ldap.schema.NameForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A parser for RFC 4512 name form descriptions
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NameFormDescriptionSchemaParser extends AbstractSchemaParser
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( NameFormDescriptionSchemaParser.class );

    /**
     * Creates a schema parser instance.
     */
    public NameFormDescriptionSchemaParser()
    {
    }


    /**
     * Parses a name form description according to RFC 4512:
     * 
     * <pre>
     * NameFormDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    SP "OC" SP oid             ; structural object class
     *    SP "MUST" SP oids          ; attribute types
     *    [ SP "MAY" SP oids ]       ; attribute types
     *    extensions WSP RPAREN      ; extensions
     * </pre>
     * 
     * @param nameFormDescription the name form description to be parsed
     * @return the parsed NameForm bean
     * @throws ParseException if there are any recognition errors (bad syntax)
     */
    public synchronized NameForm parseNameFormDescription( String nameFormDescription)
        throws ParseException
    {
        LOG.debug( "Parsing a NameForm : {}", nameFormDescription );

        if ( nameFormDescription == null )
        {
            LOG.error( I18n.err( I18n.ERR_04248 ) );
            throw new ParseException( "Null", 0 );
        }

        reset( nameFormDescription ); // reset and initialize the parser / lexer pair

        try
        {
            NameForm nameForm = parser.nameFormDescription();
            
            // Update the schemaName
            setSchemaName( nameForm );

            return nameForm;
        }
        catch ( RecognitionException re )
        {
            String msg = I18n.err( I18n.ERR_04249, nameFormDescription, re.getMessage(), re.getColumn() );
            LOG.error( msg );
            throw new ParseException( msg, re.getColumn() );
        }
        catch ( TokenStreamException tse )
        {
            String msg = I18n.err( I18n.ERR_04250, nameFormDescription, tse.getMessage() );
            LOG.error( msg );
            throw new ParseException( msg, 0 );
        }
    }


    /**
     * Parses a NameForm description
     * 
     * @param The NameForm description to parse
     * @return An instance of NameForm
     */
    public NameForm parse( String schemaDescription ) throws ParseException
    {
        return parseNameFormDescription( schemaDescription );
    }
}

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
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A parser for RFC 4512 object class descriptons
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ObjectClassDescriptionSchemaParser extends AbstractSchemaParser
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( ObjectClassDescriptionSchemaParser.class );


    /**
     * Creates a schema parser instance.
     */
    public ObjectClassDescriptionSchemaParser()
    {
        // Nothing to do
    }


    /**
     * Parses a object class definition according to RFC 4512:
     * 
     * <pre>
     * ObjectClassDescription = LPAREN WSP
     *     numericoid                 ; object identifier
     *     [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *     [ SP "DESC" SP qdstring ]  ; description
     *     [ SP "OBSOLETE" ]          ; not active
     *     [ SP "SUP" SP oids ]       ; superior object classes
     *     [ SP kind ]                ; kind of class
     *     [ SP "MUST" SP oids ]      ; attribute types
     *     [ SP "MAY" SP oids ]       ; attribute types
     *     extensions WSP RPAREN
     *
     * kind = "ABSTRACT" / "STRUCTURAL" / "AUXILIARY"
     * 
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE ) 
     * </pre>
     * 
     * @param objectClassDescription the object class description to be parsed
     * @return the parsed ObjectClassDescription bean
     * @throws ParseException if there are any recognition errors (bad syntax)
     */
    public synchronized ObjectClass parseObjectClassDescription( String objectClassDescription )
        throws ParseException
    {
        LOG.debug( "Parsing an ObjectClass : {}", objectClassDescription );

        if ( objectClassDescription == null )
        {
            LOG.error( I18n.err( I18n.ERR_04254 ) );
            throw new ParseException( "Null", 0 );
        }

        reset( objectClassDescription ); // reset and initialize the parser / lexer pair

        try
        {
            ObjectClass objectClass = parser.objectClassDescription();

            // Update the schemaName
            setSchemaName( objectClass );

            return objectClass;
        }
        catch ( RecognitionException re )
        {
            String msg = I18n.err( I18n.ERR_04255, objectClassDescription, re.getMessage(), re.getColumn() );
            LOG.error( msg );
            throw new ParseException( msg, re.getColumn() );
        }
        catch ( TokenStreamException tse )
        {
            String msg = I18n.err( I18n.ERR_04256, objectClassDescription, tse.getMessage() );
            LOG.error( msg );
            throw new ParseException( msg, 0 );
        }

    }


    public ObjectClass parse( String schemaDescription ) throws ParseException
    {
        return parseObjectClassDescription( schemaDescription );
    }

}

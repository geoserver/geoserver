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
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import antlr.TokenStreamRecognitionException;


/**
 * A parser for RFC 4512 attribute type descriptions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AttributeTypeDescriptionSchemaParser extends AbstractSchemaParser
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( AttributeTypeDescriptionSchemaParser.class );

    /**
     * Creates a schema parser instance.
     */
    public AttributeTypeDescriptionSchemaParser()
    {
    }
    

    /**
     * Parses a attribute type description according to RFC 4512:
     * 
     * <pre>
     * AttributeTypeDescription = LPAREN WSP
     *     numericoid                    ; object identifier
     *     [ SP "NAME" SP qdescrs ]      ; short names (descriptors)
     *     [ SP "DESC" SP qdstring ]     ; description
     *     [ SP "OBSOLETE" ]             ; not active
     *     [ SP "SUP" SP oid ]           ; supertype
     *     [ SP "EQUALITY" SP oid ]      ; equality matching rule
     *     [ SP "ORDERING" SP oid ]      ; ordering matching rule
     *     [ SP "SUBSTR" SP oid ]        ; substrings matching rule
     *     [ SP "SYNTAX" SP noidlen ]    ; value syntax
     *     [ SP "SINGLE-VALUE" ]         ; single-value
     *     [ SP "COLLECTIVE" ]           ; collective
     *     [ SP "NO-USER-MODIFICATION" ] ; not user modifiable
     *     [ SP "USAGE" SP usage ]       ; usage
     *     extensions WSP RPAREN         ; extensions
     * 
     * usage = "userApplications"     /  ; user
     *         "directoryOperation"   /  ; directory operational
     *         "distributedOperation" /  ; DSA-shared operational
     *         "dSAOperation"            ; DSA-specific operational     
     * 
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE ) 
     * </pre>
     * 
     * @param attributeTypeDescription the attribute type description to be parsed
     * @return the parsed AttributeTypeDescription bean
     * @throws ParseException if there are any recognition errors (bad syntax)
     */
    public synchronized AttributeType parseAttributeTypeDescription( String attributeTypeDescription )
        throws ParseException
    {

        LOG.debug( "Parsing an AttributeType : {}", attributeTypeDescription );

        if ( attributeTypeDescription == null )
        {
            LOG.error( I18n.err( I18n.ERR_04227 ) );
            throw new ParseException( "Null", 0 );
        }

        reset( attributeTypeDescription ); // reset and initialize the parser / lexer pair

        try
        {
            AttributeType attributeType = parser.attributeTypeDescription();
            
            // Update the schemaName
            setSchemaName( attributeType );

            return attributeType;
        }
        catch ( RecognitionException re )
        {
            String msg = I18n.err( I18n.ERR_04228, attributeTypeDescription , re.getMessage() , re.getColumn() );
            LOG.error( msg );
            throw new ParseException( msg, re.getColumn() );
        }
        catch ( TokenStreamRecognitionException tsre )
        {
            String msg = I18n.err( I18n.ERR_04229, attributeTypeDescription, tsre.getMessage() );
            LOG.error( msg );
            throw new ParseException( msg, 0 );
        }
        catch ( TokenStreamException tse )
        {
            String msg = I18n.err( I18n.ERR_04229, attributeTypeDescription, tse.getMessage() );
            LOG.error( msg );
            throw new ParseException( msg, 0 );
        }

    }


    /**
     * Parses a AttributeType description
     * 
     * @param The AttributeType description to parse
     * @return An instance of AttributeType
     */
    public AttributeType parse( String schemaDescription ) throws ParseException
    {
        return parseAttributeTypeDescription( schemaDescription );
    }
}

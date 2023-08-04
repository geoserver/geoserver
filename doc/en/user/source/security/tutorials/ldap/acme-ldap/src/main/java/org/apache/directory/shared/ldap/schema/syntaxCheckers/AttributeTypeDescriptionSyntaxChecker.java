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
package org.apache.directory.shared.ldap.schema.syntaxCheckers;


import java.text.ParseException;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.schema.parsers.AttributeTypeDescriptionSchemaParser;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker which verifies that a value follows the
 * attribute type descripton syntax according to RFC 4512, par 4.2.2:
 * 
*  <pre>
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
 * 
 * Each attribute type description must contain at least one of the SUP
 * or SYNTAX fields. 
 * 
 * COLLECTIVE requires usage userApplications.
 * 
 * NO-USER-MODIFICATION requires an operational usage.
 * 
 * 
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeTypeDescriptionSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AttributeTypeDescriptionSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The schema parser used to parse the AttributeTypeDescription Syntax */
    private AttributeTypeDescriptionSchemaParser schemaParser = new AttributeTypeDescriptionSchemaParser();

    /**
     * 
     * Creates a new instance of AttributeTypeDescriptionSchemaParser.
     *
     */
    public AttributeTypeDescriptionSyntaxChecker()
    {
        super( SchemaConstants.ATTRIBUTE_TYPE_DESCRIPTION_SYNTAX );
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isValidSyntax( Object value )
    {
        String strValue = null;

        if ( value == null )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }

        if ( value instanceof String )
        {
            strValue = ( String ) value;
        }
        else if ( value instanceof byte[] )
        {
            strValue = StringTools.utf8ToString( ( byte[] ) value );
        }
        else
        {
            strValue = value.toString();
        }

        try
        {
            schemaParser.parseAttributeTypeDescription( strValue );
            LOG.debug( "Syntax valid for '{}'", value );
            return true;
        }
        catch ( ParseException pe )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
    }
}

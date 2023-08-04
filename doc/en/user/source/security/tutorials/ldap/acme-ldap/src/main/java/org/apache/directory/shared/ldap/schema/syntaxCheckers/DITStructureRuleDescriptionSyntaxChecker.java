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
import org.apache.directory.shared.ldap.schema.parsers.DITStructureRuleDescriptionSchemaParser;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker which verifies that a value follows the
 * DIT structure rule descripton syntax according to RFC 4512, par 4.2.7.1:
 * 
 * <pre>
 * DITStructureRuleDescription = LPAREN WSP
 *   ruleid                     ; rule identifier
 *   [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
 *   [ SP "DESC" SP qdstring ]  ; description
 *   [ SP "OBSOLETE" ]          ; not active
 *   SP "FORM" SP oid           ; NameForm
 *   [ SP "SUP" ruleids ]       ; superior rules
 *   extensions WSP RPAREN      ; extensions
 *
 * ruleids = ruleid / ( LPAREN WSP ruleidlist WSP RPAREN )
 * ruleidlist = ruleid *( SP ruleid )
 * ruleid = numbers
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DITStructureRuleDescriptionSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DITStructureRuleDescriptionSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The schema parser used to parse the DITContentRuleDescription Syntax */
    private DITStructureRuleDescriptionSchemaParser schemaParser = new DITStructureRuleDescriptionSchemaParser();


    /**
     * Creates a new instance of DITContentRuleDescriptionSyntaxChecker.
     */
    public DITStructureRuleDescriptionSyntaxChecker()
    {
        super( SchemaConstants.DIT_STRUCTURE_RULE_SYNTAX );
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
            schemaParser.parseDITStructureRuleDescription( strValue );
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

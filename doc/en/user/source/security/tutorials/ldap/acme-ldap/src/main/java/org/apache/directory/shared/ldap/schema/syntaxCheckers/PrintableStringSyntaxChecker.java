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


import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker which verifies that a value is a Printable String according to RFC 4517.
 * 
 * From RFC 4517 :
 * 
 * PrintableString    = 1*PrintableCharacter
 * PrintableCharacter = ALPHA | DIGIT | SQUOTE | LPAREN | RPAREN |
 *                          PLUS | COMMA | HYPHEN | DOT | EQUALS |
 *                          SLASH | COLON | QUESTION | SPACE
 *                          
 * SLASH   = %x2F                ; forward slash ("/")
 * COLON   = %x3A                ; colon (":")
 * QUESTION= %x3F                ; question mark ("?")
 * 
 * From RFC 4512 :
 * ALPHA   = %x41-5A | %x61-7A   ; "A"-"Z" / "a"-"z"
 * DIGIT   = %x30 | LDIGIT       ; "0"-"9"
 * LDIGIT  = %x31-39             ; "1"-"9"
 * SQUOTE  = %x27                ; single quote ("'")
 * LPAREN  = %x28                ; left paren ("(")
 * RPAREN  = %x29                ; right paren (")")
 * PLUS    = %x2B                ; plus sign ("+")
 * COMMA   = %x2C                ; comma (",")
 * HYPHEN  = %x2D                ; hyphen ("-")
 * DOT     = %x2E                ; period (".")
 * EQUALS  = %x3D                ; equals sign ("=")
 * SPACE   = %x20                ; space (" ")
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PrintableStringSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( PrintableStringSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of PrintableStringSyntaxChecker.
     */
    public PrintableStringSyntaxChecker()
    {
        super( SchemaConstants.PRINTABLE_STRING_SYNTAX );
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

        if ( strValue.length() == 0 )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        // We must have at least one char
        if ( strValue.length() == 0 )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }

        boolean result = StringTools.isPrintableString( strValue );
        
        if ( result )
        {
            LOG.debug( "Syntax valid for '{}'", value );
        }
        else
        {
            LOG.debug( "Syntax invalid for '{}'", value );
        }
        
        return result;
    }
}

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
 * A SyntaxChecker which verifies that a value is a Telex Number according to 
 * RFC 4517 :
 * 
 * telex-number  = actual-number DOLLAR country-code DOLLAR answerback
 * actual-number = PrintableString
 * country-code  = PrintableString
 * answerback    = PrintableString
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class TelexNumberSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( TelexNumberSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of TelexNumberSyntaxChecker.
     */
    public TelexNumberSyntaxChecker()
    {
        super( SchemaConstants.TELEX_NUMBER_SYNTAX );
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

        // Search for the first '$' separator
        int dollar = strValue.indexOf( '$' );
        
        // We must have one, and not on first position
        if ( dollar <= 0 )
        {
            // No '$' => error
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        String actualNumber = strValue.substring( 0, dollar );
        
        // The actualNumber must not be empty
        if ( actualNumber.length() == 0 )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        // The actual number should be a PrintableString 
        if ( ! StringTools.isPrintableString( actualNumber ) )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        // Search for the second separator
        int dollar2 = strValue.indexOf( '$', dollar + 1 );
            
        // We must have one
        if ( dollar2 == -1 )
        {
            // No '$' => error
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }

        String countryCode = strValue.substring( dollar + 1, dollar2 );
        
        // The countryCode must not be empty
        if ( countryCode.length() == 0 )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        // The country Code should be a PrintableString 
        if ( ! StringTools.isPrintableString( countryCode ) )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        // Now, check for the answerBack
        if ( dollar2 + 1 == strValue.length() )
        {
            // The last string should not be null
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        String answerBack = strValue.substring( dollar2 + 1 );
        
        // The answerBack should be a PrintableString 
        if ( ! StringTools.isPrintableString( answerBack ) )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        // Check that the mailboxType is a PrintableString
        boolean result = StringTools.isPrintableString( answerBack );
        
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

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
 * A SyntaxChecker which verifies that a value is a TeletexTerminalIdentifier according to 
 * RFC 4517 :
 * 
 * teletex-id = ttx-term *(DOLLAR ttx-param)
 * ttx-term   = PrintableString          ; terminal identifier
 * ttx-param  = ttx-key COLON ttx-value  ; parameter
 * ttx-key    = "graphic" | "control" | "misc" | "page" | "private"
 * ttx-value  = *ttx-value-octet
 *
 * ttx-value-octet = %x00-23 | (%x5C "24") | %x25-5B | (%x5C "5C") | %x5D-FF
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class TeletexTerminalIdentifierSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( TeletexTerminalIdentifierSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of TeletexTerminalIdentifier.
     */
    public TeletexTerminalIdentifierSyntaxChecker()
    {
        super( SchemaConstants.TELETEX_TERMINAL_IDENTIFIER_SYNTAX );
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
        
        String terminalIdentifier = ( ( dollar == -1 ) ? strValue : strValue.substring( 0, dollar ) );
        
        if ( terminalIdentifier.length() == 0 )
        {
            // It should not be null
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        if ( !StringTools.isPrintableString( terminalIdentifier ) )
        {
            // It's not a valid PrintableString 
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        if ( dollar == -1 )
        {
            // No ttx-param : let's get out
            LOG.debug( "Syntax valid for '{}'", value );
            return true;
        }
        
        // Ok, now let's deal withh optional ttx-params
        String[] ttxParams = strValue.substring( dollar + 1 ).split( "\\$" );
        
        if ( ttxParams.length == 0 )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        for ( String ttxParam:ttxParams )
        {
            int colon = ttxParam.indexOf( ':' );
            
            if ( colon == -1 )
            {
                // we must have a ':' separator
                LOG.debug( "Syntax invalid for '{}'", value );
                return false;
            }
            
            String key = ttxParam.substring( 0, colon );
            
            if ( key.startsWith( "graphic" ) ||
                 key.startsWith( "control" ) ||
                 key.startsWith( "misc" ) ||
                 key.startsWith( "page" ) ||
                 key.startsWith( "private" ) )
            {
                if ( colon + 1 == ttxParam.length() )
                {
                    LOG.debug( "Syntax invalid for '{}'", value );
                    return false;
                }
                
                boolean hasEsc = false;
                
                for ( byte b:StringTools.getBytesUtf8( ttxParam ) )
                {
                    switch ( b )
                    {
                        case 0x24 :
                            // '$' is not accepted
                            LOG.debug( "Syntax invalid for '{}'", value );
                            return false;
                            
                        case 0x5c :
                            if ( hasEsc )
                            {
                                // two following \ are not accepted
                                LOG.debug( "Syntax invalid for '{}'", value );
                                return false;
                            }
                            else
                            {
                                hasEsc = true;
                            }
                            
                            continue;
                        
                        case '2' :
                            continue;

                        case '4' :
                            // We have found a "\24"
                            hasEsc = false;
                            continue;
                            
                        case '5' :
                            continue;

                        case 'c' :
                        case 'C' :
                            // We have found a "\5c" or a "\5C"
                            hasEsc = false;
                            continue;
                            
                        default :
                            if ( hasEsc )
                            {
                                // A \ should be followed by "24" or "5c" or "5C"
                                return false;
                            }
                            
                        continue;
                    }
                }
            }
            else
            {
                LOG.debug( "Syntax invalid for '{}'", value );
                return false;
            }
        }
        
        LOG.debug( "Syntax valid for '{}'", value );
        return true;
    }
}

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
 * A SyntaxChecker which verifies that a value is a DSAQualitySyntax according to 
 * http://tools.ietf.org/id/draft-ietf-asid-ldapv3-attributes-03.txt, par 5.2.2.2 :
 * 
 * <DsaQualitySyntax> ::= <DSAKeyword> [ '#' <description> ]
 *
 * <DSAKeyword> ::= 'DEFUNCT' | 'EXPERIMENTAL' | 'BEST-EFFORT' |
 *                  'PILOT-SERVICE' | 'FULL-SERVICE'
 *
 * <description> ::= encoded as a PrintableString
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DSAQualitySyntaxSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DSAQualitySyntaxSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of DSAQualitySyntaxSyntaxChecker.
     */
    public DSAQualitySyntaxSyntaxChecker()
    {
        super( SchemaConstants.DSA_QUALITY_SYNTAX );
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

        if ( strValue.length() < 7 )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }

        String remaining = null;
        
        switch ( strValue.charAt( 0 ) )
        {
            case 'B' :
                if ( !strValue.startsWith( "BEST-EFFORT" ) )
                {
                    LOG.debug( "Syntax invalid for '{}'", value );
                    return false;
                }
                
                remaining = strValue.substring( "BEST-EFFORT".length() );
                break;
                
            case 'D' :
                if ( !strValue.startsWith( "DEFUNCT" ) )
                {
                    LOG.debug( "Syntax invalid for '{}'", value );
                    return false;
                }
                
                remaining = strValue.substring( "DEFUNCT".length() );
                break;
                
            case 'E' :
                if ( !strValue.startsWith( "EXPERIMENTAL" ) )
                {
                    LOG.debug( "Syntax invalid for '{}'", value );
                    return false;
                }
                
                remaining = strValue.substring( "EXPERIMENTAL".length() );
                break;
                
            case 'F' :
                if ( !strValue.startsWith( "FULL-SERVICE" ) )
                {
                    LOG.debug( "Syntax invalid for '{}'", value );
                    return false;
                }
                
                remaining = strValue.substring( "FULL-SERVICE".length() );
                break;
                
            case 'P' :
                if ( !strValue.startsWith( "PILOT-SERVICE" ) )
                {
                    LOG.debug( "Syntax invalid for '{}'", value );
                    return false;
                }
                
                remaining = strValue.substring( "PILOT-SERVICE".length() );
                break;
                
            default :
                LOG.debug( "Syntax invalid for '{}'", value );
                return false;
        }
        
        // Now, we might have a description separated from the keyword by a '#'
        // but this is optional
        if ( remaining.length() == 0 )
        {
            LOG.debug( "Syntax valid for '{}'", value );
            return true;
        }
        
        if ( remaining.charAt( 0 ) != '#' )
        {
            // We were expecting a '#'
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        // Check that the description is a PrintableString
        boolean result = StringTools.isPrintableString( remaining.substring( 1 ) );
        
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

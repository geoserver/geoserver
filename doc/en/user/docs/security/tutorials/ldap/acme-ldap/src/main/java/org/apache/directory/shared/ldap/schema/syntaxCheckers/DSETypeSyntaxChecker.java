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


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker which verifies that a value is a DSEType according to 
 * http://tools.ietf.org/id/draft-ietf-asid-ldapv3-attributes-03.txt, par 6.2.1.5 :
 * 
 * <DSEType>    ::= '(' <sp>* <DSEBit> <sp>* <DSEBitList> ')'
 * <DSEBitList> ::= '$' <sp>* <DSEBit> <sp>* <DSEBitList> | e      
 * <DSEBit>     ::= 'root' | 'glue' | 'cp' | 'entry' | 'alias' | 'subr' |
 *                  'nssr' | 'supr' | 'xr' | 'admPoint' | 'subentry' |
 *                  'shadow' | 'zombie' | 'immSupr' | 'rhob' | 'sa'
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DSETypeSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DSETypeSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The DSE BITS keywords */
    private static final String[] DSE_BITS_STRINGS = 
        {
        "root", "glue", "cp", "entry", "alias", "subr",
        "nssr", "supr", "xr", "admPoint", "subentry",
        "shadow", "zombie", "immSupr", "rhob", "sa"
        };
    
    
    /** The Set which contains the DESBits */
    private static final Set<String> DSE_BITS = new HashSet<String>();
    
    /** Initialization of the country set */
    static
    {
        for ( String country:DSE_BITS_STRINGS )
        {
            DSE_BITS.add( country );
        }
    }

    
    /**
     * 
     * Creates a new instance of DSETypeSyntaxChecker.
     *
     */
    public DSETypeSyntaxChecker()
    {
        super( SchemaConstants.DSE_TYPE_SYNTAX );
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

        // We must have at least '(cp)', '(xr)' or '(ca)'
        if ( strValue.length() < 4 )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }

        // Check the opening and closing parenthesis
        if ( ( strValue.charAt( 0 ) != '(' ) || 
             ( strValue.charAt( strValue.length() - 1 ) != ')' ) )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }

        Set<String> keywords = new HashSet<String>();
        int len = strValue.length() - 1;
        boolean needKeyword = true;
        
        // 
        for ( int i = 1; i < len; /* */ )
        {
            // Skip spaces
            while ( ( i < len ) && ( strValue.charAt( i ) == ' ' ) )
            {
                i++;
            }
            
            int pos = i;
            
            // Search for a keyword
            while ( ( i < len ) && StringTools.isAlphaASCII( strValue, pos ) )
            {
                pos++;
            }
            
            if ( pos == i )
            {
                // No keyword : error
                LOG.debug( "Syntax invalid for '{}'", value );
                return false;
            }
            
            String keyword = strValue.substring( i, pos );
            i = pos;
            
            if ( !DSE_BITS.contains( keyword ) )
            {
                // Unknown keyword
                LOG.debug( "Syntax invalid for '{}'", value );
                return false;
            }
            
            // Check that the keyword has not been met
            if ( keywords.contains( keyword ) )
            {
                LOG.debug( "Syntax invalid for '{}'", value );
                return false;
            }
            
            keywords.add( keyword );
            needKeyword = false;
            
            // Skip spaces
            while ( ( i < len ) && ( strValue.charAt( i ) == ' ' ) )
            {
                i++;
            }
            
            // Do we have another keyword ?
            if ( ( i < len) && ( strValue.charAt( i ) == '$' ) )
            {
                // yes
                i++;
                needKeyword = true;
                continue;
            }
        }
        
        // We are done
        if ( needKeyword )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
        }
        else
        {
            LOG.debug( "Syntax valid for '{}'", value );
        }
        
        return !needKeyword;
    }
}

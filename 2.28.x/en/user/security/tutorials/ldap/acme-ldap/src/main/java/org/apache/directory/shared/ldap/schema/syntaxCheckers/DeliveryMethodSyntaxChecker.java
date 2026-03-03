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
 * A SyntaxChecker which verifies that a value is a delivery method 
 * according to RFC 4517.
 * 
 * From RFC 4517 & RFC 4512:
 * 
 * DeliveryMethod = pdm *( WSP DOLLAR WSP pdm )
 *
 * pdm = "any" | "mhs" | "physical" | "telex" | "teletex" |
 *       "g3fax" | "g4fax" | "ia5" | "videotex" | "telephone"
 *           
 * WSP     = 0*SPACE  ; zero or more " "
 * DOLLAR  = %x24 ; dollar sign ("$")
 * SPACE   = %x20 ; space (" ")
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DeliveryMethodSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DeliveryMethodSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    private static final String[] PDMS = 
        {
            "any", "mhs", "physical", "telex", "teletex",
            "g3fax", "g4fax", "ia5", "videotex", "telephone"
        };

    /** The Set which contains the delivery methods */
    private static final Set<String> DELIVERY_METHODS = new HashSet<String>();
    
    /** Initialization of the delivery methods set */
    static
    {
        for ( String country:PDMS )
        {
            DELIVERY_METHODS.add( country );
        }
    }
    
    /**
     * 
     * Creates a new instance of DeliveryMethodSyntaxChecker.
     *
     */
    public DeliveryMethodSyntaxChecker()
    {
        super( SchemaConstants.DELIVERY_METHOD_SYNTAX );
    }
    
    
    /**
     * 
     * Check if the string contains a delivery method which has 
     * not already been found.
     * 
     * @param strValue The string we want to look into for a PDM 
     * @param pos The current position in the string
     * @param pdms The set containing all the PDM
     * @return if a Prefered Delivery Method is found in the given string, returns 
     * its position, otherwise, returns -1
     */
    private int isPdm( String strValue, int start, Set<String> pdms )
    {
        int pos = start;
        
        while ( StringTools.isAlphaDigit( strValue, pos ) )
        {
            pos++;
        }
        
        // No ascii string, this is not a delivery method
        if ( pos == start )
        {
            return -1;
        }
        
        String pdm = strValue.substring( start, pos );
        
        if ( ! DELIVERY_METHODS.contains( pdm ) )
        {
            // The delivery method is unknown
            return -1;
        }
        else
        {
            if ( pdms.contains( pdm ) )
            {
                // The delivery method has already been found
                return -1;
            }
            else
            {
                pdms.add( pdm );
                return pos;
            }
        }
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
        
        // We will get the first delivery method
        int length = strValue.length();
        int pos = 0;
        Set<String> pmds = new HashSet<String>();
            
        if ( ( pos = isPdm( strValue, pos, pmds ) ) == -1)
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        // We have found at least the first pmd,
        // now iterate through the other ones. We may have
        // SP* '$' SP* before each pmd.
        while ( pos < length )
        {
            // Skip spaces
            while ( StringTools.isCharASCII( strValue, pos, ' ' ) )
            {
                pos++;
            }
            
            if ( ! StringTools.isCharASCII( strValue, pos, '$' ) )
            {
                // A '$' was expected
                LOG.debug( "Syntax invalid for '{}'", value );
                return false;
            }
            else
            {
                pos++;
            }
            
            // Skip spaces
            while ( StringTools.isCharASCII( strValue, pos, ' ' ) )
            {
                pos++;
            }

            if ( ( pos = isPdm( strValue, pos, pmds ) ) == -1 )
            {
                LOG.debug( "Syntax invalid for '{}'", value );
                return false;
            }
        }

        LOG.debug( "Syntax valid for '{}'", value );
        return true;
    }
}

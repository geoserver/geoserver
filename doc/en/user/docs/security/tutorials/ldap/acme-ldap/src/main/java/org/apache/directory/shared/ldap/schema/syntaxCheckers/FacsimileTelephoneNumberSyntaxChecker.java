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
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker which verifies that a value is a facsimile TelephoneNumber according 
 * to ITU recommendation E.123 for the Telephone number part, and from RFC 4517, par. 
 * 3.3.11 :
 * 
 * fax-number       = telephone-number *( DOLLAR fax-parameter )
 * telephone-number = PrintableString
 * fax-parameter    = "twoDimensional" |
 *                    "fineResolution" |
 *                    "unlimitedLength" |
 *                    "b4Length" |
 *                    "a3Width" |
 *                    "b4Width" |
 *                    "uncompressed"
 *
 * 
 * If needed, and to allow more syntaxes, a list of regexps has been added
 * which can be initialized to other values
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class FacsimileTelephoneNumberSyntaxChecker extends TelephoneNumberSyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( FacsimileTelephoneNumberSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** Fax parameters possible values */
    private static final String TWO_DIMENSIONAL  = "twoDimensional";
    private static final String FINE_RESOLUTION  = "fineResolution";
    private static final String UNLIMITED_LENGTH = "unlimitedLength";
    private static final String B4_LENGTH        = "b4Length";
    private static final String A3_LENGTH        = "a3Width";
    private static final String B4_WIDTH         = "b4Width";
    private static final String UNCOMPRESSED     = "uncompressed";
    
    /** A set which contaons all the possible fax parameters values */
    private static Set<String> faxParameters = new HashSet<String>();
    
    /** Initialization of the fax parameters set of values */
    static
    {
        faxParameters.add( TWO_DIMENSIONAL.toLowerCase() );
        faxParameters.add( FINE_RESOLUTION.toLowerCase() );
        faxParameters.add( UNLIMITED_LENGTH.toLowerCase() );
        faxParameters.add( B4_LENGTH.toLowerCase() );
        faxParameters.add( A3_LENGTH.toLowerCase() );
        faxParameters.add( B4_WIDTH.toLowerCase() );
        faxParameters.add( UNCOMPRESSED.toLowerCase() );
    }
    
    /**
     * Creates a new instance of TelephoneNumberSyntaxChecker.
     */
    public FacsimileTelephoneNumberSyntaxChecker()
    {
        super();
        setOid( SchemaConstants.FACSIMILE_TELEPHONE_NUMBER_SYNTAX );
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
        
        // The facsimile telephone number might be composed
        // of two parts separated by a '$'.
        int dollarPos = strValue.indexOf( '$' );
        
        if ( dollarPos == -1 )
        {
            // We have no fax-parameter : check the Telephone number
            boolean result = super.isValidSyntax( strValue );
            
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
        
        // First check the telephone number if the '$' is not at the first position
        if ( dollarPos > 0 )
        {
            if ( !super.isValidSyntax( strValue.substring( 0, dollarPos -1 ) ) )
            {
                LOG.debug( "Syntax invalid for '{}'", value );
                return false;
            }
            
            // Now, try to validate the fax-parameters : we may
            // have more than one, so we will store the seen params
            // in a set to check that we don't have the same param twice
            Set<String> paramsSeen = new HashSet<String>(); 
           
            while ( dollarPos > 0 )
            {
                String faxParam = null;
                int newDollar = strValue.indexOf( '$', dollarPos + 1 );

                if ( newDollar == -1 )
                {
                    faxParam = strValue.substring(  dollarPos+1 );
                }
                else
                {
                    faxParam = strValue.substring(  dollarPos+1, newDollar );
                }
                
                if ( faxParam == null )
                {
                    // Not allowed
                    LOG.debug( "Syntax invalid for '{}'", value );
                    return false;
                }
                
                // Relax a little bit the syntax by lowercasing the param
                faxParam = faxParam.toLowerCase();
                
                if ( !faxParameters.contains( faxParam ) )
                {
                    // This parameter is not in the possible set
                    LOG.debug( "Syntax invalid for '{}'", value );
                    return false;
                }
                else if ( paramsSeen.contains( faxParam ) )
                {
                    // We have the same parameters twice...
                    LOG.debug( "Syntax invalid for '{}'", value );
                    return false;
                } 
                else
                {
                    // It's a correct param, let's add it to the seen 
                    // params.
                    paramsSeen.add( faxParam );
                }
                
                dollarPos = newDollar;
            }
            
            LOG.debug( "Syntax valid for '{}'", value );
            return true;
        }
        
        // We must have a valid telephone number !
        LOG.debug( "Syntax invalid for '{}'", value );
        return false;
    }
}

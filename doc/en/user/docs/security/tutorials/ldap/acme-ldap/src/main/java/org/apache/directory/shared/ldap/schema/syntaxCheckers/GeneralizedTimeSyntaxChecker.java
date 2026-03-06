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


import java.util.regex.Pattern;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker which verifies that a value is a generalized time
 * according to RFC 4517.
 * 
 * From RFC 4517 :
 * GeneralizedTime = century year month day hour
 *                          [ minute [ second / leap-second ] ]
 *                          [ fraction ]
 *                          g-time-zone
 *
 * century = 2(%x30-39)            ; "00" to "99"
 * year    = 2(%x30-39)            ; "00" to "99"
 * month   = ( %x30 %x31-39 )      ; "01" (January) to "09"
 *           | ( %x31 %x30-32 )    ; "10" to "12"
 * day     = ( %x30 %x31-39 )      ; "01" to "09"
 *           | ( %x31-32 %x30-39 ) ; "10" to "29"
 *           | ( %x33 %x30-31 )    ; "30" to "31"
 * hour    = ( %x30-31 %x30-39 ) 
 *           | ( %x32 %x30-33 )    ; "00" to "23"
 * minute  = %x30-35 %x30-39       ; "00" to "59"
 *
 * second  = ( %x30-35 %x30-39 )   ; "00" to "59"
 * leap-second = ( %x36 %x30 )     ; "60"
 *
 * fraction = ( DOT / COMMA ) 1*(%x30-39)
 * g-time-zone = %x5A              ; "Z"
 *               | g-differential
 * g-differential = ( MINUS / PLUS ) hour [ minute ]
 * MINUS   = %x2D  ; minus sign ("-")
 * 
 * From RFC 4512 :
 * PLUS    = %x2B ; plus sign ("+")
 * DOT     = %x2E ; period (".")
 * COMMA   = %x2C ; comma (",")
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GeneralizedTimeSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( GeneralizedTimeSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The GeneralizedDate pattern matching */
    private static final String GENERALIZED_TIME_PATTERN = 
                "^\\d{4}" +                                 // century + year : 0000 to 9999
                "(0[1-9]|1[0-2])" +                         // month : 01 to 12
                "(0[1-9]|[12]\\d|3[01])" +                  // day : 01 to 31
                "([01]\\d|2[0-3])" +                        // hour : 00 to 23
                "(" +
                    "([0-5]\\d)" +                          // optionnal minute : 00 to 59
                    "([0-5]\\d|60)?" +                      // optionnal second | leap second
                ")?" +
                "([.,]\\d+)?" +                             // fraction       
                "(Z|[+-]([01]\\d|2[0-3])([0-5]\\d)?)$";     // time-zone
    
    // The regexp pattern matcher
    private Pattern datePattern = Pattern.compile( GENERALIZED_TIME_PATTERN ); 

    /**
     * Creates a new instance of GeneralizedTimeSyntaxChecker.
     */
    public GeneralizedTimeSyntaxChecker()
    {
        super( SchemaConstants.GENERALIZED_TIME_SYNTAX );
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

        // A generalized time must have a minimal length of 11 
        if ( strValue.length() < 11 )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        // Start the date parsing
        boolean result = datePattern.matcher( strValue ).find();
        
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

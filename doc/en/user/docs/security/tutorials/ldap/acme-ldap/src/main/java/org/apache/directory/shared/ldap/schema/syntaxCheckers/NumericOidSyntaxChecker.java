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



import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker which verifies that a value is a numeric oid 
 * according to RFC 4512.
 * 
 * From RFC 4512 :
 * 
 * numericoid = number 1*( DOT number )
 * number  = DIGIT | ( LDIGIT 1*DIGIT )
 * DIGIT   = %x30 | LDIGIT                  ; "0"-"9"
 * LDIGIT  = %x31-39                        ; "1"-"9"
 * DOT     = %x2E                           ; period (".")

 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NumericOidSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( NumericOidSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of NumericOidSyntaxChecker.
     */
    public NumericOidSyntaxChecker()
    {
        super( SchemaConstants.NUMERIC_OID_SYNTAX );
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

        // Just check that the value is a valid OID
        boolean result = ( OID.isOID( strValue ) );
        
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

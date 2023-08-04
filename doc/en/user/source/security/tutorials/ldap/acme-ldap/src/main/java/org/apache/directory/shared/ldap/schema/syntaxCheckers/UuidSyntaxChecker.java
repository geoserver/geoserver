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
 * An UUID syntax checker.
 * 
 * UUID ::= OCTET STRING (SIZE(16)) -- constrained to an UUID [RFC4122]
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 736240 $
 */
public class UuidSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( UuidSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    // Tells if the byte is alphanumeric
    private static boolean isHex( byte b )
    {
        return ( b > 0 ) && StringTools.ALPHA_DIGIT[b];
    }

    /**
     * Creates a new instance of UUIDSyntaxChecker.
     */
    public UuidSyntaxChecker()
    {
        super( SchemaConstants.UUID_SYNTAX );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isValidSyntax( Object value )
    {
        if ( value == null )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
 
        if ( ! ( value instanceof String ) )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }

        byte[] b = ((String)value).getBytes();
        
        if ( b.length < 36)
        {
            return false;
        }
        
        if ( 
            isHex( b[0] ) && isHex( b[1] ) && isHex( b[2] ) && isHex( b[3] ) &
            isHex( b[4] ) && isHex( b[5] ) && isHex( b[6] ) && isHex( b[7] ) &
            b[8] == '-' &
            isHex( b[9] ) && isHex( b[10] ) && isHex( b[11] ) && isHex( b[12] ) &
            b[13] == '-' &
            isHex( b[14] ) && isHex( b[15] ) && isHex( b[16] ) && isHex( b[17] ) &
            b[18] == '-' &
            isHex( b[19] ) && isHex( b[20] ) && isHex( b[21] ) && isHex( b[22] ) &
            b[23] == '-' &
            isHex( b[24] ) && isHex( b[25] ) && isHex( b[26] ) && isHex( b[27] ) &
            isHex( b[28] ) && isHex( b[29] ) && isHex( b[30] ) && isHex( b[31] ) &
            isHex( b[32] ) && isHex( b[33] ) && isHex( b[34] ) && isHex( b[35] ) )
        {
            // There is not that much more we can check.
            LOG.debug( "Syntax valid for '{}'", value );
            return true;
        }

        LOG.debug( "Syntax invalid for '{}'", value );
        return false;
    }
}

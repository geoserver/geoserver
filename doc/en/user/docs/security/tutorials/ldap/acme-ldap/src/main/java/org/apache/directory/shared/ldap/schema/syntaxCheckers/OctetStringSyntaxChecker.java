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


/**
 * A SyntaxChecker which verifies that a value is a Octet String according to RFC 4517.
 * 
 * From RFC 4517 :
 * OctetString = *OCTET
 * 
 * From RFC 4512 :
 * OCTET   = %x00-FF ; Any octet (8-bit data unit)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public class OctetStringSyntaxChecker extends SyntaxChecker
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of OctetStringSyntaxChecker.
     */
    public OctetStringSyntaxChecker()
    {
        super( SchemaConstants.OCTET_STRING_SYNTAX );
    }
    
    
    /**
     * Creates a new instance of OctetStringSyntaxChecker, with a specific OID
     * 
     * @param oid The Syntax's OID 
     */
    public OctetStringSyntaxChecker( String oid )
    {
        super( oid );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean isValidSyntax( Object value )
    {
        return ( value != null );
    }
}

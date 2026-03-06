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
package org.apache.directory.shared.ldap.schema;


import org.apache.directory.shared.ldap.exception.LdapException;

import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * Used to validate values of a particular syntax. This interface does not
 * correlate to any LDAP or X.500 construct. It has been created as a means to
 * enforce a syntax within the Eve server.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923524 $, $Date: 2010-03-16 02:31:36 +0200 (Tue, 16 Mar 2010) $
 */
public abstract class SyntaxChecker extends LoadableSchemaObject
{
    /** The serialversionUID */
    private static final long serialVersionUID = 1L;

    /**
     * The SyntaxChecker base constructor
     * @param oid The associated OID
     */
    protected SyntaxChecker( String oid )
    {
        super( SchemaObjectType.SYNTAX_CHECKER, oid );
    }


    /**
     * The SyntaxChecker default constructor where the oid is set after 
     * instantiation.
     */
    protected SyntaxChecker()
    {
        super( SchemaObjectType.SYNTAX_CHECKER );
    }


    /**
     * Determines if the attribute's value conforms to the attribute syntax.
     * 
     * @param value the value of some attribute with the syntax
     * @return true if the value is in the valid syntax, false otherwise
     */
    public abstract boolean isValidSyntax( Object value );


    /**
     * Asserts whether or not the attribute's value conforms to the attribute
     * syntax.
     * 
     * @param value the value of some attribute with the syntax
     * @throws LdapException if the value does not conform to the attribute syntax.
     */
    public void assertSyntax( Object value ) throws LdapException
    {
        if ( !isValidSyntax( value ) )
        {
            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX );
        }
    }


    /**
     * @see Object#equals()
     */
    public boolean equals( Object o )
    {
        if ( !super.equals( o ) )
        {
            return false;
        }

        return o instanceof SyntaxChecker;
    }

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return objectType + " " + DescriptionUtils.getDescription( this );
    }
}

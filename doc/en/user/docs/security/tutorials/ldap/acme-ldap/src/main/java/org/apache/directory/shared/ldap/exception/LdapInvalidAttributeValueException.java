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
package org.apache.directory.shared.ldap.exception;


import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * Makes a {@link LdapOperationException} unambiguous with respect to the result
 * code it corresponds to by associating an LDAP specific result code with it.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923834 $
 */
public class LdapInvalidAttributeValueException extends LdapOperationException
{
    /** The serial version UUID */
    static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of LdapInvalidAttributeValueException.
     *
     * @param resultCode the ResultCodeEnum for this exception
     * @param message The exception message
     */
    public LdapInvalidAttributeValueException( ResultCodeEnum resultCode, String message )
    {
        super( message );
        checkResultCode( resultCode );
        this.resultCode = resultCode;
    }


    /**
     * Creates a new instance of LdapInvalidAttributeValueException.
     * 
     * @param resultCode the ResultCodeEnum for this exception
     */
    public LdapInvalidAttributeValueException( ResultCodeEnum resultCode )
    {
        super( null );
        checkResultCode( resultCode );
        this.resultCode = resultCode;
    }


    /**
     * Checks to make sure the resultCode value is right for this exception
     * type.
     * 
     * @throws IllegalArgumentException
     *             if the result code is not one of
     *             {@link ResultCodeEnum#CONSTRAINT_VIOLATION},
     *             {@link ResultCodeEnum#INVALID_ATTRIBUTE_SYNTAX}.
     */
    private void checkResultCode( ResultCodeEnum resultCode )
    {
        switch ( resultCode )
        {
            case CONSTRAINT_VIOLATION :
            case INVALID_ATTRIBUTE_SYNTAX :
                return;
                
            default:
                throw new IllegalArgumentException( I18n.err( I18n.ERR_04140, resultCode ) );
        }
    }
}

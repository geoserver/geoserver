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


import org.apache.directory.shared.ldap.message.ResultCodeEnum;


/**
 * A ContextNotEmptyException which contains an LDAP result code.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923395 $
 */
public class LdapContextNotEmptyException extends LdapOperationException
{
    /** The serial version UUID */
    static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of LdapContextNotEmptyException.
     *
     * @param message The exception message
     */
    public LdapContextNotEmptyException( String message )
    {
        super( ResultCodeEnum.NOT_ALLOWED_ON_NON_LEAF, message );
    }


    /**
     * Creates a new instance of LdapContextNotEmptyException.
     */
    public LdapContextNotEmptyException()
    {
        super( ResultCodeEnum.NOT_ALLOWED_ON_NON_LEAF, null );
    }
}
